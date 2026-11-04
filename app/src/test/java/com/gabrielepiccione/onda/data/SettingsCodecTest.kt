// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsCodecTest {

    /** Backup esportato da Onda 0.3 (schema 1). */
    private val legacyBackup = """
        {
          "version": 2,
          "favorites": ["com.a/com.a.Main#0", "folder:f1"],
          "hidden": ["com.h/com.h.Main#0"],
          "renames": {"com.a/com.a.Main#0": "Posta"},
          "swipeActions": {},
          "folders": [ {"id": "f1", "name": "Lavoro", "apps": ["com.b/com.b.Main#0"], "widgets": [12]} ],
          "appWidgets": {"com.a/com.a.Main#0": 7},
          "launchCounts": {"com.a/com.a.Main#0": 3},
          "muted": ["com.spam"],
          "themeMode": "DARK",
          "font": "SERIF",
          "swipeDown": "SEARCH",
          "swipeUp": "NOTIFICATIONS",
          "iconSize": 1.2,
          "widgets": [5, 6],
          "campoSconosciuto": true
        }
    """.trimIndent()

    @Test
    fun readsLegacySchema1Backup() {
        val s = SettingsCodec.decode(legacyBackup)
        assertEquals(Settings.CURRENT_SCHEMA, s.schemaVersion)
        assertEquals(listOf("com.a/com.a.Main#0", "folder:f1"), s.favorites)
        assertEquals("Lavoro", s.folders["f1"]?.name)
        assertEquals(listOf(12), s.folders["f1"]?.widgets)
        assertEquals(setOf("com.spam"), s.mutedNotifications)
        assertEquals(listOf(5, 6), s.widgetIds)
        assertEquals(ThemeMode.DARK, s.themeMode)
        assertEquals(FontChoice.SERIF, s.font)
        assertEquals(1.2f, s.iconSize, 0.0001f)
        assertEquals(3, s.launchCounts["com.a/com.a.Main#0"])
    }

    @Test
    fun roundTripKeepsEverything() {
        val original = Settings(
            favorites = listOf("x", "folder:f"),
            folders = mapOf("f" to Folder("f", "Giochi", listOf("y"), listOf(1))),
            themeMode = ThemeMode.LIGHT,
            leftHanded = true,
            swipeUp = GestureAction.ALL_APPS,
        )
        assertEquals(original, SettingsCodec.decode(SettingsCodec.encode(original)))
    }

    @Test
    fun missingFieldsTakeDefaults() {
        val s = SettingsCodec.decode("""{"schemaVersion": 2, "favorites": ["a"]}""")
        assertEquals(listOf("a"), s.favorites)
        assertEquals(Settings().haptics, s.haptics)
        assertEquals(Settings().swipeDown, s.swipeDown)
    }

    @Test
    fun homePositionIsAutomaticByDefaultAndSurvivesRoundTrip() {
        assertTrue(SettingsCodec.decode("""{"schemaVersion": 3}""").homeTopFraction < 0f)
        val chosen = Settings(homeTopFraction = 0.4f)
        assertEquals(0.4f, SettingsCodec.decode(SettingsCodec.encode(chosen)).homeTopFraction, 0.0001f)
    }

    @Test
    fun schema3DoubleTapLockBecomesAnAction() {
        assertEquals(GestureAction.LOCK_SCREEN, SettingsCodec.decode("""{"schemaVersion": 3, "doubleTapLock": true}""").doubleTapAction)
        assertEquals(GestureAction.NONE, SettingsCodec.decode("""{"schemaVersion": 3, "doubleTapLock": false}""").doubleTapAction)
    }

    @Test
    fun unknownEnumValueFallsBackToDefault() {
        val s = SettingsCodec.decode("""{"schemaVersion": 2, "themeMode": "PURPLE"}""")
        assertEquals(ThemeMode.SYSTEM, s.themeMode)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidJsonThrows() {
        SettingsCodec.decode("non è json")
    }

    @Test
    fun schemaDetection() {
        assertEquals(1, SettingsCodec.schemaOf(legacyBackup))
        assertTrue((SettingsCodec.schemaOf(SettingsCodec.encode(Settings())) ?: 0) >= 2)
    }
}
