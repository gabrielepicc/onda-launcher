// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

/** Cosa sa fare il motore aptico del telefono. */
enum class HapticSupport { LIGHT, BASIC, UNKNOWN, NONE }

/**
 * Feedback aptico di Onda: colpi diversi per eventi diversi (vedi [HapticPlan]).
 *
 * Segue l'impostazione di sistema "Feedback al tocco": con le costanti di sistema lo fa Android,
 * con le composizioni lo controlliamo noi e usiamo l'uso "tocco", così vale anche l'intensità scelta
 * nelle impostazioni di vibrazione (Android 13+).
 */
class OndaHaptics(private val view: View, private val enabled: Boolean) {

    private val vibrator: Vibrator? = vibratorOf(view.context)
    private var lastLetter = 0L

    private val supported: Set<Int> by lazy {
        val v = vibrator
        if (v == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) emptySet() else primitivesOf(v)
    }

    // I valori vengono da HapticPlan, che usa gli stessi numeri delle costanti di Android (vedi test)
    @SuppressLint("WrongConstant")
    fun perform(event: HapticEvent) {
        if (!enabled) return
        val now = SystemClock.uptimeMillis()
        if (!HapticPlan.shouldPlay(event, now, lastLetter)) return
        if (event == HapticEvent.LETTER) lastLetter = now

        val steps = HapticPlan.composition(event, supported)
        if (steps != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && systemTouchFeedbackOn()) {
            runCatching { play(steps) }.onSuccess { return }
        }
        view.performHapticFeedback(HapticPlan.fallback(event, Build.VERSION.SDK_INT))
    }

    @SuppressLint("WrongConstant")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun play(steps: List<HapticStep>) {
        val v = vibrator ?: error("nessun vibratore")
        val composition = VibrationEffect.startComposition()
        steps.forEach { composition.addPrimitive(it.primitive, it.scale, it.delayMs) }
        val effect = composition.compose()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
        } else {
            v.vibrate(effect)
        }
    }

    /** "Feedback al tocco" di sistema: se è spento, niente composizioni. */
    private fun systemTouchFeedbackOn(): Boolean =
        runCatching { Settings.System.getInt(view.context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0 }
            .getOrDefault(true)

    companion object {
        fun vibratorOf(context: Context): Vibrator? = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        }.getOrNull()?.takeIf { it.hasVibrator() }

        @SuppressLint("WrongConstant")
        @RequiresApi(Build.VERSION_CODES.R)
        private fun primitivesOf(v: Vibrator): Set<Int> {
            val all = intArrayOf(
                HapticPlan.PRIMITIVE_CLICK, HapticPlan.PRIMITIVE_THUD, HapticPlan.PRIMITIVE_SPIN, HapticPlan.PRIMITIVE_QUICK_RISE,
                HapticPlan.PRIMITIVE_SLOW_RISE, HapticPlan.PRIMITIVE_QUICK_FALL, HapticPlan.PRIMITIVE_TICK, HapticPlan.PRIMITIVE_LOW_TICK,
            )
            val ok = runCatching { v.arePrimitivesSupported(*all) }.getOrNull() ?: return emptySet()
            return all.filterIndexed { i, _ -> ok.getOrElse(i) { false } }.toSet()
        }

        /** Per la prova nelle impostazioni: le vibrazioni leggere sono disponibili? */
        fun support(context: Context): HapticSupport {
            val v = vibratorOf(context) ?: return HapticSupport.NONE
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return HapticSupport.UNKNOWN
            val p = primitivesOf(v)
            return when {
                HapticPlan.PRIMITIVE_CLICK in p && HapticPlan.PRIMITIVE_TICK in p -> HapticSupport.LIGHT
                else -> HapticSupport.BASIC
            }
        }
    }
}

val LocalOndaHaptics = staticCompositionLocalOf<OndaHaptics?> { null }

/** Crea il motore aptico legato alla vista corrente. */
@Composable
fun rememberOndaHaptics(enabled: Boolean): OndaHaptics {
    val view = LocalView.current
    return remember(view, enabled) { OndaHaptics(view, enabled) }
}
