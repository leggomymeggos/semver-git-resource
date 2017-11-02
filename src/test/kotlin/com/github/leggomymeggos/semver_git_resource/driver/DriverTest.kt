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
    fun `checkVersion clones the git repo`() {
        driver.checkVersion(asVersion("0.0.0"))

        verify(gitService).cloneOrFetch(driver.gitUri, driver.versionBranch)
    }

    @Test
    fun `checkVersion returns an error when something goes wrong cloning the repo`() {
        val exception = Exception("fuuuuq")
        whenever(gitService.cloneOrFetch(any(), any()))
                .thenReturn(Response.Error(VersionError("something done fucked up yo", exception)))

        val response = driver.checkVersion(asVersion("0.0.0")).getError()

        assertThat(response.message).isEqualTo("something done fucked up yo")
        assertThat(response.exception).isEqualTo(exception)
    }

    @Test
    fun `checkVersion resets repo to head of version branch`() {
        whenever(gitService.cloneOrFetch(any(), any()))
                .thenReturn(Response.Success("successfully ran the thing"))

        driver.checkVersion(asVersion("0.0.0"))

        verify(gitService).resetRepoDir(driver.versionBranch)
    }

    @Test
    fun `checkVersion returns an error when there is a problem resetting the repo`() {
        whenever(gitService.resetRepoDir(any()))
                .thenReturn(Response.Error(VersionError("did not do it!!!!")))

        val response = driver.checkVersion(asVersion("0.0.0")).getError()

        assertThat(response).isEqualTo(VersionError("did not do it!!!!"))
    }

    @Test
    fun `checkVersion asks for version file`() {
        driver.checkVersion(asVersion("0.0.0"))

        verify(gitService).getFile(driver.versionFile)
    }

    @Test
    fun `checkVersion reads the version from the given file`() {
        val versionFile = File.createTempFile("version", "")
        versionFile.createNewFile()
        versionFile.writeText("1.2.3")
        whenever(gitService.getFile(any())).thenReturn(versionFile)

        val response = driver.checkVersion(asVersion("0.0.0")).getSuccess()

        assertThat(response).isEqualTo("1.2.3")
    }

    @Test
    fun `checkVersion trims the version from the file`() {
        val versionFile = File.createTempFile("version", "")
        versionFile.createNewFile()
        versionFile.writeText("\n\n 1.2.3   \n ")
        whenever(gitService.getFile(any())).thenReturn(versionFile)

        val response = driver.checkVersion(asVersion("0.0.0")).getSuccess()

        assertThat(response).isEqualTo("1.2.3")
    }

    @Test
    fun `checkVersion returns an error if the saved version is not valid`() {
        val versionFile = File.createTempFile("version", "")
        versionFile.createNewFile()
        versionFile.writeText("not valid yo")
        whenever(gitService.getFile(any())).thenReturn(versionFile)

        val response = driver.checkVersion(asVersion("0.0.0")).getError()

        assertThat(response.message).isEqualTo("Invalid version: not valid yo")
        assertThat(response.exception).isNotNull()
    }

    @Test
    fun `checkVersion returns the initial version if the version file does not exist`() {
        val response = driver.checkVersion(asVersion("0.0.0")).getSuccess()

        assertThat(response).isEqualTo("7.80.34")
    }

    @Test
    fun `checkVersion returns empty list when the passed-in version is greater than the driver initial version`() {
        val response = driver.checkVersion(asVersion("8.0.0")).getSuccess()

        assertThat(response).isEmpty()
    }

    @Test
    fun `checkVersion returns empty list when the passed-in version is greater than the saved version in the file`() {
        val versionFile = File.createTempFile("version", "")
        versionFile.createNewFile()
        versionFile.writeText("1.2.3")
        whenever(gitService.getFile(any())).thenReturn(versionFile)

        val response = driver.checkVersion(asVersion("4.0.0")).getSuccess()

        assertThat(response).isEmpty()
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

    @Test
    fun `checkRefs clones the repo for the source code branch`() {
        driver.checkRefs("abc123")

        verify(gitService).cloneOrFetch(driver.gitUri, driver.sourceCodeBranch)
    }

    @Test
    fun `checkRefs returns an error if there is a problem cloning`() {
        whenever(gitService.cloneOrFetch(any(), any())).thenReturn(Response.Error(VersionError("did not do it")))

        val response = driver.checkRefs("abc123").getError()

        assertThat(response.message).isEqualTo("did not do it")
    }

    @Test
    fun `checkRefs resets repo to source code branch`() {
        driver.checkRefs("abc123")

        verify(gitService).resetRepoDir(driver.sourceCodeBranch)
    }

    @Test
    fun `checkRefs returns an error if there is a problem resetting the repo`() {
        whenever(gitService.resetRepoDir(any())).thenReturn(Response.Error(VersionError("problem time")))

        val response = driver.checkRefs("abc123").getError()

        assertThat(response.message).isEqualTo("problem time")
    }

    @Test
    fun `checkRefs gets the commits since the most recent version`() {
        driver.checkRefs("abc123")

        verify(gitService).commitsSince("abc123")
    }

    @Test
    fun `checkRefs returns an error if there is an error getting the refs`() {
        whenever(gitService.commitsSince(any())).thenReturn(Response.Error(VersionError("problem time")))

        val response = driver.checkRefs("abc123").getError()

        assertThat(response.message).isEqualTo("problem time")
    }

    @Test
    fun `checkRefs returns list of commits since most recent version`() {
        val commits = listOf("commit1", "commit2")
        whenever(gitService.commitsSince(any())).thenReturn(Response.Success(commits))

        val response = driver.checkRefs("abc123").getSuccess()

        assertThat(response).isEqualTo(commits)
    }
}