// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.sheets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickPopupPlacementTest {

    private fun place(
        left: Int? = 50,
        top: Int? = 100,
        bottom: Int? = 200,
        width: Int = 600,
        height: Int = 500,
        contentLeft: Int? = null,
        contentRight: Int? = null,
    ) = placeQuickPopup(
        left, top, bottom, width, height, screenWidth = 1000, screenHeight = 2000, topLimit = 80, bottomLimit = 100, margin = 20,
        contentLeft = contentLeft, contentRight = contentRight,
    )

    @Test
    fun opensCenteredOnTheRowWhenThereIsRoomAboveAndBelow() {
        val spot = place(top = 900, bottom = 1000)
        assertEquals(700, spot.y) // centro della riga (950) meno metà popup
        assertTrue(spot.centered)
        assertFalse(spot.above)
    }

    @Test
    fun staysBesideTheRowContentWhenThereIsRoom() {
        // Contenuto della riga stretto, a sinistra: il popup si mette a destra
        val spot = place(top = 900, bottom = 1000, width = 400, contentLeft = 50, contentRight = 300)
        assertTrue(spot.centered)
        assertEquals(320, spot.x)
    }

    @Test
    fun usesTheLeftSideWhenTheContentIsOnTheRight() {
        val spot = place(left = 500, top = 900, bottom = 1000, width = 400, contentLeft = 700, contentRight = 950)
        assertEquals(280, spot.x)
    }

    @Test
    fun alignsWithTheRowWhenNeitherSideHasRoom() {
        val spot = place(top = 900, bottom = 1000, width = 600, contentLeft = 50, contentRight = 800)
        assertTrue(spot.centered)
        assertEquals(50, spot.x)
    }

    @Test
    fun fallsBackBelowTheRowNearTheTop() {
        val spot = place(top = 200, bottom = 300)
        assertEquals(320, spot.y)
        assertFalse(spot.centered)
        assertFalse(spot.above)
    }

    @Test
    fun fallsBackAboveWhenItDoesNotFitBelow() {
        val spot = place(top = 1700, bottom = 1800)
        assertEquals(1180, spot.y)
        assertTrue(spot.above)
        assertFalse(spot.centered)
    }

    @Test
    fun staysOnScreenWhenItFitsNeitherWay() {
        val spot = place(top = 900, bottom = 1000, height = 1700)
        assertTrue(spot.y >= 100)
        assertTrue(spot.y <= 2000 - 100 - 20 - 1700)
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
