package com.github.leggomymeggos.semver_git_resource.check

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.leggomymeggos.semver_git_resource.models.CheckRequest

fun main(args: Array<String>) {
    val mapper = ObjectMapper()
    mapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE

    val request = mapper.readValue<CheckRequest>(args[0])

    val result = Checker(DriverFactoryImpl()).check(request)

    println(mapper.writeValueAsString(result))
}
