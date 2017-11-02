package com.github.leggomymeggos.semver_git_resource.client

import com.github.leggomymeggos.semver_git_resource.models.Response
import com.github.leggomymeggos.semver_git_resource.models.VersionError
import com.github.leggomymeggos.semver_git_resource.models.getError
import com.github.leggomymeggos.semver_git_resource.models.getSuccess
import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class GitServiceTest {

    private val client = mock<BashClient>()
    private val service = GitService(client)

    @Before
    fun `set up`() {
        whenever(client.execute(any())).thenReturn(Response.Success("did the thing"))
    }

    @Test
    fun `set env sets env on the client`() {
        service.setEnv("key", "value")
        verify(client).setEnv("key", "value")
    }

    @Test
    fun `commit executes commit command with message`() {
        service.commit("doing stuff and adding things :))))))))")

        verify(client).execute("cd ${service.gitRepoDir} ; git commit -m \"doing stuff and adding things :))))))))\"")
    }

    @Test
    fun `commit returns the response from the client`() {
        val success = Response.Success("did the thing!")
        whenever(client.execute(any())).thenReturn(success)

        val result = service.commit("stuff")

        assertThat(result).isEqualTo(success)
    }

    @Test
    fun `add adds all given files`() {
        service.add("oneFile", "twoFile", "threeFile")

        verify(client).execute("cd ${service.gitRepoDir} ; git add oneFile twoFile threeFile")
    }

    @Test
    fun `add can add everything`() {
        service.add("all")

        verify(client).execute("cd ${service.gitRepoDir} ; git add .")
    }

    @Test
    fun `add does not add everything if all is not the only argument`() {
        service.add("all", "other")

        verify(client).execute("cd ${service.gitRepoDir} ; git add all other")
    }

    @Test
    fun `add returns response from the client`() {
        val success = Response.Success("did the thing!")
        whenever(client.execute(any())).thenReturn(success)

        val result = service.add("stuff")

        assertThat(result).isEqualTo(success)
    }

    @Test
    fun `cloneOrFetch clones the repo from the given uri`() {
        service.cloneOrFetch("http://git.com/repo", "pullBranch")

        verify(client).execute("git clone http://git.com/repo --branch pullBranch ${service.gitRepoDir}")
    }
    
    @Test 
    fun `cloneOrFetch fetches the repo when the temporary git repo already exists and is not empty`() {
        File("${service.gitRepoDir}/temp.txt").createNewFile()

        service.cloneOrFetch("http://git.com/repo", "pullBranch")

        verify(client).execute("cd ${service.gitRepoDir} ; git fetch origin pullBranch")
    }

    @Test
    fun `cloneOrFetch returns the result of the client`() {
        val success = Response.Success("did the thing!")
        whenever(client.execute(any())).thenReturn(success)

        val response = service.cloneOrFetch("", "")

        assertThat(response).isEqualTo(success)
    }

    @Test
    fun `resetRepoDir does a hard reset on the given branch`() {
        service.resetRepoDir("branchy_branch")

        verify(client).execute("cd ${service.gitRepoDir} ; git reset --hard origin/branchy_branch")
    }

    @Test
    fun `resetRepoDir returns the result of the client`() {
        val success = Response.Success("i am success")
        whenever(client.execute(any())).thenReturn(success)

        val response = service.resetRepoDir("branchhhh")

        assertThat(response).isEqualTo(success)
    }

    @Test
    fun `getFile returns the file from the git repo dir`() {
        val newFile = File("${service.gitRepoDir}/new_new.txt")

        val file = service.getFile("new_new.txt")

        assertThat(file).isEqualTo(newFile)
    }

    @Test
    fun `push pushes to branch`() {
        whenever(client.execute(any())).thenReturn(Response.Success("worked"))

        service.push("version")

        verify(client).execute("cd ${service.gitRepoDir} ; git push origin version")
    }

    @Test
    fun `push retries if the push is rejected`() {
        whenever(client.execute(any()))
                .thenReturn(Response.Success("[rejected]"))
                .thenReturn(Response.Error(VersionError("[rejected]")))
                .thenReturn(Response.Success("that worked"))

        service.push("version")

        verify(client, times(3)).execute("cd ${service.gitRepoDir} ; git push origin version")
    }

    @Test
    fun `push retries if the push is rejected by the remote`() {
        whenever(client.execute(any()))
                .thenReturn(Response.Success("[remote rejected]"))
                .thenReturn(Response.Error(VersionError("[remote rejected]")))
                .thenReturn(Response.Success("that worked"))

        service.push("version")

        verify(client, times(3)).execute("cd ${service.gitRepoDir} ; git push origin version")
    }

    @Test
    fun `push retries if the repo is already up to date`() {
        whenever(client.execute(any()))
                .thenReturn(Response.Success("Everything up-to-date"))
                .thenReturn(Response.Error(VersionError("Everything up-to-date")))
                .thenReturn(Response.Success("that worked"))

        service.push("version")

        verify(client, times(3)).execute("cd ${service.gitRepoDir} ; git push origin version")
    }

    @Test
    fun `push returns any other error`() {
        val error = Response.Error(VersionError("something else went wrong"))
        whenever(client.execute(any())).thenReturn(error)

        val response = service.push("version")

        verify(client, times(1)).execute("cd ${service.gitRepoDir} ; git push origin version")
        assertThat(response).isEqualTo(error)
    }

    @Test
    fun `push prints each error`() {
        val originalOut = System.out!!
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))

        whenever(client.execute(any()))
                .thenReturn(Response.Error(VersionError("Everything up-to-date")))
                .thenReturn(Response.Success("[rejected]"))
                .thenReturn(Response.Error(VersionError("[remote rejected]")))
                .thenReturn(Response.Success("that worked"))

        service.push("version")

        assertThat(outputStream.toString())
                .contains("Everything up-to-date", "[rejected]", "[remote rejected]", "Retrying...")

        System.setOut(originalOut)
    }

    @Test
    fun `push returns success`() {
        val success = Response.Success("that worked")
        whenever(client.execute(any())).thenReturn(success)

        val response = service.push("version")

        assertThat(response).isEqualTo(success)
    }

    @Test
    fun `commitsSince asks for the commits`() {
        service.commitsSince("abc123")

        verify(client).execute("cd ${service.gitRepoDir} ; git log --pretty=format:'%H'")
    }

    @Test
    fun `commitsSince returns the most recent commit`() {
        whenever(client.execute(any())).thenReturn(Response.Success("commit3\ncommit2\ncommit1"))

        val response = service.commitsSince("abc123").getSuccess()

        assertThat(response).containsExactly("commit3")
    }

    @Test
    fun `commitsSince returns an error if there is an error with git`() {
        whenever(client.execute(any())).thenReturn(Response.Error(VersionError("error")))

        val response = service.commitsSince("abc123").getError()

        assertThat(response.message).isEqualTo("error")
    }
}