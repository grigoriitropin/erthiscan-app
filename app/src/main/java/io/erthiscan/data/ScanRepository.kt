package io.erthiscan.data

import io.erthiscan.api.ErthiscanApi
import io.erthiscan.api.ScanBarcodeRequest
import io.erthiscan.api.ScanResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SCAN REPOSITORY
 * 
 * ARCHITECTURAL ROLE:
 * Specialized repository for barcode-to-entity resolution. It facilitates the core 
 * "Scan & Discover" user journey by mapping physical product identifiers (EAN/UPC) 
 * to their parent companies and ethical profiles.
 */
@Singleton
class ScanRepository @Inject constructor(
    private val api: ErthiscanApi,
) {
    /**
     * Sends a raw barcode string to the backend to identify the product 
     * and its associated ethical data.
     * 
     * @param barcode The raw numeric string from the camera scanner.
     * @return [ScanResponse] which may contain the found company detail or 
     * a "Not Found" state allowing the user to search manually.
     */
    suspend fun scan(barcode: String): ScanResponse = api.scanBarcode(ScanBarcodeRequest(barcode))
}