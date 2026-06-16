package com.example.network

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String?, val exception: Exception? = null) : ApiResult<Nothing>()
    data class ApiException(val e: Throwable) : ApiResult<Nothing>()
}
