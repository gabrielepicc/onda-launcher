// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AlphabetLayoutTest {

    @Test
    fun restFractionMovesTheColumnBetweenBottomAndTop() {
        assertEquals(1500f, AlphabetLayout.top(2000f, 10, 50f, 1f), 0.01f)
        assertEquals(750f, AlphabetLayout.top(2000f, 10, 50f, 0.5f), 0.01f)
        assertEquals(0f, AlphabetLayout.top(2000f, 10, 50f, 0f), 0.01f)
    }

    @Test
    fun anchoredColumnStaysPutWhenTouchIsOnTheLetters() {
        assertEquals(750f, AlphabetLayout.anchoredTop(1000f, 2000f, 10, 50f, 0, 0.5f), 0.01f)
    }

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
    fun touchingTheLettersDoesNotMoveTheColumn() {
        assertEquals(1500f, AlphabetLayout.anchoredTop(1500f, 2000f, 10, 50f), 0.01f)
        assertEquals(1500f, AlphabetLayout.anchoredTop(1800f, 2000f, 10, 50f), 0.01f)
    }

    @Test
    fun touchingAboveBringsTheColumnUnderTheFinger() {
        val top = AlphabetLayout.anchoredTop(600f, 2000f, 10, 50f)
        assertEquals(575f, top, 0.01f)
        assertEquals(0, AlphabetLayout.indexAt(600f, 2000f, 10, 50f, top))
        assertEquals(4, AlphabetLayout.indexAt(800f, 2000f, 10, 50f, top))
    }

    @Test
    fun theAnchorSkipsTheFavoritesMark() {
        val top = AlphabetLayout.anchoredTop(600f, 2000f, 10, 50f, anchorIndex = 1)
        assertEquals(1, AlphabetLayout.indexAt(600f, 2000f, 10, 50f, top))
    }

    @Test
    fun theColumnNeverLeavesTheZone() {
        assertEquals(0f, AlphabetLayout.anchoredTop(5f, 2000f, 10, 50f, anchorIndex = 1), 0.01f)
        assertEquals(0f, AlphabetLayout.anchoredTop(100f, 600f, 30, 50f), 0.01f)
    }

    @Test
    fun emptyAlphabetSelectsNothing() {
        assertEquals(-1, AlphabetLayout.indexAt(10f, 2000f, 0, 50f))
    }
}
