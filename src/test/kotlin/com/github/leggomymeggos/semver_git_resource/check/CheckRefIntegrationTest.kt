package com.github.leggomymeggos.semver_git_resource.check

import com.github.leggomymeggos.semver_git_resource.models.CheckRequest
import com.github.leggomymeggos.semver_git_resource.models.Source
import com.github.leggomymeggos.semver_git_resource.models.Version
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class CheckRefIntegrationTest : BaseCheckIntegrationTest() {
    companion object {
        val LOGS_DIR = "./src/test/logs/check/"

        private val quickGitProcess: Process = ProcessBuilder("quickgit")
                .redirectOutput(createFile("$LOGS_DIR/quickgit/", "success.txt"))
                .redirectError(createFile("$LOGS_DIR/quickgit/", "error.txt"))
                .start()

        @BeforeClass
        @JvmStatic
        fun `global set up`() {
            Thread.sleep(2000) // give the server some time to get up
        }

        @AfterClass
        @JvmStatic
        fun `global tear down`() {
            quickGitProcess.destroy()
        }
    }

    override fun logsDir(): String = "$LOGS_DIR/ref"

    @Test
    fun `sets initial version as the most recent commit when it is run for the first time`() {
        val expectedRef = getMostRecentRef()

        CheckRequest(
                version = null,
                source = baseSource()
        ).writeToStdIn()

        main(arrayOf())

        val result = getResult()
        assertThat(result.map { it.ref }).containsExactly(expectedRef)
    }

    @Test
    fun `returns a list of commits since the given sha`() {
        addCommit(message = "add a commit")
        val secondCommit = addCommit(message = "add another commit")
        val thirdCommit = addCommit(message = "add a third commit")
        val fourthCommit = addCommit(message = "add a fourth commit")

        CheckRequest(
                version = Version(number = "", ref = secondCommit),
                source = baseSource()
        ).writeToStdIn()

        main(arrayOf())

        val result = getResult()
        assertThat(result.map { it.ref }).containsExactly(secondCommit, thirdCommit, fourthCommit)
    }

    @Test
    fun `can handle the first commit`() {
        addCommit(message = "add a commit")

        val firstCommit = firstCommit()
        CheckRequest(
                version = Version(number = "", ref = firstCommit),
                source = baseSource()
        ).writeToStdIn()

        main(arrayOf())

        val result = getResult()

        val allCommits = allCommits(since = firstCommit)
        val map = result.map { it.ref }
        assertThat(map).containsAll(allCommits)
    }

    private fun firstCommit(): String {
        ProcessBuilder("/bin/sh", "-c",
                "cd ${tempGitRepo.path} ; " +
                        "git checkout $SOURCE_BRANCH; " +
                        "git rev-list --max-parents=0 HEAD")
                .redirectOutput(createFile("${logsDir()}/git/log", "success.txt"))
                .redirectError(createFile("${logsDir()}/git/log", "error.txt"))
                .start()
                .waitFor()

        return getFirstListedCommit()
    }

    private fun allCommits(since: String): List<String> {
        ProcessBuilder("/bin/sh", "-c",
                "cd ${tempGitRepo.path} ; " +
                        "git checkout $SOURCE_BRANCH; " +
                        "git log --reverse $since..HEAD --format='%H'")
                .redirectOutput(createFile("${logsDir()}/git/log", "success.txt"))
                .redirectError(createFile("${logsDir()}/git/log", "error.txt"))
                .start()
                .waitFor()

        val logs = File("${logsDir()}/git/log/success.txt").readLines().toMutableList()
        logs.addAll(File("${logsDir()}/git/log/error.txt").readLines())
        return logs.filterNot { it.contains(" ") }
    }

    private fun getMostRecentRef(): String {
        ProcessBuilder("/bin/sh", "-c",
                "cd ${tempGitRepo.path} ; " +
                        "git checkout $SOURCE_BRANCH; " +
                        "git log --format='%H'")
                .redirectOutput(createFile("${logsDir()}/git/log", "success.txt"))
                .redirectError(createFile("${logsDir()}/git/log", "error.txt"))
                .start()
                .waitFor()

       return getFirstListedCommit()
    }

    private fun addCommit(message: String): String {
        ProcessBuilder("/bin/sh", "-c",
                "cd ${tempGitRepo.path} ; " +
                        "git checkout $SOURCE_BRANCH; " +
                        "git commit -m \"$message\" --allow-empty ; " +
                        "git push origin $SOURCE_BRANCH")
                .redirectOutput(createFile("${logsDir()}/git", "add_commit.txt"))
                .redirectError(createFile("${logsDir()}/git", "add_commit_error.txt"))
                .start()
                .waitFor()
        return getMostRecentRef()
    }

    private fun getFirstListedCommit(): String {
        val logs = File("${logsDir()}/git/log/success.txt").readLines().toMutableList()
        logs.addAll(File("${logsDir()}/git/log/error.txt").readLines())
        return logs.first { !it.contains(" ") }
    }

    private fun baseSource() =
            Source(
                    sourceCodeBranch = SOURCE_BRANCH,
                    uri = gitUrl,
                    versionFile = VERSION_FILE,
                    versionBranch = VERSION_BRANCH,
                    privateKey = "i exist so this doesn't error"
            )
}