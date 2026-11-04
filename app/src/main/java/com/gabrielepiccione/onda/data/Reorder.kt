// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import kotlin.math.roundToInt

/** Calcoli del riordino trascinando, senza dipendenze Android (testabili su JVM). */
object Reorder {

    /** Posizione in cui finirebbe la riga [from] trascinata di [offsetPx], con righe alte [rowPx]. */
    fun target(from: Int, offsetPx: Float, rowPx: Float, size: Int): Int =
        (from + (offsetPx / rowPx).roundToInt()).coerceIn(0, size - 1)

    /** Di quante righe si sposta la riga [index] per fare posto a quella trascinata. */
    fun shift(index: Int, from: Int, target: Int): Int = when {
        from < 0 -> 0
        from < target && index in (from + 1)..target -> -1
        target < from && index in target until from -> 1
        else -> 0
    }

    fun <T> moved(list: List<T>, from: Int, to: Int): List<T> {
        if (from !in list.indices || to !in list.indices) return list
        return list.toMutableList().apply { add(to, removeAt(from)) }
    }
}
