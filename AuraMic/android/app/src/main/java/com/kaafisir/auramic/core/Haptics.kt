package com.kaafisir.auramic.core

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Small vocabulary of vibration cues. [enabled] mirrors the user's setting. */
@Singleton
@android.annotation.SuppressLint("InlinedApi") // EFFECT_* are only dereferenced on API 29+
class Haptics @Inject constructor(@ApplicationContext context: Context) {
    @Volatile var enabled: Boolean = true

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private fun play(predefined: Int, fallbackMs: Long) {
        val v = vibrator ?: return
        if (!enabled || !v.hasVibrator()) return
        runCatching {
            val effect = if (Build.VERSION.SDK_INT >= 29) VibrationEffect.createPredefined(predefined)
            else VibrationEffect.createOneShot(fallbackMs, VibrationEffect.DEFAULT_AMPLITUDE)
            v.vibrate(effect)
        }
    }

    /** Light confirmation, e.g. drag snap or cancel zone entered. */
    fun tick() = play(VibrationEffect.EFFECT_TICK, 12)
    /** Recording started. */
    fun press() = play(VibrationEffect.EFFECT_HEAVY_CLICK, 30)
    /** Successful hand-off. */
    fun success() = play(VibrationEffect.EFFECT_CLICK, 20)

    fun error() {
        val v = vibrator ?: return
        if (!enabled || !v.hasVibrator()) return
        runCatching { v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 40), -1)) }
    }
}
