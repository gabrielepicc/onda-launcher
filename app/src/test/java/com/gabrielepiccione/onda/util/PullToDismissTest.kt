// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PullToDismissTest {

    @Test
    fun dragFollowsTheFingerWithResistance() {
        assertEquals(55f, PullToDismiss.next(0f, 100f), 0.01f)
        assertEquals(-55f, PullToDismiss.next(0f, -100f), 0.01f)
    }

    @Test
    fun comingBackNeverCrossesZero() {
        assertEquals(0f, PullToDismiss.next(20f, -100f), 0.01f)
        assertEquals(0f, PullToDismiss.next(-20f, 100f), 0.01f)
    }

    @Test
    fun farEnoughClosesEvenWithoutSpeed() {
        assertTrue(PullToDismiss.shouldDismiss(200f, 0f, 150f, 1500f))
        assertTrue(PullToDismiss.shouldDismiss(-200f, 0f, 150f, 1500f))
    }

    @Test
    fun shortPullClosesOnlyWithAFlingInTheSameDirection() {
        assertFalse(PullToDismiss.shouldDismiss(40f, 100f, 150f, 1500f))
        assertTrue(PullToDismiss.shouldDismiss(40f, 2000f, 150f, 1500f))
        assertFalse(PullToDismiss.shouldDismiss(40f, -2000f, 150f, 1500f))
    }

    @Test
    fun nothingPulledNothingCloses() {
        assertFalse(PullToDismiss.shouldDismiss(0f, 5000f, 150f, 1500f))
    }
}
