// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetCatalogTest {
    private fun w(key: String, label: String, app: String, pkg: String, w: Int = 250, h: Int = 110) = WidgetEntry(key, label, app, pkg, w, h)

    private val all = listOf(
        w("cal/month", "Mese", "Calendario", "cal"),
        w("cal/agenda", "Agenda", "Calendario", "cal", 180, 180),
        w("weather/now", "Adesso", "Breezy Weather", "weather"),
        w("ass/home", "Casa", "Àssistente", "ass"),
    )

    @Test
    fun cellsFollowTheAndroidFormula() {
        assertEquals(1, WidgetCatalog.cells(40))
        assertEquals(2, WidgetCatalog.cells(110))
        assertEquals(4, WidgetCatalog.cells(250))
        assertEquals("4×2", WidgetCatalog.sizeText(all.first()))
    }

    @Test
    fun searchAlsoByAppName() {
        assertEquals(listOf("cal/month", "cal/agenda"), WidgetCatalog.filter(all, "calendario").map { it.key })
        assertEquals(listOf("weather/now"), WidgetCatalog.filter(all, "ADESSO").map { it.key })
        assertEquals(listOf("ass/home"), WidgetCatalog.filter(all, "assist").map { it.key })
    }

    @Test
    fun groupsByAppAlphabeticallyWithLetters() {
        val groups = WidgetCatalog.groups(all)
        assertEquals(listOf("Àssistente", "Breezy Weather", "Calendario"), groups.map { it.appLabel })
        assertEquals(listOf("Agenda", "Mese"), groups.last().widgets.map { it.label })
        assertEquals(listOf("A", "B", "C"), WidgetCatalog.letters(groups))
        assertEquals(2, WidgetCatalog.indexOfLetter(groups, "C"))
    }

    @Test
    fun recentFirstAndSuggestionsFromFavorites() {
        val recent = WidgetCatalog.rememberPick(WidgetCatalog.rememberPick(emptyList(), "cal/month"), "weather/now")
        assertEquals(listOf("weather/now", "cal/month"), recent)
        assertEquals(listOf("weather/now", "cal/month"), WidgetCatalog.recent(all, recent + "gone/widget").map { it.key })
        // I suggeriti escludono quelli già recenti
        assertEquals(listOf("cal/agenda"), WidgetCatalog.suggested(all, listOf("cal"), recent.toSet()).map { it.key })
    }

    @Test
    fun recentListIsCapped() {
        var keys = emptyList<String>()
        repeat(12) { keys = WidgetCatalog.rememberPick(keys, "k$it") }
        assertEquals(WidgetCatalog.MAX_RECENT, keys.size)
        assertEquals("k11", keys.first())
    }
}
