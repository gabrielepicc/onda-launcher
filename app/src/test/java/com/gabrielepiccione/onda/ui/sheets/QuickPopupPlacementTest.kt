// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.sheets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickPopupPlacementTest {

    private fun place(left: Int? = 50, top: Int? = 100, bottom: Int? = 200, width: Int = 600, height: Int = 500) =
        placeQuickPopup(left, top, bottom, width, height, screenWidth = 1000, screenHeight = 2000, topLimit = 80, bottomLimit = 100, margin = 20)

    @Test
    fun opensBelowTheRowWhenThereIsRoom() {
        val spot = place(top = 300, bottom = 400)
        assertEquals(420, spot.y)
        assertFalse(spot.above)
    }

    @Test
    fun opensAboveWhenItDoesNotFitBelow() {
        val spot = place(top = 1600, bottom = 1700)
        assertEquals(1080, spot.y)
        assertTrue(spot.above)
    }

    @Test
    fun staysOnScreenWhenItFitsNeitherWay() {
        val spot = place(top = 900, bottom = 1000, height = 1700)
        assertTrue(spot.y >= 100)
    }

    @Test
    fun neverLeavesTheSides() {
        assertEquals(20, place(left = 0).x)
        assertEquals(380, place(left = 900).x)
    }

    @Test
    fun withoutARowItStaysCentered() {
        val spot = place(left = null, top = null, bottom = null)
        assertEquals(200, spot.x)
        assertEquals(750, spot.y)
    }
}
