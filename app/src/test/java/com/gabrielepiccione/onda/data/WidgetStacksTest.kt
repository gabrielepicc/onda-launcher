// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStacksTest {

    private val base = Settings(
        widgetIds = listOf(1, 2),
        widgetHeights = mapOf(1 to 200),
        homeLayout = listOf(HomeBlocks.CLOCK, HomeBlocks.widget(1), HomeBlocks.widget(2), HomeBlocks.MEDIA, HomeBlocks.APPS),
    )

    @Test
    fun stackingAWidgetKeepsItsPositionAndHeight() {
        val s = WidgetStacks.add(base, HomeBlocks.widget(1), 9, "x")
        assertEquals(listOf(2), s.widgetIds)
        assertEquals(listOf(WidgetStack("x", listOf(1, 9), 200)), s.widgetStacks)
        assertEquals(HomeBlocks.stack("x"), HomeBlocks.resolve(s)[1])
        assertTrue(1 !in s.widgetHeights)
    }

    @Test
    fun addingToAnExistingStackAppends() {
        val s = WidgetStacks.add(WidgetStacks.add(base, HomeBlocks.widget(1), 9, "x"), HomeBlocks.stack("x"), 10, "ignored")
        assertEquals(listOf(1, 9, 10), s.widgetStacks.single().widgets)
    }

    @Test
    fun removingDownToOneWidgetTurnsTheStackBackIntoAWidget() {
        val stacked = WidgetStacks.add(base, HomeBlocks.widget(1), 9, "x")
        val s = WidgetStacks.remove(stacked, 1)
        assertTrue(s.widgetStacks.isEmpty())
        assertEquals(listOf(2, 9), s.widgetIds)
        assertEquals(HomeBlocks.widget(9), HomeBlocks.resolve(s)[1])
        assertEquals(200, s.widgetHeights[9])
    }

    @Test
    fun removingAnUnstackedWidgetChangesNothing() {
        assertEquals(base, WidgetStacks.remove(base, 1))
    }

    @Test
    fun replacingKeepsTheOrderInTheStack() {
        val stacked = WidgetStacks.add(base, HomeBlocks.widget(1), 9, "x")
        assertEquals(listOf(5, 9), WidgetStacks.replace(stacked, 1, 5).widgetStacks.single().widgets)
    }

    @Test
    fun stacksSurviveSerialization() {
        val s = WidgetStacks.add(base, HomeBlocks.widget(1), 9, "x")
        assertEquals(s.widgetStacks, SettingsCodec.decode(SettingsCodec.encode(s)).widgetStacks)
    }
}
