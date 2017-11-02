package com.github.leggomymeggos.semver_git_resource.check

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.leggomymeggos.semver_git_resource.client.EnvironmentService
import com.github.leggomymeggos.semver_git_resource.client.GitService
import com.github.leggomymeggos.semver_git_resource.driver.DriverFactoryImpl
import com.github.leggomymeggos.semver_git_resource.models.CheckRequest
import com.github.leggomymeggos.semver_git_resource.models.getError
import com.github.leggomymeggos.semver_git_resource.models.getSuccess
import java.io.BufferedReader
import java.io.InputStreamReader

fun main(args: Array<String>) {
    val mapper = ObjectMapper()
    mapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE

    val reader = BufferedReader(InputStreamReader(System.`in`))

    val request = mapper.readValue<CheckRequest>(reader.readLines().joinToString())

    val result = CheckService(
            driverFactory = DriverFactoryImpl(),
            envService = EnvironmentService(GitService())
    ).check(request)

    try {
        println(mapper.writeValueAsString(result.getSuccess()))
    } catch (e: Exception) {
        val error = result.getError()
        println(error.message)
        println(error.exception?.printStackTrace() ?: "")
        println("[]")
    }
}
