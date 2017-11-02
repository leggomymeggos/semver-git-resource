package com.github.leggomymeggos.semver_git_resource.check

import com.github.leggomymeggos.semver_git_resource.models.CheckRequest
import com.github.leggomymeggos.semver_git_resource.models.Source
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
        assertThat(result).hasSize(1)
        assertThat(result.first().ref).isEqualTo(expectedRef)
    }

    private fun baseSource() =
            Source(
                    sourceCodeBranch = SOURCE_BRANCH,
                    uri = gitUrl,
                    versionFile = VERSION_FILE,
                    versionBranch = VERSION_BRANCH,
                    privateKey = "i exist so this doesn't error"
            )

    private fun getMostRecentRef() : String {
        ProcessBuilder("/bin/sh", "-c",
                "cd ${tempGitRepo.path} ; " +
                        "git checkout $SOURCE_BRANCH; " +
                        "git log --pretty=format:'%H'")
                .redirectOutput(createFile("${logsDir()}/git/log", "success.txt"))
                .redirectError(createFile("${logsDir()}/git/log", "error.txt"))
                .start()
                .waitFor()

        val logs = File("${logsDir()}/git/log/success.txt").readLines().toMutableList()
        logs.addAll(File("${logsDir()}/git/log/error.txt").readLines())
        return logs[1] // skip first line
    }
}