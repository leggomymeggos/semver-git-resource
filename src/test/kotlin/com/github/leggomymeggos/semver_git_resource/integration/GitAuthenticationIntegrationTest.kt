package com.github.leggomymeggos.semver_git_resource.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.leggomymeggos.semver_git_resource.check.main
import com.github.leggomymeggos.semver_git_resource.models.CheckRequest
import com.github.leggomymeggos.semver_git_resource.models.Source
import com.github.leggomymeggos.semver_git_resource.models.Version
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class GitAuthenticationIntegrationTest {
    private val VERSION_FILE = "number"
    private val VERSION_BRANCH = "version"

    private val mapper = ObjectMapper()
    private val originalOut = System.out!!
    private val originalIn = System.`in`!!
    private val outputStream = ByteArrayOutputStream()

    private val PROPERTIES = loadProperties()

    private fun loadProperties(): Map<String, String> =
            File("./src/test/resources/secret.properties").readText()
                    .split("\n")
                    .map { secret ->
                        val key = secret.substring(0, secret.indexOfFirst{it == '='})
                        val value = secret.substring(secret.indexOfFirst{it == '='} + 1)
                        key to value
                    }.toMap()

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
    fun `can authenticate with ssh`() {
        CheckRequest(
                version = null,
                source = Source(
                        versionFile = VERSION_FILE,
                        versionBranch = VERSION_BRANCH,
                        uri = PROPERTIES["git.ssh.url"]!!,
                        privateKey = File(PROPERTIES["ssh.key.file"]).readText()
                )
        ).writeToStdIn()
        main(arrayOf())

        val jsonResult = outputStream.toString()
        val result: List<Version> = mapper.readValue(jsonResult.substring(jsonResult.indexOf("["), jsonResult.lastIndexOf("\n")))

        assertThat(result[0].number).isEqualTo("0.0.1") // the number in the version file
    }

    @Test
    fun `can authenticate with https`() {
        CheckRequest(
                version = null,
                source = Source(
                        versionFile = VERSION_FILE,
                        versionBranch = VERSION_BRANCH,
                        uri = PROPERTIES["git.https.url"]!!,
                        username = PROPERTIES["git.username"],
                        password = PROPERTIES["git.password"]
                )
        ).writeToStdIn()
        main(arrayOf())

        val jsonResult = outputStream.toString()
        val result: List<Version> = mapper.readValue(jsonResult.substring(jsonResult.indexOf("["), jsonResult.lastIndexOf("\n")))

        assertThat(result[0].number).isEqualTo("0.0.1") // the number in the version file
    }

    private fun CheckRequest.writeToStdIn() {
        val jsonRequest = mapper.writeValueAsString(this)

        val inputStream = ByteArrayInputStream(jsonRequest.toByteArray())
        System.setIn(inputStream)
    }
}