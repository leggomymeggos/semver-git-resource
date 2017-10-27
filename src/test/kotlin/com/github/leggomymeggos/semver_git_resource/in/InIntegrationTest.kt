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
import java.io.PrintStream

class InIntegrationTest {

    private val mapper = ObjectMapper()
    private val originalOut = System.out!!
    private val outputStream = ByteArrayOutputStream()

    private val originalIn = System.`in`

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

    private fun createSource(): Source = Source(uri = "", versionFile = "")

    private fun InRequest.writeToStdIn() {
        val jsonRequest = mapper.writeValueAsString(this)

        val inputStream = ByteArrayInputStream(jsonRequest.toByteArray())
        System.setIn(inputStream)
    }
}