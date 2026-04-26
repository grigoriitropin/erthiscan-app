package io.erthiscan

import android.app.Application
import android.util.Log
import com.google.crypto.tink.aead.AeadConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ErthiscanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            AeadConfig.register()
        } catch (e: Exception) {
            Log.e("ErthiscanApp", "Tink AeadConfig.register() failed", e)
        }
    }
}
