// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageRemindersTest {

    private val minute = 60_000L
    private fun on(m: Int, pkg: String = "app") = UsageEvent(m * minute, pkg, true)
    private fun off(m: Int, pkg: String = "app") = UsageEvent(m * minute, pkg, false)
    private fun minutes(events: List<UsageEvent>, now: Int) =
        UsageReminders.continuousMs(events, "app", now * minute) / minute

    @Test
    fun countsTheCurrentSession() {
        assertEquals(30, minutes(listOf(on(10)), 40))
    }

    @Test
    fun notInForegroundMeansZero() {
        assertEquals(0, minutes(listOf(on(10), off(20)), 40))
        assertEquals(0, minutes(emptyList(), 40))
    }

    @Test
    fun shortInterruptionsDoNotResetTheTime() {
        val events = listOf(on(0), off(10), on(12, "chat"), off(13, "chat"), on(13), off(20), on(23))
        assertEquals(10 + 7 + 7, minutes(events, 30))
    }

    @Test
    fun aLongBreakStartsAgain() {
        assertEquals(5, minutes(listOf(on(0), off(10), on(30)), 35))
    }

    @Test
    fun anotherAppOnTopEndsTheSessionEvenWithoutAClosingEvent() {
        assertEquals(0, minutes(listOf(on(0), on(10, "altra")), 20))
    }

    @Test
    fun aSecondOpenEventKeepsTheOriginalStart() {
        assertEquals(20, minutes(listOf(on(0), on(5)), 20))
    }

    @Test
    fun eachThresholdFiresOnceFromTheChosenStart() {
        assertNull(UsageReminders.thresholdToNotify(4, 5, 0))
        assertEquals(5, UsageReminders.thresholdToNotify(5, 5, 0))
        assertNull(UsageReminders.thresholdToNotify(7, 5, 5))
        assertEquals(10, UsageReminders.thresholdToNotify(12, 5, 5))
        assertNull(UsageReminders.thresholdToNotify(12, 20, 0))
        assertEquals(240, UsageReminders.thresholdToNotify(500, 5, 180))
        assertNull(UsageReminders.thresholdToNotify(500, 5, 240))
    }

    @Test
    fun appKeysAndToggle() {
        assertEquals("com.app", UsageReminders.packageOf("com.app/com.app.Main#0"))
        val on = UsageReminders.toggle(Settings(), "k")
        assertEquals(setOf("k"), on.reminderApps)
        assertEquals(emptySet<String>(), UsageReminders.toggle(on, "k").reminderApps)
    }
}
