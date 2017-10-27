package com.github.leggomymeggos.semver_git_resource.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.leggomymeggos.semver_git_resource.models.BumpFactory
import com.github.leggomymeggos.semver_git_resource.models.InRequest
import java.io.BufferedReader
import java.io.InputStreamReader
import com.github.zafarkhaja.semver.Version as SemVer

fun main(args: Array<String>) {
    val mapper = ObjectMapper()
    mapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
    if(args.size < 2) {
        println("usage: ${args[0]} <destination>")
        throw Exception("received wrong number of args")
    }

    val reader = BufferedReader(InputStreamReader(System.`in`))
    val input = reader.readLines().joinToString()

    val request = mapper.readValue<InRequest>(input)

    val inputVersion = SemVer.valueOf(request.version.number)
    val version = BumpFactory().create(request.params.bump, request.params.pre).apply(inputVersion)

    println("bumped version locally from $inputVersion to $version")
}
