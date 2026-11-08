// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/** Regole di impaginazione dei menu e delle scelte, senza dipendenze Android (testabili su JVM). */
object DesignRules {
    /** Come mostrare una scelta: pulsanti segmentati con poche opzioni, altrimenti un elenco. */
    enum class ChoiceStyle { SEGMENTED, LIST }

    const val MAX_SEGMENTS = 4

    fun choiceStyle(options: Int): ChoiceStyle = if (options in 1..MAX_SEGMENTS) ChoiceStyle.SEGMENTED else ChoiceStyle.LIST

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
