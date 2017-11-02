package com.github.leggomymeggos.semver_git_resource.check

import com.github.leggomymeggos.semver_git_resource.models.CheckRequest
import com.github.leggomymeggos.semver_git_resource.models.Source
import com.github.leggomymeggos.semver_git_resource.models.Version
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class CheckVersionIntegrationTest : BaseCheckIntegrationTest() {
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

    override fun logsDir(): String = "$LOGS_DIR/version"

    @Test
    fun `sets initial version from the source when there is no known version and no version file provided`() {
        CheckRequest(
                version = null,
                source = baseSource().copy(initialVersion = "1.0.1")
        ).writeToStdIn()

        main(arrayOf())

        val result = getResult()[0]

        assertThat(result.number).isEqualTo("1.0.1")
    }

    @Test
    fun `uses version from file when there is one`() {
        addVersionFile("0.1.0")
        CheckRequest(
                version = null,
                source = baseSource().copy(initialVersion = "1.0.1")
        ).writeToStdIn()

        main(arrayOf())

        val result = getResult()[0]

        assertThat(result.number).isEqualTo("0.1.0")
    }

    @Test
    fun `emits empty number when the last known number is higher than the number in the file`() {
        addVersionFile("2.0.1")
        CheckRequest(
                version = Version(
                        number = "2.0.2", ref = ""
                ),
                source = baseSource()
        ).writeToStdIn()

        main(arrayOf())

        val result = getResult()

        assertThat(outputStream.toString()).doesNotContain("error checking version")
        assertThat(result.first().number).isEmpty()
    }

    @Test
    fun `emits list of current version when the last known number is equal to than the number in the file`() {
        addVersionFile("2.0.2")
        CheckRequest(
                version = Version(
                        number = "2.0.2", ref = ""
                ),
                source = baseSource()
        ).writeToStdIn()

        main(arrayOf())

        val result = getResult()[0]

        assertThat(result.number).isEqualTo("2.0.2")
    }

    @Test
    fun `emits list of current version when the last known number is less than than the number in the file`() {
        addVersionFile("2.30.2")
        CheckRequest(
                version = Version(
                        number = "2.0.2", ref = ""
                ),
                source = baseSource()
        ).writeToStdIn()

        main(arrayOf())

        val result = getResult()[0]

        assertThat(result.number).isEqualTo("2.30.2")
    }

    @Test
    fun `emits empty list and prints that the file has no number when the file has invalid number`() {
        addVersionFile("not gonna work yo")
        CheckRequest(
                version = Version(
                        number = "2.1.2", ref = ""
                ),
                source = baseSource()
        ).writeToStdIn()

        main(arrayOf())

        val result = getResult()

        assertThat(outputStream.toString()).containsIgnoringCase("error checking version: Invalid version: not gonna work yo")
        assertThat(result).isEmpty()
    }

    @Test
    fun `emits empty list and prints that the initial version is invalid when the initial version is invalid`() {
        CheckRequest(
                version = Version(
                        number = "2.1.2", ref = ""
                ),
                source = Source(
                        initialVersion = "supes invalid",
                        versionFile = VERSION_FILE,
                        versionBranch = VERSION_BRANCH,
                        uri = gitUrl,
                        privateKey = "i exist so this doesn't error"
                )
        ).writeToStdIn()

        main(arrayOf())

        val result = getResult()

        assertThat(outputStream.toString()).containsIgnoringCase("invalid initial version (supes invalid)")
        assertThat(result).isEmpty()
    }

    private fun baseSource() =
            Source(
                    versionFile = VERSION_FILE,
                    versionBranch = VERSION_BRANCH,
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
                .redirectOutput(createFile("$LOGS_DIR/git", "add_version_file.txt"))
                .redirectError(createFile("$LOGS_DIR/git", "add_version_file_error.txt"))
                .start()
                .waitFor()
    }
}