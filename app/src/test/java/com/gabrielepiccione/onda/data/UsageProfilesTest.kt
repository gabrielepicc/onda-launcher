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
        assertNull(UsageProfiles.next(ProfileSuggestState(), listOf(suggestion), "2026-11-06", NOW))
    }

    @Test
    fun ignoreSilencesThatProposalForThirtyDays() {
        var state = ProfileSuggestState(enabled = true)
        state = UsageProfiles.ignore(state, suggestion, "2026-11-06")
        assertNull(UsageProfiles.next(state, listOf(suggestion), "2026-11-20", NOW))
        assertNull(UsageProfiles.next(state, listOf(suggestion), "2026-12-05", NOW))
        assertEquals(suggestion, UsageProfiles.next(state, listOf(suggestion), "2026-12-06", NOW))
    }

    @Test
    fun theStrongestComesFirstAndStaysUntilADecision() {
        var state = ProfileSuggestState(enabled = true)
        // Più suggerimenti pronti: la home ne mostra uno, il più forte
        assertEquals(suggestion, UsageProfiles.next(state, listOf(other, suggestion), DAY, NOW))
        state = UsageProfiles.shown(state, suggestion, NOW)
        // Finché non si decide resta la stessa, anche dopo ore
        assertEquals(suggestion, UsageProfiles.next(state, listOf(suggestion, other), DAY, NOW + 10 * HOUR))
    }

    @Test
    fun aNewOneWaitsFourHoursFromTheLastShown() {
        var state = UsageProfiles.shown(ProfileSuggestState(enabled = true), suggestion, NOW)
        state = UsageProfiles.accepted(state, suggestion, DAY)
        // Intervallo non trascorso: nessuna nuova scheda
        assertNull(UsageProfiles.next(state, listOf(suggestion, other), DAY, NOW + 3 * HOUR))
        // Trascorso: arriva la successiva
        assertEquals(other, UsageProfiles.next(state, listOf(suggestion, other), DAY, NOW + 4 * HOUR))
        assertEquals(4 * HOUR, UsageProfiles.SUGGESTION_MIN_GAP_MS)
    }

    @Test
    fun afterIgnoreTheNextComesAtOnceIfTheGapHasPassed() {
        var state = UsageProfiles.shown(ProfileSuggestState(enabled = true), suggestion, NOW)
        // L'utente decide dopo 5 ore: l'intervallo è già trascorso, la successiva compare subito
        state = UsageProfiles.ignore(state, suggestion, DAY)
        assertEquals(other, UsageProfiles.next(state, listOf(suggestion, other), DAY, NOW + 5 * HOUR))
        // Quella ignorata non torna per 30 giorni
        assertEquals(listOf(other), UsageProfiles.forSettings(state, listOf(suggestion, other), DAY))
    }

    @Test
    fun settingsShowEverythingNotIgnoredWithoutLimits() {
        val third = ProfileSuggestion("Weekend", listOf("a", "b", "c"), DayKind.WEEKEND, DaySlot.MORNING, null, 80)
        val state = UsageProfiles.shown(ProfileSuggestState(enabled = true), suggestion, NOW)
        assertEquals(listOf(third, suggestion, other), UsageProfiles.forSettings(state, listOf(suggestion, other, third), DAY))
    }

    @Test
    fun ignoredCanBeListedDescribedAndRestored() {
        var state = UsageProfiles.ignore(ProfileSuggestState(enabled = true), suggestion, "2026-11-06")
        state = UsageProfiles.ignore(state, other, "2026-11-07")
        val ignored = UsageProfiles.ignoredKeys(state, "2026-11-08")
        assertEquals(listOf(other.key, suggestion.key), ignored.map { it.first })
        val parts = UsageProfiles.describe(suggestion.key)
        assertEquals(DayKind.WEEKDAYS, parts.days)
        assertEquals(DaySlot.MORNING, parts.slot)
        assertEquals(setOf("mail", "calendar", "teams"), parts.apps.toSet())
        state = UsageProfiles.restore(state, suggestion.key)
        assertEquals(suggestion, UsageProfiles.next(state, listOf(suggestion), "2026-11-08", NOW))
        // Dopo 30 giorni non è più tra gli ignorati
        assertTrue(UsageProfiles.ignoredKeys(state, "2026-12-08").isEmpty())
    }

    @Test
    fun analyzeNowAtMostEveryFifteenMinutes() {
        val state = UsageProfiles.analyzed(ProfileSuggestState(enabled = true), NOW)
        assertFalse(UsageProfiles.canAnalyzeNow(state, NOW + 14 * 60_000L))
        assertTrue(UsageProfiles.canAnalyzeNow(state, NOW + 15 * 60_000L))
        assertTrue(UsageProfiles.canAnalyzeNow(ProfileSuggestState(), NOW))
    }

    @Test
    fun theWhyCountsDaysTogetherOverTheHistory() {
        val s = UsageProfiles.suggest(routine(listOf("mail", "calendar", "teams"), 9, days = 14), now, zone).single()
        // Giorni feriali negli ultimi 14: tutti con le tre app insieme
        assertEquals(10, s.togetherDays)
        assertEquals(14, s.historyDays)
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

    @Test
    fun appsUsedEverywhereDoNotDefineASlot() {
        // Mattina: tre app della routine più "chat", che si apre anche a ogni altra ora; la routine serale è un'altra
        val chat = (1..28).flatMap { d -> listOf(8, 13, 17, 20).map { h -> UsageEventCtx("chat", at(d, h, 30)) } }
        val morning = routine(listOf("news", "mail", "calendar"), 9, weekdaysOnly = false)
        val evening = routine(listOf("tv", "video", "recipes"), 20, weekdaysOnly = false)
        val morningProfile = UsageProfiles.suggest(morning + evening + chat, now, zone).first { it.slot == DaySlot.MORNING }
        assertFalse("chat" in morningProfile.apps)
        assertEquals(setOf("news", "mail", "calendar"), morningProfile.apps.toSet())
    }

    @Test
    fun concentrationComparesTheSlotWithTheRestOfTheDay() {
        val inSlot = listOf(UsageEventCtx("a", at(1, 9)), UsageEventCtx("a", at(2, 9)))
        val all = inSlot + listOf(UsageEventCtx("a", at(3, 20)), UsageEventCtx("b", at(3, 21)), UsageEventCtx("b", at(4, 21)), UsageEventCtx("b", at(5, 21)))
        // Dei 6 eventi 2 sono nella fascia (un terzo), e 2 aperture di "a" su 3 cadono lì: 0,67 / 0,33 = 2
        assertEquals(2.0, UsageProfiles.concentration("a", inSlot, all), 0.001)
    }

    @Test
    fun diagnosisExplainsWhatOndaSaw() {
        val none = UsageProfiles.diagnose(emptyList(), now, zone)
        assertEquals(0, none.openings)
        assertEquals(0, none.candidates)
        val d = UsageProfiles.diagnose(routine(listOf("mail", "calendar", "teams"), 9, days = 10), now, zone)
        assertTrue(d.openings > 0)
        assertEquals(10, d.historyDays)
        assertEquals(1, d.candidates)
    }

    private companion object {
        const val HOUR = 3_600_000L
        const val NOW = 1_800_000_000_000L
        const val DAY = "2026-11-06"
    }
}
