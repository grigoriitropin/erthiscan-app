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

@Serializable
data class GoogleAuthRequest(
    val token: String
)

@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("user_id") val userId: Int,
    val username: String
)

@Serializable
data class CompaniesResponse(
    val items: List<CompanyItem>,
    val total: Int,
    val page: Int,
    val pages: Int
)

@Serializable
data class CompanyItem(
    val id: Int,
    val name: String,
    @SerialName("ethical_score") val ethicalScore: Float,
    @SerialName("has_reports") val hasReports: Boolean
)

@Serializable
data class CompanyDetail(
    val id: Int,
    val name: String,
    @SerialName("ethical_score") val ethicalScore: Float,
    @SerialName("report_count") val reportCount: Int,
    val reports: List<ReportItem>
)

@Serializable
data class ReportItem(
    val id: Int,
    val text: String,
    val sources: List<String>,
    val author: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("vote_sum") val voteSum: Int
)
