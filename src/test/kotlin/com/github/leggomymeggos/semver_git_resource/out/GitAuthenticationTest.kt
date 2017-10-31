package com.github.leggomymeggos.semver_git_resource.out

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.leggomymeggos.semver_git_resource.models.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class GitAuthenticationTest {
    private val VERSION_FILE = "number"
    private val VERSION_BRANCH = "version"

    private val mapper = ObjectMapper()
    private val originalOut = System.out!!
    private val originalIn = System.`in`!!
    private val outputStream = ByteArrayOutputStream()

    private val PROPERTIES = loadProperties()
    private val LOGS_DIR = "./src/test/logs/out"

    private fun loadProperties(): Map<String, String> =
            File("./src/test/resources/secret.properties").readText()
                    .split("\n")
                    .map { secret ->
                        val key = secret.substring(0, secret.indexOfFirst { it == '=' })
                        val value = secret.substring(secret.indexOfFirst { it == '=' } + 1)
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
        OutRequest(
                version = Version(number = "1.4.3", ref = ""),
                params = VersionParams(pre = "pre"),
                source = Source(
                        versionFile = VERSION_FILE,
                        versionBranch = VERSION_BRANCH,
                        uri = PROPERTIES["git.ssh.url"]!!,
                        privateKey = File(PROPERTIES["ssh.key.file"]).readText()
                )
        ).writeToStdIn()
        main(arrayOf())

        val jsonResult = outputStream.toString()
        val result: OutResponse = mapper.readValue(jsonResult.substring(jsonResult.indexOf("{"), jsonResult.lastIndexOf("\n")))
        assertThat(result.version.number).containsPattern("0.0.3-pre.(\\d+)") // if this didn't bump the number in the file on gitlab, it is at least read from the file
    }

    @Test
    fun `can authenticate with https`() {
        OutRequest(
                version = Version(number = "3.5.3", ref = ""),
                params = VersionParams(pre = "pre"),
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
        val result: OutResponse = mapper.readValue(jsonResult.substring(jsonResult.indexOf("{"), jsonResult.lastIndexOf("\n")))
        assertThat(result.version.number).matches("0.0.3-pre.(\\d+)") // if this didn't bump the number in the file on gitlab, it is at least read from the file
    }

    private fun OutRequest.writeToStdIn() {
        val jsonRequest = mapper.writeValueAsString(this)

        val inputStream = ByteArrayInputStream(jsonRequest.toByteArray())
        System.setIn(inputStream)
    }

    private fun createFile(filePath: String, fileName: String): File {
        val file = File(filePath)
        file.mkdirs()
        return File(file, fileName)
    }
}