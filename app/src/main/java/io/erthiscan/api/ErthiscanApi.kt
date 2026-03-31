package io.erthiscan.api

import retrofit2.http.Body
import retrofit2.http.POST

interface ErthiscanApi {

    @POST("scan/barcode")
    suspend fun scanBarcode(@Body request: ScanBarcodeRequest): ScanResponse
}
