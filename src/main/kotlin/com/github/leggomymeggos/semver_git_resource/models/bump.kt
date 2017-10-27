package com.github.leggomymeggos.semver_git_resource.models

import com.github.zafarkhaja.semver.Version as SemVer

interface Bump {
    fun apply(version: SemVer) : SemVer
}

class PatchBump : Bump {
    override fun apply(version: SemVer): SemVer = version.incrementPatchVersion()
}

class MinorBump : Bump {
    override fun apply(version: SemVer): SemVer = version.incrementMinorVersion()
}

class MajorBump : Bump {
    override fun apply(version: SemVer): SemVer = version.incrementMajorVersion()
}