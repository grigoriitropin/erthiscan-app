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
    @SerialName("ethical_score") val ethicalScore: Float = 0f,
    @SerialName("report_count") val reportCount: Int = 0
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
data class SubReportItem(
    val id: Int,
    val text: String,
    val sources: List<String>,
    val author: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("true_count") val trueCount: Int,
    @SerialName("false_count") val falseCount: Int,
    @SerialName("user_vote") val userVote: Int? = null
)

@Serializable
data class ReportItem(
    val id: Int,
    val text: String,
    val sources: List<String>,
    val author: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("ethical_count") val ethicalCount: Int,
    @SerialName("unethical_count") val unethicalCount: Int,
    @SerialName("user_vote") val userVote: Int? = null,
    @SerialName("sub_reports") val subReports: List<SubReportItem> = emptyList()
)

@Serializable
data class CreateReportRequest(
    @SerialName("company_id") val companyId: Int,
    val text: String,
    val sources: List<String>,
    @SerialName("parent_id") val parentId: Int? = null
)

@Serializable
data class VoteRequest(val value: Int)

@Serializable
data class VoteResponse(
    @SerialName("ethical_count") val ethicalCount: Int,
    @SerialName("unethical_count") val unethicalCount: Int,
    @SerialName("user_vote") val userVote: Int?
)

@Serializable
data class UserReportItem(
    val id: Int,
    @SerialName("company_id") val companyId: Int,
    @SerialName("company_name") val companyName: String,
    val text: String,
    @SerialName("vote_sum") val voteSum: Int,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class UserProfile(
    @SerialName("user_id") val userId: Int,
    val username: String,
    @SerialName("report_count") val reportCount: Int,
    val reports: List<UserReportItem>
)
