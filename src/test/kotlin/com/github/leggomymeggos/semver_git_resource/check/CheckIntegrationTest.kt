package com.github.leggomymeggos.semver_git_resource.check

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.leggomymeggos.semver_git_resource.models.CheckRequest
import com.github.leggomymeggos.semver_git_resource.models.Source
import com.github.leggomymeggos.semver_git_resource.models.Version
import khttp.post
import org.assertj.core.api.Assertions.assertThat
import org.junit.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class CheckIntegrationTest {
    companion object {
        private val LOGS_DIR = "./src/test/logs/check"

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

        private fun createFile(filePath: String, fileName: String): File {
            val file = File(filePath)
            file.mkdirs()
            return File(file, fileName)
        }
    }

    private val VERSION_FILE = "number"
    private val VERSION_BRANCH = "version"

    private val mapper = ObjectMapper()
    private val originalOut = System.out!!
    private val outputStream = ByteArrayOutputStream()

    private val originalIn = System.`in`

    private var gitUrl: String = ""
    private val tempGitRepo = createTempDir()

    @Before
    fun `set up`() {
        gitUrl = refreshGitUrl()
        setUpTempGitRepo()

        mapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE

        System.setOut(PrintStream(outputStream))
    }

    @After
    fun `tear down`() {
        System.setIn(originalIn)
        System.setOut(originalOut)
    }

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
    fun `emits empty list when the last known number is higher than the number in the file`() {
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
        assertThat(result).isEmpty()
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

    private fun refreshGitUrl(): String {
        val response = post(url = "http://localhost:3000/", headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"))
        val text = response.text
        return "${text.substring(text.lastIndexOf("http://localhost"), text.lastIndexOf(".git"))}.git" // substring is exclusive
    }

    private fun setUpTempGitRepo() {
        ProcessBuilder(
                "/bin/sh", "-c",
                "cd ${tempGitRepo.path} ; " +
                        "git init ; " +
                        "git remote add origin $gitUrl ; " +
                        "git commit --allow-empty -m \"first commit\" ; " +
                        "git push -u origin master ; " +
                        "git checkout -b $VERSION_BRANCH ; " +
                        "git commit --allow-empty -m \"add version\" ; " +
                        "git push -u origin $VERSION_BRANCH")
                .redirectOutput(createFile("$LOGS_DIR/git", "setup_repo.txt"))
                .redirectError(createFile("$LOGS_DIR/git", "setup_repo_error.txt"))
                .start()
                .waitFor()
    }

    private fun baseSource() =
            Source(
                    versionFile = VERSION_FILE,
                    versionBranch = VERSION_BRANCH,
                    uri = gitUrl,
                    privateKey = "i exist so this doesn't error"
            )

    private fun CheckRequest.writeToStdIn() {
        val jsonRequest = mapper.writeValueAsString(this)

        val inputStream = ByteArrayInputStream(jsonRequest.toByteArray())
        System.setIn(inputStream)
    }

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

    private fun getResult(): List<Version> {
        val jsonResult = outputStream.toString()
        return mapper.readValue(jsonResult.substring(jsonResult.indexOf("["), jsonResult.lastIndexOf("\n")))
    }
}