// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockTextTest {
    @Test
    fun digitsWithAndWithoutSeconds() {
        assertEquals("14:07", ClockText.digits(14, 7, 33, seconds = false))
        assertEquals("14:07:33", ClockText.digits(14, 7, 33, seconds = true))
        assertEquals("00:00:05", ClockText.digits(0, 0, 5, seconds = true))
    }

    @Test
    fun romanNumerals() {
        assertEquals("XIV", ClockText.roman(14))
        assertEquals("VII", ClockText.roman(7))
        assertEquals("XLIX", ClockText.roman(49))
        assertEquals("LIX", ClockText.roman(59))
        assertEquals("—", ClockText.roman(0))
        assertEquals("XIV : VII", ClockText.romanTime(14, 7))
        assertEquals("— : —", ClockText.romanTime(0, 0))
    }

    @Test
    fun sevenSegmentsLightTheRightBars() {
        // 8 accende tutto, 1 solo i due a destra, 0 tutto tranne il centro
        assertTrue(ClockText.segments('8').all { it })
        assertEquals(listOf(false, true, true, false, false, false, false), ClockText.segments('1'))
        assertEquals(listOf(true, true, true, true, true, true, false), ClockText.segments('0'))
        assertTrue(ClockText.segments('x').none { it })
        assertEquals(10, ('0'..'9').count { ClockText.segments(it).any { on -> on } })
    }

    @Test
    fun dotGlyphsAreFiveByseven() {
        (('0'..'9') + ':').forEach { c ->
            val g = ClockText.dotGlyph(c)
            assertEquals("riga di $c", ClockText.DOT_ROWS, g.size)
            assertTrue("larghezza di $c", g.all { it.length == ClockText.DOT_COLUMNS && it.all { ch -> ch == '0' || ch == '1' } })
        }
        // Un carattere sconosciuto è vuoto
        assertTrue(ClockText.dotGlyph('?').all { row -> row.all { it == '0' } })
        // L'1 ha il fusto al centro
        assertEquals("00100", ClockText.dotGlyph('1')[3])
    }
}
