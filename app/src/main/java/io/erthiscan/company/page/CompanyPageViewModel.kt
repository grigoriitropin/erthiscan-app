package io.erthiscan.company.page

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.erthiscan.api.CompanyDetail
import io.erthiscan.data.CompaniesRepository
import io.erthiscan.data.ReportsRepository
import io.erthiscan.nav.Route
import io.erthiscan.ui.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * COMPANY PAGE STATE: The source of truth for the company details screen.
 * 
 * @property company The detailed model including hierarchy of reports and current scores. 
 *                   Null initially or if loading failed.
 * @property loading Global indicator for data fetching or mutation synchronizing.
 * @property error Domain-mapped error state, consumed by the UI to show Snackbars.
 */
data class CompanyPageState(
    val company: CompanyDetail? = null,
    val loading: Boolean = true,
    val error: UiError? = null,
)

/**
 * COMPANY PAGE VIEW MODEL: Orchestrates data flow and mutations for a specific company.
 * 
 * ARCHITECTURE & LIFECYCLE:
 * 1. TYPE-SAFE NAVIGATION: Uses [SavedStateHandle.toRoute] to extract the [companyId] 
 *    directly from the Navigation component's arguments.
 * 2. PERSISTENCE: Because it uses [SavedStateHandle], the [companyId] is preserved 
 *    even if the Android OS kills the process in the background.
 * 3. STATE FLOW: Emits a single [CompanyPageState] object. The UI reacts to changes 
 *    in this object using 'collectAsStateWithLifecycle'.
 */
@HiltViewModel
class CompanyPageViewModel @Inject constructor(
    // Repositories injected via Hilt (SingletonComponent).
    private val companies: CompaniesRepository,
    private val reports: ReportsRepository,
    // SAVED STATE HANDLE: Provided by Hilt/Navigation; stores activity-scoped arguments.
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // ROUTE EXTRACTION: 
    // Uses Kotlin Serialization support in Compose Navigation to pull the type-safe 
    // Route object out of the saved state.
    private val route = savedStateHandle.toRoute<Route.Company>()
    private val companyId = route.companyId

    // INTERNAL STATE: Thread-safe holder for UI-visible properties.
    private val _state = MutableStateFlow(CompanyPageState())
    
    /**
     * STATE: Public read-only flow observed by the Compose UI.
     */
    val state: StateFlow<CompanyPageState> = _state.asStateFlow()

    init { 
        // INITIAL LOAD: 
        // Automatically fetch data as soon as the ViewModel is created. 
        // This ensures the user sees data immediately upon navigation.
        refresh() 
    }

    /**
     * REFRESH: Re-fetches the company detail from the network.
     * 
     * WORKFLOW:
     * 1. Set 'loading = true' to show progress in the UI.
     * 2. Perform network I/O via the [CompaniesRepository].
     * 3. On success: Update the state with new company data and clear any previous errors.
     * 4. On failure: Map the exception to [UiError] and stop the loading indicator.
     */
    fun refresh() = viewModelScope.launch {
        // Atomic state transition.
        _state.update { it.copy(loading = true) }
        
        try {
            // Suspends until network response or timeout.
            val c = companies.detail(companyId)
            
            // Populate state with fresh hierarchical data.
            _state.update { it.copy(company = c, loading = false, error = null) }
        } catch (e: Exception) {
            // Stop loading and propagate error to the UI.
            _state.update { it.copy(loading = false, error = UiError.from(e)) }
        }
    }

    /**
     * VOTE: Sends an upvote (1) or downvote (-1) for a specific report/challenge.
     * 
     * SYNC STRATEGY (Pessimistic):
     * After the API successfully records the vote, we call [refresh] to reload 
     * the entire company data. This ensures that the local ethical score and 
     * vote counts are perfectly synchronized with the backend's recalculated values 
     * without duplicating complex calculation logic on the client side.
     */
    fun vote(reportId: Int, value: Int) = viewModelScope.launch {
        try {
            // POST request to backend.
            reports.vote(reportId, value)
            
            // Backend update successful -> Trigger UI refresh to show new counts/score.
            refresh()
        } catch (e: Exception) {
            // Error mapping: typically handles 401 (Auth required), 404, or 422.
            _state.update { it.copy(error = UiError.from(e)) }
        }
    }

    /**
     * DELETE: Permanently removes a report or challenge submitted by the current user.
     * 
     * NOTE: 
     * The backend performs the ownership/permission check. If successful, we 
     * refresh the state to physically remove the item from the displayed list.
     */
    fun delete(reportId: Int) = viewModelScope.launch {
        try {
            // DELETE request to backend.
            reports.deleteReport(reportId)
            
            // Sync with backend to reflect the removal in the ethical score.
            refresh()
        } catch (e: Exception) {
            // Standard error propagation.
            _state.update { it.copy(error = UiError.from(e)) }
        }
    }

    /**
     * DISMISS ERROR: Reset the error state to 'null'.
     * Called by the View after a Snackbar is automatically timed out or dismissed.
     */
    fun dismissError() = _state.update { it.copy(error = null) }
}
