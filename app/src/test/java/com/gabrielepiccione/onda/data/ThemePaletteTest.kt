// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Il tema, con ogni coppia di colori scelta dall'utente, resta leggibile (WCAG AA) in chiaro e in scuro. */
class ThemePaletteTest {

    /** Colori d'accento proposti, colori di sistema tipici ed estremi (bianco, nero, grigi, gialli, blu puri). */
    private val seeds = listOf(
        0xFF7FE3D1, 0xFF8AB4F8, 0xFFC3A6FF, 0xFFFFAB91, 0xFFFFD27F, 0xFFA5D6A7,
        0xFF6750A4, 0xFF006A6A, 0xFFB3261E,
        0xFFFFFFFF, 0xFF000000, 0xFF808080, 0xFFFFFF00, 0xFF0000FF, 0xFFFF0000, 0xFF00FF00,
    ).map { it.toInt() }

    private fun forEachPalette(check: (String, PaletteRoles) -> Unit) {
        for (primary in seeds) for (accent in seeds) for (dark in listOf(true, false)) {
            check("primario=${primary.toUInt().toString(16)} accento=${accent.toUInt().toString(16)} scuro=$dark", ThemePalette.roles(primary, accent, dark))
        }
    }

    private fun assertAa(where: String, what: String, fg: Int, bg: Int, min: Double = Wcag.AA_TEXT) {
        val ratio = Wcag.contrast(fg, bg)
        assertTrue("$where: $what ha contrasto $ratio, sotto $min", ratio >= min)
    }

    @Test
    fun textIsReadableOnEverySurface() = forEachPalette { where, r ->
        listOf(r.surface, r.surfaceContainerLow, r.surfaceContainer, r.surfaceContainerHigh, r.surfaceContainerHighest).forEach { bg ->
            assertAa(where, "testo su superficie", r.onSurface, bg)
            assertAa(where, "testo secondario", r.onSurfaceVariant, bg)
            assertAa(where, "primario come testo", r.primary, bg)
            assertAa(where, "accento come testo", r.accent, bg)
        }
    }

    @Test
    fun textOnColoredFillsIsReadable() = forEachPalette { where, r ->
        assertAa(where, "testo sul primario", r.onPrimary, r.primary)
        assertAa(where, "testo sul contenitore del primario", r.onPrimaryContainer, r.primaryContainer)
        assertAa(where, "testo sull'accento", r.onAccent, r.accent)
        assertAa(where, "testo sul contenitore dell'accento", r.onAccentContainer, r.accentContainer)
    }

    @Test
    fun outlinesAreVisible() = forEachPalette { where, r -> assertAa(where, "bordo", r.outline, r.surface, 3.0) }

    @Test
    fun containersStandOutFromTheSurface() = forEachPalette { where, r ->
        assertTrue("$where: contenitore troppo simile alla superficie", Wcag.contrast(r.primaryContainer, r.surface) >= ThemePalette.CONTAINER_CONTRAST - 0.001)
        assertTrue("$where: contenitore dell'accento troppo simile alla superficie", Wcag.contrast(r.accentContainer, r.surface) >= ThemePalette.CONTAINER_CONTRAST - 0.001)
    }

    @Test
    fun surfacesStepInOrder() = forEachPalette { where, r ->
        // Dal più lontano al più vicino al testo: le superfici tonali sono distinte e ordinate
        val ls = listOf(r.surface, r.surfaceContainerLow, r.surfaceContainer, r.surfaceContainerHigh, r.surfaceContainerHighest).map { Wcag.luminance(it) }
        val ordered = ls.zipWithNext().all { (a, b) -> a < b } || ls.zipWithNext().all { (a, b) -> a > b }
        assertTrue("$where: superfici non ordinate $ls", ordered)
    }

    @Test
    fun darkAndLightAreActuallyDifferent() {
        val dark = ThemePalette.roles(0xFF6750A4.toInt(), 0xFF006A6A.toInt(), true)
        val light = ThemePalette.roles(0xFF6750A4.toInt(), 0xFF006A6A.toInt(), false)
        assertTrue(Wcag.luminance(dark.surface) < 0.05)
        assertTrue(Wcag.luminance(light.surface) > 0.8)
    }

    @Test
    fun greySeedsStayNeutral() {
        val r = ThemePalette.roles(0xFF808080.toInt(), 0xFF808080.toInt(), true)
        val (_, s, _) = Hsl.of(r.surface)
        assertEquals(0.0, s, 0.02)
    }

    @Test
    fun accentFollowsTheChoiceThenTheSystemThenTheHue() {
        val sysP = 0xFF112233.toInt()
        val sysA = 0xFF445566.toInt()
        // Scelto dall'utente: vince su tutto
        assertEquals(0xFFAA0000.toInt(), ThemePalette.resolveSeeds(0xFF7FE3D1, true, 0xFFAA0000, sysP, sysA).second)
        // Dinamici attivi: primario e accento di sistema
        assertEquals(sysP to sysA, ThemePalette.resolveSeeds(0xFF7FE3D1, true, 0, sysP, sysA))
        // Dinamici spenti: primario statico, accento con la tinta ruotata
        val (p, a) = ThemePalette.resolveSeeds(0xFF7FE3D1, false, 0, sysP, sysA)
        assertEquals(0xFF7FE3D1.toInt(), p)
        assertTrue(a != p)
        // Dinamici senza colore di accento di sistema: ruotato dal primario di sistema
        assertEquals(ThemePalette.rotateHue(sysP, 40.0), ThemePalette.resolveSeeds(0xFF7FE3D1, true, 0, sysP, null).second)
    }

    @Test
    fun hslRoundTrips() {
        listOf(0xFF7FE3D1, 0xFFFF0000, 0xFF123456, 0xFFFFFF00).map { it.toInt() }.forEach { c ->
            val (h, s, l) = Hsl.of(c)
            val back = Hsl.argb(h, s, l)
            for (shift in listOf(16, 8, 0)) {
                assertEquals(((c shr shift) and 0xFF).toDouble(), ((back shr shift) and 0xFF).toDouble(), 1.0)
            }
        }
    }

    @Test
    fun wcagKnownValues() {
        assertEquals(21.0, Wcag.contrast(Wcag.WHITE, 0xFF000000.toInt()), 0.01)
        assertEquals(1.0, Wcag.contrast(0xFF123456.toInt(), 0xFF123456.toInt()), 0.001)
    }
}
