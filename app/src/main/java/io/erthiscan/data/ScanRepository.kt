package io.erthiscan.data

import io.erthiscan.api.ErthiscanApi
import io.erthiscan.api.ScanBarcodeRequest
import io.erthiscan.api.ScanResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanRepository @Inject constructor(
    private val api: ErthiscanApi,
) {
    suspend fun scan(barcode: String): ScanResponse = api.scanBarcode(ScanBarcodeRequest(barcode))
}