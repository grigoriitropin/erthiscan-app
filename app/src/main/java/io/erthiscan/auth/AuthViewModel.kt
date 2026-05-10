package io.erthiscan.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.erthiscan.data.AuthRepository
import io.erthiscan.ui.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AUTH VIEW MODEL: Global coordinator for authentication actions.
 * 
 * ARCHITECTURAL ROLE:
 * Provides a unified way to trigger Google Sign-In and observe login state 
 * from any screen in the application. This prevents duplication of 
 * CredentialManager handling and session token exchange logic.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val authManager: AuthManager,
) : ViewModel() {

    // REACTIVE STATE: Exposes the current AuthState from AuthManager.
    val state: StateFlow<AuthState> = authManager.state

    private val _error = MutableStateFlow<UiError?>(null)
    val error: StateFlow<UiError?> = _error.asStateFlow()

    /**
     * SIGN IN WITH GOOGLE: Exchanges an ID token for a session.
     */
    fun signInGoogle(idToken: String) = viewModelScope.launch {
        try {
            authRepo.signInWithGoogle(idToken)
        } catch (e: Exception) {
            _error.update { UiError.from(e) }
        }
    }

    /**
     * DISMISS ERROR: Clears transient auth-related errors.
     */
    fun dismissError() = _error.update { null }
}
