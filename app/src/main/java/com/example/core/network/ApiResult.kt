package com.example.core.network

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(
        val code: Int? = null,
        val message: String,
        val exception: Throwable? = null
    ) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}
