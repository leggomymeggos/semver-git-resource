package com.github.leggomymeggos.semver_git_resource.client

import com.github.leggomymeggos.semver_git_resource.models.Response
import com.github.leggomymeggos.semver_git_resource.models.VersionError
import java.io.File

class GitClient : BashClient {
    private val LOGS_FILE = createFile("src/main/logs/", "git_logs.txt")
    private val ERROR_FILE = createFile("src/main/logs/", "git_error.txt")
    private val envMap: MutableMap<String, String> = mutableMapOf()

    override fun setEnv(key: String, value: String) {
        envMap.put(key, value)
    }

    override fun execute(command: String): Response<String, VersionError> =
            try {
                val processBuilder = ProcessBuilder("/bin/sh", "-c", command)
                        .redirectOutput(LOGS_FILE)
                        .redirectError(ERROR_FILE)
                processBuilder.environment().putAll(envMap)
                processBuilder.start().waitFor()

                Response.Success(getLogs())
            } catch (e: Exception) {
                Response.Error(VersionError(getLogs(), e))
            }

    private fun createFile(filePath: String, fileName: String): File {
        val path = File(filePath)
        path.mkdirs()
        return File(path, fileName)
    }

    private fun getLogs() : String {
        val logs = LOGS_FILE.readLines().toMutableList()
        logs.addAll(ERROR_FILE.readLines())
        return logs.joinToString("\n")
    }
}