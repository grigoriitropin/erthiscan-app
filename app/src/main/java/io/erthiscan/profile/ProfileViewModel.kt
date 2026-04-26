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

data class ProfileUiState(
    val auth: AuthState = AuthState(),
    val profile: UserProfile? = null,
    val loading: Boolean = false,
    val error: UiError? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val reports: ReportsRepository,
    private val authManager: AuthManager,
) : ViewModel() {

    private val _ui = MutableStateFlow(ProfileUiState())
    val ui: StateFlow<ProfileUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            authManager.state.collect { s -> _ui.update { it.copy(auth = s) } }
        }
    }

    fun refresh() = viewModelScope.launch {
        if (!authManager.state.value.isLoggedIn) return@launch
        _ui.update { it.copy(loading = true) }
        try {
            val p = reports.myProfile()
            _ui.update { it.copy(profile = p, loading = false) }
        } catch (e: Exception) {
            _ui.update { it.copy(loading = false, error = UiError.from(e)) }
        }
    }

    fun signInGoogle(idToken: String) = viewModelScope.launch {
        try {
            authRepo.signInWithGoogle(idToken)
            refresh()
        } catch (e: Exception) {
            _ui.update { it.copy(error = UiError.from(e)) }
        }
    }

    fun logout() = viewModelScope.launch {
        authRepo.logout()
        _ui.update { it.copy(profile = null) }
    }

    fun deleteReport(id: Int) = viewModelScope.launch {
        try {
            reports.deleteReport(id)
            refresh()
        } catch (e: Exception) {
            _ui.update { it.copy(error = UiError.from(e)) }
        }
    }

    fun dismissError() = _ui.update { it.copy(error = null) }
}
