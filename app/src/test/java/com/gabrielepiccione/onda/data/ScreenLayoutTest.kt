// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenLayoutTest {

    @Test
    fun phonesKeepTheirMargins() {
        assertEquals(0, ScreenLayout.sideInsetDp(400))
        assertEquals(0, ScreenLayout.sideInsetDp(ScreenLayout.MAX_CONTENT_DP))
    }

    @Test
    fun wideScreensCenterTheContent() {
        assertEquals(170, ScreenLayout.sideInsetDp(900))
    }
}
