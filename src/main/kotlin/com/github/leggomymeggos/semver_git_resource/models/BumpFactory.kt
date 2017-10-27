package com.github.leggomymeggos.semver_git_resource.models

import com.github.zafarkhaja.semver.Version as SemVer

class BumpFactory {
    fun create(bumpTarget: String, preReleaseTag: String): Response<Bump, String> {
        if(bumpTarget.isEmpty() && preReleaseTag.isNotEmpty()) {
            return Response.Success(PreReleaseBump(preReleaseTag))
        }
        return when (bumpTarget) {
            "patch" -> Response.Success(PatchBump(preReleaseTag))
            "minor" -> Response.Success(MinorBump(preReleaseTag))
            "major" -> Response.Success(MajorBump(preReleaseTag))
            else -> {
                Response.Error("bump target $bumpTarget not recognized")
            }
        }
    }
}