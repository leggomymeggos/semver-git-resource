package com.github.leggomymeggos.semver_git_resource.check

import com.github.leggomymeggos.semver_git_resource.models.CheckRequest
import com.github.leggomymeggos.semver_git_resource.models.Source
import com.github.leggomymeggos.semver_git_resource.models.Version
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class EverythingTogetherIntegrationTest : BaseCheckIntegrationTest() {
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

    override fun logsDir(): String = "$LOGS_DIR/both"

    @Test
    fun `prints ref and version together`() {
        addVersionFile("1.2.4")

        val firstLine = addCommit("on an island in the sun")
        val secondLine = addCommit("we'll be playing and having fun")
        val thirdLine = addCommit("and it makes me feel so fine")
        val fourthLine = addCommit("i can't control my brain")

        CheckRequest(
                version = Version(number = "1.2.3", ref = firstLine),
                source = baseSource()
        ).writeToStdIn()

        main(arrayOf())

        val result = getResult()
        assertThat(result).containsExactly(
                Version(number = "1.2.4", ref = firstLine),
                Version(number = "1.2.4", ref = secondLine),
                Version(number = "1.2.4", ref = thirdLine),
                Version(number = "1.2.4", ref = fourthLine)
        )
    }

    private fun baseSource() =
            Source(
                    versionFile = VERSION_FILE,
                    versionBranch = VERSION_BRANCH,
                    sourceCodeBranch = SOURCE_BRANCH,
                    uri = gitUrl,
                    privateKey = "i exist so this doesn't error"
            )

    private fun addVersionFile(number: String) {
        val versionFile = File(tempGitRepo, VERSION_FILE)
        versionFile.writeText(number)

        ProcessBuilder("/bin/sh", "-c",
                "cd ${tempGitRepo.path} ; " +
                        "git checkout $VERSION_BRANCH ; " +
                        "git add $VERSION_FILE ; " +
                        "git commit -m \"add version file\" ; " +
                        "git push origin $VERSION_BRANCH")
                .redirectOutput(createFile("${logsDir()}/git", "add_version_file.txt"))
                .redirectError(createFile("${logsDir()}/git", "add_version_file_error.txt"))
                .start()
                .waitFor()
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

        val logs = File("${logsDir()}/git/log/success.txt").readLines().toMutableList()
        logs.addAll(File("${logsDir()}/git/log/error.txt").readLines())
        return logs[1] // skip first line bc it's the output from the checkout
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
}