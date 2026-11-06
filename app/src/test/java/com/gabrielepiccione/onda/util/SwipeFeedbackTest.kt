// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeFeedbackTest {
    @Test
    fun trackStartsInvisibleAndGrowsToTheLimit() {
        assertEquals(0f, SwipeFeedback.trackAlpha(0f, armed = false), 0.001f)
        assertEquals(SwipeFeedback.TRACK_MAX / 2, SwipeFeedback.trackAlpha(0.5f, armed = false), 0.001f)
        assertEquals(SwipeFeedback.TRACK_MAX, SwipeFeedback.trackAlpha(1f, armed = false), 0.001f)
    }

    @Test
    fun pastTheThresholdTheTrackIsStrongerAndTheIconGrows() {
        assertTrue(SwipeFeedback.trackAlpha(1f, true) > SwipeFeedback.trackAlpha(1f, false))
        assertTrue(SwipeFeedback.iconScale(1f, true) > SwipeFeedback.iconScale(1f, false))
    }

    @Test
    fun theIconFadesInWithTheSwipe() {
        assertEquals(0f, SwipeFeedback.iconAlpha(0f), 0.001f)
        assertEquals(1f, SwipeFeedback.iconAlpha(1f), 0.001f)
        assertEquals(0.8f, SwipeFeedback.iconScale(0f, false), 0.001f)
    }

    @Test
    fun progressIsSymmetricAndClamped() {
        assertEquals(0.5f, SwipeFeedback.progress(36f, 72f), 0.001f)
        assertEquals(0.5f, SwipeFeedback.progress(-36f, 72f), 0.001f)
        assertEquals(1f, SwipeFeedback.progress(500f, 72f), 0.001f)
        assertEquals(0f, SwipeFeedback.progress(10f, 0f), 0.001f)
    }

    @Test
    fun theVeilIsVeryLight() {
        assertTrue(SwipeFeedback.veilAlpha(1f) <= 0.08f)
        assertEquals(0f, SwipeFeedback.veilAlpha(0f), 0.001f)
    }
}
