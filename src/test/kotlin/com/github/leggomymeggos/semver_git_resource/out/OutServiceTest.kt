package com.github.leggomymeggos.semver_git_resource.out

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
import com.github.zafarkhaja.semver.Version as SemVer

class OutServiceTest {
    private val driver = mock<Driver>()
    private val driverFactory = mock<DriverFactory>()
    private val bumpFactory = mock<BumpFactory>()
    private val service = OutService(driverFactory, bumpFactory)

    @Before
    fun `set up`() {
        whenever(driverFactory.fromSource(any())).thenReturn(Response.Success(driver))
        val bump = mock<Bump>()
        whenever(bumpFactory.create(any(), any())).thenReturn(bump)
        whenever(driver.bump(any())).thenReturn(Response.Success(SemVer.valueOf("0.0.0")))
    }

    @Test
    fun `creates a driver`() {
        val request = OutRequest(
                version = Version("1.2.3", ""),
                source = Source(
                        uri = "",
                        versionFile = "version"
                ),
                params = VersionParams("", "")
        )
        service.writeVersion(request)

        verify(driverFactory).fromSource(request.source)
    }

    @Test
    fun `gets bump target`() {
        val request = OutRequest(
                version = Version("1.2.3", ""),
                source = Source(
                        uri = "",
                        versionFile = "version"
                ),
                params = VersionParams(bump = "patch", pre = "rc")
        )
        service.writeVersion(request)
        
        verify(bumpFactory).create("patch", "rc")
    }

    @Test 
    fun `bumps version`() {
        val bump = mock<Bump>()
        whenever(bumpFactory.create(any(), any())).thenReturn(bump)

        val request = OutRequest(
                version = Version("1.2.3", ""),
                source = Source(
                        uri = "",
                        versionFile = "version"
                ),
                params = VersionParams(bump = "patch", pre = "rc")
        )
        service.writeVersion(request)

        verify(driver).bump(bump)
    }

    @Test
    fun `returns bumped version`() {
        val bump = mock<Bump>()
        whenever(bumpFactory.create(any(), any())).thenReturn(bump)

        whenever(driver.bump(any())).thenReturn(Response.Success(SemVer.valueOf("1.2.3")))

        val request = OutRequest(
                version = Version("1.2.3", ""),
                source = Source(
                        uri = "",
                        versionFile = "version"
                ),
                params = VersionParams(bump = "patch", pre = "rc")
        )
        val response = service.writeVersion(request).getSuccess()

        assertThat(response.version.number).isEqualTo("1.2.3")
    }

    @Test
    fun `returns error if there is an error creating the driver`() {
        whenever(driverFactory.fromSource(any())).thenReturn(Response.Error(VersionError("something did not go right")))
        val request = OutRequest(
                version = Version("1.2.3", ""),
                source = Source(
                        uri = "",
                        versionFile = "version"
                ),
                params = VersionParams(bump = "patch", pre = "rc")
        )
        val response = service.writeVersion(request).getError()

        assertThat(response.message).isEqualTo("error creating driver: something did not go right")
    }

    @Test
    fun `returns error if there is an error bumping the version`() {
        whenever(driver.bump(any())).thenReturn(Response.Error(VersionError("uh oh spaghetti-o")))
        val request = OutRequest(
                version = Version("1.2.3", ""),
                source = Source(
                        uri = "",
                        versionFile = "version"
                ),
                params = VersionParams(bump = "patch", pre = "rc")
        )
        val response = service.writeVersion(request).getError()

        assertThat(response.message).isEqualTo("error bumping version: uh oh spaghetti-o")
    }
}