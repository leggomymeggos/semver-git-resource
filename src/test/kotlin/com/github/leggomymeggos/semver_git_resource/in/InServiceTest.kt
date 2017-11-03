package com.github.leggomymeggos.semver_git_resource.`in`

import com.github.leggomymeggos.semver_git_resource.client.GitAuthenticationService
import com.github.leggomymeggos.semver_git_resource.client.GitService
import com.github.leggomymeggos.semver_git_resource.driver.Driver
import com.github.leggomymeggos.semver_git_resource.driver.DriverFactory
import com.github.leggomymeggos.semver_git_resource.models.*
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import com.github.zafarkhaja.semver.Version as SemVer

class InServiceTest {
    private val authService = mock<GitAuthenticationService>()
    private val gitService = mock<GitService>()
    private val bumpFactory = mock<BumpFactory>()
    private val driverFactory = mock<DriverFactory>()
    private val service = InService(authService, bumpFactory, driverFactory)

    private val tempDestination = Files.createTempDirectory("temp").toAbsolutePath().toString()

    @Before
    fun `set up`() {
        whenever(bumpFactory.create(any(), any())).thenReturn(object : Bump {
            override fun apply(version: SemVer): SemVer = SemVer.valueOf("0.0.0")
        })

        whenever(authService.setUpEnv(any())).thenReturn(Response.Success("that worked"))
        whenever(driverFactory.fromSource(any())).thenReturn(Response.Success(mock()))
        whenever(authService.gitService()).thenReturn(gitService)
    }

    @Test
    fun `read sets up the git environment`() {
        val request = createRequest()
        service.read(request, tempDestination)

        verify(authService).setUpEnv(request.source)
    }

    @Test
    fun `read returns an error when there is a problem setting up the environment`() {
        whenever(authService.setUpEnv(any())).thenReturn(Response.Error(VersionError("done borked")))

        val response = service.read(createRequest(), tempDestination).getError()

        assertThat(response.message).isEqualTo("done borked")
    }

    @Test
    fun `read creates a driver`() {
        val request = createRequest()
        service.read(request, tempDestination)

        verify(driverFactory).fromSource(request.source)
    }

    @Test
    fun `read returns an error when there is an error creating the driver`() {
        whenever(driverFactory.fromSource(any())).thenReturn(Response.Error(VersionError("done went wrong")))

        val response = service.read(createRequest(), tempDestination).getError()

        assertThat(response.message).isEqualTo("done went wrong")
    }

    @Test
    fun `read sets the gitService on the driver`() {
        val driver = mock<Driver>()
        whenever(driverFactory.fromSource(any())).thenReturn(Response.Success(driver))

        service.read(createRequest(), tempDestination)

        verify(driver).updateGitService(gitService)
    }

    @Test
    fun `read creates a version bump`() {
        service.read(createRequest().withBump("major").withPre("rc"), tempDestination)

        verify(bumpFactory).create("major", "rc")
    }

    @Test
    fun `read saves version bump to version and number file in the destination`() {
        whenever(bumpFactory.create(any(), any())).thenReturn(object : Bump {
            override fun apply(version: SemVer): SemVer = SemVer.valueOf("1.3.4")
        })

        service.read(createRequest(), tempDestination)

        val versionFile = File("$tempDestination/version")
        val numberFile = File("$tempDestination/number")

        assertThat(versionFile.readText()).isEqualTo("1.3.4")
        assertThat(numberFile.readText()).isEqualTo("1.3.4")
    }

    private fun createRequest(): InRequest {
        return InRequest(
                version = Version(
                        number = "1.2.3",
                        ref = "abc123"
                ),
                source = Source(
                        uri = "git uri",
                        versionFile = "version"
                ),
                params = VersionParams(
                        bump = "minor",
                        pre = "rc"
                )
        )
    }

    private fun InRequest.withBump(bump: String): InRequest =
            copy(params = params.copy(bump = bump))

    private fun InRequest.withPre(pre: String): InRequest =
            copy(params = params.copy(pre = pre))
}