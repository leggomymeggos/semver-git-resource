package com.github.leggomymeggos.semver_git_resource.out

import com.github.leggomymeggos.semver_git_resource.client.EnvironmentService
import com.github.leggomymeggos.semver_git_resource.client.GitService
import com.github.leggomymeggos.semver_git_resource.driver.DriverFactory
import com.github.leggomymeggos.semver_git_resource.driver.DriverFactoryImpl
import com.github.leggomymeggos.semver_git_resource.models.*

class OutService(
        private val driverFactory: DriverFactory = DriverFactoryImpl(),
        private val bumpFactory: BumpFactory = BumpFactory(),
        private val envService: EnvironmentService = EnvironmentService(GitService())
) {
    fun writeVersion(request: OutRequest): Response<OutResponse, VersionError> =
            envService.setUpEnv(request.source).flatMap {
                driverFactory.fromSource(request.source)
                        .flatMapError { Response.Error(VersionError("error creating driver: ${it.message}", it.exception)) }
                        .flatMap { driver ->
                            driver.updateGitService(envService.gitService())
                            val bump = bumpFactory.create(request.params.bump, request.params.pre)
                            driver.bump(bump)
                                    .flatMapError { Response.Error(VersionError("error bumping version: ${it.message}", it.exception)) }
                                    .flatMap { version ->
                                        Response.Success(OutResponse(
                                                version = Version(number = version.toString(), ref = ""),
                                                metadata = listOf(MetadataField(name = "number", value = version.toString()))
                                        ))
                                    }
                        }
            }
}