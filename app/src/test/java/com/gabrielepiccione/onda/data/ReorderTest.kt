// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ReorderTest {

    @Test
    fun targetFollowsTheFingerAndStaysInTheList() {
        assertEquals(3, Reorder.target(1, 200f, 100f, 5))
        assertEquals(1, Reorder.target(1, 40f, 100f, 5))
        assertEquals(4, Reorder.target(1, 900f, 100f, 5))
        assertEquals(0, Reorder.target(1, -900f, 100f, 5))
    }

    @Test
    fun rowsMakeRoomForTheDraggedOne() {
        assertEquals(listOf(0, 0, -1, -1, 0), (0..4).map { Reorder.shift(it, 1, 3) })
        assertEquals(listOf(0, 1, 1, 0, 0), (0..4).map { Reorder.shift(it, 3, 1) })
        assertEquals(listOf(0, 0, 0), (0..2).map { Reorder.shift(it, -1, -1) })
    }

    @Test
    fun movedKeepsEveryElement() {
        assertEquals(listOf("b", "c", "a"), Reorder.moved(listOf("a", "b", "c"), 0, 2))
        assertEquals(listOf("c", "a", "b"), Reorder.moved(listOf("a", "b", "c"), 2, 0))
        assertEquals(listOf("a"), Reorder.moved(listOf("a"), 3, 0))
    }
}
