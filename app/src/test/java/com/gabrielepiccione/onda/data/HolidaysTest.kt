// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HolidaysTest {
    @Test
    fun easterIsRight() {
        assertEquals(LocalDate.of(2025, 4, 20), Holidays.easter(2025))
        assertEquals(LocalDate.of(2026, 4, 5), Holidays.easter(2026))
        assertEquals(LocalDate.of(2027, 3, 28), Holidays.easter(2027))
        assertEquals(LocalDate.of(2024, 3, 31), Holidays.easter(2024))
    }

    @Test
    fun nationalHolidaysIncludePasquetta() {
        assertTrue(Holidays.builtInItalian(LocalDate.of(2026, 4, 6)))
        assertTrue(Holidays.builtInItalian(LocalDate.of(2026, 12, 25)))
        assertTrue(Holidays.builtInItalian(LocalDate.of(2026, 6, 2)))
        assertTrue(Holidays.builtInItalian(LocalDate.of(2026, 1, 6)))
        assertFalse(Holidays.builtInItalian(LocalDate.of(2026, 4, 5).minusDays(1)))
        assertFalse(Holidays.builtInItalian(LocalDate.of(2026, 11, 6)))
    }

    @Test
    fun theCalendarWinsWhenItIsAvailable() {
        val fromCalendar = setOf(LocalDate.of(2026, 10, 4)) // una festa locale, per esempio San Francesco
        assertTrue(Holidays.isHoliday(LocalDate.of(2026, 10, 4), fromCalendar))
        // Con il calendario che non segna il 25 dicembre vale il calendario, non l'elenco interno
        assertFalse(Holidays.isHoliday(LocalDate.of(2026, 12, 25), fromCalendar))
        // Senza calendario si usa l'elenco interno
        assertTrue(Holidays.isHoliday(LocalDate.of(2026, 12, 25), null))
    }

    @Test
    fun googleHolidayCalendarIsRecognised() {
        assertTrue(Holidays.isItalianHolidayCalendar("it.italian#holiday@group.v.calendar.google.com", "Festività in Italia"))
        assertTrue(Holidays.isItalianHolidayCalendar("it.italian#holiday@group.v.calendar.google.com", null))
        assertTrue(Holidays.isItalianHolidayCalendar(null, "Holidays in Italy"))
        assertFalse(Holidays.isItalianHolidayCalendar("en.usa#holiday@group.v.calendar.google.com", "Holidays in United States"))
        assertFalse(Holidays.isItalianHolidayCalendar("me@gmail.com", "Personale"))
    }
}
