// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

import kotlin.math.abs

/**
 * Resa dello swipe sulle righe delle app, senza dipendenze Android (testabile su JVM). La riga che si
 * sposta resta trasparente: la traccia dietro nasce invisibile e si colora man mano che si trascina.
 */
object SwipeFeedback {
    /** Opacità massima della traccia in accento prima della soglia. */
    const val TRACK_MAX = 0.22f

    /** Superata la soglia la traccia passa al colore contenitore a questa opacità. */
    const val TRACK_ARMED = 0.35f

    /** Velatura leggerissima sulla riga che si sposta, nel colore del testo. */
    const val VEIL = 0.07f

    /** Quanto la traccia va oltre la soglia: l'icona si ingrandisce un poco una volta superata. */
    const val ARMED_SCALE = 1.15f

    /** Avanzamento da 0 a 1 verso la soglia. */
    fun progress(offset: Float, trigger: Float): Float = if (trigger <= 0f) 0f else (abs(offset) / trigger).coerceIn(0f, 1f)

    fun trackAlpha(progress: Float, armed: Boolean): Float = if (armed) TRACK_ARMED else TRACK_MAX * progress.coerceIn(0f, 1f)

    fun iconAlpha(progress: Float): Float = progress.coerceIn(0f, 1f)

    fun iconScale(progress: Float, armed: Boolean): Float = (0.8f + 0.2f * progress.coerceIn(0f, 1f)) * (if (armed) ARMED_SCALE else 1f)

    fun veilAlpha(progress: Float): Float = VEIL * progress.coerceIn(0f, 1f)
}
