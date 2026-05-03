package io.erthiscan.util

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager

/**
 * HAPTIC UTILITIES: Provides tactile feedback for user interactions.
 * 
 * ARCHITECTURAL ROLE:
 * Encapsulates the Android Vibrator API into a simple extension function. 
 * This ensures that haptic feedback (e.g., during torch toggle) is consistent 
 * across the app and uses the modern [VibratorManager] introduced in API 31.
 */

/**
 * VIBRATE SHORT: Triggers a brief tactile pulse.
 * 
 * @param durationMs Length of the vibration in milliseconds. Default (30ms) 
 *        is ideal for "tap" confirmation.
 * @param amplitude Intensity of the vibration (1-255). Default (50) is a 
 *        subtle, non-intrusive pulse.
 */
fun Context.vibrateShort(durationMs: Long = 30, amplitude: Int = 50) {
    // API 31+: Uses VibratorManager to acquire the default system vibrator.
    val vibrator = (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator

    // EXECUTION: Performs a single one-shot pulse with the specified properties.
    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
}