package com.github.leggomymeggos.semver_git_resource.client

import com.github.leggomymeggos.semver_git_resource.models.Response
import com.github.leggomymeggos.semver_git_resource.models.Source
import com.github.leggomymeggos.semver_git_resource.models.getError
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File

class GitAuthenticationServiceTest {
    private val gitService = mock<GitService>()
    private val envService = GitAuthenticationService(gitService)

    @Test
    fun `setUpEnv returns error if private key, username, and password are all empty`() {
        val source = createSource(privateKey = "", username = "", password = "")
        val response = envService.setUpEnv(source).getError()

        assertThat(response.message).isEqualTo("missing git credentials. set a username and password or a private key")
        assertThat(response.exception).isNull()
    }

    @Test
    fun `setUpEnv returns error if username is set but password is empty`() {
        val source = createSource(privateKey = "", username = "super user", password = "")
        val response = envService.setUpEnv(source).getError()

        assertThat(response.message).isEqualTo("missing git credentials. set a username and password or a private key")
        assertThat(response.exception).isNull()
    }

    @Test
    fun `setUpEnv returns error if password is set but username is empty`() {
        val source = createSource(privateKey = "", username = "", password = "souperSeekrit123")
        val response = envService.setUpEnv(source).getError()

        assertThat(response.message).isEqualTo("missing git credentials. set a username and password or a private key")
        assertThat(response.exception).isNull()
    }

    @Test
    fun `setUpEnv allows empty username and password when private key is set`() {
        val source = createSource(privateKey = "such a gr8 key", username = "", password = "")
        val response = envService.setUpEnv(source)

        assertThat(response).isInstanceOf(Response.Success::class.java)
    }

    @Test
    fun `setUpEnv allows empty private key when username and password are set`() {
        val source = createSource(privateKey = "", username = "user1", password = "password1")
        val response = envService.setUpEnv(source)

        assertThat(response).isInstanceOf(Response.Success::class.java)
    }

    @Test
    fun `setUpEnv clears netrc file if it exists`() {
        val netRc = File(System.getenv("HOME"), ".netrc")
        netRc.createNewFile()
        netRc.writeText("hey yeah here i am")

        envService.setUpEnv(createSource())

        assertThat(netRc.readText()).doesNotContain("hey yeah here i am")
    }

    @Test
    fun `setUpEnv creates netrc file with git username and password`() {
        val netRc = File(System.getenv("HOME"), ".netrc")

        envService.setUpEnv(createSource(username = "user123", password = "password456"))

        assertThat(netRc.exists()).isTrue()
        assertThat(netRc.readText()).isEqualTo("default login user123 password password456")
    }

    @Test
    fun `setUpEnv does not save new netrc file if username and password are empty`() {
        val netRc = File(System.getenv("HOME"), ".netrc")
        netRc.createNewFile()
        netRc.writeText("hey yeah here i am")

        envService.setUpEnv(createSource(username = "", password = ""))

        assertThat(netRc.readText()).isEmpty()
    }

    @Test
    fun `setUpEnv does not support encrypted keys`() {
        val response = envService.setUpEnv(createSource(privateKey = "privateENCRYPTED")).getError()

        assertThat(response.message).isEqualTo("private keys with passphrases are not supported")
    }

    @Test
    fun `setUpEnv saves private key`() {
        envService.setUpEnv(createSource(privateKey = "super private key"))

        assertThat(envService.privateKeyPath.readText()).isEqualTo("super private key")
    }

    @Test
    fun `setUpEnv exports private key`() {
        envService.setUpEnv(createSource(privateKey = "super private key"))

        verify(gitService).setEnv("GIT_SSH_COMMAND", "ssh -o StrictHostKeyChecking=no -i ${envService.privateKeyPath}")
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