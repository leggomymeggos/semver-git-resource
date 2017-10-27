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
        return version.addPreReleaseTag(tag)
    }
}

fun SemVer.addPreReleaseTag(tag: String) : SemVer {
    return when {
        preReleaseVersion.isNotEmpty() && preReleaseVersion.split(".")[0] == tag ->
            // create a new object so we're not mutating it under the hood
            SemVer.valueOf(this.toString()).incrementPreReleaseVersion()
        tag.isNotEmpty() ->
            setPreReleaseVersion(tag).incrementPreReleaseVersion()
        else -> this
    }
}