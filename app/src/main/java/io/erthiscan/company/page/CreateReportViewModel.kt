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

data class CreateReportUi(
    val text: String = "",
    val source: String = "",
    val submitting: Boolean = false,
    val error: UiError? = null,
    val submitted: Boolean = false,
)

@HiltViewModel
class CreateReportViewModel @Inject constructor(
    private val reports: ReportsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.CreateReport>()
    val companyId = route.companyId
    val parentId = route.parentId
    val editReportId = route.editReportId

    private val _state = MutableStateFlow(
        CreateReportUi(text = route.initialText, source = route.initialSource)
    )
    val state: StateFlow<CreateReportUi> = _state.asStateFlow()

    fun onText(t: String) { if (t.length <= 150) _state.update { it.copy(text = t) } }
    fun onSource(s: String) = _state.update { it.copy(source = s) }

    fun submit() = viewModelScope.launch {
        val s = _state.value
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
                reports.updateReport(editReportId, UpdateReportRequest(s.text.trim(), listOf(s.source.trim())))
            } else {
                reports.createReport(CreateReportRequest(companyId, s.text.trim(), listOf(s.source.trim()), parentId))
            }
            _state.update { it.copy(submitting = false, submitted = true) }
        } catch (_: Exception) {
            _state.update { it.copy(submitting = false, error = UiError.Resource(R.string.error_submit_failed)) }
        }
    }
}
