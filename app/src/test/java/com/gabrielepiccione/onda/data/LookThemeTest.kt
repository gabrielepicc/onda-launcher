// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LookThemeTest {

    private val personal = Settings(favorites = listOf("a", "b"), hidden = setOf("h"), widgetIds = listOf(3))

    @Test
    fun captureThenApplyRestoresLookAndKeepsPersonalData() {
        val styled = personal.copy(themeMode = ThemeMode.DARK, clockStyle = ClockStyle.WORDS, iconStyle = "MARBLE", textShadow = true)
        val theme = LookTheme.capture(styled, "t1", "Sera")
        val other = personal.copy(themeMode = ThemeMode.LIGHT, clockStyle = ClockStyle.BOLD)
        val applied = theme.applyTo(other)
        assertEquals(ThemeMode.DARK, applied.themeMode)
        assertEquals(ClockStyle.WORDS, applied.clockStyle)
        assertEquals("MARBLE", applied.iconStyle)
        assertEquals(listOf("a", "b"), applied.favorites)
        assertEquals(setOf("h"), applied.hidden)
        assertEquals(listOf(3), applied.widgetIds)
    }

    @Test
    fun themeFileRoundTrip() {
        val file = ThemeFile(theme = LookTheme("x", "Prova", accent = 0xFF112233), fonts = mapOf("a.ttf" to "AAAA"))
        assertEquals(file, ThemeFile.decode(ThemeFile.encode(file)))
    }

    @Test
    fun foreignJsonIsRejected() {
        assertNull(ThemeFile.decode("""{"format":"altro","theme":{"id":"x","name":"y"}}"""))
    }
}
