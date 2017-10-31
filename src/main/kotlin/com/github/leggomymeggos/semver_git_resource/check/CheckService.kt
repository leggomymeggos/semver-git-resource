package com.github.leggomymeggos.semver_git_resource.check

import com.github.leggomymeggos.semver_git_resource.driver.DriverFactory
import com.github.leggomymeggos.semver_git_resource.models.*
import com.github.zafarkhaja.semver.Version as SemVer

class CheckService(private val driverFactory: DriverFactory) {
    fun check(request: CheckRequest): Response<List<Version>, VersionError> {
        var version: SemVer? = null
        if (request.version != null) {
            try {
                version = SemVer.valueOf(request.version.number)
            } catch (e: Exception) {
                println("skipping invalid current version: ${request.version.number}")
                e.printStackTrace()
            }
        }

        return driverFactory.fromSource(request.source)
                .flatMapError { error -> handleDriverError(error) }
                .flatMap { driver ->
                    driver.check(version ?: SemVer.valueOf("0.0.0"))
                            .flatMapError { error -> handleCheckingError(error) }
                            .flatMap { semVersion ->
                                Response.Success(semVersion.map {
                                    Version(number = it.toString(), ref = "")
                                })
                            }
                }
    }

    private fun handleDriverError(error: VersionError): Response.Error<VersionError> =
            Response.Error(VersionError(
                    message = "error creating driver: ${error.message}",
                    exception = error.exception
            ))

    private fun handleCheckingError(error: VersionError): Response.Error<VersionError> =
            Response.Error(VersionError(
                    message = "error checking version: ${error.message}",
                    exception = error.exception
            ))
}