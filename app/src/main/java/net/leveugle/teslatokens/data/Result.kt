package net.leveugle.teslatokens.data

import java.lang.Exception

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val error: Exception) : Result<Nothing>() {
        constructor(errMsg: String): this(Exception(errMsg))
    }
}