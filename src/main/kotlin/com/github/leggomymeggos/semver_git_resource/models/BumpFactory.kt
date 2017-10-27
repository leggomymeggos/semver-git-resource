package com.github.leggomymeggos.semver_git_resource.models

import com.github.zafarkhaja.semver.Version as SemVer

class BumpFactory {
    fun create(bumpTarget: String, prereleaseTag: String) : Bump {
        return PatchBump()
    }
}