// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeBlocksTest {

    @Test
    fun defaultOrderPutsAppsLast() {
        val s = Settings(widgetIds = listOf(4))
        assertEquals(listOf("clock", "media", "w:4", "apps"), HomeBlocks.resolve(s))
    }

    @Test
    fun stacksAreBlocksAndEmptyStacksAreDropped() {
        val s = Settings(
            widgetStacks = mapOf(
                "a" to WidgetStack("a", listOf(1, 2)),
                "b" to WidgetStack("b", emptyList()),
            ),
            homeLayout = listOf("apps", "s:a", "s:b", "clock"),
        )
        // I blocchi mancanti (media) finiscono prima delle app
        assertEquals(listOf("media", "apps", "s:a", "clock"), HomeBlocks.resolve(s))
        assertEquals("a", HomeBlocks.stackId("s:a"))
        assertNull(HomeBlocks.stackId("w:3"))
        assertNull(HomeBlocks.widgetId("s:a"))
    }

    @Test
    fun stacksSurviveSerialization() {
        val s = Settings(widgetStacks = mapOf("a" to WidgetStack("a", listOf(1, 2), heightDp = 180)))
        assertEquals(s.widgetStacks, SettingsCodec.decode(SettingsCodec.encode(s)).widgetStacks)
    }
}
