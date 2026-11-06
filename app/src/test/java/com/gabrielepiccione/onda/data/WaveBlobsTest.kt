// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaveBlobsTest {
    @Test
    fun blobsStayInsideTheScreenArea() {
        for (i in 0 until WaveBlobs.COUNT) for (t in 0..200) {
            val b = WaveBlobs.blob(i, t / 10f)
            assertTrue(b.cx in 0.1f..0.9f)
            assertTrue(b.cy in 0.1f..0.9f)
        }
    }

    @Test
    fun blobsMoveOverTimeAndDifferFromEachOther() {
        assertNotEquals(WaveBlobs.blob(0, 0f), WaveBlobs.blob(0, 3f))
        assertNotEquals(WaveBlobs.blob(0, 1f), WaveBlobs.blob(1, 1f))
    }

    @Test
    fun beatWidensTheRadius() {
        assertTrue(WaveBlobs.blob(0, 0f, beat = 1f).radius > WaveBlobs.blob(0, 0f, beat = 0f).radius)
    }

    @Test
    fun colorsAreFourEvenWithoutACover() {
        assertEquals(4, WaveBlobs.readableColors(emptyList(), true).size)
        assertEquals(4, WaveBlobs.readableColors(listOf(0xFFFF0000.toInt()), false).size)
    }

    @Test
    fun idleIsFainterThanPlayingAndIntensityIsClamped() {
        assertTrue(WaveBlobs.targetAlpha(false, 0.7f) < WaveBlobs.targetAlpha(true, 0.7f))
        assertEquals(1f, WaveBlobs.targetAlpha(true, 5f), 0f)
        assertEquals(0.1f, WaveBlobs.targetAlpha(true, 0f), 0f)
    }
}
