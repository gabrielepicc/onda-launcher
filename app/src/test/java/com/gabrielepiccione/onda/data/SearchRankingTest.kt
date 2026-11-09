// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRankingTest {
    private val now = 100L * SearchRanking.HALF_LIFE_MS

    @Test fun recordCountsAndDates() {
        val h = SearchRanking.record(SearchRanking.record(emptyMap(), "a", 1), "a", 5)
        assertEquals(SearchStat(2, 5), h["a"])
    }

    @Test fun scoreHalvesAfterHalfLife() {
        assertEquals(2.0, SearchRanking.score(SearchStat(4, now - SearchRanking.HALF_LIFE_MS), now), 1e-9)
    }

    @Test fun minimumAndExclusions() {
        val h = mapOf("a" to SearchStat(1, now), "b" to SearchStat(3, now), "c" to SearchStat(5, now))
        assertEquals(listOf("b"), SearchRanking.top(h, now, 4, excluded = setOf("c")))
    }

    @Test fun decayAndTieOnRecency() {
        val h = mapOf(
            "old" to SearchStat(10, now - 5 * SearchRanking.HALF_LIFE_MS),
            "x" to SearchStat(3, now - 10),
            "y" to SearchStat(3, now),
        )
        assertEquals(listOf("y", "x", "old"), SearchRanking.top(h, now, 4))
        assertEquals(listOf("y"), SearchRanking.top(h, now, 1))
        assertTrue(SearchRanking.top(h, now, 0).isEmpty())
    }

    @Test fun fallbackToMostUsed() {
        val s = SearchRanking.section(listOf("a"), listOf("m1", "m2", "m3"), 2)
        assertFalse(s.frequentSearches)
        assertEquals(listOf("m1", "m2"), s.packages)
        assertTrue(SearchRanking.section(listOf("a", "b"), emptyList(), 4).frequentSearches)
        assertTrue(SearchRanking.section(listOf("a", "b"), listOf("m"), 0).packages.isEmpty())
    }
}

class SearchHistoryCodecTest {
    @Test fun roundTripAndBadLines() {
        val h = mapOf("com.a" to SearchStat(3, 42L), "com.b" to SearchStat(1, 7L))
        assertEquals(h, SearchHistoryStore.decode(SearchHistoryStore.encode(h) + "\nrotta\n\tx\t1"))
        assertTrue(SearchHistoryStore.decode("").isEmpty())
    }
}

class SearchRankingPickTest {
    @Test fun skipsInvisibleAndProtected() {
        val now = 1_000L
        val h = mapOf("a" to SearchStat(3, now), "gone" to SearchStat(9, now), "p" to SearchStat(9, now), "b" to SearchStat(2, now))
        val (apps, frequent) = SearchRanking.pick(h, now, 4, listOf("a", "b", "p", "c"), listOf("c", "p"), setOf("p")) { it }
        assertTrue(frequent)
        assertEquals(listOf("a", "b"), apps)
        val (fallback, f2) = SearchRanking.pick(mapOf("a" to SearchStat(3, now)), now, 4, listOf("a", "c", "p"), listOf("c", "p"), setOf("p")) { it }
        assertFalse(f2)
        assertEquals(listOf("c"), fallback)
    }
}
