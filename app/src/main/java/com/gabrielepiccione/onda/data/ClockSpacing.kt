// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import kotlin.math.roundToInt

/** Spazio tra ora, data e riga dell'evento nel blocco Orologio, senza dipendenze Android (testabile su JVM). */
object ClockSpacing {
    const val MIN = 0
    const val MAX = 32
    const val STEP = 4
    const val DEFAULT = 8

    /** Valore valido più vicino: passo da 4 dp, tra 0 e 32. */
    fun snap(value: Float): Int = ((value / STEP).roundToInt() * STEP).coerceIn(MIN, MAX)

    fun coerce(value: Int): Int = value.coerceIn(MIN, MAX)

    /** Anteprima nelle impostazioni: stesse proporzioni dello spazio tra i blocchi (diviso 3). */
    fun previewDp(value: Int): Float = coerce(value) / 3f
}
