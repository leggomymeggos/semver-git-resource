package com.github.leggomymeggos.semver_git_resource.models

import com.github.zafarkhaja.semver.Version as SemVer

open class BumpFactory {
    open fun create(bumpTarget: String, preReleaseTag: String): Bump {
        if(bumpTarget.isEmpty() && preReleaseTag.isNotEmpty()) {
            return PreReleaseBump(preReleaseTag)
        }
        return when (bumpTarget) {
            "patch" -> PatchBump(preReleaseTag)
            "minor" -> MinorBump(preReleaseTag)
            "major" -> MajorBump(preReleaseTag)
            "final" -> FinalBump()
            else -> object : Bump {
                override fun apply(version: SemVer): SemVer = version
            }
        }
    }
}