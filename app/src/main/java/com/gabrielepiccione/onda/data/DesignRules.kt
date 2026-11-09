// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/** Regole di impaginazione dei menu e delle scelte, senza dipendenze Android (testabili su JVM). */
object DesignRules {
    /** Come mostrare una scelta: pulsanti segmentati con poche opzioni, altrimenti un elenco. */
    enum class ChoiceStyle { SEGMENTED, LIST }

    /** Pulsanti segmentati solo con poche opzioni ed etichette corte: altrimenti il testo si tronca. */
    const val MAX_SEGMENTS = 3
    const val MAX_SEGMENT_LABEL = 10

    /** Scala del carattere per cui le etichette devono restare intere. */
    const val FONT_SCALE_MARGIN = 1.3f

    /**
     * Segmentato solo con 1–3 opzioni ed etichette di al più 10 caratteri (misurate con margine per la scala 1,3);
     * con un carattere più grande di 1,3 il limite si accorcia in proporzione.
     */
    fun choiceStyle(labels: List<String>, fontScale: Float = 1f): ChoiceStyle {
        val limit = if (fontScale <= FONT_SCALE_MARGIN) MAX_SEGMENT_LABEL else (MAX_SEGMENT_LABEL * FONT_SCALE_MARGIN / fontScale).toInt()
        val fits = labels.size in 1..MAX_SEGMENTS && labels.all { it.length <= limit }
        return if (fits) ChoiceStyle.SEGMENTED else ChoiceStyle.LIST
    }

    /** Ordine standard dei gruppi in ogni menu: dall'identità al pericolo, sempre in fondo. */
    enum class MenuGroup { IDENTITY, FREQUENT, APPEARANCE, BEHAVIOR, ADVANCED, DANGER }

    /** Ordina gruppi qualunque secondo l'ordine standard. */
    fun <T> ordered(groups: List<T>, kindOf: (T) -> MenuGroup): List<T> = groups.sortedBy { kindOf(it).ordinal }

    /** Passi intermedi di uno slider a tacche (valori discreti) tra [min] e [max] con passo [step]; 0 = continuo. */
    fun sliderSteps(min: Float, max: Float, step: Float): Int {
        if (step <= 0f || max <= min) return 0
        val count = ((max - min) / step + 0.5f).toInt()
        return (count - 1).coerceAtLeast(0)
    }

    /** Altezza massima di un foglio (frazione dello schermo). */
    const val SHEET_MAX_FRACTION = 0.9f
}
