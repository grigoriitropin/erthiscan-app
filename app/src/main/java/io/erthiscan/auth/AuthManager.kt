package io.erthiscan.auth

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton

data class AuthState(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val userId: Int? = null,
    val username: String? = null,
) {
    val isLoggedIn: Boolean get() = accessToken != null
}

private val Context.authDataStore by preferencesDataStore(name = "auth")

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val KEY_TOKEN = stringPreferencesKey("token")
    private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    private val KEY_USER_ID = intPreferencesKey("user_id")
    private val KEY_USERNAME = stringPreferencesKey("username")

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()

    @Volatile private var aeadCache: Aead? = null

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    // Synchronous snapshot for OkHttp interceptor
    val accessToken: String? get() = _state.value.accessToken
    val refreshToken: String? get() = _state.value.refreshToken

    private suspend fun aead(): Aead = withContext(Dispatchers.IO) {
        aeadCache ?: run {
            val handle = AndroidKeysetManager.Builder()
                .withSharedPref(context, "auth_keyset", "auth_keyset_prefs")
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri("android-keystore://erthiscan_master_key")
                .build()
                .keysetHandle
            val a = handle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
            aeadCache = a
            a
        }
    }

    private suspend fun enc(plaintext: String): String = withContext(Dispatchers.Default) {
        val ct = aead().encrypt(plaintext.toByteArray(), null)
        Base64.encodeToString(ct, Base64.NO_WRAP)
    }

    private suspend fun dec(ct: String): String = withContext(Dispatchers.Default) {
        val bytes = Base64.decode(ct, Base64.NO_WRAP)
        String(aead().decrypt(bytes, null))
    }

    suspend fun login(access: String, refresh: String, id: Int, name: String) = mutex.withLock {
        _state.value = AuthState(access, refresh, id, name)
        val encAccess = enc(access)
        val encRefresh = enc(refresh)
        val encName = enc(name)
        context.authDataStore.edit { prefs ->
            prefs[KEY_TOKEN] = encAccess
            prefs[KEY_REFRESH_TOKEN] = encRefresh
            prefs[KEY_USER_ID] = id
            prefs[KEY_USERNAME] = encName
        }
    }

    // Updates tokens synchronously in memory and triggers async DataStore write
    fun updateTokensFromAuthenticator(access: String, refresh: String) {
        _state.value = _state.value.copy(accessToken = access, refreshToken = refresh)
        scope.launch {
            mutex.withLock {
                try {
                    val encAccess = enc(access)
                    val encRefresh = enc(refresh)
                    context.authDataStore.edit { prefs ->
                        prefs[KEY_TOKEN] = encAccess
                        prefs[KEY_REFRESH_TOKEN] = encRefresh
                    }
                } catch (e: Exception) {
                    Log.e("AuthManager", "Failed to persist refreshed tokens", e)
                }
            }
        }
    }

    suspend fun restore() = mutex.withLock {
        val prefs: Preferences = try {
            context.authDataStore.data.first()
        } catch (e: Exception) {
            Log.e("AuthManager", "Failed to read DataStore; keeping in-memory state", e)
            return@withLock
        }
        val encToken = prefs[KEY_TOKEN] ?: return@withLock
        val encRefresh = prefs[KEY_REFRESH_TOKEN] ?: return@withLock
        val encName = prefs[KEY_USERNAME] ?: return@withLock
        val id = prefs[KEY_USER_ID] ?: return@withLock
        try {
            _state.value = AuthState(
                accessToken = dec(encToken),
                refreshToken = dec(encRefresh),
                userId = id,
                username = dec(encName),
            )
        } catch (e: GeneralSecurityException) {
            // Keyset irrecoverably corrupted — safe to wipe
            Log.e("AuthManager", "Keyset corrupted, wiping session", e)
            clearLocal()
        } catch (e: Exception) {
            // Transient I/O — keep stored ciphertext, user can retry
            Log.e("AuthManager", "Transient restore failure, keeping session on disk", e)
        }
    }

    suspend fun logoutLocal() = mutex.withLock {
        clearLocal()
    }

    private suspend fun clearLocal() {
        _state.value = AuthState()
        context.authDataStore.edit { it.clear() }
    }
}