// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

import kotlin.math.abs

import kotlinx.coroutines.coroutineScope

/** Regole del popup che si chiude tirandolo oltre il bordo del contenuto. Testabile su JVM. */
object PullToDismiss {
    /** Quanto segue il dito: meno di 1, così si sente la resistenza. */
    const val RESISTANCE = 0.55f

    /** Spostamento minimo (dp) oltre il quale rilasciando il popup si chiude. */
    const val THRESHOLD_DP = 72f

    /** Velocità (dp/s) oltre la quale un colpo deciso chiude anche con poco spostamento. */
    const val FLING_DP = 700f

    /** Nuovo spostamento dopo un trascinamento di [delta] pixel, senza invertire il segno di colpo. */
    fun next(pull: Float, delta: Float): Float {
        val moved = pull + delta * RESISTANCE
        return if (pull != 0f && (pull > 0f) != (moved > 0f)) 0f else moved
    }

    /** Vero se, rilasciando con lo spostamento [pull] e la velocità [velocity] (pixel), il popup si chiude. */
    fun shouldDismiss(pull: Float, velocity: Float, thresholdPx: Float, flingPx: Float): Boolean {
        if (pull == 0f) return false
        if (abs(pull) >= thresholdPx) return true
        return abs(velocity) >= flingPx && (velocity > 0f) == (pull > 0f)
    }
}
