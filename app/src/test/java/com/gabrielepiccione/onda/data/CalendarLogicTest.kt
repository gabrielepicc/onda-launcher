// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class CalendarLogicTest {
    private val rome = ZoneId.of("Europe/Rome")
    private fun ms(dt: LocalDateTime, zone: ZoneId = rome) = dt.atZone(zone).toInstant().toEpochMilli()
    private fun ev(title: String, begin: Long, end: Long = begin + 3_600_000L, allDay: Boolean = false, cal: Long = 1L) =
        CalendarEvent(title, begin, allDay, 1L, end, 0, cal)

    @Test
    fun windowAndChoices() {
        assertEquals(listOf(1, 3, 7), CalendarLogic.NEXT_EVENT_CHOICES)
        assertEquals(1, CalendarLogic.nextEventDays(5))
        assertEquals(7, CalendarLogic.nextEventDays(7))
        val now = 1_000_000_000L
        assertEquals(now - 6 * 3_600_000L, CalendarLogic.windowStart(now))
        assertEquals(now + 3 * 86_400_000L, CalendarLogic.windowEnd(now, 3))
    }

    @Test
    fun filterByCalendarsKeepsUnknownOnes() {
        val events = listOf(ev("a", 1, cal = 1), ev("b", 2, cal = 2), ev("c", 3, cal = 0))
        assertEquals(listOf("a", "c"), CalendarLogic.filter(events, setOf(2L)).map { it.title })
        assertEquals(3, CalendarLogic.filter(events, emptySet()).size)
    }

    @Test
    fun sortedAndNextPreferTimedEvents() {
        val now = 10_000_000L
        val allDay = ev("Compleanno", now - 1_000, end = now + 86_400_000L, allDay = true)
        val later = ev("Riunione", now + 3_600_000L)
        val sooner = ev("Caffè", now + 600_000L)
        assertEquals(listOf("Compleanno", "Caffè", "Riunione"), CalendarLogic.sorted(listOf(later, allDay, sooner)).map { it.title })
        assertEquals("Caffè", CalendarLogic.next(listOf(later, allDay, sooner), now)?.title)
        // Solo eventi di tutto il giorno: vale il primo
        assertEquals("Compleanno", CalendarLogic.next(listOf(allDay), now)?.title)
        // Eventi finiti non contano
        assertNull(CalendarLogic.next(listOf(ev("Vecchio", now - 7_200_000L)), now))
    }

    @Test
    fun allDayEventsLandOnTheLocalDay() {
        // 9 novembre tutto il giorno: mezzanotte UTC → mezzanotte locale dello stesso giorno
        val utc = LocalDateTime.of(2026, 11, 9, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val (begin, end) = CalendarLogic.allDayLocal(utc, utc + 86_400_000L, rome)
        assertEquals(ms(LocalDateTime.of(2026, 11, 9, 0, 0)), begin)
        assertEquals(ms(LocalDateTime.of(2026, 11, 10, 0, 0)), end)
        val ny = ZoneId.of("America/New_York")
        assertEquals(ms(LocalDateTime.of(2026, 11, 9, 0, 0), ny), CalendarLogic.allDayLocal(utc, utc + 86_400_000L, ny).first)
        // Finisce a mezzanotte: a quell'istante è finito
        assertTrue(CalendarLogic.isOver(ev("x", begin, end, allDay = true), end))
    }

    @Test
    fun durationsFromAndroid() {
        assertEquals(3_600_000L, CalendarLogic.parseDurationMs("PT1H"))
        assertEquals(3_600_000L, CalendarLogic.parseDurationMs("P3600S"))
        assertEquals(86_400_000L, CalendarLogic.parseDurationMs("P1D"))
        assertNull(CalendarLogic.parseDurationMs(null))
        assertNull(CalendarLogic.parseDurationMs("boh"))
    }

    private fun cal(id: Long, visible: Boolean = true, synced: Boolean = true, n: Int = 0) = CalendarInfo(id, "c$id", "a@b", visible, synced, 0, n)

    @Test
    fun diagnosisExplainsWhyNothingShows() {
        assertEquals(CalendarProblem.NO_PERMISSION, CalendarLogic.diagnose(false, listOf(cal(1, n = 3)), emptySet()))
        assertEquals(CalendarProblem.NO_CALENDARS, CalendarLogic.diagnose(true, emptyList(), emptySet()))
        assertEquals(CalendarProblem.NONE_SELECTED, CalendarLogic.diagnose(true, listOf(cal(1)), setOf(1L)))
        assertEquals(CalendarProblem.NONE_VISIBLE, CalendarLogic.diagnose(true, listOf(cal(1, visible = false)), emptySet()))
        assertEquals(CalendarProblem.NOT_SYNCED, CalendarLogic.diagnose(true, listOf(cal(1, synced = false)), emptySet()))
        assertEquals(CalendarProblem.NO_EVENTS, CalendarLogic.diagnose(true, listOf(cal(1)), emptySet()))
        // Con eventi in almeno un calendario scelto non c'è nessun problema
        assertNull(CalendarLogic.diagnose(true, listOf(cal(1, n = 0), cal(2, n = 2)), emptySet()))
        // Gli eventi di un calendario spento non contano
        assertEquals(CalendarProblem.NO_EVENTS, CalendarLogic.diagnose(true, listOf(cal(1), cal(2, n = 2)), setOf(2L)))
    }

    @Test
    fun recurrenceDailyWeeklyMonthlyYearly() {
        val start = ms(LocalDateTime.of(2026, 11, 2, 9, 0)) // lunedì
        val hour = 3_600_000L
        val from = ms(LocalDateTime.of(2026, 11, 9, 0, 0))
        val to = ms(LocalDateTime.of(2026, 11, 16, 0, 0))
        // Ogni giorno: 7 occorrenze nella settimana
        assertEquals(7, Recurrence.occurrences(start, hour, "FREQ=DAILY", rome, from, to).size)
        // Ogni due giorni dal 2: 2, 4, 6, 8, 10, 12, 14 → nella finestra 10, 12, 14
        assertEquals(3, Recurrence.occurrences(start, hour, "FREQ=DAILY;INTERVAL=2", rome, from, to).size)
        // Lunedì e giovedì: 9 (lun), 12 (gio)… 9 e 12 nella finestra
        val weekly = Recurrence.occurrences(start, hour, "FREQ=WEEKLY;BYDAY=MO,TH", rome, from, to)
        assertEquals(listOf(ms(LocalDateTime.of(2026, 11, 9, 9, 0)), ms(LocalDateTime.of(2026, 11, 12, 9, 0))), weekly)
        // Mensile e annuale
        assertEquals(1, Recurrence.occurrences(start, hour, "FREQ=MONTHLY", rome, ms(LocalDateTime.of(2026, 12, 1, 0, 0)), ms(LocalDateTime.of(2026, 12, 5, 0, 0))).size)
        assertEquals(1, Recurrence.occurrences(start, hour, "FREQ=YEARLY", rome, ms(LocalDateTime.of(2027, 11, 1, 0, 0)), ms(LocalDateTime.of(2027, 11, 5, 0, 0))).size)
    }

    @Test
    fun recurrenceHonoursCountUntilAndSingleEvents() {
        val start = ms(LocalDateTime.of(2026, 11, 2, 9, 0))
        val hour = 3_600_000L
        val wide = ms(LocalDateTime.of(2027, 1, 1, 0, 0))
        assertEquals(3, Recurrence.occurrences(start, hour, "FREQ=DAILY;COUNT=3", rome, start - hour, wide).size)
        assertEquals(2, Recurrence.occurrences(start, hour, "FREQ=DAILY;UNTIL=20261103T235959Z", rome, start - hour, wide).size)
        // Senza regola: l'evento singolo, solo se tocca la finestra
        assertEquals(listOf(start), Recurrence.occurrences(start, hour, null, rome, start - hour, start + hour))
        assertTrue(Recurrence.occurrences(start, hour, "", rome, start + 2 * hour, start + 3 * hour).isEmpty())
        // Regola non capita: si vede almeno il primo evento
        assertEquals(listOf(start), Recurrence.occurrences(start, hour, "FREQ=SECONDLY", rome, start - hour, start + hour))
    }
}
