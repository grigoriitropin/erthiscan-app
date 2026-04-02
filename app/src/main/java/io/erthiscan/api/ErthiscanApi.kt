package io.erthiscan.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ErthiscanApi {

    @POST("scan/barcode")
    suspend fun scanBarcode(@Body request: ScanBarcodeRequest): ScanResponse

    @POST("auth/google")
    suspend fun authGoogle(@Body request: GoogleAuthRequest): AuthResponse

    @GET("companies")
    suspend fun getCompanies(
        @Query("search") search: String = "",
        @Query("sort") sort: String = "score_desc",
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): CompaniesResponse

    @GET("companies/{id}")
    suspend fun getCompany(@Path("id") id: Int): CompanyDetail
}
