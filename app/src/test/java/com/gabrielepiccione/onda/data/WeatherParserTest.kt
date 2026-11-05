// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class WeatherParserTest {

    private val now = 1_000_000_000L
    private val hour = 3_600_000L

    private val sample = """
        {
          "refreshTime": 999999000,
          "bulletin": {"weekly": "ignorato"},
          "current": {
            "weatherText": "Sereno",
            "weatherCode": "clear",
            "temperature": {
              "temperature": {"value": 18.4, "unit": "°C"},
              "sourceFeelsLike": {"value": 16.6, "unit": "°C"}
            }
          },
          "hourly": [
            {"date": ${now - 2 * hour}, "temperature": {"temperature": {"value": 10}}},
            {"date": ${now + hour}, "temperature": {"temperature": {"value": 19.5}}}
          ],
          "daily": [
            {"date": ${now - 48 * hour}, "day": {"temperature": {"temperature": {"value": 1}}}},
            {
              "date": ${now - hour},
              "day": {"weatherText": "Sole", "temperature": {"temperature": {"value": 21}}},
              "night": {"weatherText": "Sereno", "temperature": {"temperature": {"value": 9.2}}}
            }
          ],
          "campoFuturo": {"a": 1}
        }
    """.trimIndent()

    @Test
    fun readsCurrentConditionsIgnoringUnknownFields() {
        val s = WeatherParser.parse(sample, "Milano", now)
        assertNotNull(s)
        assertEquals("Milano", s!!.place)
        assertEquals(18, s.temperature)
        assertEquals(17, s.feelsLike)
        assertEquals("Sereno", s.text)
        assertEquals(999999000L, s.refreshedAt)
    }

    @Test
    fun keepsOnlyUpcomingHoursAndDays() {
        val s = WeatherParser.parse(sample, "Milano", now)!!
        assertEquals(listOf(WeatherHour(now + hour, 20)), s.hours)
        assertEquals(listOf(WeatherDay(now - hour, 21, 9, "Sole")), s.days)
    }

    @Test
    fun limitsTheForecast() {
        val hours = (0 until 40).joinToString(",") { """{"date": ${now + it * hour}}""" }
        val days = (0 until 20).joinToString(",") { """{"date": ${now + it * 24 * hour}}""" }
        val s = WeatherParser.parse("""{"hourly": [$hours], "daily": [$days]}""", "", now)!!
        assertEquals(24, s.hours.size)
        assertEquals(7, s.days.size)
    }

    @Test
    fun noDataOrBadTextGivesNothing() {
        assertNull(WeatherParser.parse("{}", "Milano", now))
        assertNull(WeatherParser.parse("non è json", "Milano", now))
    }

    @Test
    fun readsTheCompressedField() {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).bufferedWriter(Charsets.UTF_8).use { it.write("città ☀") }
        assertEquals("città ☀", WeatherParser.gunzip(out.toByteArray()))
    }
}
