package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.error.GlobalErrorHandler
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Interface representing the UI State.
 */
interface UiState

/**
 * Interface representing UI Events (User Actions).
 */
interface UiEvent

/**
 * Interface representing One-Time UI Effects (Navigation, Snackbars, etc.).
 */
interface UiEffect

/**
 * Base ViewModel class that enforces MVI (Model-View-Intent) architectural patterns.
 * Provides structured state management and error handling across view models.
 */
abstract class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<E>()

    private val _uiEffect = Channel<F>()
    val uiEffect = _uiEffect.receiveAsFlow()

    protected val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        GlobalErrorHandler.handleThrowable(exception)
    }

    init {
        subscribeToEvents()
    }

    private fun subscribeToEvents() {
        viewModelScope.launch {
            _uiEvent.collect { event ->
                handleEvent(event)
            }
        }
    }

    /**
     * Define how the ViewModel handles specific events.
     */
    abstract fun handleEvent(event: E)

    /**
     * Dispatch a UI event to the ViewModel.
     */
    fun setEvent(event: E) {
        viewModelScope.launch { _uiEvent.emit(event) }
    }

    /**
     * Mutate the current UI state.
     */
    protected fun setState(reduce: S.() -> S) {
        _uiState.value = uiState.value.reduce()
    }

    /**
     * Send a one-time side effect to the UI.
     */
    protected fun setEffect(effect: F) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }
}
