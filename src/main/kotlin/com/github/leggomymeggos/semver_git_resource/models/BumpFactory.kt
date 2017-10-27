package com.github.leggomymeggos.semver_git_resource.models

import com.github.zafarkhaja.semver.Version as SemVer

class BumpFactory {
    fun create(bumpTarget: String, prereleaseTag: String): Response<Bump, String> {
        return when (bumpTarget) {
            "patch" -> Response.Success(PatchBump())
            "minor" -> Response.Success(MinorBump())
            "major" -> Response.Success(MajorBump())
            else -> {
                Response.Error("bump target $bumpTarget not recognized")
            }
        }
    }
}