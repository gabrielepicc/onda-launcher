// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchTest {

    @Test
    fun ignoresCaseAndAccents() {
        assertTrue(SettingsSearch.matches("ANAGRAFICA", "Anagràfica"))
        assertEquals("perche", SettingsSearch.normalize("Perché"))
    }

    @Test
    fun everyWordMustAppearInAnyOrder() {
        assertTrue(SettingsSearch.matches("colore pulsante", "Pulsante di ricerca", "Colore"))
        assertFalse(SettingsSearch.matches("colore orologio", "Pulsante di ricerca", "Colore"))
    }

    @Test
    fun emptyQueryFindsNothing() {
        assertFalse(SettingsSearch.matches("   ", "Orologio"))
        assertFalse(SettingsSearch.matches("", "Orologio"))
    }

    @Test
    fun partialWordsMatch() {
        assertTrue(SettingsSearch.matches("orolo", "Orologio e data"))
    }
}
