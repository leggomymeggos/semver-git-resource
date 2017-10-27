package com.github.leggomymeggos.semver_git_resource.check

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.leggomymeggos.semver_git_resource.models.CheckError
import com.github.leggomymeggos.semver_git_resource.models.CheckRequest
import com.github.leggomymeggos.semver_git_resource.models.Version
import java.io.BufferedReader
import java.io.InputStreamReader

fun main(args: Array<String>) {
    val mapper = ObjectMapper()
    mapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE

    val reader = BufferedReader(InputStreamReader(System.`in`))
    val input = reader.readLines().joinToString()

    val request = mapper.readValue<CheckRequest>(input)

    val result: Response<List<Version>, CheckError> = Checker(DriverFactoryImpl()).check(request)
    try {
        println(mapper.writeValueAsString(result.getSuccess()))
    } catch (e: Exception) {
        val error = result.getError()
        println(error.message)
        println(error.exception?.printStackTrace() ?: "")
        println("[]")
    }
}
