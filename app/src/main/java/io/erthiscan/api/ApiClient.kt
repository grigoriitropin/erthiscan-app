package io.erthiscan.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.erthiscan.BuildConfig
import io.erthiscan.auth.AuthManager
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object ApiClient {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = AuthManager.accessToken?.let { token ->
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } ?: chain.request()
            chain.proceed(request)
        }
        .authenticator { _, response ->
            // Only attempt refresh once per request chain
            if (response.request.header("X-Refresh-Attempted") != null) return@authenticator null

            val refresh = AuthManager.refreshToken ?: return@authenticator null

            // Call /auth/refresh synchronously
            val refreshBody = json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refresh))
            val refreshRequest = okhttp3.Request.Builder()
                .url("${BuildConfig.API_BASE_URL}auth/refresh")
                .post(okhttp3.RequestBody.create("application/json".toMediaType(), refreshBody))
                .build()

            val refreshResponse = response.request.newBuilder().build().let {
                OkHttpClient().newCall(refreshRequest).execute()
            }

            if (!refreshResponse.isSuccessful) {
                refreshResponse.close()
                return@authenticator null
            }

            val body = refreshResponse.body?.string() ?: return@authenticator null
            val parsed = json.decodeFromString(RefreshResponse.serializer(), body)
            AuthManager.updateTokens(parsed.accessToken, parsed.refreshToken)

            // Retry original request with new token
            response.request.newBuilder()
                .header("Authorization", "Bearer ${parsed.accessToken}")
                .header("X-Refresh-Attempted", "true")
                .build()
        }
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                        else HttpLoggingInterceptor.Level.NONE
            }
        )
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val api: ErthiscanApi = retrofit.create(ErthiscanApi::class.java)
}
