// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BreakerGateTest {
    private val watched = { pkg: String -> pkg == "social" }
    private fun gate() = BreakerGate(setOf("systemui", "keyboard"))

    @Test
    fun enteringAWatchedAppAsksForThePause() {
        val g = gate()
        assertNull(g.onForeground("onda", watched))
        assertEquals("social", g.onForeground("social", watched))
    }

    @Test
    fun aScreenChangeInsideTheSameAppDoesNotAskAgain() {
        val g = gate()
        g.onForeground("social", watched)
        assertNull(g.onForeground("social", watched))
    }

    @Test
    fun theShadeOrTheKeyboardAreNotALeave() {
        val g = gate()
        g.onForeground("onda", watched)
        g.allow("social")
        assertNull(g.onForeground("social", watched))
        assertNull(g.onForeground("systemui", watched))
        assertNull(g.onForeground("keyboard", watched))
        assertNull(g.onForeground("social", watched))
    }

    @Test
    fun afterConfirmingTheAppIsFreeUntilYouLeaveIt() {
        val g = gate()
        g.onForeground("onda", watched)
        g.allow("social")
        assertNull(g.onForeground("social", watched))
        g.onForeground("onda", watched)
        assertEquals("social", g.onForeground("social", watched))
    }

    @Test
    fun otherAppsAreNeverPaused() {
        assertNull(gate().onForeground("maps", watched))
    }
}
