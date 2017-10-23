package com.github.leggomymeggos.semver_git_resource.models

import com.fasterxml.jackson.annotation.JsonProperty

data class Version(
        @JsonProperty("number") val number: String,
        @JsonProperty("ref")    val ref:    String
)

data class Source(
        @JsonProperty("uri") val uri: String,
        @JsonProperty("version_file") val versionFile: String,
        @JsonProperty("private_key") val privateKey: String? = null,
        @JsonProperty("username") val username: String? = null,
        @JsonProperty("password") val password: String? = null,
        @JsonProperty("tag_filter") val tagFilter: String? = null,
        @JsonProperty("skip_ssl_verification") val skipSslVerification: Boolean? = null,
        @JsonProperty("source_code_branch") val sourceCodeBranch: String? = null,
        @JsonProperty("version_branch") val versionBranch: String? = null,
        @JsonProperty("initial_version") val initialVersion: String? = null
)

data class CheckRequest(
        @JsonProperty("version") val version: Version,
        @JsonProperty("source")  val source: Source
)

data class CheckError(val message: String, val exception: Exception? = null)