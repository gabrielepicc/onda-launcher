// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSuggestionsTest {

    private val base = Settings(
        suggestInHome = true,
        launchCounts = mapOf("a" to 10, "b" to 30, "c" to 20, "d" to 5, "poco" to 2),
    )
    private val all = setOf("a", "b", "c", "d", "poco")

    @Test
    fun picksTheMostOpenedUpToTheLimit() {
        assertEquals(listOf("b", "c", "a"), HomeSuggestions.pick(base, all))
    }

    @Test
    fun offByDefaultAndWhenTurnedOff() {
        assertTrue(HomeSuggestions.pick(Settings(launchCounts = base.launchCounts), all).isEmpty())
    }

    @Test
    fun skipsFavoritesAndAppsInFolders() {
        val s = base.copy(
            favorites = listOf("b"),
            folders = mapOf("f" to Folder("f", "Lavoro", listOf("c"))),
        )
        assertEquals(listOf("a", "d"), HomeSuggestions.pick(s, all))
    }

    @Test
    fun skipsWhatCannotBeShownOrIsRarelyUsed() {
        assertEquals(listOf("d"), HomeSuggestions.pick(base, setOf("d", "poco")))
    }

    @Test
    fun dismissedAppsStayOutUntilRestored() {
        val dismissed = HomeSuggestions.dismiss(base, "b")
        assertEquals(listOf("c", "a", "d"), HomeSuggestions.pick(dismissed, all))
        assertEquals(listOf("b", "c", "a"), HomeSuggestions.pick(HomeSuggestions.restore(dismissed, "b"), all))
    }
}
