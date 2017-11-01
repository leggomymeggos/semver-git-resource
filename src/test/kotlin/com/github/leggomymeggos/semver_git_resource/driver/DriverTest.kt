package com.github.leggomymeggos.semver_git_resource.driver

import com.github.leggomymeggos.semver_git_resource.client.GitService
import com.github.leggomymeggos.semver_git_resource.models.*
import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.io.File
import com.github.zafarkhaja.semver.Version as SemVer
import com.github.zafarkhaja.semver.Version.valueOf as asVersion

class DriverTest {
    private val gitService = mock<GitService>()
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
            initialVersion = asVersion("7.80.34"),
            gitService = gitService
    )

    @Before
    fun `set up`() {
        whenever(gitService.cloneOrFetch(any(), any()))
                .thenReturn(Response.Success("successfully ran the thing"))
        whenever(gitService.resetRepoDir(any()))
                .thenReturn(Response.Success("successfully ran the thing"))

        val nonExistentFile = mock<File>()
        whenever(nonExistentFile.exists()).thenReturn(false)

        whenever(gitService.getFile(any()))
                .thenReturn(nonExistentFile)
                .thenReturn(File.createTempFile("tempVersion", ""))

        whenever(gitService.add(any())).thenReturn(Response.Success("added!"))
        whenever(gitService.commit(any())).thenReturn(Response.Success("committed!"))
        whenever(gitService.push(any())).thenReturn(Response.Success("pushed!"))
    }

    @Test
    fun `check clears netrc file if it exists`() {
        val netRc = File(System.getenv("HOME"), ".netrc")
        netRc.createNewFile()
        netRc.writeText("hey yeah here i am")

        driver.check(asVersion("0.0.0"))

        assertThat(netRc.readText()).doesNotContain("hey yeah here i am")
    }

    @Test
    fun `check creates netrc file with git username and password`() {
        val netRc = File(System.getenv("HOME"), ".netrc")

        driver.check(asVersion("0.0.0"))

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
                initialVersion = asVersion("7.80.34"),
                gitService = gitService
        )

        driver.check(asVersion("0.0.0"))

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
                initialVersion = asVersion("7.80.34"),
                gitService = gitService
        )
        val response = driver.check(asVersion("0.0.0")).getError()

        assertThat(response.message).isEqualTo("private keys with passphrases are not supported")
    }

    @Test
    fun `check saves private key`() {
        driver.check(asVersion("0.0.0"))

        assertThat(driver.privateKeyPath.readText()).isEqualTo(driver.privateKey)
    }

    @Test
    fun `check exports private key`() {
        driver.check(asVersion("0.0.0"))

        verify(gitService).setEnv("GIT_SSH_COMMAND", "ssh -o StrictHostKeyChecking=no -i ${driver.privateKeyPath}")
    }

    @Test
    fun `check clones the git repo`() {
        driver.check(asVersion("0.0.0"))

        verify(gitService).cloneOrFetch(driver.gitUri, driver.versionBranch)
    }

    @Test
    fun `check returns an error when something goes wrong cloning the repo`() {
        val exception = Exception("fuuuuq")
        whenever(gitService.cloneOrFetch(any(), any()))
                .thenReturn(Response.Error(VersionError("something done fucked up yo", exception)))

        val response = driver.check(asVersion("0.0.0")).getError()

        assertThat(response.message).isEqualTo("something done fucked up yo")
        assertThat(response.exception).isEqualTo(exception)
    }

    @Test
    fun `check resets repo to head of version branch`() {
        whenever(gitService.cloneOrFetch(any(), any()))
                .thenReturn(Response.Success("successfully ran the thing"))

        driver.check(asVersion("0.0.0"))

        verify(gitService).resetRepoDir(driver.versionBranch)
    }

    @Test
    fun `check returns an error when there is a problem resetting the repo`() {
        whenever(gitService.resetRepoDir(any()))
                .thenReturn(Response.Error(VersionError("did not do it!!!!")))

        val response = driver.check(asVersion("0.0.0")).getError()

        assertThat(response).isEqualTo(VersionError("did not do it!!!!"))
    }

    @Test
    fun `check asks for version file`() {
        driver.check(asVersion("0.0.0"))

        verify(gitService).getFile(driver.versionFile)
    }

    @Test
    fun `check reads the version from the given file`() {
        val versionFile = File.createTempFile("version", "")
        versionFile.createNewFile()
        versionFile.writeText("1.2.3")
        whenever(gitService.getFile(any())).thenReturn(versionFile)

        val response = driver.check(asVersion("0.0.0")).getSuccess()

        assertThat(response).containsExactly(asVersion("1.2.3"))
    }

    @Test
    fun `check trims the version from the file`() {
        val versionFile = File.createTempFile("version", "")
        versionFile.createNewFile()
        versionFile.writeText("\n\n 1.2.3   \n ")
        whenever(gitService.getFile(any())).thenReturn(versionFile)

        val response = driver.check(asVersion("0.0.0")).getSuccess()

        assertThat(response).containsExactly(asVersion("1.2.3"))
    }

    @Test
    fun `check returns an error if the saved version is not valid`() {
        val versionFile = File.createTempFile("version", "")
        versionFile.createNewFile()
        versionFile.writeText("not valid yo")
        whenever(gitService.getFile(any())).thenReturn(versionFile)

        val response = driver.check(asVersion("0.0.0")).getError()

        assertThat(response.message).isEqualTo("Invalid version: not valid yo")
        assertThat(response.exception).isNotNull()
    }

    @Test
    fun `check returns the initial version if the version file does not exist`() {
        val response = driver.check(asVersion("0.0.0")).getSuccess()

        assertThat(response).containsExactly(asVersion("7.80.34"))
    }

    @Test
    fun `check returns empty list when the passed-in version is greater than the driver initial version`() {
        val response = driver.check(asVersion("8.0.0")).getSuccess()

        assertThat(response).isEmpty()
    }

    @Test
    fun `check returns empty list when the passed-in version is greater than the saved version in the file`() {
        val versionFile = File.createTempFile("version", "")
        versionFile.createNewFile()
        versionFile.writeText("1.2.3")
        whenever(gitService.getFile(any())).thenReturn(versionFile)

        val response = driver.check(asVersion("4.0.0")).getSuccess()

        assertThat(response).isEmpty()
    }

    @Test
    fun `bump clears netrc file if it exists`() {
        val netRc = File(System.getenv("HOME"), ".netrc")
        netRc.createNewFile()
        netRc.writeText("hey yeah here i am")

        driver.bump(PatchBump())

        assertThat(netRc.readText()).doesNotContain("hey yeah here i am")
    }

    @Test
    fun `bump creates netrc file with git username and password`() {
        val netRc = File(System.getenv("HOME"), ".netrc")

        driver.bump(MajorBump())

        assertThat(netRc.exists()).isTrue()
        assertThat(netRc.readText()).isEqualTo("default login user123 password password456")
    }

    @Test
    fun `bump does not save new netrc file if username and password are empty`() {
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
                initialVersion = asVersion("7.80.34"),
                gitService = gitService
        )

        driver.bump(MinorBump())

        assertThat(netRc.readText()).isEmpty()
    }

    @Test
    fun `bump does not support encrypted keys`() {
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
                initialVersion = asVersion("7.80.34"),
                gitService = gitService
        )
        val response = driver.bump(FinalBump()).getError()

        assertThat(response.message).isEqualTo("private keys with passphrases are not supported")
    }

    @Test
    fun `bump saves private key`() {
        driver.bump(FinalBump())

        assertThat(driver.privateKeyPath.readText()).isEqualTo(driver.privateKey)
    }

    @Test
    fun `bump exports private key`() {
        driver.bump(MajorBump())

        verify(gitService).setEnv("GIT_SSH_COMMAND", "ssh -o StrictHostKeyChecking=no -i ${driver.privateKeyPath}")
    }

    @Test
    fun `bump clones the git repo`() {
        driver.bump(MinorBump())

        verify(gitService).cloneOrFetch(driver.gitUri, driver.versionBranch)
    }

    @Test
    fun `bump returns an error when something goes wrong cloning the repo`() {
        val exception = Exception("fuuuuq")
        whenever(gitService.cloneOrFetch(any(), any()))
                .thenReturn(Response.Error(VersionError("something done fucked up yo", exception)))

        val response = driver.bump(FinalBump()).getError()

        assertThat(response.message).isEqualTo("something done fucked up yo")
        assertThat(response.exception).isEqualTo(exception)
    }

    @Test
    fun `bump resets repo to head of version branch`() {
        driver.bump(MajorBump())

        verify(gitService).resetRepoDir(driver.versionBranch)
    }

    @Test
    fun `bump returns an error when there is a problem resetting the repo`() {
        whenever(gitService.resetRepoDir(any()))
                .thenReturn(Response.Error(VersionError("did not do it!!!!")))

        val response = driver.bump(FinalBump()).getError()

        assertThat(response).isEqualTo(VersionError("did not do it!!!!"))
    }

    @Test
    fun `bump asks for version file`() {
        driver.bump(FinalBump())

        verify(gitService).getFile(driver.versionFile)
    }

    @Test
    fun `bump applies bump to the version from the given file`() {
        val bump = object : Bump {
            override fun apply(version: SemVer): SemVer = asVersion("100.200.300")
        }

        val versionFile = File.createTempFile("version", "")
        versionFile.createNewFile()
        versionFile.writeText("1.2.3")
        whenever(gitService.getFile(any())).thenReturn(versionFile)

        val response = driver.bump(bump).getSuccess()

        assertThat(response).isEqualTo(asVersion("100.200.300"))
    }

    @Test
    fun `bump trims the version from the file`() {
        val versionFile = File.createTempFile("version", "")
        versionFile.createNewFile()
        versionFile.writeText("\n\n 1.2.3   \n ")
        whenever(gitService.getFile(any())).thenReturn(versionFile)

        val bump = mock<Bump>()
        whenever(bump.apply(any())).thenReturn(asVersion("0.0.0"))
        driver.bump(bump)

        verify(bump).apply(asVersion("1.2.3"))
    }

    @Test
    fun `bump writes new version`() {
        val bump = object : Bump {
            override fun apply(version: SemVer): SemVer = asVersion("100.200.300")
        }

        val versionFile = File.createTempFile("version", "")
        versionFile.createNewFile()
        versionFile.writeText("1.2.3")
        whenever(gitService.getFile(any())).thenReturn(versionFile)

        driver.bump(bump)

        assertThat(versionFile.readText()).isEqualTo("100.200.300")
    }

    @Test
    fun `bump adds new version`() {
        val bump = object : Bump {
            override fun apply(version: SemVer): SemVer = asVersion("100.200.300")
        }

        driver.bump(bump)

        verify(gitService).add(driver.versionFile)
    }

    @Test
    fun `bump commits the new version when the add is successful`() {
        whenever(gitService.add(any())).thenReturn(Response.Success("added!"))
        val bump = object : Bump {
            override fun apply(version: SemVer): SemVer = asVersion("100.200.300")
        }

        driver.bump(bump)

        verify(gitService).commit("bumped version to 100.200.300")
    }

    @Test
    fun `bump returns an error if there is an error adding`() {
        val bump = object : Bump {
            override fun apply(version: SemVer): SemVer = asVersion("100.200.300")
        }
        whenever(gitService.add(any()))
                .thenReturn(Response.Error(VersionError("problem time", Exception())))

        val response = driver.bump(bump).getError()

        assertThat(response.message).isEqualTo("error with git: problem time")
        assertThat(response.exception).isNotNull()
    }

    @Test
    fun `bump pushes the new version when the commit is successful`() {
        whenever(gitService.commit(any())).thenReturn(Response.Success("committed!"))

        driver.bump(MajorBump())

        verify(gitService).push(driver.versionBranch)
    }
    
    @Test 
    fun `bump returns success with the new version when the push is successful`() {
        whenever(gitService.push(any())).thenReturn(Response.Success("pushed!"))
        val bump = object : Bump {
            override fun apply(version: SemVer): SemVer = asVersion("100.200.300")
        }
        val response = driver.bump(bump).getSuccess()
        
        assertThat(response).isEqualTo(asVersion("100.200.300"))
    }

    @Test
    fun `bump does not push version if it is the same as the current version`() {
        val bump = object : Bump {
            override fun apply(version: SemVer): SemVer = asVersion("1.2.3")
        }
        val versionFile = File.createTempFile("version", "")
        versionFile.createNewFile()
        versionFile.writeText("1.2.3")
        whenever(gitService.getFile(any())).thenReturn(versionFile)

        driver.bump(bump)

        verify(gitService, never()).add(any())
        verify(gitService, never()).commit(any())
        verify(gitService, never()).push(any())
    }

    @Test
    fun `bump returns an error if the saved version is not valid`() {
        val versionFile = File.createTempFile("version", "")
        versionFile.createNewFile()
        versionFile.writeText("not valid yo")
        whenever(gitService.getFile(any())).thenReturn(versionFile)

        val response = driver.bump(PreReleaseBump("")).getError()

        assertThat(response.message).isEqualTo("Invalid version: not valid yo")
        assertThat(response.exception).isNotNull()
    }

    @Test
    fun `bump returns the initial version if the version file does not exist`() {
        val nonExistentFile = mock<File>()
        whenever(nonExistentFile.exists()).thenReturn(false)
        whenever(gitService.getFile(any())).thenReturn(nonExistentFile)

        val response = driver.bump(FinalBump()).getSuccess()

        assertThat(response).isEqualTo(asVersion("7.80.34"))
    }
}