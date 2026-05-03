package io.erthiscan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.erthiscan.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * STARTUP VIEW MODEL: The architectural gatekeeper for the app's initial state.
 * 
 * DESIGN GOAL: 
 * This ViewModel ensures that the application does not render its primary UI until 
 * the underlying authentication and configuration state is fully restored from 
 * encrypted storage.
 * 
 * WORKFLOW:
 * 1. INITIALIZATION: Triggered immediately upon MainActivity creation via Hilt.
 * 2. RESTORATION: Calls [AuthManager.restore] to load encrypted tokens and sync with the backend.
 * 3. SIGNALING: Updates [isRestored] to 'true', which MainActivity observes to swap 
 *    the manual splash screen for the main UI.
 */
@HiltViewModel
class StartupViewModel @Inject constructor(
    // AUTH MANAGER: Injected singleton that handles encrypted storage and JWT logic.
    private val authManager: AuthManager
) : ViewModel() {
    
    // BACKING PROPERTY: 
    // Private MutableStateFlow prevents external components from prematurely 
    // signaling that the session is restored.
    private val _isRestored = MutableStateFlow(false)
    
    /**
     * IS RESTORED: A reactive stream that emits 'true' once the session is ready.
     * Observed by MainActivity's setContent block via collectAsStateWithLifecycle.
     */
    val isRestored: StateFlow<Boolean> = _isRestored.asStateFlow()

    init {
        // COROUTINE SCOPE: 
        // viewModelScope ensures the restoration task is cancelled if the user 
        // closes the app before it completes.
        viewModelScope.launch {
            // BLOCKING OPERATION (IO): 
            // AuthManager performs DataStore reads and potentially network JWT refreshes.
            // Execution suspends here until 'restore()' returns.
            authManager.restore()
            
            // COMPLETION: 
            // Once tokens are decrypted and validated, we signal the UI to proceed.
            _isRestored.value = true
        }
    }
}