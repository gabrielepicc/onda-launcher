// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReturnPolicyTest {
    @Test
    fun refreshOnlyWhenOlderThanAMinute() {
        assertTrue(ReturnPolicy.shouldRefresh(0L, 1_000L))
        assertFalse(ReturnPolicy.shouldRefresh(10_000L, 69_999L))
        assertTrue(ReturnPolicy.shouldRefresh(10_000L, 70_000L))
        // Orologio tornato indietro: si aggiorna
        assertTrue(ReturnPolicy.shouldRefresh(10_000L, 5_000L))
    }

    @Test
    fun homePressOnlyWhenAlreadyVisible() {
        assertTrue(ReturnPolicy.isHomePress(ReturnPolicy.ACTION_MAIN, visible = true))
        assertFalse(ReturnPolicy.isHomePress(ReturnPolicy.ACTION_MAIN, visible = false))
        assertFalse(ReturnPolicy.isHomePress("android.intent.action.VIEW", visible = true))
        assertTrue(ReturnPolicy.isReturnFromApp(ReturnPolicy.ACTION_MAIN, visible = false))
        assertFalse(ReturnPolicy.isReturnFromApp(ReturnPolicy.ACTION_MAIN, visible = true))
        assertFalse(ReturnPolicy.isReturnFromApp(null, visible = false))
    }

    @Test
    fun entryAnimationRules() {
        assertTrue(ReturnPolicy.animateEntry(returnFromApp = true, enabled = true, animationScale = 1f))
        assertFalse(ReturnPolicy.animateEntry(returnFromApp = false, enabled = true, animationScale = 1f))
        assertFalse(ReturnPolicy.animateEntry(returnFromApp = true, enabled = false, animationScale = 1f))
        // «Rimuovi animazioni» di sistema: scala 0
        assertFalse(ReturnPolicy.animateEntry(returnFromApp = true, enabled = true, animationScale = 0f))
    }

    @Test
    fun closeInstantlyOnlyOutOfSight() {
        assertTrue(ReturnPolicy.closeInstantly(visible = false))
        assertFalse(ReturnPolicy.closeInstantly(visible = true))
    }

    @Test
    fun keepsTheLastFiveTimes() {
        val times = (1L..7L).fold(emptyList<Long>()) { acc, t -> ReturnPolicy.addTime(acc, t) }
        assertEquals(listOf(3L, 4L, 5L, 6L, 7L), times)
    }
}
