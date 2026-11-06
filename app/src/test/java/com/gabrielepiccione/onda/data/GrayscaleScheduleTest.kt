// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GrayscaleScheduleTest {
    private fun at(h: Int, m: Int = 0) = h * 60 + m
    private val start = at(22, 30)
    private val end = at(7)

    @Test
    fun aSlotAcrossMidnightCoversTheNightOnly() {
        assertTrue(GrayscaleSchedule.inside(at(23), start, end))
        assertTrue(GrayscaleSchedule.inside(at(0), start, end))
        assertTrue(GrayscaleSchedule.inside(at(6, 59), start, end))
        assertFalse(GrayscaleSchedule.inside(at(7), start, end))
        assertFalse(GrayscaleSchedule.inside(at(12), start, end))
        assertFalse(GrayscaleSchedule.inside(at(22, 29), start, end))
        assertTrue(GrayscaleSchedule.inside(at(22, 30), start, end))
    }

    @Test
    fun aSlotInsideTheSameDay() {
        assertTrue(GrayscaleSchedule.inside(at(10), at(9), at(17)))
        assertFalse(GrayscaleSchedule.inside(at(17), at(9), at(17)))
        assertFalse(GrayscaleSchedule.inside(at(8), at(9), at(17)))
    }

    @Test
    fun equalStartAndEndIsEmpty() {
        assertFalse(GrayscaleSchedule.inside(at(12), at(8), at(8)))
    }

    @Test
    fun modesDecideWhoTurnsItOn() {
        assertFalse(GrayscaleSchedule.active(GrayscaleMode.OFF, at(23), start, end, "", null))
        assertTrue(GrayscaleSchedule.active(GrayscaleMode.SCHEDULE, at(23), start, end, "", null))
        assertTrue(GrayscaleSchedule.active(GrayscaleMode.PROFILE, at(12), start, end, "sera", "sera"))
        assertFalse(GrayscaleSchedule.active(GrayscaleMode.PROFILE, at(12), start, end, "sera", "lavoro"))
        assertFalse(GrayscaleSchedule.active(GrayscaleMode.PROFILE, at(12), start, end, "", ""))
        assertFalse(GrayscaleSchedule.active(GrayscaleMode.PROFILE, at(12), start, end, "sera", null))
    }

    @Test
    fun saturationFollowsTheTransition() {
        assertEquals(1f, GrayscaleSchedule.saturation(0f), 0.001f)
        assertEquals(0f, GrayscaleSchedule.saturation(1f), 0.001f)
        assertEquals(0.5f, GrayscaleSchedule.saturation(0.5f), 0.001f)
        assertEquals(1f, GrayscaleSchedule.saturation(-3f), 0.001f)
    }

    @Test
    fun minutesToChangeLooksAtTheNextEdge() {
        assertEquals(90, GrayscaleSchedule.minutesToChange(at(21), start, end))
        assertEquals(60, GrayscaleSchedule.minutesToChange(at(6), start, end))
        assertEquals(1, GrayscaleSchedule.minutesToChange(at(22, 29), start, end))
    }
}
