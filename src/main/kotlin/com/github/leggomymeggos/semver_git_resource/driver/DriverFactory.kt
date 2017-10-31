package com.github.leggomymeggos.semver_git_resource.driver

import com.github.leggomymeggos.semver_git_resource.models.Response
import com.github.leggomymeggos.semver_git_resource.models.Source
import com.github.leggomymeggos.semver_git_resource.models.VersionError

interface DriverFactory {
    fun fromSource(source: Source): Response<Driver, VersionError>
}