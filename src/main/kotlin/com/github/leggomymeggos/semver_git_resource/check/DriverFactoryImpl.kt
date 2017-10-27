package com.github.leggomymeggos.semver_git_resource.check

import com.github.leggomymeggos.semver_git_resource.models.CheckError
import com.github.leggomymeggos.semver_git_resource.models.Response
import com.github.leggomymeggos.semver_git_resource.models.Source
import com.github.zafarkhaja.semver.Version as SemVer

class DriverFactoryImpl : DriverFactory {
    override fun fromSource(source: Source): Response<Driver, CheckError> {
        val version: SemVer
        version = if (source.initialVersion?.isNotEmpty() == true) {
            try {
                SemVer.valueOf(source.initialVersion)
            } catch (e: Exception) {
                return Response.Error(CheckError("invalid initial version (${source.initialVersion})", e))
            }
        } else {
            SemVer.valueOf("0.0.0")
        }

        if (source.hasMissingCredentials()) {
            return Response.Error(CheckError("missing git credentials. set a username and password or a private key"))
        }

        return Response.Success(Driver(
                gitUri = source.uri,
                initialVersion = version,
                privateKey = source.privateKey ?: "",
                username = source.username ?: "",
                password = source.password ?: "",
                tagFilter = source.tagFilter ?: "",
                skipSslVerification = source.skipSslVerification?.equals(true) ?: false,
                sourceCodeBranch = source.sourceCodeBranch ?: "master",
                versionBranch = source.versionBranch ?: "version",
                versionFile = source.versionFile
        ))
    }

    private fun Source.hasMissingCredentials(): Boolean {
        if (privateKey.isNullOrEmpty()) {
            return (username.isNullOrEmpty() || password.isNullOrEmpty())
        }
        return false
    }
}