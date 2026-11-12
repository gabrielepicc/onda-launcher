// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetOrderTest {
    private val stackA = WidgetGroupRef.Stack("a")
    private val stackB = WidgetGroupRef.Stack("b")
    private val folder = WidgetGroupRef.Folder("f")

    private val base = Settings(
        widgetIds = listOf(9),
        widgetStacks = listOf(WidgetStack("a", listOf(1, 2, 3)), WidgetStack("b", listOf(4, 5))),
        folders = mapOf("f" to Folder("f", "Lavoro", listOf("com.x"), widgets = listOf(7, 8))),
        homeLayout = listOf(HomeBlocks.CLOCK, HomeBlocks.stack("a"), HomeBlocks.stack("b"), HomeBlocks.widget(9), HomeBlocks.APPS),
    )

    @Test
    fun reorderMovesOneEntry() {
        assertEquals(listOf(2, 3, 1), WidgetOrder.reorder(listOf(1, 2, 3), 0, 2))
        assertEquals(listOf(3, 1, 2), WidgetOrder.reorder(listOf(1, 2, 3), 2, 0))
        // Fuori dai limiti o stessa posizione: invariato
        assertEquals(listOf(1, 2, 3), WidgetOrder.reorder(listOf(1, 2, 3), 0, 5))
        assertEquals(listOf(1, 2, 3), WidgetOrder.reorder(listOf(1, 2, 3), 1, 1))
    }

    @Test
    fun moveUpAndDownInStacksAndFolders() {
        assertEquals(listOf(1, 3, 2), WidgetOrder.widgetsOf(WidgetOrder.moveBy(base, stackA, 2, 1), stackA))
        assertEquals(listOf(2, 1, 3), WidgetOrder.widgetsOf(WidgetOrder.moveBy(base, stackA, 2, -1), stackA))
        // In cima non si può salire
        assertEquals(listOf(1, 2, 3), WidgetOrder.widgetsOf(WidgetOrder.moveBy(base, stackA, 1, -1), stackA))
        assertEquals(listOf(8, 7), WidgetOrder.widgetsOf(WidgetOrder.moveBy(base, folder, 7, 1), folder))
        // Gli altri gruppi non cambiano
        assertEquals(listOf(4, 5), WidgetOrder.widgetsOf(WidgetOrder.moveBy(base, stackA, 2, 1), stackB))
    }

    @Test
    fun dragTargetFollowsTheRowsCrossed() {
        assertEquals(1, WidgetOrder.targetIndex(0, 70f, 64f, 3))
        assertEquals(2, WidgetOrder.targetIndex(0, 500f, 64f, 3))
        assertEquals(0, WidgetOrder.targetIndex(1, -200f, 64f, 3))
        // Mezza riga non basta
        assertEquals(1, WidgetOrder.targetIndex(1, 20f, 64f, 3))
        assertEquals(1, WidgetOrder.targetIndex(1, 10f, 0f, 3))
    }

    @Test
    fun moveBetweenStacksAndFolders() {
        // Da un carosello a un altro
        val toB = WidgetOrder.moveToGroup(base, stackA, stackB, 2)
        assertEquals(listOf(1, 3), WidgetOrder.widgetsOf(toB, stackA))
        assertEquals(listOf(4, 5, 2), WidgetOrder.widgetsOf(toB, stackB))
        // Da un carosello a una cartella e viceversa
        val toFolder = WidgetOrder.moveToGroup(base, stackA, folder, 1)
        assertEquals(listOf(7, 8, 1), WidgetOrder.widgetsOf(toFolder, folder))
        val back = WidgetOrder.moveToGroup(toFolder, folder, stackB, 7)
        assertEquals(listOf(8, 1), WidgetOrder.widgetsOf(back, folder))
        assertEquals(listOf(4, 5, 7), WidgetOrder.widgetsOf(back, stackB))
        // Una cartella verso un'altra non c'è qui; stesso gruppo o widget non presente: invariato
        assertEquals(base, WidgetOrder.moveToGroup(base, stackA, stackA, 1))
        assertEquals(base, WidgetOrder.moveToGroup(base, stackA, stackB, 99))
        assertEquals(base, WidgetOrder.moveToGroup(base, stackA, WidgetGroupRef.Stack("zzz"), 1))
    }

    @Test
    fun aStackLeftWithOneWidgetBecomesASingleWidget() {
        val s = WidgetOrder.moveToGroup(base, stackB, folder, 4)
        // «b» aveva due widget: ne resta uno, che torna widget singolo nello stesso punto della home
        assertTrue(s.widgetStacks.none { it.id == "b" })
        assertTrue(5 in s.widgetIds)
        assertTrue(HomeBlocks.widget(5) in HomeBlocks.resolve(s))
        assertEquals(listOf(7, 8, 4), WidgetOrder.widgetsOf(s, folder))
    }

    @Test
    fun targetsAreTheOtherGroups() {
        assertEquals(listOf<WidgetGroupRef>(stackB, folder), WidgetOrder.targets(base, stackA))
        assertEquals(listOf<WidgetGroupRef>(stackA, stackB), WidgetOrder.targets(base, folder))
    }
}
