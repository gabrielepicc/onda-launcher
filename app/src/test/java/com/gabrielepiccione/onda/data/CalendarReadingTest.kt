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
    fun pathReportsWhichSourcesGaveRows() {
        assertEquals(ReadPath.UNION, CalendarReading.path(instancesRows = 12, eventsRows = 3))
        assertEquals(ReadPath.INSTANCES, CalendarReading.path(12, 0))
        assertEquals(ReadPath.EVENTS, CalendarReading.path(0, 4))
        assertEquals(ReadPath.NONE, CalendarReading.path(0, 0))
    }

    private fun ev(id: Long, begin: Long, cal: Long = 1L, color: Int = 0) = CalendarEvent("t$id", begin, false, id, begin + 1, color, cal)

    @Test
    fun mergeUnitesBothSourcesWithoutDuplicates() {
        // Instances ha gli eventi 1 e 2; la tabella Events ha 1 (di nuovo), 3 e 4: l'unione ha 1, 2, 3, 4 una volta sola
        val merged = CalendarReading.merge(listOf(ev(1, 100), ev(2, 200)), listOf(ev(1, 100), ev(3, 300), ev(4, 400)))
        assertEquals(listOf(1L, 2L, 3L, 4L), merged.map { it.eventId })
        // Un evento che c'è solo nella tabella Events (calendario non visibile, o Instances vuota) si aggiunge
        assertEquals(listOf(7L), CalendarReading.merge(emptyList(), listOf(ev(7, 10))).map { it.eventId })
        // E viceversa: solo Instances
        assertEquals(listOf(8L), CalendarReading.merge(listOf(ev(8, 10)), emptyList()).map { it.eventId })
    }

    @Test
    fun mergeTrustsInstancesForRecurringEventsItKnows() {
        // Instances ha una sola occorrenza (le altre sono cancellate); la tabella Events le calcola tutte: non devono rivivere
        val instances = listOf(ev(5, 1_000))
        val computed = listOf(ev(5, 1_000), ev(5, 2_000), ev(5, 3_000))
        assertEquals(listOf(1_000L), CalendarReading.merge(instances, computed).map { it.begin })
        // Stesso evento e stesso inizio non si ripete mai
        assertEquals(1, CalendarReading.merge(listOf(ev(6, 50), ev(6, 50)), emptyList()).size)
    }

    @Test
    fun allDayEventsFromBothSourcesLandOnTheSameLocalDay() {
        val zone = java.time.ZoneId.of("Europe/Rome")
        val utc = java.time.LocalDate.of(2026, 11, 9).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val (b, e) = CalendarLogic.allDayLocal(utc, utc + 86_400_000L, zone)
        val fromInstances = CalendarEvent("Oggi", b, true, 9, e, 0, 1)
        val fromTable = CalendarEvent("Oggi", b, true, 9, e, 0, 1)
        val merged = CalendarReading.merge(listOf(fromInstances), listOf(fromTable))
        assertEquals(1, merged.size)
        assertEquals(java.time.LocalDate.of(2026, 11, 9), CalendarDays.firstDay(merged.single(), zone))
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
    fun copyTextHasPerSourceCountsDeviceAndNoPersonalData() {
        val calendars = listOf(
            CalendarInfo(
                3, "Anna personale", "anna@gmail.com", visible = true, synced = true, color = 0, events14 = 4, allDay14 = 1,
                accountType = "com.google", access = 700, primary = true,
                instances = SourceCounts(0, 0, 0), events = SourceCounts(1, 4, 1),
            ),
            CalendarInfo(4, "Lavoro", "x@azienda.it", visible = false, synced = false, color = 0, accountType = "com.microsoft.exchange", access = 200),
        )
        val read = CalendarReadInfo(ReadPath.EVENTS, listOf(SourceReport(CalendarSource.INSTANCES, 0, "SQLiteException: no such column")))
        val probe = CalendarProbe(
            listOf(ProbeResult(ProbeStep.INSTANCES_TODAY, 0), ProbeResult(ProbeStep.INSTANCES_14, 0, "boom"), ProbeResult(ProbeStep.EVENTS_14_NO_FILTER, 5)),
            eventsTotalRows = 120, nearestFuture = "2026-11-12",
        )
        val text = CalendarReading.copyText(true, calendars, setOf(4L), read, AllDayMode.ALWAYS, probe, "realme Android 15 (API 35)")
        assertTrue(text.contains("realme Android 15"))
        assertTrue(text.contains("inclusi 1 su 2"))
        assertTrue(text.contains("#3 com.google visibile=true sync=true accesso=owner principale=true incluso=true"))
        assertTrue(text.contains("#4 com.microsoft.exchange visibile=false sync=false accesso=read principale=? incluso=false"))
        assertTrue(text.contains("Instances oggi=0 14g=0 tuttoilgiorno=0"))
        assertTrue(text.contains("Events    oggi=1 14g=4 tuttoilgiorno=1"))
        assertTrue(text.contains("EVENTS_14_NO_FILTER: righe=5"))
        assertTrue(text.contains("INSTANCES_14: righe=0 errore=boom"))
        assertTrue(text.contains("righe totali=120"))
        assertTrue(text.contains("2026-11-12"))
        assertTrue(text.contains("SQLiteException"))
        // Niente nomi, indirizzi o titoli
        assertFalse(text.contains("Anna"))
        assertFalse(text.contains("gmail"))
        assertFalse(text.contains("Lavoro"))
        assertFalse(text.contains("azienda"))
    }
}
