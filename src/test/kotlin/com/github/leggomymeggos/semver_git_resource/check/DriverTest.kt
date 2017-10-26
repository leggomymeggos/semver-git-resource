package com.github.leggomymeggos.semver_git_resource.check

import com.github.leggomymeggos.semver_git_resource.models.CheckError
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.io.File
import com.github.zafarkhaja.semver.Version as SemVer

class DriverTest {
    private val gitClient = mock<BashClient>()
    private val driver = Driver(
            gitUri = "http://example.com/",
            privateKey = "private",
            username = "user123",
            password = "password456",
            tagFilter = "",
            skipSslVerification = false,
            sourceCodeBranch = "masterBranch",
            versionBranch = "versionBranch",
            versionFile = "versiony",
            initialVersion = SemVer.valueOf("7.80.34"),
            gitClient = gitClient
    )

    @Before
    fun `set up`() {
        whenever(gitClient.execute(any())).thenReturn(Response.Success(""))
    }

    @Test
    fun `check clears netrc file if it exists`() {
        val netRc = File(System.getenv("HOME"), ".netrc")
        netRc.createNewFile()
        netRc.writeText("hey yeah here i am")

        driver.check(SemVer.valueOf("0.0.0"))

        assertThat(netRc.readText()).doesNotContain("hey yeah here i am")
    }

    @Test
    fun `check creates netrc file with git username and password`() {
        val netRc = File(System.getenv("HOME"), ".netrc")

        driver.check(SemVer.valueOf("0.0.0"))

        assertThat(netRc.exists()).isTrue()
        assertThat(netRc.readText()).isEqualTo("default login user123 password password456")
    }

    @Test
    fun `check does not save new netrc file if username and password are empty`() {
        val netRc = File(System.getenv("HOME"), ".netrc")
        netRc.createNewFile()
        netRc.writeText("hey yeah here i am")

        val driver = Driver(
                gitUri = "http://example.com/",
                privateKey = "private",
                username = "",
                password = "",
                tagFilter = "",
                skipSslVerification = false,
                sourceCodeBranch = "masterBranch",
                versionBranch = "versionBranch",
                versionFile = "versiony",
                initialVersion = SemVer.valueOf("7.80.34"),
                gitClient = gitClient
        )

        driver.check(SemVer.valueOf("0.0.0"))

        assertThat(netRc.readText()).isEmpty()
    }

    @Test
    fun `check does not support encrypted keys`() {
        val driver = Driver(
                gitUri = "http://example.com/",
                privateKey = "privateENCRYPTED",
                username = "user123",
                password = "password456",
                tagFilter = "",
                skipSslVerification = false,
                sourceCodeBranch = "masterBranch",
                versionBranch = "versionBranch",
                versionFile = "versiony",
                initialVersion = SemVer.valueOf("7.80.34"),
                gitClient = gitClient
        )
        val response = driver.check(SemVer.valueOf("0.0.0")).getError()

        assertThat(response.message).isEqualTo("private keys with passphrases are not supported")
    }

    @Test
    fun `check saves private key`() {
        driver.check(SemVer.valueOf("0.0.0"))

        assertThat(driver.privateKeyPath.readText()).isEqualTo(driver.privateKey)
    }

    @Test
    fun `check exports private key`() {
        driver.check(SemVer.valueOf("0.0.0"))

        verify(gitClient).setEnv("GIT_SSH_COMMAND", "ssh -o StrictHostKeyChecking=no -i ${driver.privateKeyPath}")
    }

    @Test
    fun `check clones the git repo`() {
        driver.check(SemVer.valueOf("0.0.0"))

        verify(gitClient).execute("git clone ${driver.gitUri} --branch ${driver.versionBranch} ${driver.gitRepoDir}")
    }

    @Test
    fun `check fetches the git repo when the repoDir is not empty`() {
        val newFile = File("${driver.gitRepoDir}/temp", ".txt")
        newFile.mkdirs()
        newFile.createNewFile()

        driver.check(SemVer.valueOf("0.0.0"))

        verify(gitClient).execute("cd ${driver.gitRepoDir} ; git fetch origin ${driver.versionBranch}")
    }

    @Test
    fun `check returns an error when something goes wrong cloning the repo`() {
        val exception = Exception("fuuuuq")
        whenever(gitClient.execute(any())).thenReturn(Response.Error(CheckError("something done fucked up yo", exception)))

        val response = driver.check(SemVer.valueOf("0.0.0")).getError()

        assertThat(response.message).isEqualTo("something done fucked up yo")
        assertThat(response.exception).isEqualTo(exception)
    }

    @Test
    fun `check returns an error when something goes wrong fetching the repo`() {
        val exception = Exception("fuuuuq")
        whenever(gitClient.execute(any())).thenReturn(Response.Error(CheckError("something else done fucked up yo", exception)))
        val newFile = File("${driver.gitRepoDir}/temp", ".txt")
        newFile.mkdirs()
        newFile.createNewFile()

        val response = driver.check(SemVer.valueOf("0.0.0")).getError()

        assertThat(response.message).isEqualTo("something else done fucked up yo")
        assertThat(response.exception).isEqualTo(exception)
    }

    @Test 
    fun `check resets repo to head of version branch`() {
        whenever(gitClient.execute(any())).thenReturn(Response.Success("successfully ran the thing"))

        driver.check(SemVer.valueOf("0.0.0"))

        verify(gitClient).execute("cd ${driver.gitRepoDir} ; git reset --hard origin/${driver.versionBranch}")
    }

    @Test
    fun `check returns an error when there is a problem resetting the repo`() {
        // we know this should be the order based on the other tests
        whenever(gitClient.execute(any()))
                .thenReturn(Response.Success("successfully ran the thing"))
                .thenReturn(Response.Error(CheckError("did not do it!!!!")))

        val response = driver.check(SemVer.valueOf("0.0.0")).getError()

        assertThat(response).isEqualTo(CheckError("did not do it!!!!"))
    }
    
    @Test 
    fun `check reads the version from the given file`() {
        val versionFile = File("${driver.gitRepoDir}/${driver.versionFile}")
        versionFile.createNewFile()

        versionFile.writeText("1.2.3")

        val response = driver.check(SemVer.valueOf("0.0.0")).getSuccess()

        assertThat(response).containsExactly(SemVer.valueOf("1.2.3"))
    }

    @Test
    fun `check trims the version from the file`() {
        val versionFile = File("${driver.gitRepoDir}/${driver.versionFile}")
        versionFile.createNewFile()

        versionFile.writeText("\n\n 1.2.3   \n ")

        val response = driver.check(SemVer.valueOf("0.0.0")).getSuccess()

        assertThat(response).containsExactly(SemVer.valueOf("1.2.3"))
    }

    @Test
    fun `check returns an error if the saved version is not valid`() {
        val versionFile = File("${driver.gitRepoDir}/${driver.versionFile}")
        versionFile.createNewFile()

        versionFile.writeText("not valid yo")

        val response = driver.check(SemVer.valueOf("0.0.0")).getError()

        assertThat(response.message).isEqualTo("Invalid version: not valid yo")
        assertThat(response.exception).isNotNull()
    }

    @Test
    fun `check returns the initial version if the version file does not exist`() {
        val response = driver.check(SemVer.valueOf("0.0.0")).getSuccess()

        assertThat(response).containsExactly(SemVer.valueOf("7.80.34"))
    }
    
    @Test 
    fun `check returns empty list when the passed-in version is greater than the driver initial version`() {
        val response = driver.check(SemVer.valueOf("8.0.0")).getSuccess()

        assertThat(response).isEmpty()
    }

    @Test
    fun `check returns empty list when the passed-in version is greater than the saved version in the file`() {
        val versionFile = File("${driver.gitRepoDir}/${driver.versionFile}")
        versionFile.createNewFile()

        versionFile.writeText("1.2.3")

        val response = driver.check(SemVer.valueOf("4.0.0")).getSuccess()

        assertThat(response).isEmpty()
    }
}