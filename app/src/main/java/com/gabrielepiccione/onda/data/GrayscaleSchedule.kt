// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import kotlinx.serialization.Serializable

/** Quando Onda diventa in bianco e nero. */
@Serializable
enum class GrayscaleMode { OFF, SCHEDULE, PROFILE }

/** Regole del bianco e nero a orario, senza dipendenze Android (testabili su JVM). */
object GrayscaleSchedule {
    const val DEFAULT_START = 22 * 60 + 30
    const val DEFAULT_END = 7 * 60

    /**
     * Vero se [minute] (minuti dalla mezzanotte) cade tra [start] e [end]. La fascia può passare la
     * mezzanotte (22:30–7:00); con inizio e fine uguali la fascia è vuota.
     */
    fun inside(minute: Int, start: Int, end: Int): Boolean = when {
        start == end -> false
        start < end -> minute in start until end
        else -> minute >= start || minute < end
    }

    /**
     * Onda è in bianco e nero adesso? [activeProfileId] è il profilo dei preferiti attivo ora (vuoto se
     * nessuno): con la modalità "profilo" il bianco e nero dura finché quel profilo è attivo.
     */
    fun active(
        mode: GrayscaleMode,
        minute: Int,
        start: Int,
        end: Int,
        profileId: String,
        activeProfileId: String?,
    ): Boolean = when (mode) {
        GrayscaleMode.OFF -> false
        GrayscaleMode.SCHEDULE -> inside(minute, start, end)
        GrayscaleMode.PROFILE -> profileId.isNotEmpty() && profileId == activeProfileId
    }

    /** Saturazione da dare ai colori: 1 = a colori, 0 = bianco e nero. [progress] va da 0 a 1 col passaggio morbido. */
    fun saturation(progress: Float): Float = 1f - progress.coerceIn(0f, 1f)

    /** Tra quanti minuti cambia lo stato, per riguardare l'orario al momento giusto (almeno 1). */
    fun minutesToChange(minute: Int, start: Int, end: Int): Int {
        if (start == end) return 24 * 60
        val toStart = ((start - minute) % (24 * 60) + 24 * 60) % (24 * 60)
        val toEnd = ((end - minute) % (24 * 60) + 24 * 60) % (24 * 60)
        return minOf(if (toStart == 0) 24 * 60 else toStart, if (toEnd == 0) 24 * 60 else toEnd).coerceAtLeast(1)
    }
}
