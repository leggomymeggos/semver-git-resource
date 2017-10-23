package com.github.leggomymeggos.semver_git_resource.check

import com.github.leggomymeggos.semver_git_resource.models.CheckError
import com.github.leggomymeggos.semver_git_resource.models.CheckRequest
import com.github.leggomymeggos.semver_git_resource.models.Version
import com.github.zafarkhaja.semver.Version as SemVer

class Checker(private val driverFactory: DriverFactory) {
    fun check(request: CheckRequest): Response<List<Version>, CheckError> {
        val version: SemVer
        try {
            version = SemVer.valueOf(request.version.number)
        } catch (e: Exception) {
            return Response.Error(CheckError("skipping invalid current version", e))
        }

        return driverFactory.fromSource(request.source)
                .flatMapError { error -> handleDriverError(error) }
                .flatMap { driver ->
                    driver.check(version)
                            .flatMapError { error -> handleCheckingError(error) }
                            .flatMap { semVersion ->
                                Response.Success(listOf(Version(number = semVersion.toString(), ref = "")))
                            }
                }
    }

    private fun handleDriverError(error: CheckError): Response.Error<CheckError> =
            Response.Error(CheckError(
                    message = "error creating driver: ${error.message}",
                    exception = error.exception
            ))

    private fun handleCheckingError(error: CheckError): Response.Error<CheckError> =
            Response.Error(CheckError(
                    message = "error checking version: ${error.message}",
                    exception = error.exception
            ))
}