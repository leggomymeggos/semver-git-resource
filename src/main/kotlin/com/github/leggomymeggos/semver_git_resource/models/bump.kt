package com.github.leggomymeggos.semver_git_resource.models

import com.github.zafarkhaja.semver.Version

interface Bump {
    fun apply(version: Version) : Version
}

class PatchBump : Bump {
    override fun apply(version: Version): Version = version.incrementPatchVersion()
}