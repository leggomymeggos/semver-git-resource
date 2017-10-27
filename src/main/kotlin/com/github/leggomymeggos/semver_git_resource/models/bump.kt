package com.github.leggomymeggos.semver_git_resource.models

import com.github.zafarkhaja.semver.Version as SemVer

interface Bump {
    fun apply(version: SemVer) : SemVer
}

class PatchBump(private val tag: String = "") : Bump {
    override fun apply(version: SemVer): SemVer {
        return version.incrementPatchVersion().addPreReleaseTag(tag)
    }
}

class MinorBump(private val tag: String = "") : Bump {
    override fun apply(version: SemVer): SemVer = version.incrementMinorVersion().addPreReleaseTag(tag)
}

class MajorBump(private val tag: String = "") : Bump {
    override fun apply(version: SemVer): SemVer = version.incrementMajorVersion().addPreReleaseTag(tag)
}

class PreReleaseBump(private val tag: String) : Bump {
    override fun apply(version: SemVer): SemVer {
        return version.setPreReleaseVersion(tag).incrementPreReleaseVersion()
    }
}

fun SemVer.addPreReleaseTag(tag: String) : SemVer {
    return if(tag.isNotEmpty()) {
        this.setPreReleaseVersion(tag).incrementPreReleaseVersion()
    } else {
        this
    }
}