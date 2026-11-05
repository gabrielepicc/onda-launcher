// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentInstallsTest {

    private val apps = listOf("Alfa" to 300L, "Beta" to 0L, "Gamma" to 500L, "Delta" to 300L, "Epsilon" to 100L)

    @Test
    fun newestFirstWithoutUndatedApps() {
        assertEquals(listOf("Gamma", "Alfa", "Delta", "Epsilon"), RecentInstalls.pick(apps) { it.second }.map { it.first })
    }

    @Test
    fun respectsLimit() {
        assertEquals(listOf("Gamma", "Alfa"), RecentInstalls.pick(apps, limit = 2) { it.second }.map { it.first })
    }

    @Test
    fun emptyWhenNoDates() {
        assertEquals(emptyList<Pair<String, Long>>(), RecentInstalls.pick(listOf("A" to 0L)) { it.second })
    }
}
