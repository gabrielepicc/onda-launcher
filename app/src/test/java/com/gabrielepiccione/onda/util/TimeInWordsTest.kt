// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeInWordsTest {
    @Test fun itQuarterPast() = assertEquals("Sono le dieci e un quarto", TimeInWords.format(10, 15, "it"))
    @Test fun itOne() = assertEquals("È l'una", TimeInWords.format(13, 0, "it"))
    @Test fun itToOne() = assertEquals("È l'una meno venti", TimeInWords.format(12, 40, "it"))
    @Test fun itNoon() = assertEquals("È mezzogiorno", TimeInWords.format(12, 1, "it"))
    @Test fun itMidnightHalf() = assertEquals("È mezzanotte e mezza", TimeInWords.format(0, 29, "it"))
    @Test fun itRoundsToNextHour() = assertEquals("Sono le otto", TimeInWords.format(7, 58, "it"))
    @Test fun enQuarterPast() = assertEquals("It's quarter past ten", TimeInWords.format(10, 14, "en"))
    @Test fun enTwentyTo() = assertEquals("It's twenty to one", TimeInWords.format(0, 40, "en"))
    @Test fun enOClock() = assertEquals("It's seven o'clock", TimeInWords.format(19, 0, "en"))
    @Test fun enNoon() = assertEquals("It's noon", TimeInWords.format(11, 59, "en"))
}
