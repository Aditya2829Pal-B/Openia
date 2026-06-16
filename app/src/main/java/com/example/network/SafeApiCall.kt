package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response

suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): ApiResult<T> {
    return withContext(Dispatchers.IO) {
        try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    ApiResult.Success(body)
                } else {
                    ApiResult.Error(response.code(), "Response body is null")
                }
            } else {
                ApiResult.Error(response.code(), response.message())
            }
        } catch (e: HttpException) {
            ApiResult.Error(e.code(), e.message(), e)
        } catch (e: Exception) {
            ApiResult.ApiException(e)
        }
    }
}
