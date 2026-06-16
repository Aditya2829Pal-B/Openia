package com.example.core.error

import com.example.network.ApiResult
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

sealed class AppError(override val message: String) : Exception(message) {
    class NetworkError(message: String = "No internet connection") : AppError(message)
    class ServerError(val code: Int, message: String) : AppError(message)
    class TimeoutError(message: String = "Request timed out") : AppError(message)
    class DatabaseError(message: String = "Database operation failed") : AppError(message)
    class AuthenticationError(message: String = "Authentication failed") : AppError(message)
    class PermissionError(message: String = "Permission denied") : AppError(message)
    class UnknownError(message: String = "An unexpected error occurred") : AppError(message)
}

object ErrorMapper {
    fun mapThrowableToAppError(throwable: Throwable): AppError {
        return when (throwable) {
            is IOException -> AppError.NetworkError()
            is SocketTimeoutException -> AppError.TimeoutError()
            is HttpException -> {
                when (throwable.code()) {
                    401, 403 -> AppError.AuthenticationError()
                    in 400..499 -> AppError.ServerError(throwable.code(), "Client error")
                    in 500..599 -> AppError.ServerError(throwable.code(), "Server error")
                    else -> AppError.UnknownError(throwable.message())
                }
            }
            is AppError -> throwable
            else -> AppError.UnknownError(throwable.message ?: "Unknown error")
        }
    }

    fun mapApiResultError(error: ApiResult.Error): AppError {
        return when (error.code) {
            401, 403 -> AppError.AuthenticationError()
            in 400..499 -> AppError.ServerError(error.code, error.message ?: "Client error")
            in 500..599 -> AppError.ServerError(error.code, error.message ?: "Server error")
            else -> AppError.UnknownError(error.message ?: "Unknown error")
        }
    }
}
