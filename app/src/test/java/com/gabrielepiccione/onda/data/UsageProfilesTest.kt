// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class UsageProfilesTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 6) // venerdì
    private val now = LocalDateTime.of(2026, 11, 6, 12, 0).toInstant(zone).toEpochMilli()

    private fun at(daysAgo: Int, hour: Int, minute: Int = 0) =
        today.minusDays(daysAgo.toLong()).atTime(hour, minute).toInstant(zone).toEpochMilli()

    /** Ogni giorno lavorativo alle [hour] si aprono queste app, per [days] giorni indietro. */
    private fun routine(apps: List<String>, hour: Int, days: Int = 28, weekdaysOnly: Boolean = true, extra: (Int) -> UsageEventCtx? = { null }) =
        (1..days).flatMap { d ->
            val date = today.minusDays(d.toLong())
            if (weekdaysOnly && date.dayOfWeek.value > 5) emptyList()
            else apps.mapIndexed { i, pkg -> UsageEventCtx(pkg, at(d, hour, i * 5)) } + listOfNotNull(extra(d))
        }

    @Test
    fun emptyHistoryGivesNothing() {
        assertTrue(UsageProfiles.suggest(emptyList(), now, zone).isEmpty())
    }

    @Test
    fun aRoutineBecomesAProfileForThatSlot() {
        val events = routine(listOf("mail", "calendar", "teams", "notes"), hour = 9)
        val found = UsageProfiles.suggest(events, now, zone)
        val s = found.single()
        assertEquals(DayKind.WEEKDAYS, s.days)
        assertEquals(DaySlot.MORNING, s.slot)
        assertEquals(setOf("mail", "calendar", "teams", "notes"), s.apps.toSet())
        assertEquals("Mattina feriali", s.name)
    }

    @Test
    fun tooShortAHistoryStaysSilent() {
        assertTrue(UsageProfiles.suggest(routine(listOf("mail", "calendar", "teams"), 9, days = 4), now, zone).isEmpty())
    }

    @Test
    fun tenDaysOfSystemHistoryAreEnoughRightAfterTheGrant() {
        // Android conserva circa dieci giorni di aperture: il primo suggerimento non deve aspettare due settimane
        assertEquals(1, UsageProfiles.suggest(routine(listOf("mail", "calendar", "teams"), 9, days = 10), now, zone).size)
    }

    @Test
    fun notificationsAreAtMostOneAWeekAndOnlyWhenEnabled() {
        val on = ProfileSuggestState(enabled = true)
        assertTrue(UsageProfiles.shouldNotify(on, "2026-11-06"))
        val after = UsageProfiles.notified(on, "2026-11-06")
        assertFalse(UsageProfiles.shouldNotify(after, "2026-11-10"))
        assertTrue(UsageProfiles.shouldNotify(after, "2026-11-13"))
        assertFalse(UsageProfiles.shouldNotify(ProfileSuggestState(enabled = false), "2026-11-06"))
    }

    @Test
    fun fewerThanThreeAppsOrFewerThanThreeDaysIsNotEnough() {
        assertTrue(UsageProfiles.suggest(routine(listOf("mail", "calendar"), 9), now, zone).isEmpty())
        val rare = listOf(28, 2).flatMap { d -> listOf("a", "b", "c").map { UsageEventCtx(it, at(d, 9)) } }
        assertTrue(UsageProfiles.suggest(rare, now, zone).isEmpty())
    }

    @Test
    fun noiseDoesNotEnter() {
        // Una routine solida più aperture sparse di app diverse a ore casuali
        val noise = (1..28).map { d -> UsageEventCtx("random$d", at(d, (d * 7) % 24)) }
        val s = UsageProfiles.suggest(routine(listOf("mail", "calendar", "teams"), 9) + noise, now, zone).single()
        assertEquals(setOf("mail", "calendar", "teams"), s.apps.toSet())
    }

    @Test
    fun excludedAppsAreNeverSuggested() {
        val found = UsageProfiles.suggest(routine(listOf("mail", "calendar", "teams", "launcher"), 9), now, zone, exclude = setOf("launcher"))
        assertFalse("launcher" in found.single().apps)
    }

    @Test
    fun atMostSixApps() {
        val many = (1..9).map { "app$it" }
        assertEquals(6, UsageProfiles.suggest(routine(many, 9), now, zone).single().apps.size)
    }

    @Test
    fun aSignalIsFoundOnlyWhenTheContextIsRecorded() {
        val withHeadphones = routine(listOf("music", "podcasts", "audiobooks"), 18, weekdaysOnly = false).map { it.copy(headphones = true) }
        val signal = UsageProfiles.suggest(withHeadphones, now, zone).first { it.signal == SuggestedSignal.HEADPHONES }
        assertEquals(setOf("music", "podcasts", "audiobooks"), signal.apps.toSet())
        assertTrue(UsageProfiles.suggest(routine(listOf("music", "podcasts", "audiobooks"), 18, weekdaysOnly = false), now, zone).none { it.signal != null })
    }

    @Test
    fun wifiIsOnlyUsedWhenAllowed() {
        val events = routine(listOf("tv", "news", "recipes"), 21, weekdaysOnly = false).map { it.copy(wifiHash = "abc123") }
        assertTrue(UsageProfiles.suggest(events, now, zone, useWifi = false).none { it.signal == SuggestedSignal.WIFI })
        assertTrue(UsageProfiles.suggest(events, now, zone, useWifi = true).any { it.signal == SuggestedSignal.WIFI && it.wifiHash == "abc123" })
    }

    @Test
    fun theNightAfterMidnightCountsForTheDayItStarted() {
        // Venerdì notte (sab 1:00) conta come "weekdays" notte
        val e = UsageEventCtx("x", LocalDateTime.of(2026, 11, 7, 1, 0).toInstant(zone).toEpochMilli())
        val kinds = listOf(DayKind.WEEKDAYS, DayKind.WEEKEND)
        val matches = kinds.filter { UsageProfiles.belongs(e, it, DaySlot.NIGHT, zone) }
        assertEquals(listOf(DayKind.WEEKDAYS), matches)
    }

    // --- Quando proporre ---

    private val suggestion = ProfileSuggestion("Mattina feriali", listOf("mail", "calendar", "teams"), DayKind.WEEKDAYS, DaySlot.MORNING, null, 60)
    private val other = ProfileSuggestion("Sera feriali", listOf("tv", "news", "recipes"), DayKind.WEEKDAYS, DaySlot.EVENING, null, 40)

    @Test
    fun offByDefaultAndNothingWhenOff() {
        assertNull(UsageProfiles.next(ProfileSuggestState(), listOf(suggestion), "2026-11-06"))
    }

    @Test
    fun ignoreSilencesThatProposalForThirtyDays() {
        var state = ProfileSuggestState(enabled = true)
        state = UsageProfiles.ignore(state, suggestion, "2026-11-06")
        assertNull(UsageProfiles.next(state, listOf(suggestion), "2026-11-20"))
        assertNull(UsageProfiles.next(state, listOf(suggestion), "2026-12-05"))
        assertEquals(suggestion, UsageProfiles.next(state, listOf(suggestion), "2026-12-06"))
    }

    @Test
    fun atMostOneProposalPerWeek() {
        var state = ProfileSuggestState(enabled = true)
        state = UsageProfiles.shown(state, suggestion, "2026-11-06")
        // Finché non si decide, la stessa resta
        assertEquals(suggestion, UsageProfiles.next(state, listOf(suggestion, other), "2026-11-08"))
        state = UsageProfiles.accepted(state, suggestion, "2026-11-08")
        // Una nuova non prima di sette giorni dalla prima
        assertNull(UsageProfiles.next(state, listOf(suggestion, other), "2026-11-10"))
        assertEquals(other, UsageProfiles.next(state, listOf(suggestion, other), "2026-11-13"))
    }

    @Test
    fun theProfileThatComesOutHasTheConditionsAndNeverStartsByItself() {
        val profile = UsageProfiles.toProfile(suggestion, "p1")
        val time = profile.groups.single().triggers.single()
        assertEquals(TriggerType.TIME, time.type)
        assertEquals(DayKind.WEEKDAYS.days, time.days)
        assertEquals(6 * 60, time.startMinute)
        assertEquals(listOf("mail", "calendar", "teams"), profile.apps)
        // Non esiste attivazione automatica: l'uso resta quello dei profili, con le sue regole
        assertTrue(profile.enabled)
    }
}
