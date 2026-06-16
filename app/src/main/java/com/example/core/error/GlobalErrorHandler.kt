package com.example.core.error

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ErrorEvent(
    val error: AppError,
    val retryAction: (() -> Unit)? = null
)

object GlobalErrorHandler {
    private val _errorEvents = MutableSharedFlow<ErrorEvent>(replay = 0, extraBufferCapacity = 1)
    val errorEvents: SharedFlow<ErrorEvent> = _errorEvents.asSharedFlow()

    fun handleError(error: AppError, retryAction: (() -> Unit)? = null) {
        _errorEvents.tryEmit(ErrorEvent(error, retryAction))
    }

    fun handleThrowable(throwable: Throwable, retryAction: (() -> Unit)? = null) {
        val appError = ErrorMapper.mapThrowableToAppError(throwable)
        handleError(appError, retryAction)
    }
}
