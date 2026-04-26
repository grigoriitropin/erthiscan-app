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

// SPKI pin set for Let's Encrypt CA hierarchy (not the leaf — LE rotates leaves every ~60 days).
// All SPKIs computed from PEMs fetched directly from https://letsencrypt.org/certificates/ ,
// verified via: openssl x509 -in <file>.pem -pubkey -noout
//                 | openssl pkey -pubin -outform der
//                 | openssl dgst -sha256 -binary | base64
//
// Strategy: pin ALL current LE intermediates (ECDSA E5–E9, RSA R10–R14) plus both ISRG roots.
// Intermediates expire 2027-03-12; roots X1 (2035), X2 (2040) are the real long-term anchors.
// When LE issues new intermediates (late 2026 / early 2027), regenerate this list.
private val CERTIFICATE_PINNER = CertificatePinner.Builder()
    // ECDSA intermediates (E5–E9) — pjdth.xyz currently chains through E8.
    .add("pjdth.xyz", "sha256/NYbU7PBwV4y9J67c4guWTki8FJ+uudrXL0a4V4aRcrg=") // E5
    .add("pjdth.xyz", "sha256/0Bbh/jEZSKymTy3kTOhsmlHKBB32EDu1KojrP3YfV9c=") // E6
    .add("pjdth.xyz", "sha256/y7xVm0TVJNahMr2sZydE2jQH8SquXV9yLF9seROHHHU=") // E7
    .add("pjdth.xyz", "sha256/iFvwVyJSxnQdyaUvUERIf+8qk7gRze3612JMwoO3zdU=") // E8
    .add("pjdth.xyz", "sha256/8UQKm3bh5B5TpMtGEym/Yze0GXJr5RPkLhnxxpHF1LI=") // E9
    // RSA intermediates (R10–R14) — covers LE switching chains.
    .add("pjdth.xyz", "sha256/K7rZOrXHknnsEhUH8nLL4MZkejquUuIvOIr6tCa0rbo=") // R10
    .add("pjdth.xyz", "sha256/bdrBhpj38ffhxpubzkINl0rG+UyossdhcBYj+Zx2fcc=") // R11
    .add("pjdth.xyz", "sha256/kZwN96eHtZftBWrOZUsd6cA4es80n3NzSk/XtYz2EqQ=") // R12
    .add("pjdth.xyz", "sha256/AlSQhgtJirc8ahLyekmtX+Iw+v46yPYRLJt9Cq1GlB0=") // R13
    .add("pjdth.xyz", "sha256/8WR6XuPvrFTIkukwWE/keXm3rNHHbBJxvKHFB22GmIg=") // R14
    // Long-lived roots — ultimate fallback if every intermediate rotates.
    .add("pjdth.xyz", "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=") // ISRG Root X1 (2035)
    .add("pjdth.xyz", "sha256/diGVwiVYbubAI3RW4hB9xU8e/CH2GnkuvVFZE8zmgzI=") // ISRG Root X2 (2040)
    .build()

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides @Singleton @Named("RefreshClient")
    fun provideRefreshClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .certificatePinner(CERTIFICATE_PINNER)
        .build()

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
            redactHeader("Authorization")
            redactHeader("Cookie")
        }

        fun clearSession() {
            appScope.launch { authManager.logoutLocal() }
        }

        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .certificatePinner(CERTIFICATE_PINNER)
            .addInterceptor { chain ->
                val req = authManager.accessToken?.let { token ->
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } ?: chain.request()
                chain.proceed(req)
            }
            .authenticator { _, response ->
                if (response.request.header("X-Refresh-Attempted") != null) {
                    clearSession()
                    return@authenticator null
                }
                val refresh = authManager.refreshToken ?: return@authenticator null

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
                    if (refreshResp.code in 400..499) {
                        clearSession()
                        return@authenticator null
                    }
                    if (!refreshResp.isSuccessful) return@authenticator null
                    val raw = refreshResp.body?.string() ?: return@authenticator null
                    val parsed = json.decodeFromString(RefreshResponse.serializer(), raw)
                    authManager.updateTokensFromAuthenticator(parsed.accessToken, parsed.refreshToken)
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${parsed.accessToken}")
                        .header("X-Refresh-Attempted", "true")
                        .build()
                } finally {
                    refreshResp.close()
                }
            }
            .addInterceptor(logging)
            .build()
    }

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides @Singleton
    fun provideApi(retrofit: Retrofit): ErthiscanApi = retrofit.create(ErthiscanApi::class.java)
}