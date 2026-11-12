// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class CalendarDaysTest {
    private val rome = ZoneId.of("Europe/Rome")
    private val auckland = ZoneId.of("Pacific/Auckland") // UTC+13 a novembre
    private val honolulu = ZoneId.of("Pacific/Honolulu") // UTC−10

    /** Come lo consegna Android: mezzanotte UTC del giorno di calendario, poi portato al giorno locale da allDayLocal. */
    private fun allDay(title: String, date: LocalDate, days: Int, zone: ZoneId, id: Long = 1L, cal: Long = 1L): CalendarEvent {
        val beginUtc = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endUtc = date.plusDays(days.toLong()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val (begin, end) = CalendarLogic.allDayLocal(beginUtc, endUtc, zone)
        return CalendarEvent(title, begin, true, id, end, 0xFF0000, cal)
    }

    private fun timed(title: String, at: LocalDateTime, zone: ZoneId, id: Long = 50L): CalendarEvent {
        val begin = at.atZone(zone).toInstant().toEpochMilli()
        return CalendarEvent(title, begin, false, id, begin + 3_600_000L, 0, 1L)
    }

    private fun now(zone: ZoneId, y: Int = 2026, m: Int = 11, d: Int = 9, h: Int = 12) = LocalDateTime.of(y, m, d, h, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun todayAndTomorrowAllDayEventsStayOnTheirOwnDay() {
        val today = allDay("Oggi", LocalDate.of(2026, 11, 9), 1, rome)
        val tomorrow = allDay("Domani", LocalDate.of(2026, 11, 10), 1, rome, id = 2)
        val strip = CalendarDays.strip(listOf(today, tomorrow), now(rome), rome, AllDayMode.ALWAYS)
        assertEquals(listOf("Oggi"), strip.labels.map { it.event.title })
        // Domani compare solo nell'agenda di domani
        val groups = CalendarDays.groups(listOf(today, tomorrow), AllDayMode.ALWAYS, now(rome), rome, LocalDate.of(2026, 11, 9), 3)
        assertEquals(listOf(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 10)), groups.map { it.date })
        assertEquals("Domani", groups[1].allDay.single().event.title)
    }

    @Test
    fun threeDayEventAppearsEveryDayWithDayNOfM() {
        val holiday = allDay("Ferie", LocalDate.of(2026, 11, 9), 3, rome)
        assertEquals(3, CalendarDays.dayCount(holiday, rome))
        val groups = CalendarDays.groups(listOf(holiday), AllDayMode.ALWAYS, now(rome), rome, LocalDate.of(2026, 11, 9), 5)
        assertEquals(3, groups.size)
        assertEquals(listOf(1, 2, 3), groups.map { it.allDay.single().dayIndex })
        assertTrue(groups.all { it.allDay.single().dayCount == 3 && it.allDay.single().multiDay })
        // Il secondo giorno la riga sotto l'orologio dice «giorno 2 di 3»
        val strip = CalendarDays.strip(listOf(holiday), now(rome, d = 10), rome, AllDayMode.ALWAYS)
        assertEquals(2, strip.labels.single().dayIndex)
        // Il quarto giorno non c'è più
        assertTrue(CalendarDays.strip(listOf(holiday), now(rome, d = 12), rome, AllDayMode.ALWAYS).isEmpty)
    }

    @Test
    fun allDayEventsDoNotSlideDayInFarTimeZones() {
        // Evento «10 novembre» creato in un fuso lontano: sempre il 10 nel giorno locale di chi lo guarda
        listOf(auckland, honolulu, rome, ZoneId.of("UTC")).forEach { zone ->
            val e = allDay("Dieci", LocalDate.of(2026, 11, 10), 1, zone)
            assertEquals(zone.id, LocalDate.of(2026, 11, 10), CalendarDays.firstDay(e, zone))
            assertEquals(zone.id, 1, CalendarDays.dayCount(e, zone))
            // Il 10 alle 00:30 locale è già «oggi»; il 9 alle 23:30 locale non lo è ancora
            val early = LocalDateTime.of(2026, 11, 10, 0, 30).atZone(zone).toInstant().toEpochMilli()
            val late = LocalDateTime.of(2026, 11, 9, 23, 30).atZone(zone).toInstant().toEpochMilli()
            assertEquals(zone.id, 1, CalendarDays.strip(listOf(e), early, zone, AllDayMode.ALWAYS).labels.size)
            assertEquals(zone.id, 0, CalendarDays.strip(listOf(e), late, zone, AllDayMode.ALWAYS).labels.size)
        }
    }

    @Test
    fun onlyAllDayTodayShowsThemInsteadOfNoEvent() {
        val birthday = allDay("Compleanno di Anna", LocalDate.of(2026, 11, 9), 1, rome)
        val strip = CalendarDays.strip(listOf(birthday), now(rome), rome, AllDayMode.ALWAYS)
        assertNull(strip.timed)
        assertEquals(1, strip.labels.size)
        assertTrue(!strip.isEmpty)
        assertTrue(CalendarDays.strip(emptyList(), now(rome), rome, AllDayMode.ALWAYS).isEmpty)
    }

    @Test
    fun modesAlwaysOnlyIfNoneAndHide() {
        val birthday = allDay("Compleanno", LocalDate.of(2026, 11, 9), 1, rome)
        val meeting = timed("Riunione", LocalDateTime.of(2026, 11, 9, 15, 0), rome)
        val events = listOf(birthday, meeting)
        val n = now(rome)
        assertEquals(1, CalendarDays.strip(events, n, rome, AllDayMode.ALWAYS).labels.size)
        assertEquals("Riunione", CalendarDays.strip(events, n, rome, AllDayMode.ALWAYS).timed?.title)
        // Solo se non ci sono altri eventi: con la riunione le etichette spariscono, da sole restano
        assertTrue(CalendarDays.strip(events, n, rome, AllDayMode.ONLY_IF_NONE).labels.isEmpty())
        assertEquals(1, CalendarDays.strip(listOf(birthday), n, rome, AllDayMode.ONLY_IF_NONE).labels.size)
        assertTrue(CalendarDays.strip(events, n, rome, AllDayMode.HIDE).labels.isEmpty())
        assertTrue(CalendarDays.groups(listOf(birthday), AllDayMode.HIDE, n, rome, LocalDate.of(2026, 11, 9), 3).isEmpty())
    }

    @Test
    fun agendaPutsAllDayFirstThenTimedAndLimitsTheStripToTwo() {
        val a = allDay("A", LocalDate.of(2026, 11, 9), 1, rome, id = 1)
        val b = allDay("B", LocalDate.of(2026, 11, 9), 1, rome, id = 2)
        val c = allDay("C", LocalDate.of(2026, 11, 9), 1, rome, id = 3)
        val morning = timed("Caffè", LocalDateTime.of(2026, 11, 9, 13, 0), rome, id = 10)
        val groups = CalendarDays.groups(listOf(morning, c, a, b), AllDayMode.ALWAYS, now(rome), rome, LocalDate.of(2026, 11, 9), 1)
        assertEquals(listOf("A", "B", "C"), groups.single().allDay.map { it.event.title })
        assertEquals(listOf("Caffè"), groups.single().timed.map { it.title })
        val (shown, more) = CalendarDays.splitForStrip(groups.single().allDay)
        assertEquals(2, shown.size)
        assertEquals(1, more)
    }
}
