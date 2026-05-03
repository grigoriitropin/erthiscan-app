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

/**
 * AUTH STATE: An immutable snapshot of the current user session.
 */
data class AuthState(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val userId: Int? = null,
    val username: String? = null,
) {
    /**
     * IS LOGGED IN: Simple helper to determine if we have a session.
     * Note: This doesn't guarantee the token is not expired, just that it exists.
     */
    val isLoggedIn: Boolean get() = accessToken != null
}

// DATASTORE EXTENSION: Scoped to the context for easy access.
private val Context.authDataStore by preferencesDataStore(name = "auth")

/**
 * AUTH MANAGER: The central authority for session persistence and security.
 * 
 * SECURITY ARCHITECTURE:
 * 1. STORAGE: Uses [PreferencesDataStore] for session persistence.
 * 2. ENCRYPTION (Tink): All sensitive strings (tokens, username) are encrypted 
 *    using AES-256 GCM (AEAD) before being written to disk.
 * 3. KEY PROTECTION: The Tink Master Key is stored in the [Android Keystore] 
 *    ('android-keystore://erthiscan_master_key'), ensuring it never leaves 
 *    the device's hardware-backed secure element.
 * 4. THREAD SAFETY: Uses a [Mutex] to prevent race conditions during 
 *    concurrent login/refresh/logout operations.
 */
@Singleton
class AuthManager @Inject constructor(
    // Context is required for DataStore initialization and shared preferences access.
    @ApplicationContext private val context: Context,
) {
    // PREFERENCE KEYS: Define the identifiers for stored session data.
    private val KEY_TOKEN = stringPreferencesKey("token")
    private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    private val KEY_USER_ID = intPreferencesKey("user_id")
    private val KEY_USERNAME = stringPreferencesKey("username")

    // CONCURRENCY TOOLS
    // SupervisorJob: Prevents a failure in one job from cancelling the whole scope.
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    // Mutex: Ensures that only one thread can modify the local session state or write 
    // to DataStore at any given time.
    private val mutex = Mutex()

    // CRYPTO CACHE: 
    // Volatile: Ensures that changes to the AEAD instance are immediately visible to all threads.
    @Volatile private var aeadCache: Aead? = null

    // REACTIVE STATE: 
    // The single source of truth for the entire UI (e.g. Profile, Voting logic).
    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    /**
     * SYNCHRONOUS ACCESSORS: 
     * Required for OkHttp Interceptors/Authenticators where a Flow collection 
     * or asynchronous wait is not practical during the request pipeline.
     */
    val accessToken: String? get() = _state.value.accessToken
    val refreshToken: String? get() = _state.value.refreshToken

    /**
     * AEAD PRIMITIVE: 
     * Lazy-loads or builds the Tink encryption engine.
     * Integrates with Android Keystore for the hardware-backed master key.
     */
    private suspend fun aead(): Aead = withContext(Dispatchers.IO) {
        // Double-check locking (or simple thread-safe assignment) for the cache.
        aeadCache ?: run {
            // ANDROID KEYSET MANAGER: Bridges Tink keysets with Android's secure storage.
            val handle = AndroidKeysetManager.Builder()
                // SharedPref: Stores the encrypted keyset on disk.
                .withSharedPref(context, "auth_keyset", "auth_keyset_prefs")
                // Template: AES256_GCM is the industry standard for AEAD.
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                // Master Key: Points to the Android Keystore entry.
                .withMasterKeyUri("android-keystore://erthiscan_master_key")
                .build()
                .keysetHandle
            
            // PRIMITIVE: The actual object that performs encrypt/decrypt operations.
            val a = handle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
            aeadCache = a
            a
        }
    }

    /**
     * ENCRYPT: 
     * Converts plaintext to an AEAD-encrypted byte array, then Base64 encodes it.
     * Use of NO_WRAP ensures the resulting string is a single line, suitable for DataStore.
     */
    private suspend fun enc(plaintext: String): String = withContext(Dispatchers.Default) {
        val ct = aead().encrypt(plaintext.toByteArray(), null)
        Base64.encodeToString(ct, Base64.NO_WRAP)
    }

    /**
     * DECRYPT: 
     * Decodes the Base64 ciphertext and decrypts using the hardware-protected key.
     */
    private suspend fun dec(ct: String): String = withContext(Dispatchers.Default) {
        val bytes = Base64.decode(ct, Base64.NO_WRAP)
        String(aead().decrypt(bytes, null))
    }

    /**
     * LOGIN: 
     * Encrypts and persists new session data obtained from the API.
     */
    suspend fun login(access: String, refresh: String, id: Int, name: String) = mutex.withLock {
        // Update in-memory state for immediate UI reaction.
        _state.value = AuthState(access, refresh, id, name)
        
        // Encrypt sensitive fields before disk I/O.
        val encAccess = enc(access)
        val encRefresh = enc(refresh)
        val encName = enc(name)
        
        // Persist to DataStore.
        context.authDataStore.edit { prefs ->
            prefs[KEY_TOKEN] = encAccess
            prefs[KEY_REFRESH_TOKEN] = encRefresh
            prefs[KEY_USER_ID] = id
            prefs[KEY_USERNAME] = encName
        }
    }

    /**
     * UPDATE TOKENS: 
     * Synchronously updates memory for immediate network retries (OkHttp 401 handling) 
     * and triggers an asynchronous background write to DataStore.
     */
    fun updateTokensFromAuthenticator(access: String, refresh: String) {
        // STEP 1: Update memory state (Fast).
        _state.value = _state.value.copy(accessToken = access, refreshToken = refresh)
        
        // STEP 2: Persist to disk (Slow/IO). We use the manager's internal scope.
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

    /**
     * RESTORE: 
     * Loads session from disk and decrypts. Called by [StartupViewModel] on app launch.
     */
    suspend fun restore() = mutex.withLock {
        // Read the first snapshot from DataStore.
        val prefs: Preferences = try {
            context.authDataStore.data.first()
        } catch (e: Exception) {
            Log.e("AuthManager", "Failed to read DataStore", e)
            return@withLock
        }
        
        // Check for existing data.
        val encToken = prefs[KEY_TOKEN] ?: return@withLock
        val encRefresh = prefs[KEY_REFRESH_TOKEN] ?: return@withLock
        val encName = prefs[KEY_USERNAME] ?: return@withLock
        val id = prefs[KEY_USER_ID] ?: return@withLock
        
        try {
            // Decrypt and populate the reactive state.
            _state.value = AuthState(
                accessToken = dec(encToken),
                refreshToken = dec(encRefresh),
                userId = id,
                username = dec(encName),
            )
        } catch (e: GeneralSecurityException) {
            // CRITICAL: If keyset is corrupted (e.g. system key was lost/wiped), 
            // the session ciphertext is useless. Wiping local data to force re-login.
            Log.e("AuthManager", "Keyset corrupted, wiping session", e)
            clearLocal()
        } catch (e: Exception) {
            Log.e("AuthManager", "Restore failure", e)
        }
    }

    /**
     * LOGOUT: Clears all local state.
     */
    suspend fun logoutLocal() = mutex.withLock {
        clearLocal()
    }

    /**
     * CLEAR LOCAL: Resets state and wipes the DataStore file.
     */
    private suspend fun clearLocal() {
        _state.value = AuthState()
        context.authDataStore.edit { it.clear() }
    }
}