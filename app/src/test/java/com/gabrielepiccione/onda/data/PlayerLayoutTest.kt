// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerLayoutTest {
    @Test
    fun cornersAreConcentric() {
        assertEquals(20f, PlayerLayout.concentricRadius(28f, 8f), 0f)
        assertEquals(18f, PlayerLayout.concentricRadius(28f, 10f), 0f)
        assertEquals(0f, PlayerLayout.concentricRadius(8f, 12f), 0f)
    }

    @Test
    fun artFillsTheHeightMinusPadding() {
        assertEquals(48f, PlayerLayout.artSize(PlayerLayout.COMPACT_HEIGHT, PlayerLayout.COMPACT_PADDING), 0f)
        assertEquals(0f, PlayerLayout.artSize(10f, 8f), 0f)
    }

    @Test
    fun paddingStaysInTheRequestedRange() {
        listOf(PlayerLayout.COMPACT_PADDING, PlayerLayout.FULL_PADDING).forEach { assertEquals(true, it in 8f..10f) }
    }
}
