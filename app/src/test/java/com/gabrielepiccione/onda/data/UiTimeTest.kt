// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class UiTimeTest {
    private val rome = ZoneId.of("Europe/Rome")
    private fun ms(dt: LocalDateTime, zone: ZoneId = rome) = dt.atZone(zone).toInstant().toEpochMilli()

    @Test
    fun ageGoesFromNowToMinutesHoursAndDate() {
        val post = 1_000_000_000L
        assertEquals(Age(AgeKind.NOW), UiTime.age(post, post + 59_999L))
        assertEquals(Age(AgeKind.MINUTES, 1), UiTime.age(post, post + 60_000L))
        assertEquals(Age(AgeKind.MINUTES, 4), UiTime.age(post, post + 4 * 60_000L + 1_000L))
        assertEquals(Age(AgeKind.MINUTES, 59), UiTime.age(post, post + 59 * 60_000L))
        assertEquals(Age(AgeKind.HOURS, 1), UiTime.age(post, post + 60 * 60_000L))
        assertEquals(Age(AgeKind.HOURS, 23), UiTime.age(post, post + (24 * 60 - 1) * 60_000L))
        assertEquals(Age(AgeKind.DATE), UiTime.age(post, post + 24 * 60 * 60_000L))
    }

    @Test
    fun ageChangesAsTheClockAdvances() {
        // Lo stesso post: «4 min» diventa «5 min» appena passa il minuto, senza ridisegnare altro
        val post = 5_000_000L
        assertEquals(Age(AgeKind.MINUTES, 4), UiTime.age(post, post + 4 * 60_000L + 30_000L))
        assertEquals(Age(AgeKind.MINUTES, 5), UiTime.age(post, post + 5 * 60_000L))
    }

    @Test
    fun clockMovedBackNeverGivesNegativeAges() {
        val post = 10_000_000L
        assertEquals(0L, UiTime.elapsedMinutes(post, post - 5 * 60_000L))
        assertEquals(Age(AgeKind.NOW), UiTime.age(post, post - 3_600_000L))
    }

    @Test
    fun nextTickAlignsToTheSystemClockAndIsAtMostOnePeriod() {
        // A 12:00:20 il prossimo minuto è tra 40 s (+30 ms di margine)
        val t = ms(LocalDateTime.of(2026, 11, 9, 12, 0, 20))
        assertEquals(40_030L, UiTime.nextTickDelayMs(t))
        // Subito dopo il minuto si aspetta quasi un minuto intero, mai di più
        val boundary = ms(LocalDateTime.of(2026, 11, 9, 12, 1, 0))
        assertEquals(60_000L, UiTime.nextTickDelayMs(boundary))
        assertEquals(60_000L, UiTime.nextTickDelayMs(boundary + 1L))
        assertTrue(UiTime.nextTickDelayMs(t) <= UiTime.MINUTE_MS)
        // Con i secondi: ogni secondo, riallineato all'ora reale
        assertEquals(780L, UiTime.nextTickDelayMs(boundary + 250L, periodMs = 1_000L))
        assertTrue(UiTime.nextTickDelayMs(boundary + 999L, periodMs = 1_000L) <= 1_000L)
    }

    @Test
    fun minuteOfDayFollowsTheLocalZone() {
        val t = ms(LocalDateTime.of(2026, 11, 9, 14, 7), rome)
        assertEquals(14 * 60 + 7, UiTime.minuteOfDay(t, rome))
        // Cambiando fuso lo stesso istante cade a un'altra ora
        assertEquals(8 * 60 + 7, UiTime.minuteOfDay(t, ZoneId.of("America/New_York")))
        assertEquals(22 * 60 + 7, UiTime.minuteOfDay(t, ZoneId.of("Asia/Tokyo")))
    }

    @Test
    fun dayChangesAtLocalMidnightAndWithTheTimeZone() {
        val before = ms(LocalDateTime.of(2026, 11, 9, 23, 59))
        val after = ms(LocalDateTime.of(2026, 11, 10, 0, 1))
        assertTrue(UiTime.dayChanged(before, after, rome))
        assertFalse(UiTime.dayChanged(before, before + 60_000L - 1, rome))
        // Lo stesso intervallo, visto da un altro fuso, non è un cambio di giorno
        assertFalse(UiTime.dayChanged(before, after, ZoneId.of("Pacific/Honolulu")))
        // Cambio di fuso: lo stesso istante, due giorni diversi
        val t = ms(LocalDateTime.of(2026, 11, 9, 23, 30), rome)
        assertEquals(java.time.LocalDate.of(2026, 11, 9), UiTime.localDate(t, rome))
        assertEquals(java.time.LocalDate.of(2026, 11, 10), UiTime.localDate(t, ZoneId.of("Asia/Tokyo")))
    }

    @Test
    fun clockJumpsAreDetected() {
        assertFalse(UiTime.clockJumped(1_000_000L, 1_060_000L))
        assertTrue(UiTime.clockJumped(1_000_000L, 900_000L))
        assertTrue(UiTime.clockJumped(1_000_000L, 1_000_000L + 3 * 60_000L))
    }
}
