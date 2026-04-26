package io.erthiscan.util

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager

fun Context.vibrateShort(durationMs: Long = 30, amplitude: Int = 50) {
    val vibrator = (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
}