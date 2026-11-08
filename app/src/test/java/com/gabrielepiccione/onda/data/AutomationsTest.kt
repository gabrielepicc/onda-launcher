// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationsTest {
    private fun env(
        atHome: Boolean = false,
        headphones: Boolean = false,
        charging: Boolean = false,
        devices: Set<String> = emptySet(),
        minute: Int = 600,
        day: Int = 2,
        sunrise: Int? = 420,
        sunset: Int? = 1080,
    ) = EnvSnapshot(atHome, headphones, charging, devices, minute, day, sunrise, sunset)

    private fun auto(trigger: AutomationTrigger, device: String = "", minute: Int = 1380, days: Set<Int> = emptySet(), enabled: Boolean = true) =
        Automation("a", "Prova", trigger, enabled, device, minute, days, actions = listOf("FLASHLIGHT"))

    private fun ids(list: List<Automation>) = list.map { it.trigger }

    @Test
    fun nothingFiresOnTheFirstSnapshot() {
        assertTrue(Automations.fired(listOf(auto(AutomationTrigger.HOME_ARRIVE)), null, env(atHome = true)).isEmpty())
    }

    @Test
    fun arrivingAndLeavingHaveToChangeState() {
        val arrive = auto(AutomationTrigger.HOME_ARRIVE)
        val leave = auto(AutomationTrigger.HOME_LEAVE)
        assertEquals(listOf(AutomationTrigger.HOME_ARRIVE), ids(Automations.fired(listOf(arrive, leave), env(), env(atHome = true))))
        assertEquals(listOf(AutomationTrigger.HOME_LEAVE), ids(Automations.fired(listOf(arrive, leave), env(atHome = true), env())))
        assertTrue(Automations.fired(listOf(arrive, leave), env(atHome = true), env(atHome = true)).isEmpty())
    }

    @Test
    fun headphonesAndChargingFireOnTheEdge() {
        val list = listOf(auto(AutomationTrigger.HEADPHONES_ON), auto(AutomationTrigger.HEADPHONES_OFF), auto(AutomationTrigger.CHARGING_ON), auto(AutomationTrigger.CHARGING_OFF))
        assertEquals(
            listOf(AutomationTrigger.HEADPHONES_ON, AutomationTrigger.CHARGING_ON),
            ids(Automations.fired(list, env(), env(headphones = true, charging = true))),
        )
        assertEquals(
            listOf(AutomationTrigger.HEADPHONES_OFF, AutomationTrigger.CHARGING_OFF),
            ids(Automations.fired(list, env(headphones = true, charging = true), env())),
        )
    }

    @Test
    fun aNamedDeviceFiresOnlyWhenThatOneConnects() {
        val car = auto(AutomationTrigger.DEVICE_ON, device = "Auto di Gabriele")
        assertTrue(Automations.fired(listOf(car), env(), env(devices = setOf("Cuffie"))).isEmpty())
        assertEquals(1, Automations.fired(listOf(car), env(), env(devices = setOf("auto di gabriele"))).size)
        // Già collegato prima: nessuna novità
        assertTrue(Automations.fired(listOf(car), env(devices = setOf("Auto di Gabriele")), env(devices = setOf("Auto di Gabriele", "Cuffie"))).isEmpty())
    }

    @Test
    fun anyDeviceAndDisconnection() {
        assertEquals(1, Automations.fired(listOf(auto(AutomationTrigger.DEVICE_ON)), env(), env(devices = setOf("X"))).size)
        assertEquals(1, Automations.fired(listOf(auto(AutomationTrigger.DEVICE_OFF, device = "X")), env(devices = setOf("X")), env()).size)
    }

    @Test
    fun timeFiresOnceWhenTheClockCrossesIt() {
        val night = auto(AutomationTrigger.TIME, minute = 23 * 60)
        assertEquals(1, Automations.fired(listOf(night), env(minute = 22 * 60 + 59), env(minute = 23 * 60)).size)
        assertTrue(Automations.fired(listOf(night), env(minute = 23 * 60), env(minute = 23 * 60 + 1)).isEmpty())
        assertTrue(Automations.fired(listOf(night), env(minute = 22 * 60), env(minute = 22 * 60 + 1)).isEmpty())
    }

    @Test
    fun timeCrossingWorksAcrossMidnightAndLongGaps() {
        assertTrue(Automations.crossed(23 * 60 + 50, 5, 0))
        assertTrue(Automations.crossed(23 * 60 + 50, 5, 3))
        assertFalse(Automations.crossed(23 * 60 + 50, 5, 12 * 60))
        // Il telefono è rimasto addormentato per ore: l'orario saltato parte comunque
        assertTrue(Automations.crossed(6 * 60, 9 * 60, 7 * 60))
    }

    @Test
    fun daysRestrictTimedTriggers() {
        val weekdays = auto(AutomationTrigger.TIME, minute = 420, days = setOf(1, 2, 3, 4, 5))
        assertEquals(1, Automations.fired(listOf(weekdays), env(minute = 419, day = 3), env(minute = 420, day = 3)).size)
        assertTrue(Automations.fired(listOf(weekdays), env(minute = 419, day = 6), env(minute = 420, day = 6)).isEmpty())
    }

    @Test
    fun sunTriggersFollowTheSunTimes() {
        val sunset = auto(AutomationTrigger.SUNSET)
        assertEquals(1, Automations.fired(listOf(sunset), env(minute = 1079), env(minute = 1080)).size)
        // Senza posizione di casa il sole non si conosce: non parte
        assertTrue(Automations.fired(listOf(sunset), env(minute = 1079, sunset = null), env(minute = 1080, sunset = null)).isEmpty())
    }

    @Test
    fun disabledOrEmptyAutomationsNeverFire() {
        assertTrue(Automations.fired(listOf(auto(AutomationTrigger.HOME_ARRIVE, enabled = false)), env(), env(atHome = true)).isEmpty())
        val empty = Automation("e", "Vuota", AutomationTrigger.HOME_ARRIVE)
        assertFalse(empty.isUsable)
        assertTrue(Automations.fired(listOf(empty), env(), env(atHome = true)).isEmpty())
    }

    @Test
    fun upsertAndRemove() {
        val a = auto(AutomationTrigger.TIME)
        val list = Automations.upsert(emptyList(), a)
        assertEquals(1, list.size)
        assertEquals("Nuova", Automations.upsert(list, a.copy(name = "Nuova")).single().name)
        assertTrue(Automations.remove(list, "a").isEmpty())
    }

    @Test
    fun templatesAreUsableOrNeedOnlyTheUsersChoice() {
        assertTrue(Automations.TEMPLATES.size >= 8)
        val t = Automations.TEMPLATES.first { it.trigger == AutomationTrigger.TIME && it.minute == 23 * 60 }
        val a = Automations.fromTemplate(t, "x", "Buonanotte", "")
        assertEquals(listOf("SILENT_ON"), a.actions)
        assertTrue(a.isUsable)
    }

    @Test
    fun carModeTemplateEnlargesIconsAndTextUntilDisconnection() {
        val t = Automations.TEMPLATES.first { it.trigger == AutomationTrigger.DEVICE_ON }
        val a = Automations.fromTemplate(t, "car", "Auto", "")
        val changes = a.actions.map { Actions.decode(it) }.filter { it.action == GestureAction.LAUNCHER_SET }.mapNotNull { LauncherChange.decode(it.arg) }
        assertEquals(setOf(LauncherSetting.ICON_SIZE, LauncherSetting.TEXT_SIZE), changes.map { it.setting }.toSet())
        assertTrue(changes.all { it.restore })
    }
}
