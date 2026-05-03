package io.erthiscan.company

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.erthiscan.api.CompanyItem
import io.erthiscan.data.CompaniesRepository
import io.erthiscan.ui.UiError
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * COMPANIES STATE: Represents the current UI state for the company listing.
 */
data class CompaniesState(
    val companies: List<CompanyItem> = emptyList(),
    val query: String = "",
    val sort: String = "reports_desc",
    val page: Int = 1,
    val pages: Int = 1,
    val loading: Boolean = true,
    val error: UiError? = null,
)

/**
 * COMPANIES VIEW MODEL: Manages the business logic for searching and sorting companies.
 * 
 * REACTIVE ARCHITECTURE:
 * This ViewModel uses a "Trigger-Response" pattern. It exposes three private flows 
 * (query, sort, page) and combines them into a single reactive pipeline.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class CompaniesViewModel @Inject constructor(
    // Repository handles the actual network abstraction.
    private val repo: CompaniesRepository,
) : ViewModel() {

    // UI STATE: The single source of truth for the View.
    // MutableStateFlow: Thread-safe, observable state holder.
    private val _state = MutableStateFlow(CompaniesState())
    
    /**
     * STATE: Public read-only flow observed by the Compose UI.
     * asStateFlow(): Prevents callers from modifying the state directly.
     */
    val state: StateFlow<CompaniesState> = _state.asStateFlow()

    // INPUT FLOWS: Internal triggers that drive the data pipeline.
    private val queryFlow = MutableStateFlow("")
    private val sortFlow = MutableStateFlow("reports_desc")
    private val pageFlow = MutableStateFlow(1)

    init {
        // COROUTINE SCOPE: ViewModel-bound lifecycle; cancels automatically on destruction.
        viewModelScope.launch {
            // REACTIVE PIPELINE:
            // combine: Synchronizes the three trigger flows. When ANY one of them 
            // changes, the entire pipeline is re-evaluated.
            combine(
                // DEBOUNCE (300ms): 
                // Only proceeds if the user hasn't typed anything for 300ms. 
                // This prevents firing an API request for every single keystroke.
                queryFlow.debounce(300).distinctUntilChanged(),
                sortFlow,
                pageFlow,
            ) { q, s, p -> 
                // Transform current flow values into a Triple payload.
                Triple(q, s, p) 
            }
                // COLLECT LATEST:
                // If a previous 'load' request is still running when a new Triple 
                // arrives, the old coroutine is immediately cancelled and 'load' 
                // starts fresh. This prevents "out of order" data UI glitches.
                .collectLatest { (q, s, p) ->
                    load(q, s, p)
                }
        }
    }

    /**
     * LOAD: Performs the actual network I/O.
     * 
     * WORKFLOW:
     * 1. Trigger 'loading' state to show UI progress indicators.
     * 2. Call the repository to fetch data from the API.
     * 3. Atomically update the state with results or error mapping.
     */
    private suspend fun load(q: String, s: String, p: Int) {
        // UPDATE: Atomic transition to loading state.
        _state.update { it.copy(loading = true) }
        
        try {
            // NETWORK I/O: Suspends the coroutine until the response arrives.
            val resp = repo.list(q, s, p)
            
            // SUCCESS: Populate state with company list and pagination metadata.
            _state.update {
                it.copy(
                    companies = resp.items,
                    pages = resp.pages,
                    query = q,
                    sort = s,
                    page = p,
                    loading = false,
                    error = null, // Clear any previous transient errors.
                )
            }
        } catch (e: Exception) {
            // FAILURE: Map backend/network exceptions to user-friendly UI errors.
            _state.update { it.copy(loading = false, error = UiError.from(e)) }
        }
    }

    /**
     * ON QUERY CHANGE: 
     * Updates the search trigger and resets to page 1 to ensure the user 
     * starts from the top of the new search results.
     */
    fun onQueryChange(q: String) {
        queryFlow.value = q
        pageFlow.value = 1
    }

    /**
     * ON SORT CHANGE: 
     * Updates the sorting trigger and resets pagination.
     */
    fun onSortChange(s: String) {
        sortFlow.value = s
        pageFlow.value = 1
    }

    // PAGINATION ACTIONS
    // Synchronous state reads + flow value updates. 
    // This will trigger the 'combine' pipeline in 'init'.
    fun nextPage() { if (_state.value.page < _state.value.pages) pageFlow.value = _state.value.page + 1 }
    fun prevPage() { if (_state.value.page > 1) pageFlow.value = _state.value.page - 1 }
    
    /**
     * DISMISS ERROR: Reset the error state to 'null'.
     * Used to hide the Snackbar after the user interacts with it or it times out.
     */
    fun dismissError() = _state.update { it.copy(error = null) }
}