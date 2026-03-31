package io.erthiscan.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScanBarcodeRequest(
    val barcode: String
)

@Serializable
data class ScanResponse(
    val status: String,
    val product: ProductData,
    val company: CompanyData
)

@Serializable
data class ProductData(
    val barcode: String,
    val name: String,
    @SerialName("open_facts_url") val openFactsUrl: String? = null
)

@Serializable
data class CompanyData(
    val id: Int,
    val name: String,
    @SerialName("ethical_score") val ethicalScore: Float = 0f
)
