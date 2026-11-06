// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import kotlin.math.cos
import kotlin.math.sin

/** Una macchia di colore: centro in frazioni della larghezza e dell'altezza, raggio in frazioni del lato maggiore. */
data class Blob(val cx: Float, val cy: Float, val radius: Float)

/**
 * Moto delle macchie di colore dello sfondo musicale: lo stesso disegno per la home (versione senza shader)
 * e per il live wallpaper di sistema. Logica pura, testabile su JVM.
 */
object WaveBlobs {
    const val COUNT = 3

    /** Posizione della macchia [index] al tempo [time] (secondi di animazione); [beat] 0–1 ne allarga il raggio. */
    fun blob(index: Int, time: Float, beat: Float = 0f): Blob {
        val a = time * (0.6f + 0.25f * index) + index * 2.1f
        return Blob(0.5f + 0.35f * cos(a), 0.5f + 0.3f * sin(a * 0.8f), 0.55f + 0.08f * beat)
    }

    /** Quattro colori leggibili per lo sfondo, dai colori della copertina (o da quelli predefiniti se mancano). */
    fun readableColors(cover: List<Int>, dark: Boolean): List<Int> {
        val base = cover.ifEmpty { listOf(0xFF3D5AFE.toInt(), 0xFF00BFA5.toInt(), 0xFF7C4DFF.toInt()) }
        return List(4) { i -> MusicMood.readable(base[i % base.size], dark) }
    }

    /** Trasparenza verso cui tende lo sfondo: piena intensità con la musica, appena visibile senza. */
    fun targetAlpha(playing: Boolean, intensity: Float): Float =
        if (playing) intensity.coerceIn(0.1f, 1f) else IDLE_ALPHA

    const val IDLE_ALPHA = 0.25f
}
