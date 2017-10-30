package com.github.leggomymeggos.semver_git_resource.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.github.leggomymeggos.semver_git_resource.models.InRequest
import com.github.leggomymeggos.semver_git_resource.models.Source
import com.github.leggomymeggos.semver_git_resource.models.Version
import com.github.leggomymeggos.semver_git_resource.models.VersionParams
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Files

class InIntegrationTest {

    private val mapper = ObjectMapper()
    private val originalOut = System.out!!
    private val outputStream = ByteArrayOutputStream()

    private val originalIn = System.`in`
    private val destination = Files.createTempDirectory("tmp").toAbsolutePath().toString()

    @Before
    fun `set up`() {
        mapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE

        System.setOut(PrintStream(outputStream))
    }

    @After
    fun `tear down`() {
        System.setIn(originalIn)
        System.setOut(originalOut)
    }

    @Test
    fun `requires a destination`() {
        try {
            main(arrayOf("first arg"))
        } catch (e: Exception) {
            assertThat(e.message).isEqualTo("received wrong number of args")
        }

        assertThat(outputStream.toString()).contains("usage: first arg <destination>")
    }

    @Test
    fun `prints patch version bump`() {
        InRequest(
                version = Version(number = "1.5.3", ref = ""),
                params = VersionParams(bump = "patch"),
                source = createSource()
        ).writeToStdIn()

        main(arrayOf("", ""))

        assertThat(outputStream.toString()).contains("bumped version locally from 1.5.3 to 1.5.4")
    }

    @Test
    fun `prints minor version bump`() {
        InRequest(
                version = Version(number = "1.5.3", ref = ""),
                params = VersionParams(bump = "minor"),
                source = createSource()
        ).writeToStdIn()

        main(arrayOf("", ""))

        assertThat(outputStream.toString()).contains("bumped version locally from 1.5.3 to 1.6.0")
    }

    @Test
    fun `prints major version bump`() {
        InRequest(
                version = Version(number = "1.5.3", ref = ""),
                params = VersionParams(bump = "major"),
                source = createSource()
        ).writeToStdIn()

        main(arrayOf("", ""))

        assertThat(outputStream.toString()).contains("bumped version locally from 1.5.3 to 2.0.0")
    }

    @Test
    fun `prints version bump with prerelease tags`() {
        InRequest(
                version = Version(number = "1.5.3", ref = ""),
                params = VersionParams(pre = "rc"),
                source = createSource()
        ).writeToStdIn()

        main(arrayOf("", ""))

        assertThat(outputStream.toString()).contains("bumped version locally from 1.5.3 to 1.5.3-rc.1")
    }

    @Test
    fun `prints version bump with incremented prerelease tags`() {
        InRequest(
                version = Version(number = "1.5.3-rc.1", ref = ""),
                params = VersionParams(pre = "rc"),
                source = createSource()
        ).writeToStdIn()

        main(arrayOf("", ""))

        assertThat(outputStream.toString()).contains("bumped version locally from 1.5.3-rc.1 to 1.5.3-rc.2")
    }

    @Test
    fun `prints final version bump`() {
        InRequest(
                version = Version(number = "1.5.3-rc.1", ref = ""),
                params = VersionParams(bump = "final"),
                source = createSource()
        ).writeToStdIn()

        main(arrayOf("", ""))

        assertThat(outputStream.toString()).contains("bumped version locally from 1.5.3-rc.1 to 1.5.3")
    }

    @Test
    fun `does not print anything if the request contains no versioning params`() {
        InRequest(
                version = Version(number = "1.5.3", ref = ""),
                params = VersionParams(),
                source = createSource()
        ).writeToStdIn()

        main(arrayOf("", ""))

        assertThat(outputStream.toString()).doesNotContain("bumped")
    }

    @Test
    fun `saves the bumped version in a number and a version file in the destination`() {
        InRequest(
                version = Version(number = "1.5.3", ref = ""),
                params = VersionParams(bump = "minor", pre = "pre"),
                source = createSource()
        ).writeToStdIn()

        main(arrayOf("", destination))

        assertThat(File(destination, "version").readText()).contains("1.6.0-pre.1")
        assertThat(File(destination, "number").readText()).contains("1.6.0-pre.1")
    }

    @Test
    fun `saves the current version in a number and a version file in the destination`() {
        InRequest(
                version = Version(number = "1.5.3", ref = ""),
                params = VersionParams(),
                source = createSource()
        ).writeToStdIn()

        main(arrayOf("", destination))
        
        assertThat(File(destination, "version").readText()).contains("1.5.3")
        assertThat(File(destination, "number").readText()).contains("1.5.3")
    }

    private fun createSource(): Source = Source(uri = "", versionFile = "")

    private fun InRequest.writeToStdIn() {
        val jsonRequest = mapper.writeValueAsString(this)

        val inputStream = ByteArrayInputStream(jsonRequest.toByteArray())
        System.setIn(inputStream)
    }
}