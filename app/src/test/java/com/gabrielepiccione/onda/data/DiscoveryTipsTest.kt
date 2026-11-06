// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryTipsTest {
    private val quiet = TipSignals(
        today = "2026-11-06", minuteOfDay = 12 * 60, daysSinceFirst = 0, oftenOpenedNotFavorite = false,
        hasNotifications = false, playerSeen = false, headphonesSeen = false, grayscaleOn = false, hasProfiles = false,
    )

    @Test
    fun nothingToSayWhenNothingHappened() {
        assertNull(DiscoveryTips.next(TipState(), quiet))
    }

    @Test
    fun eachConditionPicksItsTip() {
        assertEquals(Tip.FAVORITE, DiscoveryTips.next(TipState(), quiet.copy(oftenOpenedNotFavorite = true)))
        assertEquals(Tip.SWIPE_NOTIFICATIONS, DiscoveryTips.next(TipState(), quiet.copy(hasNotifications = true)))
        assertEquals(Tip.ALPHABET, DiscoveryTips.next(TipState(listOpens = 3), quiet))
        assertEquals(Tip.PLAYER, DiscoveryTips.next(TipState(), quiet.copy(playerSeen = true)))
        assertEquals(Tip.HEADPHONES, DiscoveryTips.next(TipState(), quiet.copy(headphonesSeen = true)))
        assertEquals(Tip.GRAYSCALE, DiscoveryTips.next(TipState(), quiet.copy(minuteOfDay = 21 * 60 + 30)))
        assertEquals(Tip.PROFILES, DiscoveryTips.next(TipState(), quiet.copy(daysSinceFirst = 7)))
    }

    @Test
    fun whatTheUserAlreadyDoesIsNotSuggested() {
        assertNull(DiscoveryTips.next(TipState(swiped = true), quiet.copy(hasNotifications = true)))
        assertNull(DiscoveryTips.next(TipState(listOpens = 9, alphabetUsed = true), quiet))
        assertNull(DiscoveryTips.next(TipState(), quiet.copy(minuteOfDay = 22 * 60, grayscaleOn = true)))
        assertNull(DiscoveryTips.next(TipState(), quiet.copy(daysSinceFirst = 30, hasProfiles = true)))
    }

    @Test
    fun oneTipPerDayAndItStaysUntilClosed() {
        val s = quiet.copy(hasNotifications = true, playerSeen = true)
        val first = DiscoveryTips.next(TipState(), s)!!
        val state = DiscoveryTips.shown(TipState(), first, s.today)
        // Nuovi segnali non cambiano il suggerimento di oggi
        assertEquals(first, DiscoveryTips.next(state, s.copy(headphonesSeen = true)))
        // Chiuso con "Non ora": oggi non ne compaiono altri
        assertNull(DiscoveryTips.next(DiscoveryTips.notNow(state, first, s.today), s))
        // Il giorno dopo ne arriva uno
        assertNotNull(DiscoveryTips.next(DiscoveryTips.notNow(state, first, s.today), s.copy(today = "2026-11-07")))
    }

    @Test
    fun neverMeansNever() {
        val s = quiet.copy(playerSeen = true)
        val state = DiscoveryTips.never(TipState(), Tip.PLAYER)
        assertNull(DiscoveryTips.next(state, s))
        assertNull(DiscoveryTips.next(state, s.copy(today = "2027-01-01")))
    }

    @Test
    fun aTipComesBackAtMostThreeTimes() {
        var state = TipState()
        val s = quiet.copy(playerSeen = true)
        for (day in 1..3) {
            state = DiscoveryTips.shown(state, Tip.PLAYER, "2026-11-0$day")
            state = DiscoveryTips.notNow(state, Tip.PLAYER, "2026-11-0$day")
        }
        assertNull(DiscoveryTips.next(state, s.copy(today = "2026-11-09")))
    }

    @Test
    fun nothingDuringAGestureOrWhenOff() {
        assertNull(DiscoveryTips.next(TipState(), quiet.copy(playerSeen = true, busy = true)))
        assertNull(DiscoveryTips.next(TipState(enabled = false), quiet.copy(playerSeen = true)))
    }

    @Test
    fun resetBringsEverythingBack() {
        val state = DiscoveryTips.reset(TipState(enabled = false, dismissed = setOf("PLAYER"), shownCount = mapOf("PLAYER" to 3)))
        assertTrue(state.enabled)
        assertEquals(Tip.PLAYER, DiscoveryTips.next(state, quiet.copy(playerSeen = true)))
    }

    @Test
    fun daysBetweenDates() {
        assertEquals(7, DiscoveryTips.daysBetween("2026-11-01", "2026-11-08"))
        assertEquals(0, DiscoveryTips.daysBetween("", "2026-11-08"))
        assertFalse(DiscoveryTips.daysBetween("boh", "2026-11-08") != 0)
    }

    private fun assertNotNull(value: Any?) = assertTrue(value != null)
}
