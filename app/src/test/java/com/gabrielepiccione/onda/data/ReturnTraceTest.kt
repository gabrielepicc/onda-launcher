// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReturnTraceTest {
    @Test
    fun recordsFirstArrivalOfEachSignalIncludingLateOnes() {
        val t = ReturnTrace(1_000L)
        assertTrue(t.mark(TraceMark.RESUME, 1_012L))
        assertFalse(t.mark(TraceMark.RESUME, 1_500L))
        // Segnale tardivo, dopo l'avvio della molla: si registra lo stesso
        assertTrue(t.mark(TraceMark.SPRING_START, 1_450L))
        assertTrue(t.mark(TraceMark.ENTER_COMPLETE, 2_300L))
        val marks = t.snapshot().marks
        assertEquals(12L, marks[TraceMark.RESUME])
        assertEquals(1_300L, marks[TraceMark.ENTER_COMPLETE])
        assertNull(marks[TraceMark.FOCUS])
    }

    @Test
    fun ignoresMarksFarTooLateOrBeforeTheStart() {
        val t = ReturnTrace(1_000L)
        assertFalse(t.mark(TraceMark.FOCUS, 999L))
        assertFalse(t.mark(TraceMark.FOCUS, 1_000L + ReturnTrace.MAX_MARK_MS + 1))
    }

    @Test
    fun countsFramesPerHundredMillisecondsInTheFirstSecond() {
        val t = ReturnTrace(0L)
        listOf(5L, 20L, 90L, 100L, 150L, 999L).forEach { assertTrue(t.frame(it)) }
        // Dopo il primo secondo non si contano più
        assertFalse(t.frame(1_000L))
        val frames = t.snapshot().framesPer100ms
        assertEquals(10, frames.size)
        assertEquals(3, frames[0])
        assertEquals(2, frames[1])
        assertEquals(1, frames[9])
        assertEquals(0, frames[4])
    }

    @Test
    fun keepsTheLastThreeReturns() {
        val snaps = (1..5).map { TraceSnapshot(mapOf(TraceMark.RESUME to it.toLong()), emptyList()) }
        val kept = snaps.fold(emptyList<TraceSnapshot>()) { acc, s -> ReturnTrace.keepLast(acc, s) }
        assertEquals(listOf(3L, 4L, 5L), kept.map { it.marks.getValue(TraceMark.RESUME) })
    }

    @Test
    fun entryPhasesFromDelayAndDuration() {
        // Ritardo 450, durata 300: attesa fino a 449, in corso fino a 749, poi finita
        assertEquals(EntryTiming.Phase.WAITING, EntryTiming.phase(0, 450, 300))
        assertEquals(EntryTiming.Phase.WAITING, EntryTiming.phase(449, 450, 300))
        assertEquals(EntryTiming.Phase.RUNNING, EntryTiming.phase(450, 450, 300))
        assertEquals(EntryTiming.Phase.RUNNING, EntryTiming.phase(749, 450, 300))
        assertEquals(EntryTiming.Phase.DONE, EntryTiming.phase(750, 450, 300))
        assertEquals(0f, EntryTiming.progress(100, 450, 300), 0f)
        assertEquals(0.5f, EntryTiming.progress(600, 450, 300), 0.001f)
        assertEquals(1f, EntryTiming.progress(2_000, 450, 300), 0f)
    }

    @Test
    fun delayAndDurationStayInRangeAndOnTheStep() {
        assertEquals(0, EntryTiming.delay(-30))
        assertEquals(900, EntryTiming.delay(2_000))
        assertEquals(450, EntryTiming.delay(460))
        assertEquals(150, EntryTiming.duration(10))
        assertEquals(1_200, EntryTiming.duration(5_000))
        assertEquals(300, EntryTiming.duration(290))
    }
}
