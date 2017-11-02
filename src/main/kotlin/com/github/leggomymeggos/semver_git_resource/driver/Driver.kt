package com.github.leggomymeggos.semver_git_resource.driver

import com.github.leggomymeggos.semver_git_resource.client.GitService
import com.github.leggomymeggos.semver_git_resource.models.*
import com.github.zafarkhaja.semver.Version as SemVer

open class Driver(
        val gitUri: String,
        val tagFilter: String,
        val skipSslVerification: Boolean,
        val sourceCodeBranch: String,
        val versionBranch: String,
        val versionFile: String,
        val initialVersion: SemVer,
        var gitService: GitService = GitService()
) {
    open fun updateGitService(gitService: GitService) {
        this.gitService = gitService
    }

    open fun checkVersion(version: SemVer): Response<String, VersionError> =
            getRepoForVersion { number ->
                if (number.greaterThanOrEqualTo(version)) {
                    Response.Success(number.toString())
                } else {
                    Response.Success("")
                }
            }

    open fun checkRefs(currentRef: String): Response<List<String>, VersionError> =
            cloneOrFetchRepo(sourceCodeBranch).flatMap {
                gitService.resetRepoDir(sourceCodeBranch).flatMap {
                    gitService.commitsSince(currentRef)
                }
            }

    open fun bump(bump: Bump): Response<SemVer, VersionError> =
            getRepoForVersion { version ->
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

    private fun <T> getRepoForVersion(block: (SemVer) -> Response<T, VersionError>): Response<T, VersionError> =
            cloneOrFetchRepo(versionBranch).flatMap {
                gitService.resetRepoDir(versionBranch).flatMap {
                    readVersion().flatMap { version ->
                        block(version)
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

    private fun cloneOrFetchRepo(branch: String): Response<String, VersionError> =
            gitService.cloneOrFetch(gitUri, branch)
}
