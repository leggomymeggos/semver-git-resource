package com.github.leggomymeggos.semver_git_resource.out

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.leggomymeggos.semver_git_resource.models.OutRequest
import com.github.leggomymeggos.semver_git_resource.models.OutResponse
import com.github.leggomymeggos.semver_git_resource.models.getError
import com.github.leggomymeggos.semver_git_resource.models.getSuccess
import java.io.BufferedReader
import java.io.InputStreamReader

fun main(args: Array<String>) {

    val mapper = ObjectMapper()
    mapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE

    val reader = BufferedReader(InputStreamReader(System.`in`))

    val request = mapper.readValue<OutRequest>(reader.readLines().joinToString())

    val service = OutService()
    val response = service.writeVersion(request)

    try {
        val outResponse = OutResponse(version = response.getSuccess(), metadata = emptyList())
        println(mapper.writeValueAsString(outResponse))
    } catch (e: Exception) {
        val error = response.getError()
        println(error.message)
        println(error.exception?.printStackTrace() ?: "")
    }
}
