// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import com.gabrielepiccione.onda.util.SwipeFeedback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationLayoutTest {
    @Test
    fun paddingNeverGoesBelowTheMinimum() {
        assertEquals(16, NotificationLayout.horizontalPaddingDp(4))
        assertEquals(16, NotificationLayout.horizontalPaddingDp(0))
        assertEquals(20, NotificationLayout.horizontalPaddingDp(20))
    }

    @Test
    fun titlesFillTheRowAndWrapOnTwoLines() {
        assertTrue(NotificationLayout.TITLE_FILLS_ROW)
        assertEquals(2, NotificationLayout.TITLE_MAX_LINES)
    }

    @Test
    fun swipeArmsOnEitherSideBeyondTheTrigger() {
        val t = NotificationLayout.SWIPE_TRIGGER_DP.toFloat()
        assertFalse(SwipeFeedback.isArmed(t - 1, t))
        assertTrue(SwipeFeedback.isArmed(t, t))
        assertTrue(SwipeFeedback.isArmed(-t - 5, t))
        assertFalse(SwipeFeedback.isArmed(500f, 0f))
    }

    @Test
    fun cardResistsBeyondTheTrigger() {
        assertEquals(1f, SwipeFeedback.resistance(10f, 72f), 0f)
        assertTrue(SwipeFeedback.resistance(100f, 72f) < 1f)
    }

    @Test
    fun onlyClearableNotificationsSwipeAway() {
        assertTrue(NotificationLayout.canSwipeAway(true))
        assertFalse(NotificationLayout.canSwipeAway(false))
    }
}
