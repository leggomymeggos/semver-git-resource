package com.github.leggomymeggos.semver_git_resource.check

import com.github.leggomymeggos.semver_git_resource.models.CheckError
import com.github.leggomymeggos.semver_git_resource.models.CheckRequest
import com.github.leggomymeggos.semver_git_resource.models.Source
import com.github.leggomymeggos.semver_git_resource.models.Version
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

class CheckerTest {
    private val driver = mock<Driver>()
    private val driverFactory = mock<DriverFactory>()
    private val checker = Checker(driverFactory)

    @Before
    fun `set up`() {
        whenever(driver.check(any())).thenReturn(Response.Success(SemVer.valueOf("0.0.0")))
        whenever(driverFactory.fromSource(any())).thenReturn(Response.Success(driver))
    }

    @Test
    fun `creates a driver`() {
        val request = createRequest()
        checker.check(request)

        verify(driverFactory).fromSource(request.source)
    }

    @Test
    fun `returns an error when there is an error creating the driver`() {
        whenever(driverFactory.fromSource(any())).thenReturn(
                Response.Error(CheckError("invalid version", Exception("bad driver")))
        )

        val response = checker.check(createRequest()).getError()

        assertThat(response.message).isEqualTo("error creating driver: invalid version")
        assertThat(response.exception).isEqualToComparingFieldByField(Exception("bad driver"))
    }

    @Test
    fun `checks for new versions`() {
        val request = createRequest(versionNumber = "4.5.6")
        checker.check(request)

        verify(driver).check(SemVer.valueOf("4.5.6"))
    }

    @Test
    fun `continues when the current version number is invalid`() {
        val request = createRequest(versionNumber = "this is not a real thing bruhhhh")
        checker.check(request)

        verify(driver).check(SemVer.valueOf("0.0.0"))
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
    fun `returns an error if there is an unsuccessful check`() {
        whenever(driver.check(any())).thenReturn(Response.Error(CheckError("did not check out", Exception("something rull bad happen"))))

        val response = checker.check(createRequest()).getError()

        assertThat(response.message).isEqualTo("error checking version: did not check out")
        assertThat(response.exception).isEqualToComparingFieldByField(Exception("something rull bad happen"))
    }

    @Test
    fun `maps successful check to correct response`() {
        whenever(driver.check(any())).thenReturn(Response.Success(SemVer.valueOf("1.3.2")))

        val response = checker.check(createRequest()).getSuccess()

        assertThat(response.map { it.number }).containsExactly("1.3.2")
    }

    private fun createRequest(versionNumber: String = "1.2.3"): CheckRequest {
        return CheckRequest(Version(versionNumber, ""), Source("", "'"))
    }
}