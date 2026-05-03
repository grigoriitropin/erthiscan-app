package io.erthiscan.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * ERTHISCAN API: The primary Retrofit service interface for backend communication.
 * 
 * DESIGN PATTERN: 
 * This interface defines the contract with the FastAPI backend. All methods are 
 * [suspend] functions, designed to be called from Kotlin Coroutines (typically 
 * within Repository or ViewModel scopes).
 * 
 * SECURITY NOTE: 
 * Most endpoints require a valid JWT 'Authorization: Bearer <token>' header. 
 * This is handled transparently by the [AuthInterceptor] and [TokenAuthenticator] 
 * configured in the [NetworkModule].
 */
interface ErthiscanApi {

    /**
     * SCAN BARCODE: 
     * Matches a raw barcode string against the product database.
     * @param request Contains the barcode (EAN-13/UPC) captured by the scanner.
     * @return [ScanResponse] including product name and the responsible company's summary.
     */
    @POST("scan/barcode")
    suspend fun scanBarcode(@Body request: ScanBarcodeRequest): ScanResponse

    /**
     * GOOGLE AUTH: 
     * Primary login mechanism. Exchanges a Google ID Token for an Erthiscan session.
     * @param request The cryptographically signed ID Token from Android Credentials Manager.
     * @return [AuthResponse] containing the internal user ID and long-lived session tokens.
     */
    @POST("auth/google")
    suspend fun authGoogle(@Body request: GoogleAuthRequest): AuthResponse

    /**
     * SEARCH COMPANIES: 
     * Retrieves a paginated list of companies with optional filtering.
     * @param search Fuzzy search string to match against company names.
     * @param sort Ordering criteria (e.g. "score_desc", "reports_desc").
     * @param page Target page number for results.
     * @param perPage Number of items to return in a single batch.
     */
    @GET("companies")
    suspend fun getCompanies(
        @Query("search") search: String = "",
        @Query("sort") sort: String = "score_desc",
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): CompaniesResponse

    /**
     * COMPANY DETAIL: 
     * Fetches the full ethical profile, including the complete hierarchy of reports.
     * @param id The internal database ID of the company.
     */
    @GET("companies/{id}")
    suspend fun getCompany(@Path("id") id: Int): CompanyDetail

    /**
     * REFRESH TOKEN: 
     * Rotates expired access tokens without requiring user re-authentication.
     * @param request The current long-lived Refresh Token.
     */
    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): RefreshResponse

    /**
     * LOGOUT: 
     * Server-side session invalidation.
     */
    @POST("auth/logout")
    suspend fun logout(): Unit

    /**
     * CREATE REPORT: 
     * Submits a new ethical claim or a challenge (if parent_id is set).
     * Requires authentication.
     */
    @POST("reports")
    suspend fun createReport(@Body request: CreateReportRequest): Unit

    /**
     * UPDATE REPORT: 
     * Allows a user to modify the text or sources of their own previous submission.
     * @param reportId ID of the report being edited.
     */
    @PATCH("reports/{id}")
    suspend fun updateReport(@Path("id") reportId: Int, @Body request: UpdateReportRequest): Unit

    /**
     * DELETE REPORT: 
     * Soft-deletes a user's report. Backend removes it from scoring calculations.
     */
    @DELETE("reports/{id}")
    suspend fun deleteReport(@Path("id") reportId: Int): Unit

    /**
     * VOTE: 
     * Casts a community vote on a specific claim to influence the ethical score.
     * @param reportId Target report/challenge ID.
     * @param request Contains the vote value (1 for Ethical/True, -1 for Unethical/False).
     */
    @POST("reports/{id}/vote")
    suspend fun vote(@Path("id") reportId: Int, @Body request: VoteRequest): VoteResponse

    /**
     * USER PROFILE: 
     * Retrieves the private profile of the authenticated user.
     * Includes personal history of reports and challenges.
     */
    @GET("reports/me")
    suspend fun getMyProfile(): UserProfile
}
