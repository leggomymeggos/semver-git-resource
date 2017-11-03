package com.github.leggomymeggos.semver_git_resource.`in`

import com.github.leggomymeggos.semver_git_resource.client.GitAuthenticationService
import com.github.leggomymeggos.semver_git_resource.driver.DriverFactory
import com.github.leggomymeggos.semver_git_resource.models.*
import java.io.File
import com.github.zafarkhaja.semver.Version as SemVer

class InService(
        val authService: GitAuthenticationService,
        val bumpFactory: BumpFactory,
        val driverFactory: DriverFactory
) {
    fun read(request: InRequest, destination: String): Response<InResponse, VersionError> {

        return authService.setUpEnv(request.source).flatMap {
            driverFactory.fromSource(request.source).flatMap { driver ->
                driver.updateGitService(authService.gitService())

                val destFile = File(destination)
                destFile.mkdirs()
                // clone repo (instead of making the file; git clone does this for you)
                // clean repo
                //reset to request.version.ref
                //save request.version.ref or most recent ref in $destination/.git/ref


                val inputVersion = SemVer.valueOf(request.version.number)
                val result = bumpFactory.create(request.params.bump, request.params.pre)
                val version = result.apply(inputVersion)
                if (version != inputVersion) {
                    println("bumped version locally from $inputVersion to $version")
                }
                // save files to number & version
                listOf("version", "number").forEach { fileName ->
                    try {
                        val file = File("$destination/$fileName")
                        file.writeText(version.toString())
                    } catch (e: Exception) {
                        return Response.Error(VersionError(
                                "error writing to file: $destination/$fileName",
                                e
                        ))
                    }
                }

                // add git metadata
                Response.Success(InResponse(
                        version = request.version,
                        metadata = listOf(MetadataField(name = "number", value = request.version.number))
                ))
            }
        }
    }
}