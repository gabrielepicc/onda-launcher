// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.abs

class HomeLightsTest {
    private fun utc(y: Int, m: Int, d: Int, h: Int, min: Int) =
        ZonedDateTime.of(y, m, d, h, min, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
    private fun near(expected: Long, actual: Long, minutes: Int = 6) =
        assertTrue("differenza ${(actual - expected) / 60_000} min", abs(actual - expected) <= minutes * 60_000L)

    @Test
    fun romeSummerSolstice() {
        // Roma, 21 giugno: alba 03:35 UTC circa, tramonto 18:48 UTC circa
        val sun = HomeLights.sunTimes(LocalDate.of(2026, 6, 21), 41.9, 12.5)!!
        near(utc(2026, 6, 21, 3, 36), sun.sunrise)
        near(utc(2026, 6, 21, 18, 48), sun.sunset)
    }

    @Test
    fun romeWinterSolstice() {
        // Roma, 21 dicembre: alba 06:34 UTC circa, tramonto 15:42 UTC circa
        val sun = HomeLights.sunTimes(LocalDate.of(2026, 12, 21), 41.9, 12.5)!!
        near(utc(2026, 12, 21, 6, 35), sun.sunrise)
        near(utc(2026, 12, 21, 15, 42), sun.sunset)
    }

    @Test
    fun polarNightHasNoSunset() {
        assertNull(HomeLights.sunTimes(LocalDate.of(2026, 12, 21), 78.2, 15.6))
    }

    @Test
    fun ssidIsComparedWithoutQuotes() {
        assertEquals("Casa", HomeLights.normalizeSsid("\"Casa\""))
        assertNull(HomeLights.normalizeSsid("<unknown ssid>"))
        assertTrue(HomeLights.isHome("\"Casa\"", "Casa"))
        assertFalse(HomeLights.isHome("\"Ufficio\"", "Casa"))
        assertFalse(HomeLights.isHome(null, "Casa"))
        assertFalse(HomeLights.isHome("\"Casa\"", ""))
    }

    @Test
    fun darkFromJustBeforeSunsetUntilSunrise() {
        val sun = SunTimes(sunrise = 1_000_000L, sunset = 10_000_000L)
        assertTrue(HomeLights.isDark(500_000L, sun, 0))
        assertFalse(HomeLights.isDark(5_000_000L, sun, 0))
        assertTrue(HomeLights.isDark(10_000_000L - 60_000L, sun, 0))
        // Senza posizione: dalle 19 alle 7
        assertTrue(HomeLights.isDark(5_000_000L, null, 21 * 60 + 35))
        assertTrue(HomeLights.isDark(5_000_000L, null, 6 * 60))
        assertFalse(HomeLights.isDark(5_000_000L, null, 12 * 60))
    }

    @Test
    fun showsOnlyWithEverythingInPlace() {
        val sun = SunTimes(1_000_000L, 10_000_000L)
        assertTrue(HomeLights.shouldShow(true, 7, "\"Casa\"", "Casa", 11_000_000L, sun, 0))
        assertFalse(HomeLights.shouldShow(false, 7, "\"Casa\"", "Casa", 11_000_000L, sun, 0))
        assertFalse(HomeLights.shouldShow(true, 0, "\"Casa\"", "Casa", 11_000_000L, sun, 0))
        assertFalse(HomeLights.shouldShow(true, 7, "\"Casa\"", "Casa", 5_000_000L, sun, 0))
    }

    @Test
    fun missingSaysWhatToFixFirst() {
        assertEquals(HomeLights.Missing.PERMISSION, HomeLights.missing(true, false, true, 7, "Casa", "Casa", true))
        assertEquals(HomeLights.Missing.LOCATION_OFF, HomeLights.missing(true, true, false, 7, null, "Casa", true))
        assertEquals(HomeLights.Missing.NO_HOME_WIFI, HomeLights.missing(true, true, true, 7, "Casa", "", true))
        assertEquals(HomeLights.Missing.NOT_HOME, HomeLights.missing(true, true, true, 7, "Ufficio", "Casa", true))
        assertEquals(HomeLights.Missing.NO_WIDGET, HomeLights.missing(true, true, true, 0, "Casa", "Casa", true))
        assertEquals(HomeLights.Missing.NOT_DARK, HomeLights.missing(true, true, true, 7, "Casa", "Casa", false))
        assertNull(HomeLights.missing(true, true, true, 7, "Casa", "Casa", true))
    }
}
