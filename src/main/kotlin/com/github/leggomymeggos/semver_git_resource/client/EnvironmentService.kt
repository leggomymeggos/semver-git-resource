package com.github.leggomymeggos.semver_git_resource.client

import com.github.leggomymeggos.semver_git_resource.models.Response
import com.github.leggomymeggos.semver_git_resource.models.Source
import com.github.leggomymeggos.semver_git_resource.models.VersionError
import com.github.leggomymeggos.semver_git_resource.models.flatMap
import java.io.File

open class EnvironmentService(private val gitService: GitService) {
    private val netRcFile = File(System.getenv("HOME"), ".netrc")
    val privateKeyPath = File.createTempFile("tmp/private-key", ".txt")!!

    open fun gitService(): GitService = gitService

    open fun setUpEnv(source: Source): Response<String, VersionError> =
            validate(source).flatMap {
                setUpUsernamePassword(source.username, source.password).flatMap {
                    setUpKey(source.privateKey)
                }
            }

    private fun setUpKey(privateKey: String): Response<String, VersionError> {
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

    private fun setUpUsernamePassword(username: String, password: String): Response<String, VersionError> =
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

    private fun validate(source: Source): Response<String, VersionError> =
            if (source.hasMissingCredentials()) {
                Response.Error(VersionError("missing git credentials. set a username and password or a private key"))
            } else Response.Success("source is valid")

    private fun Source.hasMissingCredentials(): Boolean {
        if (privateKey.isEmpty()) {
            return (username.isEmpty() || password.isEmpty())
        }
        return false
    }
}