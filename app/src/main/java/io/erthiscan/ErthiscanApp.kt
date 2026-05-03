package io.erthiscan

import android.app.Application
import android.util.Log
import com.google.crypto.tink.aead.AeadConfig
import dagger.hilt.android.HiltAndroidApp

/**
 * CORE APPLICATION CLASS: The primary entry point for the Erthiscan Android application process.
 *
 * ARCHITECTURE RESPONSIBILITIES:
 * 1. HILT (@HiltAndroidApp): Triggers Hilt's code generation, creating the base DI container
 *    (SingletonComponent). This container lives for the entire application lifetime 
 *    and provides singleton instances like AuthManager and Retrofit.
 * 2. CRYPTOGRAPHY (Tink): Initializes the AEAD (Authenticated Encryption with Associated Data)
 *    subsystem. This is a hard requirement for secure session token storage in DataStore.
 */
@HiltAndroidApp
class ErthiscanApp : Application() {
    /**
     * ON CREATE: 
     * Called when the application is starting, before any activity, service, 
     * or receiver objects (excluding content providers) have been created.
     */
    override fun onCreate() {
        super.onCreate()
        try {
            // TINK REGISTRATION: 
            // Must be called before any cryptographic primitive (Aead) is requested.
            // This populates the internal Tink registry with standard AES-GCM configurations.
            AeadConfig.register()
        } catch (e: Exception) {
            // Failure here is catastrophic for authenticated features. 
            // Logging for debugging purposes.
            Log.e("ErthiscanApp", "Tink AeadConfig.register() failed", e)
        }
    }
}
