package io.erthiscan.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.erthiscan.api.ScanResponse
import io.erthiscan.data.ScanRepository
import io.erthiscan.ui.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class ScanState(
    val torch: Boolean = false,
    val lastBarcode: String? = null,
    val result: ScanResponse? = null,
    val notFoundBarcode: String? = null,
    val error: UiError? = null,
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val repo: ScanRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state.asStateFlow()

    fun toggleTorch() = _state.update { it.copy(torch = !it.torch) }

    fun onBarcode(code: String) {
        if (_state.value.lastBarcode == code) return
        _state.update { it.copy(lastBarcode = code) }
        viewModelScope.launch {
            try {
                val r = repo.scan(code)
                _state.update { it.copy(result = r, notFoundBarcode = null) }
            } catch (e: HttpException) {
                if (e.code() == 404) {
                    _state.update { it.copy(notFoundBarcode = code, result = null) }
                    return@launch
                }
                _state.update { it.copy(error = UiError.from(e)) }
            } catch (e: Exception) {
                _state.update { it.copy(error = UiError.from(e)) }
            }
        }
    }

    fun dismissResult() = _state.update { it.copy(result = null, lastBarcode = null) }
    fun dismissNotFound() = _state.update { it.copy(notFoundBarcode = null, lastBarcode = null) }
    fun dismissError() = _state.update { it.copy(error = null) }
}