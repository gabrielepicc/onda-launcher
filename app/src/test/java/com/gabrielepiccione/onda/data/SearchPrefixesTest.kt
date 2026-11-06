// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchPrefixesTest {
    private val set = SearchPrefixSet()

    @Test
    fun symbolPrefixesWorkWithoutSpace() {
        assertEquals(PrefixKind.CALC to "2+2", SearchPrefixes.parse("=2+2", set))
        assertEquals(PrefixKind.CONTACTS to "anna", SearchPrefixes.parse("@anna", set))
        assertEquals(PrefixKind.SETTINGS to "wifi", SearchPrefixes.parse("/wifi", set))
    }

    @Test
    fun wordPrefixesNeedASpace() {
        assertEquals(PrefixKind.PLAY to "bohemian rhapsody", SearchPrefixes.parse("p bohemian rhapsody", set))
        assertEquals(PrefixKind.PLAY to "queen", SearchPrefixes.parse("P queen", set))
        assertEquals(null to "pizza", SearchPrefixes.parse("pizza", set))
        assertEquals(null to "p", SearchPrefixes.parse("p", set))
    }

    @Test
    fun longerPrefixWins() {
        assertEquals(PrefixKind.SPOTIFY to "daft punk", SearchPrefixes.parse("sp daft punk", set))
        assertEquals(null to "spotify", SearchPrefixes.parse("spotify", set))
    }

    @Test
    fun customPrefixesAreUsed() {
        val custom = set.with(PrefixKind.PLAY, "play").with(PrefixKind.CALC, "#")
        assertEquals(PrefixKind.PLAY to "jazz", SearchPrefixes.parse("play jazz", custom))
        assertEquals(PrefixKind.CALC to "3*3", SearchPrefixes.parse("#3*3", custom))
        assertEquals(null to "p jazz", SearchPrefixes.parse("p jazz", custom))
    }

    @Test
    fun validation() {
        assertTrue(SearchPrefixes.valid(set))
        assertFalse(SearchPrefixes.valid(set.with(PrefixKind.SPOTIFY, "p")))
        assertFalse(SearchPrefixes.valid(set.with(PrefixKind.PLAY, "")))
        assertFalse(SearchPrefixes.valid(set.with(PrefixKind.PLAY, "a b")))
        assertFalse(SearchPrefixes.valid(set.with(PrefixKind.PLAY, "lungo")))
    }

    @Test
    fun spotifySearchLinkIsEncoded() {
        assertEquals("spotify:search:daft%20punk", SpotifyLinks.search(" daft punk "))
        assertEquals("spotify:search:caf%C3%A9%20%26%20co", SpotifyLinks.search("café & co"))
    }
}
