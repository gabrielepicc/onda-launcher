// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockSpacingTest {
    @Test
    fun snapsToStepsOfFour() {
        assertEquals(8, ClockSpacing.snap(9f))
        assertEquals(12, ClockSpacing.snap(10.5f))
        assertEquals(0, ClockSpacing.snap(1.9f))
    }

    @Test
    fun staysWithinRange() {
        assertEquals(0, ClockSpacing.snap(-10f))
        assertEquals(32, ClockSpacing.snap(100f))
        assertEquals(32, ClockSpacing.coerce(50))
        assertEquals(0, ClockSpacing.coerce(-3))
    }

    @Test
    fun defaultMatchesSettings() {
        assertEquals(Settings().clockInnerSpacing, ClockSpacing.DEFAULT)
    }

    @Test
    fun previewIsAThirdLikeTheBlockSpacing() {
        assertEquals(8f / 3f, ClockSpacing.previewDp(8), 0.001f)
    }
}
