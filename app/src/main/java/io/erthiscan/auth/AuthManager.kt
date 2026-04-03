package io.erthiscan.auth

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.flow.first
import android.util.Base64
import android.util.Log
import io.erthiscan.api.ApiClient

private val Context.authDataStore by preferencesDataStore(name = "auth")

object AuthManager {
    var accessToken: String? by mutableStateOf(null)
        private set
    var userId: Int? by mutableStateOf(null)
        private set
    var username: String? by mutableStateOf(null)
        private set

    val isLoggedIn: Boolean get() = accessToken != null

    private val KEY_TOKEN = stringPreferencesKey("token")
    private val KEY_USER_ID = intPreferencesKey("user_id")
    private val KEY_USERNAME = stringPreferencesKey("username")

    private fun getAead(context: Context): Aead {
        AeadConfig.register()
        return AndroidKeysetManager.Builder()
            .withSharedPref(context, "auth_keyset", "auth_keyset_prefs")
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri("android-keystore://erthiscan_master_key")
            .build()
            .keysetHandle
            .getPrimitive(com.google.crypto.tink.RegistryConfiguration.get(), Aead::class.java)
    }

    private fun encrypt(aead: Aead, plaintext: String): String {
        val ciphertext = aead.encrypt(plaintext.toByteArray(), null)
        return Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    private fun decrypt(aead: Aead, ciphertext: String): String {
        val decoded = Base64.decode(ciphertext, Base64.NO_WRAP)
        return String(aead.decrypt(decoded, null))
    }

    suspend fun login(token: String, id: Int, name: String, context: Context) {
        accessToken = token
        userId = id
        username = name

        val aead = getAead(context)
        context.authDataStore.edit { prefs ->
            prefs[KEY_TOKEN] = encrypt(aead, token)
            prefs[KEY_USER_ID] = id
            prefs[KEY_USERNAME] = encrypt(aead, name)
        }
    }

    suspend fun restore(context: Context) {
        val aead = getAead(context)
        val prefs = context.authDataStore.data.first()
        val encryptedToken = prefs[KEY_TOKEN] ?: return
        val encryptedName = prefs[KEY_USERNAME] ?: return
        val id = prefs[KEY_USER_ID] ?: return

        try {
            accessToken = decrypt(aead, encryptedToken)
            userId = id
            username = decrypt(aead, encryptedName)
        } catch (e: Exception) {
            Log.e("AuthManager", "Failed to restore session", e)
            logout(context)
        }
    }

    suspend fun logout(context: Context) {
        if (accessToken != null) {
            try {
                ApiClient.api.logout()
            } catch (e: Exception) {
                Log.e("AuthManager", "Failed to call server logout", e)
            }
        }
        accessToken = null
        userId = null
        username = null
        context.authDataStore.edit { it.clear() }
    }
}
