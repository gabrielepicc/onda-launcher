// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarReadingTest {
    private val instancesOk = SourceReport(CalendarSource.INSTANCES, 12)

    @Test
    fun fallbackWhenInstancesFailsOrIsEmptyForVisibleCalendars() {
        assertFalse(CalendarReading.needsFallback(3, instancesOk))
        assertTrue(CalendarReading.needsFallback(3, SourceReport(CalendarSource.INSTANCES, 0)))
        assertTrue(CalendarReading.needsFallback(3, SourceReport(CalendarSource.INSTANCES, 0, "SQLiteException: no such column")))
        assertTrue(CalendarReading.needsFallback(3, null))
        // Nessun calendario visibile: Instances non deve dare nulla, non è un guasto
        assertFalse(CalendarReading.needsFallback(0, SourceReport(CalendarSource.INSTANCES, 0)))
    }

    @Test
    fun pathReportsWhoWorked() {
        assertEquals(ReadPath.INSTANCES, CalendarReading.path(3, instancesOk, fellBack = false))
        assertEquals(ReadPath.EVENTS_FALLBACK, CalendarReading.path(3, SourceReport(CalendarSource.INSTANCES, 0), fellBack = true))
        assertEquals(ReadPath.EVENTS_ONLY, CalendarReading.path(0, SourceReport(CalendarSource.INSTANCES, 0), fellBack = false))
        assertEquals(ReadPath.NONE, CalendarReading.path(2, SourceReport(CalendarSource.INSTANCES, 0, "boom"), fellBack = false))
    }

    private fun ev(id: Long, begin: Long, cal: Long = 1L, color: Int = 0) = CalendarEvent("t$id", begin, false, id, begin + 1, color, cal)

    @Test
    fun mergeDropsTheSameOccurrenceButKeepsOtherOccurrences() {
        val merged = CalendarReading.merge(listOf(ev(1, 100), ev(2, 200)), listOf(ev(1, 100), ev(1, 900), ev(3, 300)))
        assertEquals(listOf(1L to 100L, 2L to 200L, 1L to 900L, 3L to 300L), merged.map { it.eventId to it.begin })
    }

    @Test
    fun colorsComeFromTheCalendarsTable() {
        val out = CalendarReading.withColors(listOf(ev(1, 1, cal = 7), ev(2, 2, cal = 9, color = 5), ev(3, 3, cal = 0)), mapOf(7L to 0xFF00FF, 9L to 1))
        assertEquals(0xFF00FF, out[0].color)
        // Un colore già noto resta; un evento senza calendario resta senza colore
        assertEquals(5, out[1].color)
        assertEquals(0, out[2].color)
    }

    @Test
    fun errorMessagesAreShortAndHaveNoPersonalData() {
        val message = CalendarReading.describeError(IllegalArgumentException("Invalid column display_color for anna.rossi@gmail.com at 1762000000000"))
        assertTrue(message.startsWith("IllegalArgumentException: "))
        assertFalse(message.contains("anna.rossi"))
        assertFalse(message.contains("1762000000000"))
        assertTrue(message.contains("display_color"))
        assertEquals("SecurityException", CalendarReading.describeError(SecurityException()))
        assertTrue(CalendarReading.describeError(RuntimeException("x".repeat(500))).length <= 120)
    }

    @Test
    fun rowsAndErrorsPerSource() {
        val sources = listOf(SourceReport(CalendarSource.CALENDARS, 8), SourceReport(CalendarSource.INSTANCES, 0, "boom"), SourceReport(CalendarSource.EVENTS, 5))
        assertEquals(5, CalendarReading.rows(sources, CalendarSource.EVENTS))
        assertEquals(0, CalendarReading.rows(sources, CalendarSource.INSTANCES))
        assertEquals(listOf(CalendarSource.INSTANCES), CalendarReading.errors(sources).map { it.source })
    }

    @Test
    fun copyTextHasCountsAndErrorsButNoNamesOrAccounts() {
        val calendars = listOf(CalendarInfo(3, "Anna personale", "anna@gmail.com", visible = true, synced = true, color = 0, events14 = 4, allDay14 = 1))
        val read = CalendarReadInfo(ReadPath.EVENTS_FALLBACK, listOf(SourceReport(CalendarSource.INSTANCES, 0, "SQLiteException: no such column")))
        val text = CalendarReading.copyText(true, calendars, setOf(3L), read, AllDayMode.ALWAYS)
        assertTrue(text.contains("#3"))
        assertTrue(text.contains("eventi14=4"))
        assertTrue(text.contains("tuttoilgiorno14=1"))
        assertTrue(text.contains("EVENTS_FALLBACK"))
        assertTrue(text.contains("SQLiteException"))
        assertFalse(text.contains("Anna"))
        assertFalse(text.contains("gmail"))
    }
}
