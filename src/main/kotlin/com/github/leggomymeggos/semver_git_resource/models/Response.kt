package com.github.leggomymeggos.semver_git_resource.models

sealed class Response<out S, out E> {
    class Success<out S>(val value: S) : Response<S, Nothing>()
    class Error<out E>(val error: E) : Response<Nothing, E>()
}

fun <S, E> Response<S, E>.getSuccess(): S {
    return when (this) {
        is Response.Success -> value
        is Response.Error -> throw IllegalStateException("expected a success; got an error: $error")
    }
}

inline fun <S, E, T> Response<S, E>.flatMap(block: (S) -> Response<T, E>): Response<T, E> {
    return when (this) {
        is Response.Success -> block(value)
        is Response.Error -> Response.Error(error)
    }
}

fun <S, E> Response<S, E>.getError(): E {
    return when (this) {
        is Response.Error -> error
        is Response.Success -> throw IllegalStateException("expected an error; got a success: $value")
    }
}

inline fun <S, E, T> Response<S, E>.flatMapError(block: (E) -> Response<S, T>): Response<S, T> {
    return when (this) {
        is Response.Error -> block(error)
        is Response.Success -> Response.Success(value)
    }
}