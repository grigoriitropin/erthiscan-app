package io.erthiscan.company.page

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.erthiscan.R
import io.erthiscan.api.CreateReportRequest
import io.erthiscan.api.UpdateReportRequest
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
 * CREATE REPORT UI STATE
 * Represents the transient state of the report submission form.
 * 
 * @property text The primary claim or challenge text being written.
 * @property source The evidence URL supporting the claim.
 * @property submitting Boolean flag to drive the "Loading" state on the submit button.
 * @property error Encapsulated UI error (Resource or String) for snackbar display.
 * @property submitted Boolean flag used to trigger navigation back to the previous screen.
 */
data class CreateReportUi(
    val text: String = "",
    val source: String = "",
    val submitting: Boolean = false,
    val error: UiError? = null,
    val submitted: Boolean = false,
)

/**
 * CREATE REPORT VIEWMODEL
 * 
 * ARCHITECTURAL ROLE:
 * Handles the logic for creating new reports, challenges (nested reports), and 
 * editing existing entries. It acts as a bridge between the [CreateReportScreen] 
 * and the [ReportsRepository].
 * 
 * CONTEXTUAL DATA:
 * It utilizes [SavedStateHandle] to retrieve [Route.CreateReport] arguments, 
 * allowing the form to be dynamically configured based on whether it is a:
 * 1. NEW REPORT: companyId is set, parentId is null.
 * 2. NEW CHALLENGE: companyId is set, parentId is the ID of the claim being challenged.
 * 3. EDIT: editReportId is set, initialText and initialSource are pre-filled.
 */
@HiltViewModel
class CreateReportViewModel @Inject constructor(
    private val reports: ReportsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // NAVIGATION ARGUMENTS: Extracted directly from the route definition for type safety.
    private val route = savedStateHandle.toRoute<Route.CreateReport>()
    val companyId = route.companyId
    val parentId = route.parentId
    val editReportId = route.editReportId

    // REACTIVE STATE: Initialized with pre-filled data for Edit flows or empty strings for New flows.
    private val _state = MutableStateFlow(
        CreateReportUi(text = route.initialText, source = route.initialSource)
    )
    val state: StateFlow<CreateReportUi> = _state.asStateFlow()

    /**
     * Updates the report text with a strict 150-character limit.
     * WHY: To encourage concise, data-driven claims and prevent "wall of text" spam.
     */
    fun onText(t: String) { if (t.length <= 150) _state.update { it.copy(text = t) } }

    /**
     * Updates the source URL field.
     */
    fun onSource(s: String) = _state.update { it.copy(source = s) }

    /**
     * SUBMIT FLOW:
     * 1. VALIDATION: Ensures required fields are not blank before hitting the network.
     * 2. LOADING STATE: Prevents double-submissions.
     * 3. API ROUTING: Automatically chooses between 'create' and 'update' based on [editReportId].
     */
    fun submit() = viewModelScope.launch {
        val s = _state.value
        
        // CLIENT-SIDE VALIDATION: Fast feedback before network calls.
        if (s.text.isBlank()) {
            _state.update { it.copy(error = UiError.Resource(R.string.report_text_required)) }
            return@launch
        }
        if (s.source.isBlank()) {
            _state.update { it.copy(error = UiError.Resource(R.string.source_url_required)) }
            return@launch
        }
        
        _state.update { it.copy(submitting = true, error = null) }
        try {
            if (editReportId != null) {
                // EDIT MODE: Update existing report using the dedicated PATCH endpoint.
                reports.updateReport(editReportId, UpdateReportRequest(s.text.trim(), listOf(s.source.trim())))
            } else {
                // CREATE MODE: Post new report or challenge. 
                // parentId determines if this is a top-level claim or a nested challenge.
                reports.createReport(CreateReportRequest(companyId, s.text.trim(), listOf(s.source.trim()), parentId))
            }
            _state.update { it.copy(submitting = false, submitted = true) }
        } catch (_: Exception) {
            // ERROR HANDLING: Catches network timeouts or 4xx/5xx responses.
            _state.update { it.copy(submitting = false, error = UiError.Resource(R.string.error_submit_failed)) }
        }
    }
}
