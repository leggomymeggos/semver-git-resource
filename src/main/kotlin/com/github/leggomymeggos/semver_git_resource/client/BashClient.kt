package com.github.leggomymeggos.semver_git_resource.client

import com.github.leggomymeggos.semver_git_resource.models.Response
import com.github.leggomymeggos.semver_git_resource.models.VersionError

interface BashClient {
    fun setEnv(key: String, value: String)
    fun execute(command: String): Response<String, VersionError>
}