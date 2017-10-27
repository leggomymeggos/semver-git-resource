package com.github.leggomymeggos.semver_git_resource.check

import com.github.leggomymeggos.semver_git_resource.models.CheckError
import com.github.leggomymeggos.semver_git_resource.models.Response

interface BashClient {
    fun setEnv(key: String, value: String)
    fun execute(command: String): Response<String, CheckError>
}