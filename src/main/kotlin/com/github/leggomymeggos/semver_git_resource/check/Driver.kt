package com.github.leggomymeggos.semver_git_resource.check

import com.github.leggomymeggos.semver_git_resource.models.CheckError
import java.io.File
import java.nio.file.Files
import java.util.stream.Collectors
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
        val gitClient: BashClient = GitClient()
) {
    private val netRcFile = File(System.getenv("HOME"), ".netrc")
    val gitRepoDir = Files.createTempDirectory("semver-git-repo")!!
    val privateKeyPath = File.createTempFile("tmp/private-key", ".txt")!!

    open fun check(version: SemVer): Response<List<SemVer>, CheckError> =
            setUpUsernamePassword()
                    .flatMap {
                        setUpKey().flatMap {
                            cloneOrFetchRepo().flatMap {
                                gitClient.execute("cd $gitRepoDir ; git reset --hard origin/$versionBranch").flatMap {
                                    readVersion().flatMap { number ->
                                        if (number.greaterThanOrEqualTo(version)) {
                                            Response.Success(listOf(number))
                                        } else {
                                            Response.Success(emptyList())
                                        }
                                    }
                                }
                            }
                        }
                    }.flatMapError { Response.Error(it) }

    private fun readVersion(): Response<SemVer, CheckError> {
        val versionFile = File("$gitRepoDir/$versionFile")
        return if (versionFile.exists()) {
            val number = versionFile.readText().trim()
            try {
                Response.Success(SemVer.valueOf(number))
            } catch (e: Exception) {
                Response.Error(CheckError("Invalid version: $number", e))
            }
        } else {
            Response.Success(initialVersion)
        }
    }

    private fun cloneOrFetchRepo(): Response<String, CheckError> {
        val gitFiles = Files.list(gitRepoDir).collect(Collectors.toList())

        return if (!Files.exists(gitRepoDir) || gitFiles.isEmpty()) {
            gitClient.execute("git clone $gitUri --branch $versionBranch $gitRepoDir")
        } else {
            gitClient.execute("cd $gitRepoDir ; git fetch origin $versionBranch")
        }
    }

    private fun setUpKey(): Response<String, CheckError> {
        if (privateKey.contains("ENCRYPTED")) {
            return Response.Error(CheckError("private keys with passphrases are not supported"))
        }

        try {
            privateKeyPath.mkdirs()
            privateKeyPath.writeText(privateKey)
            ProcessBuilder("/bin/sh", "-c", "chmod 600 $privateKeyPath").start().waitFor()
        } catch (e: Exception) {
            return Response.Error(CheckError("error saving private key", e))
        }
        gitClient.setEnv("GIT_SSH_COMMAND", "ssh -o StrictHostKeyChecking=no -i $privateKeyPath")
        return Response.Success("successfully saved private key")
    }

    private fun setUpUsernamePassword(): Response<String, CheckError> {
        return try {
            netRcFile.createNewFile()
            netRcFile.writeText("")
            if (username.isNotEmpty() && password.isNotEmpty()) {
                netRcFile.writeText("default login $username password $password")
            }
            return Response.Success("")
        } catch (e: Exception) {
            Response.Error(CheckError("error saving username and password", e))
        }
    }
}
