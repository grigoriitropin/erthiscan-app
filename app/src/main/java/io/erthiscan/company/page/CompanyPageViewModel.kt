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

data class CompanyPageState(
    val company: CompanyDetail? = null,
    val loading: Boolean = true,
    val error: UiError? = null,
)

@HiltViewModel
class CompanyPageViewModel @Inject constructor(
    private val companies: CompaniesRepository,
    private val reports: ReportsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.Company>()
    private val companyId = route.companyId

    private val _state = MutableStateFlow(CompanyPageState())
    val state: StateFlow<CompanyPageState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        try {
            val c = companies.detail(companyId)
            _state.update { it.copy(company = c, loading = false, error = null) }
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = UiError.from(e)) }
        }
    }

    fun vote(reportId: Int, value: Int) = viewModelScope.launch {
        try {
            reports.vote(reportId, value)
            refresh()
        } catch (e: Exception) {
            _state.update { it.copy(error = UiError.from(e)) }
        }
    }

    fun delete(reportId: Int) = viewModelScope.launch {
        try {
            reports.deleteReport(reportId)
            refresh()
        } catch (e: Exception) {
            _state.update { it.copy(error = UiError.from(e)) }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }
}
