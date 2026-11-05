// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class ContextProfilesTest {
    // Lunedì 2 novembre 2026, 9:30
    private val monday930 = LocalDateTime.of(2026, 11, 2, 9, 30)
    private fun snap(
        time: LocalDateTime = monday930,
        wifi: String? = null,
        bt: Set<String> = emptySet(),
        calendar: List<String> = emptyList(),
        charging: Boolean = false,
    ) = ContextSnapshot(time, wifi = wifi, bluetooth = bt, calendarNow = calendar, charging = charging)

    private val work = Trigger(TriggerType.TIME, days = setOf(1, 2, 3, 4, 5), startMinute = 9 * 60, endMinute = 18 * 60)
    private val car = Trigger(TriggerType.BLUETOOTH, text = "Auto")
    private fun profile(id: String, priority: Int, vararg groups: List<Trigger>) =
        ContextProfile(id, id, priority, groups.map { TriggerGroup(it) }, apps = listOf("$id.app"))

    @Test
    fun timeRangeWithDaysAndOvernight() {
        assertTrue(ContextProfiles.inTimeRange(work, monday930))
        assertFalse(ContextProfiles.inTimeRange(work, monday930.withHour(19)))
        assertFalse(ContextProfiles.inTimeRange(work, LocalDateTime.of(2026, 11, 1, 10, 0)))
        val night = Trigger(TriggerType.TIME, days = setOf(5), startMinute = 22 * 60, endMinute = 6 * 60)
        // Venerdì 23:00 e la notte successiva (sabato 2:00) sì; sabato 23:00 no
        assertTrue(ContextProfiles.inTimeRange(night, LocalDateTime.of(2026, 11, 6, 23, 0)))
        assertTrue(ContextProfiles.inTimeRange(night, LocalDateTime.of(2026, 11, 7, 2, 0)))
        assertFalse(ContextProfiles.inTimeRange(night, LocalDateTime.of(2026, 11, 7, 23, 0)))
    }

    @Test
    fun andInsideGroupsOrBetweenGroups() {
        val p = profile("drive", 1, listOf(car, work), listOf(Trigger(TriggerType.CHARGING)))
        assertTrue(ContextProfiles.matches(p, snap(bt = setOf("Auto"))))
        assertFalse(ContextProfiles.matches(p, snap(bt = setOf("Auto"), time = monday930.withHour(20))))
        assertTrue(ContextProfiles.matches(p, snap(time = monday930.withHour(20), charging = true)))
    }

    @Test
    fun negatedConditions() {
        val notHome = Trigger(TriggerType.WIFI, text = "Casa", negate = true)
        assertTrue(ContextProfiles.matches(notHome, snap(wifi = "Ufficio")))
        assertFalse(ContextProfiles.matches(notHome, snap(wifi = "casa")))
    }

    @Test
    fun highestPriorityWinsAndTiesGoToTheFirst() {
        val office = profile("office", 1, listOf(work))
        val driving = profile("driving", 5, listOf(car))
        val other = profile("other", 1, listOf(work))
        val s = snap(bt = setOf("Auto"))
        assertEquals("driving", ContextProfiles.active(listOf(office, driving, other), s)?.id)
        assertEquals("office", ContextProfiles.active(listOf(office, other), s)?.id)
    }

    @Test
    fun aProfileWithoutConditionsNeverStarts() {
        assertNull(ContextProfiles.active(listOf(ContextProfile("empty", "empty")), snap()))
        assertNull(ContextProfiles.active(listOf(profile("off", 1, listOf(work)).copy(enabled = false)), snap()))
    }

    @Test
    fun manualOverrideLastsAnHour() {
        val office = profile("office", 1, listOf(work))
        val home = profile("home", 1, listOf(Trigger(TriggerType.WIFI, text = "Casa")))
        val override = ContextProfiles.nextOverride(listOf(office, home), "office", now = 1_000L)
        assertEquals("home", override.profileId)
        assertEquals(1_000L + ContextProfiles.OVERRIDE_MS, override.until)
        assertEquals("home", ContextProfiles.resolve(listOf(office, home), snap(), override, now = 2_000L)?.id)
        assertEquals("office", ContextProfiles.resolve(listOf(office, home), snap(), override, now = override.until + 1)?.id)
        // Dopo l'ultimo profilo si torna a "nessuno", che vince anche su un profilo vero
        val none = ContextProfiles.nextOverride(listOf(office, home), "home", now = 1_000L)
        assertEquals("", none.profileId)
        assertNull(ContextProfiles.resolve(listOf(office, home), snap(), none, now = 2_000L))
    }

    @Test
    fun fixedSlotsNeverMove() {
        val base = listOf("phone", "messages", "maps", "music")
        val p = ContextProfile("work", "work", apps = listOf("teams", "phone", "notes"))
        assertEquals(listOf("phone", "messages", "teams", "notes"), ContextProfiles.favorites(base, 2, p))
        assertEquals(base, ContextProfiles.favorites(base, 2, null))
    }

    @Test
    fun calendarMatchesTeamsLinksOrAnyEvent() {
        val teams = Trigger(TriggerType.CALENDAR, text = ContextProfiles.TEAMS_LINK)
        val any = Trigger(TriggerType.CALENDAR)
        val meeting = snap(calendar = listOf("Stand-up https://teams.microsoft.com/l/meetup-join/xyz"))
        assertTrue(ContextProfiles.matches(teams, meeting))
        assertFalse(ContextProfiles.matches(teams, snap(calendar = listOf("Dentista"))))
        assertTrue(ContextProfiles.matches(any, snap(calendar = listOf("Dentista"))))
        assertFalse(ContextProfiles.matches(any, snap()))
    }

    @Test
    fun placeWithinRadius() {
        val office = Trigger(TriggerType.PLACE, latitude = 45.4642, longitude = 9.19, radiusMeters = 300)
        assertTrue(ContextProfiles.matches(office, snap().copy(location = 45.4655 to 9.19)))
        assertFalse(ContextProfiles.matches(office, snap().copy(location = 45.48 to 9.19)))
        assertFalse(ContextProfiles.matches(office, snap()))
    }

    @Test
    fun suggestsOnlyRepeatedHabits() {
        val zone = ZoneOffset.UTC
        fun monday9(weeksAgo: Long) = LocalDateTime.of(2026, 11, 2, 9, 5).minusWeeks(weeksAgo).toInstant(zone).toEpochMilli()
        val now = LocalDateTime.of(2026, 11, 2, 12, 0).toInstant(zone).toEpochMilli()
        val records = listOf(0L, 1L, 2L).map { LaunchRecord("mail", monday9(it)) } +
            listOf(0L, 1L).map { LaunchRecord("news", monday9(it)) }
        val found = ContextProfiles.suggest(records, now, zone, emptyList(), emptySet())
        assertEquals(listOf("mail"), found.map { it.appKey })
        assertEquals(1, found.single().dayOfWeek)
        assertEquals(9, found.single().hour)
        assertTrue(ContextProfiles.suggest(records, now, zone, emptyList(), setOf(found.single().id)).isEmpty())
        val created = ContextProfiles.fromSuggestion(found.single(), "Lunedì 9", "p1")
        assertTrue(ContextProfiles.suggest(records, now, zone, listOf(created), emptySet()).isEmpty())
    }
}
