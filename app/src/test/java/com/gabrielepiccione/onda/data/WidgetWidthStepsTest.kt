// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetWidthStepsTest {
    @Test
    fun widthSnapsToHalfThreeQuartersOrFull() {
        assertEquals(2, WidgetSizing.widthStep(0, 400))
        assertEquals(2, WidgetSizing.widthStep(390, 400))
        assertEquals(1, WidgetSizing.widthStep(300, 400))
        assertEquals(0, WidgetSizing.widthStep(190, 400))
    }

    @Test
    fun stepsBecomeWidths() {
        assertEquals(0, WidgetSizing.widthForStep(2, 400))
        assertEquals(300, WidgetSizing.widthForStep(1, 400))
        assertEquals(200, WidgetSizing.widthForStep(0, 400))
        // Mai sotto la larghezza minima
        assertEquals(WidgetSizing.MIN_WIDTH, WidgetSizing.widthForStep(0, 100))
    }
}
