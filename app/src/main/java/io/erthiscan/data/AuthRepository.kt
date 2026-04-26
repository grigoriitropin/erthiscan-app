package io.erthiscan.data

import io.erthiscan.api.AuthResponse
import io.erthiscan.api.ErthiscanApi
import io.erthiscan.api.GoogleAuthRequest
import io.erthiscan.auth.AuthManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ErthiscanApi,
    private val authManager: AuthManager,
) {
    suspend fun signInWithGoogle(idToken: String): AuthResponse {
        val resp = api.authGoogle(GoogleAuthRequest(token = idToken))
        authManager.login(resp.accessToken, resp.refreshToken, resp.userId, resp.username)
        return resp
    }

    suspend fun logout() {
        try { api.logout() } catch (_: Exception) {}
        authManager.logoutLocal()
    }
}