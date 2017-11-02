package com.github.leggomymeggos.semver_git_resource.driver

import com.github.leggomymeggos.semver_git_resource.models.Response
import com.github.leggomymeggos.semver_git_resource.models.Source
import com.github.leggomymeggos.semver_git_resource.models.VersionError
import com.github.zafarkhaja.semver.Version as SemVer

class DriverFactoryImpl : DriverFactory {
    override fun fromSource(source: Source): Response<Driver, VersionError> {
        val version: SemVer
        version = if (source.initialVersion?.isNotEmpty() == true) {
            try {
                SemVer.valueOf(source.initialVersion)
            } catch (e: Exception) {
                return Response.Error(VersionError("invalid initial version (${source.initialVersion})", e))
            }
        } else {
            SemVer.valueOf("0.0.0")
        }

        return Response.Success(Driver(
                gitUri = source.uri,
                initialVersion = version,
                tagFilter = source.tagFilter ?: "",
                skipSslVerification = source.skipSslVerification?.equals(true) ?: false,
                sourceCodeBranch = source.sourceCodeBranch ?: "master",
                versionBranch = source.versionBranch ?: "version",
                versionFile = source.versionFile
        ))
    }
}