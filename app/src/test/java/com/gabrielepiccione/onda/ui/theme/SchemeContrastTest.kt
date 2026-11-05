// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/** I temi senza colori dinamici devono restare leggibili con ogni accento, chiari e scuri. */
class SchemeContrastTest {

    private fun ratio(a: Color, b: Color): Float {
        val high = max(a.luminance(), b.luminance())
        val low = min(a.luminance(), b.luminance())
        return (high + 0.05f) / (low + 0.05f)
    }

    private fun forEachScheme(check: (String, ColorScheme) -> Unit) {
        AccentChoices.forEach { accent ->
            listOf(true, false).forEach { dark ->
                check("accent=${accent.toString(16)} dark=$dark", staticScheme(Color(accent), dark))
            }
        }
    }

    private fun assertReadable(where: String, text: Color, background: Color) {
        val value = ratio(text, background)
        assertTrue("$where: contrasto $value sotto 4,5", value >= 4.5f)
    }

    @Test
    fun textIsReadableOnPanels() = forEachScheme { where, s ->
        assertReadable(where, s.onSurface, s.surfaceContainer)
        assertReadable(where, s.onSurfaceVariant, s.surfaceContainer)
        assertReadable(where, s.onSurfaceVariant, s.surfaceContainerHigh)
    }

    @Test
    fun accentTextIsReadableOnPanels() = forEachScheme { where, s ->
        assertReadable(where, s.primary, s.surfaceContainer)
    }

    @Test
    fun textOnAccentIsReadable() = forEachScheme { where, s ->
        assertReadable(where, s.onPrimary, s.primary)
        assertReadable(where, s.onPrimaryContainer, s.primaryContainer)
    }
}
