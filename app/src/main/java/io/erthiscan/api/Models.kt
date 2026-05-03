package io.erthiscan.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API MODELS: Data Transfer Objects (DTOs) for the Erthiscan ecosystem.
 * 
 * SERIALIZATION STRATEGY:
 * These classes use [kotlinx.serialization] for highly efficient JSON parsing. 
 * Since the FastAPI backend follows snake_case naming conventions, we use 
 * [@SerialName] to map them to Kotlin-standard camelCase properties.
 */

// --- SCANNING MODELS ---

/**
 * SCAN BARCODE REQUEST: Sent when the MLKit analyzer detects a barcode.
 * @property barcode The raw numeric or alphanumeric string from the scanned item (e.g. EAN-13).
 */
@Serializable
data class ScanBarcodeRequest(
    val barcode: String
)

/**
 * SCAN RESPONSE: Returned after the backend matches a barcode to a known product.
 * @property status Execution status (e.g., "success" or "not_found").
 * @property product Metadata about the physical item.
 * @property company The ethical entity responsible for the product.
 */
@Serializable
data class ScanResponse(
    val status: String,
    val product: ProductData,
    val company: CompanyData
)

/**
 * PRODUCT DATA: Basic information about the scanned item.
 * @property barcode The EAN/UPC string.
 * @property name The human-readable name of the product.
 * @property openFactsUrl External reference to the OpenFoodFacts or OpenBeautyFacts database.
 */
@Serializable
data class ProductData(
    val barcode: String,
    val name: String,
    @SerialName("open_facts_url") val openFactsUrl: String? = null
)

/**
 * COMPANY DATA: High-level ethical summary for a company.
 * @property id Unique database primary key.
 * @property name Legal or commercial name of the company.
 * @property ethicalScore Current community-weighted score normalized by the backend.
 * @property reportCount Total number of claims and challenges submitted for this company.
 */
@Serializable
data class CompanyData(
    val id: Int,
    val name: String,
    @SerialName("ethical_score") val ethicalScore: Float = 0f,
    @SerialName("report_count") val reportCount: Int = 0
)

// --- AUTHENTICATION MODELS ---

/**
 * GOOGLE AUTH REQUEST: Used for server-side OAuth2 verification.
 * @property token The ID Token string obtained via the Android Credentials Manager.
 */
@Serializable
data class GoogleAuthRequest(
    val token: String
)

/**
 * AUTH RESPONSE: Contains the tokens required for session persistence.
 * These are stored in an encrypted DataStore.
 * @property accessToken Short-lived JWT used for 'Authorization: Bearer' headers.
 * @property refreshToken Long-lived token used to obtain new access tokens.
 * @property userId The persistent ID of the user in the Erthiscan database.
 * @property username The user's display name or handle.
 */
@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("user_id") val userId: Int,
    val username: String
)

/**
 * REFRESH REQUEST: Sent to rotate expired session tokens.
 * @property refreshToken The token obtained during the last successful login or refresh.
 */
@Serializable
data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class RefreshResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String
)

// --- COMPANY LIST & DETAIL MODELS ---

/**
 * COMPANIES RESPONSE: Wrapper for paginated company listings.
 * @property items The current slice of the company database.
 * @property total Total number of companies matching the search criteria.
 * @property page Current page index.
 * @property pages Total available pages.
 */
@Serializable
data class CompaniesResponse(
    val items: List<CompanyItem>,
    val total: Int,
    val page: Int,
    val pages: Int
)

/**
 * COMPANY ITEM: Summary used for list rendering.
 * @property hasReports Boolean flag to differentiate between 0 score and "No data".
 */
@Serializable
data class CompanyItem(
    val id: Int,
    val name: String,
    @SerialName("ethical_score") val ethicalScore: Float,
    @SerialName("has_reports") val hasReports: Boolean,
    @SerialName("report_count") val reportCount: Int = 0
)

/**
 * COMPANY DETAIL: Full ethical profile of a company.
 * @property reports The top-level hierarchical claims submitted by the community.
 */
@Serializable
data class CompanyDetail(
    val id: Int,
    val name: String,
    @SerialName("ethical_score") val ethicalScore: Float,
    @SerialName("report_count") val reportCount: Int,
    val reports: List<ReportItem>
)

// --- REPORTS & VOTING MODELS ---

/**
 * SUB REPORT: A challenge or comment attached to a parent report.
 * @property trueCount Votes agreeing with this challenge.
 * @property falseCount Votes disagreeing with this challenge.
 * @property userVote Indicates if the current user has already voted on this challenge (1/-1/null).
 */
@Serializable
data class SubReportItem(
    val id: Int,
    @SerialName("user_id") val userId: Int = 0,
    val text: String,
    val sources: List<String>,
    val author: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("true_count") val trueCount: Int,
    @SerialName("false_count") val falseCount: Int,
    @SerialName("user_vote") val userVote: Int? = null 
)

/**
 * REPORT ITEM: A primary ethical claim about a company.
 * @property ethicalCount Votes marking the claim as verified/ethical.
 * @property unethicalCount Votes marking the claim as debunked/unethical.
 * @property subReports Nested challenges that target this specific claim.
 */
@Serializable
data class ReportItem(
    val id: Int,
    @SerialName("user_id") val userId: Int = 0,
    val text: String,
    val sources: List<String>,
    val author: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("ethical_count") val ethicalCount: Int,
    @SerialName("unethical_count") val unethicalCount: Int,
    @SerialName("user_vote") val userVote: Int? = null,
    @SerialName("sub_reports") val subReports: List<SubReportItem> = emptyList()
)

/**
 * CREATE REPORT REQUEST: Data sent when submitting a new claim.
 * @property parentId If set, the backend treats this as a challenge to another report.
 */
@Serializable
data class CreateReportRequest(
    @SerialName("company_id") val companyId: Int,
    val text: String,
    val sources: List<String>,
    @SerialName("parent_id") val parentId: Int? = null 
)

@Serializable
data class UpdateReportRequest(
    val text: String,
    val sources: List<String>
)

@Serializable
data class VoteRequest(val value: Int)

@Serializable
data class VoteResponse(
    @SerialName("ethical_count") val ethicalCount: Int,
    @SerialName("unethical_count") val unethicalCount: Int,
    @SerialName("user_vote") val userVote: Int?
)

// --- USER PROFILE MODELS ---

@Serializable
data class UserReportItem(
    val id: Int,
    @SerialName("company_id") val companyId: Int,
    @SerialName("company_name") val companyName: String,
    val text: String,
    val sources: List<String> = emptyList(),
    @SerialName("vote_sum") val voteSum: Int,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class UserChallengeItem(
    val id: Int,
    @SerialName("parent_id") val parentId: Int,
    @SerialName("company_id") val companyId: Int,
    @SerialName("company_name") val companyName: String,
    val text: String,
    val sources: List<String> = emptyList(),
    @SerialName("vote_sum") val voteSum: Int,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class UserProfile(
    @SerialName("user_id") val userId: Int,
    val username: String,
    @SerialName("report_count") val reportCount: Int,
    @SerialName("challenge_count") val challengeCount: Int = 0,
    val reports: List<UserReportItem>,
    val challenges: List<UserChallengeItem> = emptyList()
)
