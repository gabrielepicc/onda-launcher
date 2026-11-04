// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DigestScheduleTest {

    private val slots = listOf(9 * 60, 13 * 60 + 30, 19 * 60)

    @Test
    fun nextSlotSameDay() {
        assertEquals(30, DigestSchedule.minutesUntilNext(8 * 60 + 30, slots))
        assertEquals(13 * 60 + 30, DigestSchedule.nextSlot(10 * 60, slots))
    }

    @Test
    fun nextSlotWrapsToTomorrow() {
        // 22:00 -> 9:00 del giorno dopo = 11 ore
        assertEquals(11 * 60, DigestSchedule.minutesUntilNext(22 * 60, slots))
        assertEquals(9 * 60, DigestSchedule.nextSlot(22 * 60, slots))
    }

    @Test
    fun exactlyOnSlotMeansNextOne() {
        assertEquals(4 * 60 + 30, DigestSchedule.minutesUntilNext(9 * 60, slots))
        assertEquals(0, DigestSchedule.minutesSinceLast(9 * 60, slots))
    }

    @Test
    fun singleSlotIsAFullDayAway() {
        assertEquals(DigestSchedule.DAY, DigestSchedule.minutesUntilNext(600, listOf(600)))
    }

    @Test
    fun noSlots() {
        assertNull(DigestSchedule.minutesUntilNext(600, emptyList()))
        assertNull(DigestSchedule.minutesSinceLast(600, listOf(-5, 5000)))
    }

    @Test
    fun formatting() {
        assertEquals("9:05", DigestSchedule.format(545))
    }
}
