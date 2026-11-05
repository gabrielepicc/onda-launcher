// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import com.gabrielepiccione.onda.data.UsageKind.BACKGROUND
import com.gabrielepiccione.onda.data.UsageKind.FOREGROUND
import com.gabrielepiccione.onda.data.UsageKind.SCREEN_OFF
import com.gabrielepiccione.onda.data.UsageKind.UNLOCK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageStatsMathTest {
    private val day = 1_000L * UsageStatsMath.DAY_MS
    private val h = UsageStatsMath.HOUR_MS
    private val m = 60_000L
    private val launcher = "launcher"
    private fun at(hours: Long, minutes: Long = 0) = day + hours * h + minutes * m
    private fun rec(t: Long, pkg: String, kind: UsageKind) = UsageRecord(t, pkg, kind)
    private fun summarize(vararg r: UsageRecord, now: Long = day + UsageStatsMath.DAY_MS) =
        UsageStatsMath.day(r.toList(), day, now, setOf(launcher))

    @Test
    fun countsTimeAndLaunchesPerApp() {
        val d = summarize(
            rec(at(9), "chat", FOREGROUND), rec(at(9, 10), "chat", BACKGROUND),
            rec(at(10), "chat", FOREGROUND), rec(at(10, 5), "chat", BACKGROUND),
            rec(at(11), "maps", FOREGROUND), rec(at(11, 30), "maps", BACKGROUND),
        )
        assertEquals(45 * m, d.screenMs)
        assertEquals("maps", d.apps.first().packageName)
        assertEquals(2, d.apps.first { it.packageName == "chat" }.launches)
        assertEquals(3, d.launches)
    }

    @Test
    fun aScreenChangeInsideTheSameAppIsNotANewLaunch() {
        val d = summarize(
            rec(at(9), "chat", FOREGROUND), rec(at(9, 5), "chat", BACKGROUND),
            rec(at(9, 5) + 500, "chat", FOREGROUND), rec(at(9, 10), "chat", BACKGROUND),
        )
        val chat = d.apps.single()
        assertEquals(1, chat.launches)
        assertEquals(1, chat.sessions)
        assertEquals(10 * m - 500, chat.foregroundMs)
    }

    @Test
    fun theLauncherDoesNotCountAndClosesTheAppBelow() {
        val d = summarize(
            rec(at(9), "chat", FOREGROUND),
            rec(at(9, 3), launcher, FOREGROUND),
            rec(at(9, 20), launcher, BACKGROUND),
        )
        assertEquals(3 * m, d.screenMs)
        assertEquals(listOf("chat"), d.apps.map { it.packageName })
    }

    @Test
    fun screenOffEndsTheSession() {
        val d = summarize(rec(at(22), "video", FOREGROUND), rec(at(22, 40), "", SCREEN_OFF))
        assertEquals(40 * m, d.screenMs)
        assertEquals(listOf(40 * m), d.sessionLengths)
    }

    @Test
    fun usageAcrossMidnightCountsOnlyTheMinutesOfThisDay() {
        val d = summarize(rec(day - 30 * m, "video", FOREGROUND), rec(at(0, 20), "video", BACKGROUND))
        assertEquals(20 * m, d.screenMs)
        assertEquals(0, d.launches)
        assertEquals(20 * m, d.nightMs)
    }

    @Test
    fun anAppStillOpenCountsUntilNow() {
        val d = summarize(rec(at(15), "game", FOREGROUND), now = at(15, 25))
        assertEquals(25 * m, d.screenMs)
    }

    @Test
    fun hoursSplitAtTheirBoundaries() {
        val d = summarize(rec(at(8, 50), "news", FOREGROUND), rec(at(9, 10), "news", BACKGROUND))
        assertEquals(10 * m, d.hourlyMs[8])
        assertEquals(10 * m, d.hourlyMs[9])
        assertEquals(20 * m, d.hourlyMs.sum())
    }

    @Test
    fun unlocksAndFirstUse() {
        val d = summarize(rec(at(7, 2), "", UNLOCK), rec(at(7, 3), "mail", FOREGROUND), rec(at(12), "", UNLOCK))
        assertEquals(2, d.unlocks)
        assertEquals(at(7, 2), d.firstUse)
    }

    @Test
    fun shortChecksAreSessionsUnderAMinute() {
        val d = summarize(
            rec(at(9), "social", FOREGROUND), rec(at(9) + 20_000, "social", BACKGROUND),
            rec(at(10), "social", FOREGROUND), rec(at(10, 5), "social", BACKGROUND),
        )
        assertEquals(1, d.shortChecks)
    }

    @Test
    fun summaryComparesWithThePeriodBefore() {
        fun usage(ms: Long, start: Long) = DayUsage(start, ms, 10, 20, emptyList(), List(24) { 0L }, null, listOf(ms), 0L)
        val now = listOf(usage(2 * h, 1), usage(4 * h, 2))
        val before = listOf(usage(2 * h, -1), usage(2 * h, -2))
        val s = UsageStatsMath.summary(now, before)
        assertEquals(3 * h, s.averageMs)
        assertEquals(50, s.changePercent)
        assertEquals(2L, s.busiestDayStart)
        assertEquals(1L, s.quietestDayStart)
        assertEquals(3 * h, s.medianSessionMs)
        assertNull(UsageStatsMath.summary(now).changePercent)
    }

    @Test
    fun medianAndPercent() {
        assertEquals(0L, UsageStatsMath.median(emptyList()))
        assertEquals(2L, UsageStatsMath.median(listOf(3, 1, 2)))
        assertNull(UsageStatsMath.percentChange(10, 0))
        assertEquals(-25, UsageStatsMath.percentChange(75, 100))
    }

    @Test
    fun limits() {
        assertFalse(UsageStatsMath.overLimit(5 * h, 0))
        assertTrue(UsageStatsMath.overLimit(30 * m, 30))
        assertFalse(UsageStatsMath.overLimit(29 * m, 30))
    }
}
