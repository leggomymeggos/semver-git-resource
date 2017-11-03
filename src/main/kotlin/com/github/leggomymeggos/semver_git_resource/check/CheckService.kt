package com.github.leggomymeggos.semver_git_resource.check

import com.github.leggomymeggos.semver_git_resource.client.GitAuthenticationService
import com.github.leggomymeggos.semver_git_resource.client.GitService
import com.github.leggomymeggos.semver_git_resource.driver.DriverFactory
import com.github.leggomymeggos.semver_git_resource.driver.DriverFactoryImpl
import com.github.leggomymeggos.semver_git_resource.models.*
import com.github.zafarkhaja.semver.Version as SemVer

class CheckService(
        private val driverFactory: DriverFactory = DriverFactoryImpl(),
        private val authService: GitAuthenticationService = GitAuthenticationService(GitService())
) {
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

        return authService.setUpEnv(request.source).flatMap {
            driverFactory.fromSource(request.source)
                    .flatMapError { it.driverError() }
                    .flatMap { driver ->
                        driver.updateGitService(authService.gitService())
                        driver.checkRefs(request.version?.ref ?: "")
                                .flatMapError { it.refsError() }
                                .flatMap { refs ->
                                    driver.checkVersion(version ?: SemVer.valueOf("0.0.0"))
                                            .flatMapError { it.versionError() }
                                            .flatMap { version ->
                                                Response.Success(refs.map { ref ->
                                                    Version(number = version, ref = ref)
                                                })
                                            }
                                }
                    }
        }
    }

    private fun VersionError.refsError(): Response.Error<VersionError> =
            Response.Error(VersionError(
                    message = "error checking refs: $message",
                    exception = exception
            ))

    private fun VersionError.driverError(): Response.Error<VersionError> =
            Response.Error(VersionError(
                    message = "error creating driver: $message",
                    exception = exception
            ))

    private fun VersionError.versionError(): Response.Error<VersionError> =
            Response.Error(VersionError(
                    message = "error checking version: $message",
                    exception = exception
            ))
}