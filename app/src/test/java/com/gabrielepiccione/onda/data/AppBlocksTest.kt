// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class AppBlocksTest {
    private val social = "com.social/com.social.Main#0"
    private val noHoliday: (LocalDate) -> Boolean = { false }

    // 2026-11-06 è un venerdì
    private fun at(day: Int, hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 11, day, hour, minute)

    private fun block(
        start: Int,
        end: Int,
        days: Set<Int> = emptySet(),
        skipHolidays: Boolean = true,
        enabled: Boolean = true,
        apps: Set<String> = setOf(social),
    ) = AppBlock("b", "Studio", apps, days, start, end, skipHolidays, enabled)

    @Test
    fun aDaytimeWindowIncludesStartAndExcludesEnd() {
        val b = block(9 * 60, 13 * 60)
        assertFalse(AppBlocks.isActive(b, at(6, 8, 59), noHoliday))
        assertTrue(AppBlocks.isActive(b, at(6, 9), noHoliday))
        assertTrue(AppBlocks.isActive(b, at(6, 12, 59), noHoliday))
        assertFalse(AppBlocks.isActive(b, at(6, 13), noHoliday))
    }

    @Test
    fun aWindowCanCrossMidnightAndBelongsToTheDayItStarted() {
        // Solo il venerdì (5) dalle 22 alle 7: il sabato alle 2 è ancora la fascia di venerdì
        val b = block(22 * 60, 7 * 60, days = setOf(5))
        assertTrue(AppBlocks.isActive(b, at(6, 23), noHoliday)) // venerdì sera
        assertTrue(AppBlocks.isActive(b, at(7, 2), noHoliday)) // sabato notte
        assertFalse(AppBlocks.isActive(b, at(7, 23), noHoliday)) // sabato sera: non è venerdì
        assertFalse(AppBlocks.isActive(b, at(6, 2), noHoliday)) // venerdì notte: iniziata giovedì
        assertFalse(AppBlocks.isActive(b, at(6, 12), noHoliday))
    }

    @Test
    fun daysRestrictTheBlock() {
        val weekdays = block(9 * 60, 18 * 60, days = setOf(1, 2, 3, 4, 5))
        assertTrue(AppBlocks.isActive(weekdays, at(6, 10), noHoliday)) // venerdì
        assertFalse(AppBlocks.isActive(weekdays, at(7, 10), noHoliday)) // sabato
    }

    @Test
    fun holidaysSwitchTheBlockOffWhenAsked() {
        val b = block(9 * 60, 18 * 60, days = setOf(1, 2, 3, 4, 5))
        val friday = LocalDate.of(2026, 11, 6)
        assertFalse(AppBlocks.isActive(b, at(6, 10)) { it == friday })
        assertTrue(AppBlocks.isActive(b.copy(skipHolidays = false), at(6, 10)) { it == friday })
    }

    @Test
    fun theHolidayCheckUsesTheDayTheWindowStarted() {
        val b = block(22 * 60, 7 * 60)
        val thursday = LocalDate.of(2026, 11, 5)
        // Venerdì alle 2 la fascia è iniziata giovedì: se giovedì è festivo non vale
        assertFalse(AppBlocks.isActive(b, at(6, 2)) { it == thursday })
        assertTrue(AppBlocks.isActive(b, at(6, 2), noHoliday))
    }

    @Test
    fun sameStartAndEndMeansAllDay() {
        assertTrue(AppBlocks.isActive(block(0, 0), at(6, 15), noHoliday))
    }

    @Test
    fun disabledOrEmptyBlocksNeverApply() {
        assertFalse(AppBlocks.isActive(block(0, 0, enabled = false), at(6, 15), noHoliday))
        assertFalse(AppBlocks.isActive(block(0, 0, apps = emptySet()), at(6, 15), noHoliday))
    }

    @Test
    fun hitReportsTheEndOfTheWindow() {
        val s = Settings(appBlocks = listOf(block(9 * 60, 13 * 60)))
        val hit = AppBlocks.hit(s, social, at(6, 10), noHoliday)
        assertNotNull(hit)
        assertEquals("Studio", hit!!.name)
        assertEquals(13 * 60, hit.until)
        assertNull(AppBlocks.hit(s, "com.other/com.other.Main#0", at(6, 10), noHoliday))
        assertNull(AppBlocks.hit(s, social, at(6, 14), noHoliday))
    }

    @Test
    fun hitMatchesByPackageForTheAccessibilityService() {
        val s = Settings(appBlocks = listOf(block(9 * 60, 13 * 60)))
        assertNotNull(AppBlocks.hitForPackage(s, "com.social", at(6, 10), noHoliday))
        assertNull(AppBlocks.hitForPackage(s, "com.other", at(6, 10), noHoliday))
    }

    @Test
    fun manualBlocksHoldUntilUnblocked() {
        val s = AppBlocks.block(Settings(), social)
        assertNull(AppBlocks.hit(s, social, at(6, 3), noHoliday)!!.until)
        assertNotNull(AppBlocks.hitForPackage(s, "com.social", at(6, 3), noHoliday))
        assertTrue(AppBlocks.any(s))
        val free = AppBlocks.unblock(s, social)
        assertNull(AppBlocks.hit(free, social, at(6, 3), noHoliday))
        assertFalse(AppBlocks.any(free))
    }

    @Test
    fun upsertRemoveAndTimeText() {
        val b = block(0, 60)
        val list = AppBlocks.upsert(emptyList(), b)
        assertEquals("Nuovo", AppBlocks.upsert(list, b.copy(name = "Nuovo")).single().name)
        assertTrue(AppBlocks.remove(list, "b").isEmpty())
        assertEquals("07:05", AppBlocks.timeText(7 * 60 + 5))
    }
}
