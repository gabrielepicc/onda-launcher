// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.core.icons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IconMatcherTest {

    @Test fun exactMatch() = assertEquals(100, IconMatcher.score("Maps", "com.google.android.apps.maps", "maps"))

    @Test fun allWordsMatch() = assertTrue(IconMatcher.score("Google Maps", "com.google.android.apps.maps", "google_maps_alt") >= 90)

    @Test fun packageWordMatches() = assertTrue(IconMatcher.score("Musica", "com.spotify.music", "spotify") >= 70)

    @Test fun unrelatedIsLow() = assertTrue(IconMatcher.score("Calcolatrice", "com.android.calculator2", "weather") < 50)

    @Test
    fun suggestOrdersByRelevance() {
        val s = IconMatcher.suggest("WhatsApp", "com.whatsapp", listOf("weather", "whatsapp_alt", "whatsapp", "whats"))
        assertEquals("whatsapp", s.first())
        assertTrue("weather" !in s)
    }

    @Test fun camelCaseTokens() = assertEquals(listOf("my", "files"), IconMatcher.tokens("MyFiles"))
}
