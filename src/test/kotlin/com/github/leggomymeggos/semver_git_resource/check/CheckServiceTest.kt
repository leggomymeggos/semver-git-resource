package com.github.leggomymeggos.semver_git_resource.check

import com.github.leggomymeggos.semver_git_resource.client.EnvironmentService
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
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import com.github.zafarkhaja.semver.Version as SemVer

class CheckServiceTest {
    private val driver = mock<Driver>()
    private val driverFactory = mock<DriverFactory>()
    private val envService = mock<EnvironmentService>()
    private val checker = CheckService(driverFactory, envService)

    @Before
    fun `set up`() {
        whenever(driver.checkVersion(any())).thenReturn(Response.Success("0.0.0"))
        whenever(driver.checkRefs(any())).thenReturn(Response.Success(listOf("abc123")))
        whenever(driverFactory.fromSource(any())).thenReturn(Response.Success(driver))

        whenever(envService.setUpEnv(any())).thenReturn(Response.Success("went grape"))
        whenever(envService.gitService()).thenReturn(mock())
    }

    @Test
    fun `sets up environment`() {
        val request = createRequest()
        checker.check(request)

        verify(envService).setUpEnv(request.source)
    }
    
    @Test 
    fun `returns an error if there is a set up error`() {
        whenever(envService.setUpEnv(any())).thenReturn(Response.Error(VersionError("something is not right")))

        val response = checker.check(createRequest()).getError()

        assertThat(response.message).isEqualTo("something is not right")
    }

    @Test
    fun `creates a driver`() {
        val request = createRequest()
        checker.check(request)

        verify(driverFactory).fromSource(request.source)
    }

    @Test
    fun `sets same git service on the driver`() {
        val gitService = mock<GitService>()
        whenever(envService.gitService()).thenReturn(gitService)

        checker.check(createRequest())

        verify(envService).gitService()
        verify(driver).updateGitService(gitService)
    }

    @Test
    fun `returns an error when there is an error creating the driver`() {
        whenever(driverFactory.fromSource(any())).thenReturn(
                Response.Error(VersionError("invalid version", Exception("bad driver")))
        )

        val response = checker.check(createRequest()).getError()

        assertThat(response.message).isEqualTo("error creating driver: invalid version")
        assertThat(response.exception).isEqualToComparingFieldByField(Exception("bad driver"))
    }

    @Test
    fun `checks for new refs`() {
        val request = createRequest(ref = "abc123")
        checker.check(request)

        verify(driver).checkRefs("abc123")
    }

    @Test
    fun `bails if there is an error checking refs`() {
        whenever(driver.checkRefs(any())).thenReturn(Response.Error(VersionError("PROBLEM? PROBLEM!")))

        val response = checker.check(createRequest()).getError()

        assertThat(response.message).isEqualTo("error checking refs: PROBLEM? PROBLEM!")
    }

    @Test
    fun `checks for new versions`() {
        val request = createRequest(versionNumber = "4.5.6")
        checker.check(request)

        verify(driver).checkVersion(SemVer.valueOf("4.5.6"))
    }

    @Test
    fun `continues when the current version number is invalid`() {
        val request = createRequest(versionNumber = "1")
        checker.check(request)

        verify(driver).checkVersion(SemVer.valueOf("0.0.0"))
    }

    @Test
    fun `reports when the current version is invalid`() {
        val originalOut = System.out!!
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))

        val request = createRequest(versionNumber = "this is not a real thing broseph")
        checker.check(request)

        assertThat(outputStream.toString()).contains("skipping invalid current version: this is not a real thing broseph")

        System.setOut(originalOut)
    }

    @Test
    fun `does not report that the current version is invalid when it is null`() {
        val originalOut = System.out!!
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))

        val request = createRequest().copy(version = null)
        checker.check(request)

        assertThat(outputStream.toString()).doesNotContain("skipping invalid current version")

        System.setOut(originalOut)
    }

    @Test
    fun `returns an error if there is an unsuccessful check`() {
        whenever(driver.checkVersion(any())).thenReturn(Response.Error(VersionError("did not check out", Exception("something rull bad happen"))))

        val response = checker.check(createRequest()).getError()

        assertThat(response.message).isEqualTo("error checking version: did not check out")
        assertThat(response.exception).isEqualToComparingFieldByField(Exception("something rull bad happen"))
    }

    @Test
    fun `maps successful check to correct response`() {
        whenever(driver.checkRefs(any())).thenReturn(Response.Success(listOf("def456")))
        whenever(driver.checkVersion(any())).thenReturn(Response.Success("1.3.2"))

        val response = checker.check(createRequest()).getSuccess()

        assertThat(response).containsExactly(Version(number = "1.3.2", ref = "def456"))
    }

    private fun createRequest(versionNumber: String = "1.2.3", ref: String = ""): CheckRequest {
        return CheckRequest(Version(versionNumber, ref), Source("", ""))
    }
}