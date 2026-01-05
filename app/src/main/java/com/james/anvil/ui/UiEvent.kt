package com.james.anvil.ui

/**
 * Sealed class representing UI events that can be consumed by the UI layer.
 * These are one-shot events that should trigger UI actions like showing
 * Snackbars, navigating, or displaying dialogs.
 */
sealed class UiEvent {
    /**
     * Show a Snackbar message.
     */
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null
    ) : UiEvent()
    
    /**
     * Navigate to a specific destination.
     */
    data class Navigate<T : Any>(val route: T) : UiEvent()
    
    /**
     * Navigate back in the navigation stack.
     */
    data object NavigateBack : UiEvent()
    
    /**
     * Show an error with optional retry action.
     */
    data class ShowError(
        val message: String,
        val throwable: Throwable? = null,
        val onRetry: (() -> Unit)? = null
    ) : UiEvent()
    
    /**
     * Show a success confirmation.
     */
    data class ShowSuccess(val message: String) : UiEvent()
}

/**
 * Standard UI state wrapper that includes loading and error states.
 */
sealed class UiState<out T> {
    /**
     * Initial loading state before data is available.
     */
    data object Loading : UiState<Nothing>()
    
    /**
     * Successfully loaded data.
     */
    data class Success<T>(val data: T) : UiState<T>()
    
    /**
     * Error state with message and optional retry.
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : UiState<Nothing>()
    
    /**
     * Empty state when data is successfully fetched but there's nothing to show.
     */
    data object Empty : UiState<Nothing>()
    
    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isEmpty: Boolean get() = this is Empty
    
    /**
     * Returns the data if in Success state, null otherwise.
     */
    fun getOrNull(): T? = (this as? Success)?.data
}
