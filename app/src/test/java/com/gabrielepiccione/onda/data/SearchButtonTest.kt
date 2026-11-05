// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchButtonTest {

    @Test
    fun savedIconIsKeptWhenItExists() {
        assertEquals("bolt", SearchButton.iconOrDefault("bolt"))
    }

    @Test
    fun unknownIconFallsBackToTheSearchLens() {
        assertEquals("search", SearchButton.iconOrDefault("sparito"))
        assertEquals("search", SearchButton.iconOrDefault(""))
    }

    @Test
    fun defaultIconIsInTheList() {
        assertTrue(SearchButton.DEFAULT_ICON in SearchButton.ICONS)
    }

    @Test
    fun zeroMeansThemeColor() {
        assertTrue(SearchButton.isAutoColor(0))
        assertFalse(SearchButton.isAutoColor(0xFF7FE3D1))
    }

    @Test
    fun newButtonSettingsKeepTheOldBehaviour() {
        val s = Settings()
        assertEquals(GestureAction.SEARCH, s.searchButtonTap)
        assertEquals(GestureAction.NONE, s.searchButtonLong)
        assertEquals(GestureAction.NONE, s.searchButtonSwipe)
    }
}
