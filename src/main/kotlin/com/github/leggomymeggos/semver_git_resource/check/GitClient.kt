package com.github.leggomymeggos.semver_git_resource.check

import com.github.leggomymeggos.semver_git_resource.models.CheckError
import java.io.File

class GitClient : BashClient {
    private val LOGS_FILE = File("git_logs.txt")
    private val ERROR_FILE = File("git_error.txt")
    private val envMap: MutableMap<String, String> = mutableMapOf()

    override fun setEnv(key: String, value: String) {
        envMap.put(key, value)
    }

    override fun execute(command: String): Response<String, CheckError> =
            try {
                val processBuilder = ProcessBuilder(mutableListOf("/bin/sh", "-c", command))
                        .redirectOutput(LOGS_FILE)
                        .redirectError(ERROR_FILE)
                processBuilder.environment().putAll(envMap)
                processBuilder.start()

                Response.Success("Successfully executed command: $command")
            } catch (e: Exception) {
                Response.Error(CheckError("Error executing command: $command", e))
            }
}