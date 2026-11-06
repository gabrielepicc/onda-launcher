// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSizingTest {
    @Test
    fun defaultIsFullWidthAndPreferredHeight() {
        assertEquals(WidgetBox(null, 180), WidgetSizing.resolve(null, null, 250, 180, 380))
    }

    @Test
    fun chosenSizesAreKeptWithinLimits() {
        assertEquals(WidgetBox(200, 300), WidgetSizing.resolve(WidgetSize(200), 300, 250, 180, 380))
        assertEquals(WidgetBox(WidgetSizing.MIN_WIDTH, WidgetSizing.MAX_HEIGHT), WidgetSizing.resolve(WidgetSize(10), 9999, 250, 180, 380))
        // Più larga dello spazio: tutta la larghezza
        assertEquals(WidgetBox(null, 180), WidgetSizing.resolve(WidgetSize(900), null, 250, 180, 380))
    }

    @Test
    fun automaticUsesThePreferredSize() {
        assertEquals(WidgetBox(250, 110), WidgetSizing.resolve(WidgetSize(200, auto = true), 300, 250, 110, 380))
        assertEquals(WidgetBox(null, 110), WidgetSizing.resolve(WidgetSize(auto = true), null, 600, 110, 380))
    }

    @Test
    fun defaultSizeIsNotStored() {
        val s = WidgetSizing.set(Settings(), "w1", WidgetSize(200))
        assertEquals(WidgetSize(200), s.widgetSizes["w1"])
        assertTrue(WidgetSizing.set(s, "w1", WidgetSize()).widgetSizes.isEmpty())
    }
}
