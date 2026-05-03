package io.erthiscan.data

import io.erthiscan.api.AuthResponse
import io.erthiscan.api.ErthiscanApi
import io.erthiscan.api.GoogleAuthRequest
import io.erthiscan.auth.AuthManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AUTH REPOSITORY
 * 
 * ARCHITECTURAL ROLE:
 * This repository serves as the single source of truth for authentication-related 
 * operations. It coordinates between the network layer [ErthiscanApi] and the 
 * local session persistence layer [AuthManager].
 * 
 * SCOPE:
 * Annotated with [@Singleton] because authentication state must be consistent 
 * across the entire application lifecycle.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val api: ErthiscanApi,
    private val authManager: AuthManager,
) {
    /**
     * Executes the Google Sign-In flow by exchanging an OAuth ID Token for 
     * ErthiScan-specific session tokens.
     * 
     * @param idToken The JWT token received from the Google Identity Services.
     * @return [AuthResponse] containing the new access/refresh tokens and user profile.
     * 
     * WHY PERSIST LOCAL: 
     * After a successful network exchange, we immediately call [authManager.login] 
     * to ensure subsequent authenticated requests include the correct 'Authorization' 
     * headers.
     */
    suspend fun signInWithGoogle(idToken: String): AuthResponse {
        val resp = api.authGoogle(GoogleAuthRequest(token = idToken))
        authManager.login(resp.accessToken, resp.refreshToken, resp.userId, resp.username)
        return resp
    }

    /**
     * Clears the user session both on the server and locally.
     * 
     * NETWORK STRATEGY: 
     * The server-side logout is "best-effort" (wrapped in try-catch). If it fails 
     * (e.g., due to no connectivity), we still proceed with [authManager.logoutLocal] 
     * to ensure the user is effectively logged out from the device's perspective.
     */
    suspend fun logout() {
        try { api.logout() } catch (_: Exception) {}
        authManager.logoutLocal()
    }
}