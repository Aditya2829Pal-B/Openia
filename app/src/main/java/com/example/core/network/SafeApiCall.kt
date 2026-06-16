package com.example.core.network

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

suspend fun <T> safeApiCall(
    dispatcher: CoroutineDispatcher,
    apiCall: suspend () -> T
): ApiResult<T> {
    return withContext(dispatcher) {
        try {
            ApiResult.Success(apiCall())
        } catch (e: HttpException) {
            ApiResult.Error(code = e.code(), message = e.message(), exception = e)
        } catch (e: IOException) {
            ApiResult.Error(message = "Network error. Please check your connection.", exception = e)
        } catch (e: Exception) {
            ApiResult.Error(message = "An unknown error occurred.", exception = e)
        }
    }
}
