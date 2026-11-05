// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlugPlayTest {
    private val buds = setOf("Galaxy Buds2")

    @Test
    fun anyHeadphonesOrTheChosenOne() {
        assertTrue(PlugPlay.matches("", buds))
        assertTrue(PlugPlay.matches("galaxy buds2 ", buds))
        assertFalse(PlugPlay.matches("Auto", buds))
    }

    @Test
    fun resumesOnlyWhenEverythingFits() {
        assertTrue(PlugPlay.shouldResume(true, true, "", buds, inCall = false, somethingPlaying = false))
        assertFalse(PlugPlay.shouldResume(false, true, "", buds, inCall = false, somethingPlaying = false))
        assertFalse(PlugPlay.shouldResume(true, false, "", buds, inCall = false, somethingPlaying = false))
        assertFalse(PlugPlay.shouldResume(true, true, "Auto", buds, inCall = false, somethingPlaying = false))
        assertFalse(PlugPlay.shouldResume(true, true, "", buds, inCall = true, somethingPlaying = false))
        assertFalse(PlugPlay.shouldResume(true, true, "", buds, inCall = false, somethingPlaying = true))
    }
}
