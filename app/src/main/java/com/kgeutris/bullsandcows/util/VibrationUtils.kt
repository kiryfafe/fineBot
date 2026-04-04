package com.kgeutris.bullsandcows.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

@Suppress("DEPRECATION")
fun Context.vibrateShort() {
    vibrate(50L)
}

@Suppress("DEPRECATION")
fun Context.vibrateError() {
    vibrate(100L)
}

@Suppress("DEPRECATION")
private fun Context.vibrate(durationMs: Long) {
    // Получаем Vibrator
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (!vibrator.hasVibrator()) return

    // Вибрация
    val effect = VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
    // Используем безопасный вызов через try-catch (на случай ProGuard/Shrink)
    try {
        vibrator.vibrate(effect)
    } catch (_: Exception) {
        // Fallback на старый метод
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            vibrator.vibrate(durationMs)
        }
    }
}