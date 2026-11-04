// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/** Calcoli di layout per schermi larghi, senza dipendenze Android (testabili su JVM). */
object ScreenLayout {
    const val MAX_CONTENT_DP = 560

    /** Margine extra per lato: su tablet e in orizzontale il contenuto resta centrato e leggibile. */
    fun sideInsetDp(screenWidthDp: Int, maxContentDp: Int = MAX_CONTENT_DP): Int =
        ((screenWidthDp - maxContentDp) / 2).coerceAtLeast(0)
}
