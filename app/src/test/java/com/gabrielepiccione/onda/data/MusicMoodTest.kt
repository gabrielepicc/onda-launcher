// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicMoodTest {
    private val darkText = 0xFFE3E6E8.toInt()
    private val lightText = 0xFF171C1F.toInt()

    @Test
    fun vividContrastedCoversMoveFasterThanDullOnes() {
        val vivid = MusicMood.from(listOf(0xFFFF1744.toInt(), 0xFF00E5FF.toInt(), 0xFF111111.toInt(), 0xFFFFEA00.toInt()))
        val dull = MusicMood.from(listOf(0xFF3E3A39.toInt(), 0xFF4A4644.toInt(), 0xFF35302F.toInt()))
        assertTrue(vivid.speed > dull.speed)
        assertTrue(vivid.turbulence > dull.turbulence)
        assertTrue(vivid.pulse > dull.pulse)
    }

    @Test
    fun allParametersStayInRange() {
        val covers = listOf(
            listOf(0xFFFFFFFF.toInt()),
            listOf(0xFF000000.toInt()),
            listOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt()),
            listOf(0xFF808080.toInt(), 0xFF7F7F7F.toInt()),
        )
        covers.forEach { c ->
            val m = MusicMood.from(c)
            assertTrue(m.speed in 0.15f..1f)
            assertTrue(m.turbulence in 0f..1f)
            assertTrue(m.brightness in 0.35f..0.85f)
            assertTrue(m.pulse in 0f..0.15f)
        }
        assertEquals(MusicMood.NEUTRAL, MusicMood.from(emptyList()))
    }

    @Test
    fun textStaysReadableOnAnyCover() {
        val samples = listOf(0xFFFFFFFF, 0xFF000000, 0xFFFFEB3B, 0xFF00E5FF, 0xFFFF1744, 0xFF7C4DFF, 0xFF8BC34A).map { it.toInt() }
        samples.forEach { c ->
            val dark = MusicMood.readable(c, dark = true)
            val light = MusicMood.readable(c, dark = false)
            assertTrue("scuro ${Integer.toHexString(c)}: ${MusicMood.contrast(dark, darkText)}", MusicMood.contrast(dark, darkText) >= 4.5)
            assertTrue("chiaro ${Integer.toHexString(c)}: ${MusicMood.contrast(light, lightText)}", MusicMood.contrast(light, lightText) >= 4.5)
        }
    }

    @Test
    fun stoppingSlowsDownThenFades() {
        val p = MusicMood.NEUTRAL
        assertEquals(p.speed, MusicMood.speedAfterStop(p, 0), 0.001f)
        assertEquals(0f, MusicMood.speedAfterStop(p, MusicMood.SLOWDOWN_MS), 0.001f)
        assertEquals(1f, MusicMood.alphaAfterStop(0), 0.001f)
        assertEquals(0f, MusicMood.alphaAfterStop(MusicMood.SLOWDOWN_MS + MusicMood.FADE_MS), 0.001f)
    }

    @Test
    fun hslOfPrimaryColors() {
        val red = MusicMood.hsl(0xFFFF0000.toInt())
        assertEquals(0.0, red[0], 0.001)
        assertEquals(1.0, red[1], 0.001)
        assertEquals(0.5, red[2], 0.001)
        assertEquals(0.0, MusicMood.hsl(0xFF808080.toInt())[1], 0.001)
    }
}
