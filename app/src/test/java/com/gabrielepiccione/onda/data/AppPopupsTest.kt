// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPopupsTest {

    @Test
    fun theOldAppWidgetComesFirstAndCountsTowardsTheLimit() {
        var s = Settings(appWidgets = mapOf("a" to 1))
        (2..10).forEach { s = AppPopups.addWidget(s, "a", it) }
        assertEquals(listOf(1, 2, 3, 4), AppPopups.widgetsOf(s, "a"))
        assertFalse(AppPopups.canAddWidget(s, "a"))
    }

    @Test
    fun removingAWidgetWorksForBothPlaces() {
        val s = AppPopups.addWidget(Settings(appWidgets = mapOf("a" to 1)), "a", 2)
        assertEquals(listOf(2), AppPopups.widgetsOf(AppPopups.removeWidget(s, "a", 1), "a"))
        assertEquals(listOf(1), AppPopups.widgetsOf(AppPopups.removeWidget(s, "a", 2), "a"))
    }

    @Test
    fun relatedAppsSkipItselfDuplicatesAndTheLimit() {
        var s = AppPopups.addRelated(Settings(), "a", "a")
        assertTrue(AppPopups.of(s, "a").relatedApps.isEmpty())
        s = AppPopups.addRelated(AppPopups.addRelated(s, "a", "b"), "a", "b")
        assertEquals(listOf("b"), AppPopups.of(s, "a").relatedApps)
        (1..20).forEach { s = AppPopups.addRelated(s, "a", "x$it") }
        assertEquals(AppPopups.MAX_RELATED, AppPopups.of(s, "a").relatedApps.size)
    }

    @Test
    fun shortcutsToggleAndEmptyPopupsDisappear() {
        val hidden = AppPopups.toggleShortcut(Settings(), "a", "sc")
        assertEquals(setOf("sc"), AppPopups.of(hidden, "a").hiddenShortcuts)
        assertTrue(AppPopups.toggleShortcut(hidden, "a", "sc").appPopups.isEmpty())
    }

    @Test
    fun backupDropsWidgetsAndRestoreKeepsThoseOfThisPhone() {
        val phone = mapOf(
            "a" to AppPopup(relatedApps = listOf("old"), widgets = listOf(7)),
            "w" to AppPopup(widgets = listOf(9)),
        )
        val saved = AppPopups.forBackup(phone)
        assertEquals(mapOf("a" to AppPopup(relatedApps = listOf("old"))), saved)

        val restored = AppPopups.forRestore(mapOf("a" to AppPopup(relatedApps = listOf("new"))), phone)
        assertEquals(AppPopup(relatedApps = listOf("new"), widgets = listOf(7)), restored["a"])
        assertEquals(AppPopup(widgets = listOf(9)), restored["w"])
    }

    @Test
    fun survivesSerialization() {
        val s = AppPopups.toggleShortcut(AppPopups.addRelated(Settings(), "a", "b"), "a", "sc")
        assertEquals(s.appPopups, SettingsCodec.decode(SettingsCodec.encode(s)).appPopups)
    }
}
