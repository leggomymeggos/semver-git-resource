package com.github.leggomymeggos.semver_git_resource.driver

import com.github.leggomymeggos.semver_git_resource.models.Source
import com.github.leggomymeggos.semver_git_resource.models.getError
import com.github.leggomymeggos.semver_git_resource.models.getSuccess
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.*
import com.github.zafarkhaja.semver.Version as SemVer

class DriverFactoryImplTest {
    private val factory = DriverFactoryImpl()

    @Test
    fun `returns error if initial version is invalid`() {
        val source = createSource(initialVersion = "a totally cool version")
        val response = factory.fromSource(source).getError()

        assertThat(response.message).isEqualTo("invalid initial version (a totally cool version)")
        assertThat(response.exception).isNotNull()
    }

    @Test
    fun `sets initial version to 0,0,0 if initial version is empty`() {
        val source = createSource(initialVersion = "")
        val response = factory.fromSource(source).getSuccess()

        assertThat(response.initialVersion).isEqualTo(SemVer.valueOf("0.0.0"))
    }

    @Test
    fun `sets initial version to 0,0,0 if initial version is null`() {
        val source = createSource(initialVersion = null)
        val response = factory.fromSource(source).getSuccess()

        assertThat(response.initialVersion).isEqualTo(SemVer.valueOf("0.0.0"))
    }

    @Test
    fun `sets git url`() {
        val source = createSource(uri = "git@git.git:repo/path")
        val response = factory.fromSource(source).getSuccess()

        assertThat(response.gitUri).isEqualTo("git@git.git:repo/path")
    }

    @Test
    fun `sets version file`() {
        val source = createSource(versionFile = "number")
        val response = factory.fromSource(source).getSuccess()

        assertThat(response.versionFile).isEqualTo("number")
    }

    @Test
    fun `sets skipSslVerification`() {
        val skipSslVerification = listOf(true, false, null)[Random().nextInt(3)]
        val source = createSource(skipSslVerification = skipSslVerification)
        val response = factory.fromSource(source).getSuccess()

        assertThat(response.skipSslVerification).isEqualTo(skipSslVerification == true) // does not allow null
    }

    @Test
    fun `sets source code branch`() {
        val source = createSource(sourceCodeBranch = "some_Branch")
        val response = factory.fromSource(source).getSuccess()

        assertThat(response.sourceCodeBranch).isEqualTo("some_Branch")
    }

    @Test
    fun `defaults source code branch to master`() {
        val source = createSource(sourceCodeBranch = null)
        val response = factory.fromSource(source).getSuccess()

        assertThat(response.sourceCodeBranch).isEqualTo("master")
    }

    @Test
    fun `sets version branch`() {
        val source = createSource(versionBranch = "some_version_Branch")
        val response = factory.fromSource(source).getSuccess()

        assertThat(response.versionBranch).isEqualTo("some_version_Branch")
    }

    @Test
    fun `defaults version branch to version`() {
        val source = createSource(versionBranch = null)
        val response = factory.fromSource(source).getSuccess()

        assertThat(response.versionBranch).isEqualTo("version")
    }

    @Test
    fun `sets tag filter`() {
        val source = createSource(tagFilter = "the tag-os")
        val response = factory.fromSource(source).getSuccess()

        assertThat(response.tagFilter).isEqualTo("the tag-os")
    }

    private fun createSource(
            uri: String = "",
            versionFile: String = "",
            privateKey: String = "so private",
            username: String = "such user",
            password: String = "much secret",
            tagFilter: String? = null,
            skipSslVerification: Boolean? = null,
            sourceCodeBranch: String? = null,
            versionBranch: String? = null,
            initialVersion: String? = null
    ): Source {
        return Source(
                uri = uri,
                versionFile = versionFile,
                privateKey = privateKey,
                username = username,
                password = password,
                tagFilter = tagFilter,
                skipSslVerification = skipSslVerification,
                sourceCodeBranch = sourceCodeBranch,
                versionBranch = versionBranch,
                initialVersion = initialVersion
        )
    }
}