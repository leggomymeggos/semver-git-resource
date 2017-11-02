package com.github.leggomymeggos.semver_git_resource.client

import com.github.leggomymeggos.semver_git_resource.models.Response
import com.github.leggomymeggos.semver_git_resource.models.VersionError
import com.github.leggomymeggos.semver_git_resource.models.flatMap
import java.io.File
import java.nio.file.Files
import java.util.stream.Collectors

open class GitService(private val gitClient: BashClient = GitClient()) {
    val gitRepoDir = Files.createTempDirectory("semver-git-repo")!!

    open fun add(vararg files: String): Response<String, VersionError> {
        val filesToAdd = if (files.size == 1 && files.first() == "all") {
            "."
        } else {
            files.joinToString(" ")
        }

        return gitClient.execute("cd $gitRepoDir ; git add $filesToAdd").parseLogs()
    }

    open fun commit(message: String) =
            gitClient.execute("cd $gitRepoDir ; git commit -m \"$message\"").parseLogs()

    open fun push(branch: String): Response<String, VersionError> {
        val pushResult = gitClient.execute("cd $gitRepoDir ; git push origin $branch").parseLogs()

        val logs = pushResult.getLogs()
        return if (logs.contains("[rejected]") ||
                logs.contains("[remote rejected]") ||
                logs.contains("Everything up-to-date")) {
            println("Retrying...\n")
            push(branch)
        } else pushResult
    }

    open fun cloneOrFetch(uri: String, branch: String): Response<String, VersionError> {
        val gitFiles = Files.list(gitRepoDir).collect(Collectors.toList())

        return if (!Files.exists(gitRepoDir) || gitFiles.isEmpty()) {
            gitClient.execute("git clone $uri --branch $branch $gitRepoDir").parseLogs()
        } else {
            gitClient.execute("cd $gitRepoDir ; git fetch origin $branch").parseLogs()
        }
    }

    open fun resetRepoDir(branch: String): Response<String, VersionError> {
        return gitClient.execute("cd $gitRepoDir ; git reset --hard origin/$branch").parseLogs()
    }

    open fun getFile(file: String): File {
        return File("$gitRepoDir/$file")
    }

    open fun setEnv(key: String, value: String) =
            gitClient.setEnv(key, value)

    open fun commitsSince(sha: String): Response<List<String>, VersionError> =
            gitClient.execute("cd $gitRepoDir ; git log --pretty=format:'%H'")
                    .parseLogs()
                    .flatMap { message ->
                        val mostRecentCommit = message.split("\n").first()
                        Response.Success(listOf(mostRecentCommit))
                    }

    private fun Response<String, VersionError>.getLogs(): String =
            when (this) {
                is Response.Error -> error.message
                is Response.Success -> value
            }

    private fun Response<String, VersionError>.parseLogs(): Response<String, VersionError> {
        val message = getLogs()
        println(message)

        return if (message.contains("fatal")) {
            return when (this) {
                is Response.Success -> Response.Error(VersionError(this.value))
                is Response.Error -> this
            }
        } else this
    }
}