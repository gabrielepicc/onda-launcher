// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageBreakerTest {

    @Test
    fun countsOpensOfTheSameDay() {
        val s = UsageBreaker.recordOpen(UsageBreaker.recordOpen(Settings(), "a", "2026-11-04"), "a", "2026-11-04")
        assertEquals(2, UsageBreaker.opensToday(s, "a", "2026-11-04"))
    }

    @Test
    fun countersRestartOnANewDay() {
        val yesterday = UsageBreaker.recordOpen(Settings(), "a", "2026-11-04")
        assertEquals(0, UsageBreaker.opensToday(yesterday, "a", "2026-11-05"))
        val today = UsageBreaker.recordOpen(yesterday, "b", "2026-11-05")
        assertEquals(mapOf("b" to 1), today.breakerOpens)
    }

    @Test
    fun toggleAddsAndRemoves() {
        val on = UsageBreaker.toggle(Settings(), "a")
        assertEquals(setOf("a"), on.breakerApps)
        assertTrue(UsageBreaker.toggle(on, "a").breakerApps.isEmpty())
    }
}
