package io.erthiscan.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.erthiscan.api.UserProfile
import io.erthiscan.auth.AuthManager
import io.erthiscan.auth.AuthState
import io.erthiscan.data.AuthRepository
import io.erthiscan.data.ReportsRepository
import io.erthiscan.ui.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PROFILE UI STATE: Represents the complete visual state of the Profile screen.
 * 
 * @property auth Current authentication status (logged in, tokens, etc.).
 * @property profile The fetched user profile data from the backend.
 * @property loading Indicates if a network request is currently in progress.
 * @property error Encapsulates transient errors for UI display.
 */
data class ProfileUiState(
    val auth: AuthState = AuthState(),
    val profile: UserProfile? = null,
    val loading: Boolean = false,
    val error: UiError? = null,
)

/**
 * PROFILE VIEW MODEL: Orchestrates user data, authentication, and report management.
 * 
 * ARCHITECTURAL ROLE:
 * Acts as the bridge between the authentication layer (AuthManager/AuthRepository) 
 * and the user-specific data layer (ReportsRepository). It maintains a reactive 
 * state that automatically updates when the user's login status changes.
 * 
 * KEY RESPONSIBILITIES:
 * 1. AUTH SYNC: Observes the global AuthManager to keep the local UI state in sync.
 * 2. DATA FETCHING: Handles profile retrieval and error mapping.
 * 3. ACCOUNT ACTIONS: Manages Google Sign-In and Logout workflows.
 * 4. CONTENT MANAGEMENT: Allows users to delete their own reports.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val reports: ReportsRepository,
    private val authManager: AuthManager,
) : ViewModel() {

    // BACKING PROPERTY: Private mutable state to prevent external modification.
    private val _ui = MutableStateFlow(ProfileUiState())
    // PUBLIC STATE: Immutable flow exposed to the Compose UI for observation.
    val ui: StateFlow<ProfileUiState> = _ui.asStateFlow()

    init {
        // AUTH OBSERVATION: 
        // We launch a collection on the global AuthManager state immediately.
        // This ensures that if the user logs out from another part of the app, 
        // the Profile screen reacts instantly without a manual refresh.
        viewModelScope.launch {
            authManager.state.collect { s -> _ui.update { it.copy(auth = s) } }
        }
    }

    /**
     * REFRESH: Re-fetches the user profile if they are authenticated.
     */
    fun refresh() = viewModelScope.launch {
        // GUARD: Prevents unnecessary API calls if the user isn't logged in.
        if (!authManager.state.value.isLoggedIn) return@launch
        
        _ui.update { it.copy(loading = true) }
        try {
            // API CALL: Retrieves the authenticated user's profile and reports.
            val p = reports.myProfile()
            _ui.update { it.copy(profile = p, loading = false) }
        } catch (e: Exception) {
            // ERROR HANDLING: Maps backend/network exceptions to a UI-friendly format.
            _ui.update { it.copy(loading = false, error = UiError.from(e)) }
        }
    }

    /**
     * SIGN IN WITH GOOGLE: Exchanges an ID token for a session with our backend.
     * 
     * @param idToken The credential retrieved from the Google Sign-In intent.
     */
    fun signInGoogle(idToken: String) = viewModelScope.launch {
        try {
            // DELEGATION: Offloads the token exchange to the AuthRepository.
            authRepo.signInWithGoogle(idToken)
            // REFRESH: Immediately fetch the new user's profile upon success.
            refresh()
        } catch (e: Exception) {
            _ui.update { it.copy(error = UiError.from(e)) }
        }
    }

    /**
     * LOGOUT: Clears local tokens and resets the UI state.
     */
    fun logout() = viewModelScope.launch {
        authRepo.logout()
        // RESET: Explicitly clear the profile to avoid showing stale data 
        // while the AuthManager collection (in init) is still propagating.
        _ui.update { it.copy(profile = null) }
    }

    /**
     * DELETE REPORT: Destructive action to remove a user's own contribution.
     * 
     * @param id The unique identifier of the report to be deleted.
     */
    fun deleteReport(id: Int) = viewModelScope.launch {
        try {
            reports.deleteReport(id)
            // SYNC: Re-fetch the profile to ensure the UI reflects the removal.
            refresh()
        } catch (e: Exception) {
            _ui.update { it.copy(error = UiError.from(e)) }
        }
    }

    /**
     * DISMISS ERROR: Clears the transient error state, typically after a Snackbar display.
     */
    fun dismissError() = _ui.update { it.copy(error = null) }
}
