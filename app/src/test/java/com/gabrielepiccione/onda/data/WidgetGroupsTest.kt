// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetGroupsTest {
    private val base = Settings(
        widgetIds = listOf(1, 2, 3),
        homeLayout = listOf(HomeBlocks.CLOCK, HomeBlocks.widget(1), HomeBlocks.widget(2), HomeBlocks.widget(3), HomeBlocks.APPS),
    )

    @Test
    fun mergeTwoWidgetsIntoACarousel() {
        val s = WidgetGroups.mergeWithNext(base, HomeBlocks.widget(1), "g")
        val g = s.widgetStacks.single()
        assertEquals(listOf(1, 2), g.widgets)
        assertEquals(StackLayout.CAROUSEL, g.layout)
        assertEquals(listOf(3), s.widgetIds)
        // Il gruppo è una sola voce della home, al posto del primo widget
        assertEquals(listOf(HomeBlocks.CLOCK, HomeBlocks.stack("g"), HomeBlocks.widget(3), HomeBlocks.APPS), HomeBlocks.resolve(s).filter { it != HomeBlocks.MEDIA })
    }

    @Test
    fun groupAbsorbsTheNextWidgetAndKeepsItsLayout() {
        val rows = WidgetGroups.setLayout(WidgetGroups.mergeWithNext(base, HomeBlocks.widget(1), "g"), "g", StackLayout.ROWS)
        val s = WidgetGroups.mergeWithNext(rows, HomeBlocks.stack("g"), "x")
        assertEquals(listOf(1, 2, 3), s.widgetStacks.single().widgets)
        assertEquals(StackLayout.ROWS, s.widgetStacks.single().layout)
        assertTrue(s.widgetIds.isEmpty())
    }

    @Test
    fun splitGivesBackSeparateRowsInPlace() {
        val grouped = WidgetGroups.mergeWithNext(base, HomeBlocks.widget(1), "g").let { WidgetStacks.setHeight(it, "g", 200) }
        val s = WidgetGroups.split(grouped, "g")
        assertTrue(s.widgetStacks.isEmpty())
        assertEquals(setOf(1, 2, 3), s.widgetIds.toSet())
        assertEquals(listOf(HomeBlocks.widget(1), HomeBlocks.widget(2), HomeBlocks.widget(3)), HomeBlocks.resolve(s).filter { HomeBlocks.widgetId(it) != null })
        assertEquals(200, s.widgetHeights[1])
    }

    @Test
    fun detachMovesAWidgetBetweenGroups() {
        val all = WidgetGroups.mergeWithNext(WidgetGroups.mergeWithNext(base, HomeBlocks.widget(1), "g"), HomeBlocks.stack("g"), "x")
        val s = WidgetGroups.detach(all, 2)
        assertEquals(listOf(1, 3), s.widgetStacks.single().widgets)
        val blocks = HomeBlocks.resolve(s)
        assertEquals(blocks.indexOf(HomeBlocks.stack("g")) + 1, blocks.indexOf(HomeBlocks.widget(2)))
        // Con due widget, staccarne uno scioglie il gruppo
        assertTrue(WidgetGroups.detach(s, 1).widgetStacks.isEmpty())
        assertEquals(listOf(3, 1), WidgetGroups.moveInside(s, 3, -1).widgetStacks.single().widgets)
    }

    @Test
    fun oldStacksStayCarousels() {
        val old = """{"id":"p","widgets":[4,5],"heightDp":180}"""
        val stack = Json { ignoreUnknownKeys = true }.decodeFromString(WidgetStack.serializer(), old)
        assertEquals(StackLayout.CAROUSEL, stack.layout)
        assertEquals(listOf(4, 5), stack.widgets)
    }
}
