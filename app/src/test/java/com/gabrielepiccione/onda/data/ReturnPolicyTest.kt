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

    /**
     * Simula la MainActivity: il flag «fermato dall'ultimo resume» è true da onStop a onResume, e la decisione
     * su un MAIN si prende in onNewIntent. Restituisce (tasto Home, ritorno da un'app) per ogni MAIN ricevuto.
     */
    private class Sim {
        var stopped = false
        var homePresses = 0
        var returns = 0
        fun onStop() { stopped = true }
        fun onStart() {}
        fun onResume() { stopped = false }
        fun onNewIntent(action: String?) {
            if (ReturnPolicy.isHomePress(action, stopped)) homePresses++
            if (ReturnPolicy.isReturnFromApp(action, stopped)) returns++
        }
    }

    @Test
    fun swipeUpFromAppIsAReturnNotAHomePress() {
        // singleTask: onStop → onRestart → onStart → onNewIntent(MAIN) → onResume
        val a = Sim()
        a.onResume()
        a.onStop(); a.onStart(); a.onNewIntent(ReturnPolicy.ACTION_MAIN); a.onResume()
        assertEquals(1, a.returns)
        assertEquals(0, a.homePresses)
        // Il flag si azzera al resume: il MAIN successivo a launcher aperto è un vero tasto Home
        a.onNewIntent(ReturnPolicy.ACTION_MAIN)
        assertEquals(1, a.returns)
        assertEquals(1, a.homePresses)
    }

    @Test
    fun swipeBackHasNoMainAndNoEntry() {
        val a = Sim()
        a.onResume()
        a.onStop(); a.onStart(); a.onResume()
        assertEquals(0, a.returns)
        assertEquals(0, a.homePresses)
        // Il ritorno con «indietro» non lascia stati: il MAIN dopo è un tasto Home
        a.onNewIntent(ReturnPolicy.ACTION_MAIN)
        assertEquals(1, a.homePresses)
        assertEquals(0, a.returns)
    }

    @Test
    fun otherIntentsAreNeither() {
        assertFalse(ReturnPolicy.isHomePress("android.intent.action.VIEW", stoppedSinceResume = false))
        assertFalse(ReturnPolicy.isReturnFromApp(null, stoppedSinceResume = true))
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
