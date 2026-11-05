// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AlphabetLayoutTest {

    @Test
    fun columnStaysCompactAndAtTheBottom() {
        assertEquals(50f, AlphabetLayout.step(2000f, 10, 50f), 0.01f)
        assertEquals(1500f, AlphabetLayout.top(2000f, 10, 50f), 0.01f)
    }

    @Test
    fun columnShrinksWhenLettersDoNotFit() {
        assertEquals(20f, AlphabetLayout.step(600f, 30, 50f), 0.01f)
        assertEquals(0f, AlphabetLayout.top(600f, 30, 50f), 0.01f)
    }

    @Test
    fun touchingAboveTheColumnSelectsTheFirstMark() {
        assertEquals(0, AlphabetLayout.indexAt(10f, 2000f, 10, 50f))
        assertEquals(0, AlphabetLayout.indexAt(1499f, 2000f, 10, 50f))
    }

    @Test
    fun touchingTheColumnSelectsTheLetterUnderTheFinger() {
        assertEquals(0, AlphabetLayout.indexAt(1525f, 2000f, 10, 50f))
        assertEquals(1, AlphabetLayout.indexAt(1560f, 2000f, 10, 50f))
        assertEquals(9, AlphabetLayout.indexAt(1999f, 2000f, 10, 50f))
        assertEquals(9, AlphabetLayout.indexAt(3000f, 2000f, 10, 50f))
    }

    @Test
    fun emptyAlphabetSelectsNothing() {
        assertEquals(-1, AlphabetLayout.indexAt(10f, 2000f, 0, 50f))
    }
}
