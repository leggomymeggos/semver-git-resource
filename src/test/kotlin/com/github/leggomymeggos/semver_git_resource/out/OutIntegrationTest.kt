package com.github.leggomymeggos.semver_git_resource.out

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.github.leggomymeggos.semver_git_resource.models.OutRequest
import com.github.leggomymeggos.semver_git_resource.models.Source
import com.github.leggomymeggos.semver_git_resource.models.Version
import com.github.leggomymeggos.semver_git_resource.models.VersionParams
import khttp.post
import org.assertj.core.api.Assertions.assertThat
import org.junit.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class OutIntegrationTest {
    companion object {
        private val LOGS_DIR = "./src/test/logs/out"

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

    private val tempGitRepo = createTempDir()
    private var gitUrl: String = ""
    private var tempDir: File = File("")

    @Before
    fun `set up`() {
        tempDir = createTempDir()
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
    fun `can bump patch version`() {
        addVersionFile("1.2.3")

        OutRequest(
                version = Version(number = "1.2.3", ref = ""),
                params = VersionParams(bump = "patch"),
                source = baseSource()
        ).writeToStdIn()

        main(arrayOf())
        pullDownGitRepo()

        assertThat(File("${tempDir.path}/${gitUrl.repoName()}/$VERSION_FILE").readText()).contains("1.2.4")
    }

    @Test
    fun `can bump minor version`() {
        addVersionFile("1.2.3")

        OutRequest(
                version = Version(number = "1.2.3", ref = ""),
                params = VersionParams(bump = "minor"),
                source = baseSource()
        ).writeToStdIn()

        main(arrayOf())
        pullDownGitRepo()

        assertThat(File("${tempDir.path}/${gitUrl.repoName()}/$VERSION_FILE").readText()).contains("1.3.0")
    }

    @Test
    fun `can bump major version`() {
        addVersionFile("1.2.3")

        OutRequest(
                version = Version(number = "1.2.3", ref = ""),
                params = VersionParams(bump = "major"),
                source = baseSource()
        ).writeToStdIn()

        main(arrayOf())
        pullDownGitRepo()

        assertThat(File("${tempDir.path}/${gitUrl.repoName()}/$VERSION_FILE").readText()).contains("2.0.0")
    }

    @Test
    fun `can add prerelease tags`() {
        addVersionFile("1.2.3")

        OutRequest(
                version = Version(number = "1.2.3", ref = ""),
                params = VersionParams(pre = "rc"),
                source = baseSource()
        ).writeToStdIn()

        main(arrayOf())
        pullDownGitRepo()

        assertThat(File("${tempDir.path}/${gitUrl.repoName()}/$VERSION_FILE").readText()).contains("1.2.3-rc.1")
    }

    @Test
    fun `can add prerelease tags with a version bump`() {
        addVersionFile("1.2.3")
        OutRequest(
                version = Version(number = "1.2.3", ref = ""),
                params = VersionParams(bump = "major", pre = "rc"),
                source = baseSource()
        ).writeToStdIn()

        main(arrayOf())
        pullDownGitRepo()

        assertThat(File("${tempDir.path}/${gitUrl.repoName()}/$VERSION_FILE").readText()).contains("2.0.0-rc.1")
    }

    @Test
    fun `can increment prerelease tags`() {
        addVersionFile("1.2.3-rc.1")
        OutRequest(
                version = Version(number = "1.2.3", ref = ""),
                params = VersionParams(pre = "rc"),
                source = baseSource()
        ).writeToStdIn()

        main(arrayOf())
        pullDownGitRepo()

        assertThat(File("${tempDir.path}/${gitUrl.repoName()}/$VERSION_FILE").readText()).contains("1.2.3-rc.2")
    }

    @Test
    fun `uses a new prerelease tag`() {
        addVersionFile("1.2.3-rc.1")
        OutRequest(
                version = Version(number = "1.2.3", ref = ""),
                params = VersionParams(pre = "pre"),
                source = baseSource()
        ).writeToStdIn()

        main(arrayOf())
        pullDownGitRepo()

        assertThat(File("${tempDir.path}/${gitUrl.repoName()}/$VERSION_FILE").readText()).contains("1.2.3-pre.1")
    }

    @Test
    fun `can bump final version`() {
        addVersionFile("1.2.3-rc.1")
        OutRequest(
                version = Version(number = "1.2.3", ref = ""),
                params = VersionParams(bump = "final"),
                source = baseSource()
        ).writeToStdIn()

        main(arrayOf())
        pullDownGitRepo()

        assertThat(File("${tempDir.path}/${gitUrl.repoName()}/$VERSION_FILE").readText()).contains("1.2.3")
    }

    private fun refreshGitUrl(): String {
        val response = post(url = "http://localhost:3000/", headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"))
        val text = response.text
        return "${text.substring(text.lastIndexOf("http://localhost"), text.lastIndexOf(".git"))}.git" // substring is exclusive
    }

    private fun String.repoName() =
            this.substring(this.lastIndexOf("/") + 1, this.lastIndexOf(".git"))

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

    private fun pullDownGitRepo() {
        ProcessBuilder("/bin/sh", "-c",
                "cd ${tempDir.path} ; " +
                        "git clone $gitUrl ; " +
                        "cd ${gitUrl.repoName()} ; " +
                        "git checkout $VERSION_BRANCH")
                .redirectOutput(createFile("$LOGS_DIR/", "success.txt"))
                .redirectError(createFile("$LOGS_DIR/", "error.txt"))
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

    private fun OutRequest.writeToStdIn() {
        val jsonRequest = mapper.writeValueAsString(this)

        val inputStream = ByteArrayInputStream(jsonRequest.toByteArray())
        System.setIn(inputStream)
    }
}
