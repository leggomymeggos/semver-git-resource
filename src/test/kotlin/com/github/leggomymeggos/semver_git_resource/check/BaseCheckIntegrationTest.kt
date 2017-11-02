package com.github.leggomymeggos.semver_git_resource.check

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.leggomymeggos.semver_git_resource.models.CheckRequest
import com.github.leggomymeggos.semver_git_resource.models.Version
import khttp.post
import org.junit.After
import org.junit.Before
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

abstract class BaseCheckIntegrationTest {
    protected val SOURCE_BRANCH = "source"
    protected val VERSION_BRANCH = "version"
    protected val VERSION_FILE = "number"

    private val mapper = ObjectMapper()
    private val originalOut = System.out!!
    val outputStream = ByteArrayOutputStream()

    private val originalIn = System.`in`
    protected var gitUrl: String = ""
    protected val tempGitRepo = createTempDir()

    companion object {
        fun createFile(filePath: String, fileName: String): File {
            val file = File(filePath)
            file.mkdirs()
            return File(file, fileName)
        }
    }

    @Before
    fun `set up`() {
        gitUrl = refreshGitUrl()
        setUpTempGitRepo()

        mapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE

        System.setOut(PrintStream(outputStream))
    }

    @After
    fun `tear down`() {
        System.setIn(originalIn)
        System.setOut(originalOut)
    }

    abstract fun logsDir() : String

    protected fun getResult(): List<Version> {
        val jsonResult = outputStream.toString()
        return mapper.readValue(jsonResult.substring(jsonResult.indexOf("["), jsonResult.lastIndexOf("\n")))
    }

    protected fun CheckRequest.writeToStdIn() {
        val jsonRequest = mapper.writeValueAsString(this)

        val inputStream = ByteArrayInputStream(jsonRequest.toByteArray())
        System.setIn(inputStream)
    }

    private fun refreshGitUrl(): String {
        val response = post(url = "http://localhost:3000/", headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"))
        val text = response.text
        return "${text.substring(text.lastIndexOf("http://localhost"), text.lastIndexOf(".git"))}.git" // substring is exclusive
    }

    private fun setUpTempGitRepo() {
        ProcessBuilder(
                "/bin/sh", "-c",
                "cd ${tempGitRepo.path} ; " +
                        "git init ; " +
                        "git remote add origin $gitUrl ; " +
                        "git commit --allow-empty -m \"first commit\" ; " +
                        "git push -u origin master ; " +
                        "git checkout -b $SOURCE_BRANCH ; " +
                        "git commit --allow-empty -m \"add branch $SOURCE_BRANCH\" ; " +
                        "git push -u origin $SOURCE_BRANCH ; " +
                        "git checkout -b $VERSION_BRANCH ; " +
                        "git commit --allow-empty -m \"add branch $VERSION_BRANCH\" ; " +
                        "git push -u origin $VERSION_BRANCH")
                .redirectOutput(createFile("${logsDir()}/git", "setup_repo.txt"))
                .redirectError(createFile("${logsDir()}/git", "setup_repo_error.txt"))
                .start()
                .waitFor()
    }
}