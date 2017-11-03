package com.github.leggomymeggos.semver_git_resource.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.leggomymeggos.semver_git_resource.client.GitAuthenticationService
import com.github.leggomymeggos.semver_git_resource.client.GitService
import com.github.leggomymeggos.semver_git_resource.driver.DriverFactoryImpl
import com.github.leggomymeggos.semver_git_resource.models.BumpFactory
import com.github.leggomymeggos.semver_git_resource.models.InRequest
import com.github.leggomymeggos.semver_git_resource.models.getError
import com.github.leggomymeggos.semver_git_resource.models.getSuccess
import java.io.BufferedReader
import java.io.InputStreamReader
import com.github.zafarkhaja.semver.Version as SemVer

fun main(args: Array<String>) {
    val mapper = ObjectMapper()
    mapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
    if (args.size < 2) {
        println("usage: ${args[0]} <destination>")
        throw Exception("received wrong number of args")
    }

    val reader = BufferedReader(InputStreamReader(System.`in`))
    val request = mapper.readValue<InRequest>(reader.readLines().joinToString())

    val inResponse = InService(
            authService = GitAuthenticationService(GitService()),
            bumpFactory = BumpFactory(),
            driverFactory = DriverFactoryImpl()
    ).read(request, args[1])
    try {
        println(mapper.writeValueAsString(inResponse.getSuccess()))
    } catch (e: Exception) {
        println(inResponse.getError().message)
        inResponse.getError().exception?.printStackTrace()
    }
}