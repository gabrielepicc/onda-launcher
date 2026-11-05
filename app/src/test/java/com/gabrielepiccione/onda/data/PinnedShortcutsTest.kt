// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinnedShortcutsTest {

    private val maps = PinnedShortcut("com.maps", "casa", 0, "Casa")
    private val entry = SHORTCUT_PREFIX + maps.key

    @Test
    fun addingGoesAfterFavoritesWithoutDuplicates() {
        val once = PinnedShortcuts.add(Settings(favorites = listOf("a")), maps)
        assertEquals(listOf("a", entry), once.favorites)
        val twice = PinnedShortcuts.add(once, maps.copy(label = "Casa mia"))
        assertEquals(listOf("a", entry), twice.favorites)
        assertEquals("Casa mia", twice.pinnedShortcuts.single().label)
    }

    @Test
    fun removingLeavesNoTrace() {
        val s = PinnedShortcuts.remove(PinnedShortcuts.add(Settings(favorites = listOf("a")), maps), maps.key)
        assertEquals(listOf("a"), s.favorites)
        assertTrue(s.pinnedShortcuts.isEmpty())
    }

    @Test
    fun idsAreGroupedByAppAndUser() {
        val s = Settings(
            pinnedShortcuts = listOf(
                maps,
                maps.copy(shortcutId = "lavoro"),
                maps.copy(shortcutId = "altro", userSerial = 10),
                PinnedShortcut("com.chrome", "sito", 0, "Sito"),
            ),
        )
        assertEquals(listOf("casa", "lavoro"), PinnedShortcuts.idsFor(s, "com.maps", 0))
    }

    @Test
    fun backupLeavesThemOutAndRestoreKeepsThoseOfThisPhone() {
        val now = PinnedShortcuts.add(Settings(favorites = listOf("a")), maps)
        val saved = PinnedShortcuts.forBackup(now)
        assertEquals(listOf("a"), saved.favorites)
        assertTrue(saved.pinnedShortcuts.isEmpty())

        val restored = PinnedShortcuts.forRestore(Settings(favorites = listOf("b")), now)
        assertEquals(listOf("b", entry), restored.favorites)
        assertEquals(listOf(maps), restored.pinnedShortcuts)
    }

    @Test
    fun survivesSerialization() {
        val s = PinnedShortcuts.add(Settings(), maps)
        assertEquals(s.pinnedShortcuts, SettingsCodec.decode(SettingsCodec.encode(s)).pinnedShortcuts)
    }
}
