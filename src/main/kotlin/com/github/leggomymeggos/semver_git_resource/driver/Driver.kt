package com.github.leggomymeggos.semver_git_resource.driver

import com.github.leggomymeggos.semver_git_resource.client.GitService
import com.github.leggomymeggos.semver_git_resource.models.*
import java.io.File
import com.github.zafarkhaja.semver.Version as SemVer

open class Driver(
        val gitUri: String,
        val privateKey: String,
        val username: String,
        val password: String,
        val tagFilter: String,
        val skipSslVerification: Boolean,
        val sourceCodeBranch: String,
        val versionBranch: String,
        val versionFile: String,
        val initialVersion: SemVer,
        val gitService: GitService = GitService()
) {
    private val netRcFile = File(System.getenv("HOME"), ".netrc")
    val privateKeyPath = File.createTempFile("tmp/private-key", ".txt")!!

    open fun check(version: SemVer): Response<List<SemVer>, VersionError> =
            getRepo { number ->
                if (number.greaterThanOrEqualTo(version)) {
                    Response.Success(listOf(number))
                } else {
                    Response.Success(emptyList())
                }
            }

    open fun bump(bump: Bump): Response<SemVer, VersionError> =
            getRepo { version ->
                val newVersion = bump.apply(version)
                if (version == newVersion) {
                    Response.Success(version)
                } else {
                    val writableVersion = gitService.getFile(versionFile)
                    writableVersion.writeText(newVersion.toString())
                    gitService.add(versionFile).flatMap {
                        gitService.commit("bumped version to $newVersion").flatMap {
                            gitService.push(versionBranch).flatMap {
                                Response.Success(newVersion)
                            }
                        }
                    }.flatMapError {
                        Response.Error(VersionError("error with git: ${it.message}", it.exception))
                    }
                }
            }

    private fun <T> getRepo(block: (SemVer) -> Response<T, VersionError>): Response<T, VersionError> =
            setUpUsernamePassword()
                    .flatMap {
                        setUpKey().flatMap {
                            cloneOrFetchRepo().flatMap {
                                gitService.resetRepoDir(versionBranch).flatMap {
                                    readVersion().flatMap { version ->
                                        block(version)
                                    }
                                }
                            }
                        }
                    }.flatMapError { Response.Error(it) }

    private fun readVersion(): Response<SemVer, VersionError> {
        val versionFile = gitService.getFile(versionFile)
        return if (versionFile.exists()) {
            val number = versionFile.readText().trim()
            try {
                Response.Success(SemVer.valueOf(number))
            } catch (e: Exception) {
                Response.Error(VersionError("Invalid version: $number", e))
            }
        } else {
            Response.Success(initialVersion)
        }
    }

    private fun cloneOrFetchRepo(): Response<String, VersionError> {
       return gitService.cloneOrFetch(gitUri, versionBranch)
    }

    private fun setUpKey(): Response<String, VersionError> {
        if (privateKey.contains("ENCRYPTED")) {
            return Response.Error(VersionError("private keys with passphrases are not supported"))
        }

        try {
            privateKeyPath.mkdirs()
            privateKeyPath.writeText(privateKey)
            ProcessBuilder("/bin/sh", "-c", "chmod 600 $privateKeyPath").start().waitFor()
        } catch (e: Exception) {
            return Response.Error(VersionError("error saving private key", e))
        }
        gitService.setEnv("GIT_SSH_COMMAND", "ssh -o StrictHostKeyChecking=no -i $privateKeyPath")
        return Response.Success("successfully saved private key")
    }

    private fun setUpUsernamePassword(): Response<String, VersionError> =
            try {
                netRcFile.createNewFile()
                netRcFile.writeText("")
                if (username.isNotEmpty() && password.isNotEmpty()) {
                    netRcFile.writeText("default login $username password $password")
                }
                Response.Success("successfully saved username and password")
            } catch (e: Exception) {
                Response.Error(VersionError("error saving username and password", e))
            }
}
