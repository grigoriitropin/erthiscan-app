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

/**
 * SCAN UI STATE: Encapsulates the reactive state of the scanning process.
 * 
 * @property torch Hardware flashlight status.
 * @property lastBarcode Memory of the most recently scanned code to prevent duplicate processing.
 * @property result Successful product lookup data.
 * @property notFoundBarcode The code that failed lookup (triggers the Not Found sheet).
 * @property error Network or server errors.
 */
data class ScanState(
    val torch: Boolean = false,
    val lastBarcode: String? = null,
    val result: ScanResponse? = null,
    val notFoundBarcode: String? = null,
    val error: UiError? = null,
)

/**
 * SCAN VIEW MODEL: Manages the business logic for the scanning flow.
 * 
 * ARCHITECTURAL ROLE:
 * This ViewModel acts as the controller for the [ScanScreen]. it coordinates 
 * raw barcode detections from the UI with the backend via the [ScanRepository].
 * 
 * KEY RESPONSIBILITIES:
 * 1. STATE MANAGEMENT: Maintains the reactive [ScanState] for UI consumption.
 * 2. DUPLICATE SUPPRESSION: Ensures that a single barcode isn't processed 
 *    multiple times in rapid succession.
 * 3. ERROR MAPPING: Specifically handles 404 (Not Found) statuses to trigger 
 *    alternative UI flows (Open Food Facts).
 * 4. HARDWARE ABSTRACTION: Exposes methods to control the device's torch.
 */
@HiltViewModel
class ScanViewModel @Inject constructor(
    private val repo: ScanRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state.asStateFlow()

    /**
     * TOGGLE TORCH: Flips the boolean state which is observed by CameraPreview.
     */
    fun toggleTorch() = _state.update { it.copy(torch = !it.torch) }

    /**
     * ON BARCODE: Triggered when the CameraPreview confirms a barcode detection.
     * 
     * @param code The raw barcode string (e.g., EAN-13).
     */
    fun onBarcode(code: String) {
        // PERFORMANCE: If we just scanned this exact code, ignore it to prevent 
        // redundant network requests while the user is still looking at the result.
        if (_state.value.lastBarcode == code) return

        // OPTIMISTIC UPDATE: Record the code immediately to block further attempts.
        _state.update { it.copy(lastBarcode = code) }

        viewModelScope.launch {
            try {
                // API CALL: Attempt to find product and company data for the code.
                val r = repo.scan(code)
                _state.update { it.copy(result = r, notFoundBarcode = null) }
            } catch (e: HttpException) {
                // SPECIAL CASE: 404 indicates the product isn't in our database yet.
                // We show the "Not Found" sheet instead of a generic error.
                if (e.code() == 404) {
                    _state.update { it.copy(notFoundBarcode = code, result = null) }
                    return@launch
                }
                _state.update { it.copy(error = UiError.from(e)) }
            } catch (e: Exception) {
                // GENERIC ERROR: Network failures, timeouts, etc.
                _state.update { it.copy(error = UiError.from(e)) }
            }
        }
    }

    /**
     * DISMISS RESULT: Clears the successful scan state. 
     * Resets lastBarcode so the same product can be scanned again if needed.
     */
    fun dismissResult() = _state.update { it.copy(result = null, lastBarcode = null) }

    /**
     * DISMISS NOT FOUND: Clears the 404 state.
     */
    fun dismissNotFound() = _state.update { it.copy(notFoundBarcode = null, lastBarcode = null) }

    /**
     * DISMISS ERROR: Clears transient error notifications.
     */
    fun dismissError() = _state.update { it.copy(error = null) }
}