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

data class CompaniesState(
    val companies: List<CompanyItem> = emptyList(),
    val query: String = "",
    val sort: String = "reports_desc",
    val page: Int = 1,
    val pages: Int = 1,
    val loading: Boolean = true,
    val error: UiError? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class CompaniesViewModel @Inject constructor(
    private val repo: CompaniesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CompaniesState())
    val state: StateFlow<CompaniesState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private val sortFlow = MutableStateFlow("reports_desc")
    private val pageFlow = MutableStateFlow(1)

    init {
        viewModelScope.launch {
            combine(
                queryFlow.debounce(300).distinctUntilChanged(),
                sortFlow,
                pageFlow,
            ) { q, s, p -> Triple(q, s, p) }
                .collectLatest { (q, s, p) ->
                    load(q, s, p)
                }
        }
    }

    private suspend fun load(q: String, s: String, p: Int) {
        _state.update { it.copy(loading = true) }
        try {
            val resp = repo.list(q, s, p)
            _state.update {
                it.copy(
                    companies = resp.items,
                    pages = resp.pages,
                    query = q,
                    sort = s,
                    page = p,
                    loading = false,
                    error = null,
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = UiError.from(e)) }
        }
    }

    fun onQueryChange(q: String) {
        queryFlow.value = q
        pageFlow.value = 1
    }

    fun onSortChange(s: String) {
        sortFlow.value = s
        pageFlow.value = 1
    }

    fun nextPage() { if (_state.value.page < _state.value.pages) pageFlow.value = _state.value.page + 1 }
    fun prevPage() { if (_state.value.page > 1) pageFlow.value = _state.value.page - 1 }
    fun dismissError() = _state.update { it.copy(error = null) }
}