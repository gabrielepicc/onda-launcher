// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BeatTrackerTest {
    // 1024 campioni a 44,1 kHz: ogni coppia vale circa 43 Hz
    private val rate = 44_100_000
    private fun spectrum(vararg bins: Pair<Int, Int>): ByteArray {
        val fft = ByteArray(1024)
        bins.forEach { (k, value) -> fft[2 * k] = value.toByte() }
        return fft
    }

    @Test
    fun onlyBassBinsCount() {
        // Bin 1 (43 Hz) e 3 (129 Hz) sono bassi, bin 10 (430 Hz) no
        assertTrue(BeatTracker.bassEnergy(spectrum(1 to 100, 3 to 100), rate) > 0f)
        assertEquals(0f, BeatTracker.bassEnergy(spectrum(10 to 100), rate), 0.001f)
        assertEquals(0f, BeatTracker.bassEnergy(ByteArray(2), rate), 0.001f)
    }

    @Test
    fun pulseRisesFastAndFallsSlowly() {
        val t = BeatTracker()
        val up = t.update(60f)
        assertTrue(up > 0.5f)
        val down1 = t.update(0f)
        val down2 = t.update(0f)
        assertTrue(down1 < up && down1 > 0.4f)
        assertTrue(down2 < down1)
    }

    @Test
    fun staysWithinZeroAndOne() {
        val t = BeatTracker()
        repeat(200) { i -> assertTrue(t.update(if (i % 7 == 0) 500f else 3f) in 0f..1f) }
        t.reset()
        assertEquals(0f, t.update(0f), 0.001f)
    }
}
