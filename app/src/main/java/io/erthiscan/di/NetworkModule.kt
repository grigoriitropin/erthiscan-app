package io.erthiscan.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.erthiscan.BuildConfig
import io.erthiscan.api.ErthiscanApi
import io.erthiscan.auth.AuthManager
import io.erthiscan.api.RefreshRequest
import io.erthiscan.api.RefreshResponse
import kotlinx.serialization.json.Json
import okhttp3.CertificatePinner
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

import javax.inject.Named

/**
 * SSL PINNING CONFIGURATION
 * 
 * SECURITY STRATEGY:
 * We pin the Subject Public Key Info (SPKI) for the Let's Encrypt CA hierarchy rather 
 * than leaf certificates. This prevents MITM attacks while remaining resilient 
 * to frequent (60-day) leaf rotations.
 * 
 * COVERAGE: 
 * Includes all current ECDSA and RSA intermediates plus the ISRG Root X1/X2 anchors.
 */
private val CERTIFICATE_PINNER = CertificatePinner.Builder()
    .add("pjdth.xyz", "sha256/NYbU7PBwV4y9J67c4guWTki8FJ+uudrXL0a4V4aRcrg=") // E5
    .add("pjdth.xyz", "sha256/0Bbh/jEZSKymTy3kTOhsmlHKBB32EDu1KojrP3YfV9c=") // E6
    .add("pjdth.xyz", "sha256/y7xVm0TVJNahMr2sZydE2jQH8SquXV9yLF9seROHHHU=") // E7
    .add("pjdth.xyz", "sha256/iFvwVyJSxnQdyaUvUERIf+8qk7gRze3612JMwoO3zdU=") // E8
    .add("pjdth.xyz", "sha256/8UQKm3bh5B5TpMtGEym/Yze0GXJr5RPkLhnxxpHF1LI=") // E9
    .add("pjdth.xyz", "sha256/K7rZOrXHknnsEhUH8nLL4MZkejquUuIvOIr6tCa0rbo=") // R10
    .add("pjdth.xyz", "sha256/bdrBhpj38ffhxpubzkINl0rG+UyossdhcBYj+Zx2fcc=") // R11
    .add("pjdth.xyz", "sha256/kZwN96eHtZftBWrOZUsd6cA4es80n3NzSk/XtYz2EqQ=") // R12
    .add("pjdth.xyz", "sha256/AlSQhgtJirc8ahLyekmtX+Iw+v46yPYRLJt9Cq1GlB0=") // R13
    .add("pjdth.xyz", "sha256/8WR6XuPvrFTIkukwWE/keXm3rNHHbBJxvKHFB22GmIg=") // R14
    .add("pjdth.xyz", "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=") // ISRG Root X1
    .add("pjdth.xyz", "sha256/diGVwiVYbubAI3RW4hB9xU8e/CH2GnkuvVFZE8zmgzI=") // ISRG Root X2
    .build()

/**
 * NETWORK MODULE
 * 
 * ARCHITECTURAL ROLE:
 * Configures the entire network stack. It handles serialization, security (SSL pinning), 
 * authenticated request headers, and automatic token refresh logic.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Provides a [Json] instance configured to be resilient to backend API changes 
     * by ignoring unknown keys during deserialization.
     */
    @Provides @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    /**
     * Dedicated [OkHttpClient] for token refresh requests.
     * WHY: Separated from the main client to prevent infinite recursion when 
     * the authenticator itself needs to make a network call.
     */
    @Provides @Singleton @Named("RefreshClient")
    fun provideRefreshClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .certificatePinner(CERTIFICATE_PINNER)
        .build()

    /**
     * Main [OkHttpClient] providing the foundation for all API traffic.
     * 
     * KEY FEATURES:
     * 1. AUTH INTERCEPTOR: Inject Bearer tokens into headers.
     * 2. REFRESH AUTHENTICATOR: Transparently recovers from 401 errors.
     * 3. LOGGING INTERCEPTOR: Debug-only visibility with credential redaction.
     */
    @Provides @Singleton
    fun provideOkHttpClient(
        authManager: AuthManager,
        json: Json,
        @Named("RefreshClient") refreshClient: OkHttpClient,
        @ApplicationScope appScope: CoroutineScope,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
            // DATA PRIVACY: Ensures tokens and cookies never leak into logcat.
            redactHeader("Authorization")
            redactHeader("Cookie")
        }

        /**
         * Cleans up local state on refresh failure. 
         * Uses [appScope] because the current request's coroutine may be cancelled.
         */
        fun clearSession() {
            appScope.launch { authManager.logoutLocal() }
        }

        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .certificatePinner(CERTIFICATE_PINNER)
            // LAYER 1: Token Injection
            .addInterceptor { chain ->
                val req = authManager.accessToken?.let { token ->
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } ?: chain.request()
                chain.proceed(req)
            }
            // LAYER 2: Token Recovery (Authenticator)
            // Triggered automatically by OkHttp when the server returns 401.
            .authenticator { _, response ->
                // RECURSION GUARD: Stop if we already tried to refresh once for this request.
                if (response.request.header("X-Refresh-Attempted") != null) {
                    clearSession()
                    return@authenticator null
                }

                val refresh = authManager.refreshToken ?: return@authenticator null

                // PREPARE REFRESH CALL: Synchronous execution within the authenticator thread.
                val body = json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refresh))
                    .toRequestBody("application/json".toMediaType())
                val refreshReq = Request.Builder()
                    .url("${BuildConfig.API_BASE_URL}auth/refresh")
                    .post(body)
                    .build()

                val refreshResp = try {
                    refreshClient.newCall(refreshReq).execute()
                } catch (_: Exception) {
                    return@authenticator null
                }

                try {
                    // SESSION TERMINATION: If refresh token is expired/invalid (4xx), force logout.
                    if (refreshResp.code in 400..499) {
                        clearSession()
                        return@authenticator null
                    }
                    if (!refreshResp.isSuccessful) return@authenticator null

                    val raw = refreshResp.body?.string() ?: return@authenticator null
                    val parsed = json.decodeFromString(RefreshResponse.serializer(), raw)

                    // ATOMIC UPDATE: Persist new tokens.
                    authManager.updateTokensFromAuthenticator(parsed.accessToken, parsed.refreshToken)

                    // RETRY ORIGINAL REQUEST: Re-builds the failed request with the fresh token.
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${parsed.accessToken}")
                        .header("X-Refresh-Attempted", "true") // Marks request to prevent infinite retry loops.
                        .build()
                } finally {
                    refreshResp.close()
                }
            }
            .addInterceptor(logging)
            .build()
    }

    /**
     * Configures [Retrofit] with the provided [OkHttpClient] and [Json] serializer.
     */
    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    /**
     * Generates the concrete implementation of the [ErthiscanApi] interface.
     */
    @Provides @Singleton
    fun provideApi(retrofit: Retrofit): ErthiscanApi = retrofit.create(ErthiscanApi::class.java)
}