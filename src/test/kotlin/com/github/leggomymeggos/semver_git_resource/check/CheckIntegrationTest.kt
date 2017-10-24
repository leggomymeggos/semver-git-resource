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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class CheckIntegrationTest {
    private val mapper = ObjectMapper()
    private val originalOut = System.out!!
    private val outputStream = ByteArrayOutputStream()
    private var gitUrl: String = ""

    companion object {
        private val LOGS_DIR = "./src/test/logs"

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

    private val tempGitRepo = createTempDir()

    @Before
    fun `set up`() {
        val response = post(url = "http://localhost:3000/", headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"))
        val text = response.text
        gitUrl = "${text.substring(text.lastIndexOf("http://localhost"), text.lastIndexOf(".git"))}.git" // substring is exclusive
        ProcessBuilder("/bin/sh", "-c", "cd ${tempGitRepo.path} ; git init ; git remote add origin $gitUrl ; git commit --allow-empty -m \"first commit\" ; git push -u origin master ; git checkout -b $VERSION_BRANCH ; git commit --allow-empty -m \"add version\" ; git push -u origin $VERSION_BRANCH")
                .redirectOutput(createFile("$LOGS_DIR/git", "setup_repo.txt"))
                .redirectError(createFile("$LOGS_DIR/git", "setup_repo_error.txt"))
                .start()
                .waitFor()

        mapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE

        System.setOut(PrintStream(outputStream))
    }

    @After
    fun `tear down`() {
        System.setOut(originalOut)
    }

    @Test
    fun `sets initial version from the source when there is no version provided`() {
        val request = CheckRequest(
                version = Version(
                        number = "", ref = ""
                ),
                source = Source(
                        initialVersion = "1.0.1",
                        versionFile = VERSION_FILE,
                        versionBranch = VERSION_BRANCH,
                        uri = gitUrl,
                        username = "username",
                        password = "password"
                ))

        val jsonRequest = mapper.writeValueAsString(request)

        main(arrayOf(jsonRequest))

        val jsonResult = outputStream.toString()
        val result = mapper.readValue<List<Version>>(jsonResult.substring(jsonResult.indexOf("["), jsonResult.lastIndexOf("\n")))

        assertThat(result[0].number).isEqualTo("1.0.1")
    }
}