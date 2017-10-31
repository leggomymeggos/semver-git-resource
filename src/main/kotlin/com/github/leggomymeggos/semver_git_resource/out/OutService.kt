package com.github.leggomymeggos.semver_git_resource.out

import com.github.leggomymeggos.semver_git_resource.driver.DriverFactory
import com.github.leggomymeggos.semver_git_resource.driver.DriverFactoryImpl
import com.github.leggomymeggos.semver_git_resource.models.*

class OutService(
        private val driverFactory: DriverFactory = DriverFactoryImpl(),
        private val bumpFactory: BumpFactory = BumpFactory()
) {
    fun writeVersion(request: OutRequest): Response<Version, VersionError> {
        return driverFactory.fromSource(request.source)
                .flatMapError { Response.Error(VersionError("error creating driver: ${it.message}", it.exception)) }
                .flatMap { driver ->
                    val bump = bumpFactory.create(request.params.bump, request.params.pre)
                    driver.bump(bump)
                            .flatMapError { Response.Error(VersionError("error bumping version: ${it.message}", it.exception)) }
                            .flatMap { version ->
                                Response.Success(Version(number = version.toString(), ref = ""))
                            }
                }
    }
}