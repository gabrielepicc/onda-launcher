// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetVisibilityTest {
    private fun env(
        headphones: Boolean = false,
        charging: Boolean = false,
        minute: Int = 12 * 60,
        music: Boolean = false,
    ) = EnvSnapshot(
        atHome = false, headphones = headphones, charging = charging, devices = emptySet(),
        minuteOfDay = minute, dayOfWeek = 1, sunriseMinute = 7 * 60, sunsetMinute = 19 * 60, musicPlaying = music,
    )

    private val headphones = WidgetRule(always = false, conditions = listOf(WidgetCondition(AutomationTrigger.HEADPHONES_ON)))

    @Test
    fun stateConditionsHaveStartAndEnd() {
        assertTrue(WidgetVisibility.isVisible(headphones, env(headphones = true)))
        assertFalse(WidgetVisibility.isVisible(headphones, env(headphones = false)))
        // Senza stato noto una regola non vale
        assertFalse(WidgetVisibility.isVisible(headphones, null))
        // Con regole ma senza condizioni: nascosto
        assertFalse(WidgetVisibility.isVisible(WidgetRule(always = false), env()))
        assertTrue(WidgetVisibility.isVisible(WidgetRule(), null))
    }

    @Test
    fun timeAndSunWindows() {
        val work = WidgetCondition(AutomationTrigger.TIME, fromMinute = 8 * 60, toMinute = 18 * 60)
        assertTrue(WidgetVisibility.holds(work, env(minute = 9 * 60)))
        assertFalse(WidgetVisibility.holds(work, env(minute = 18 * 60)))
        // A cavallo della mezzanotte
        assertTrue(WidgetVisibility.inWindow(23 * 60, 22 * 60, 6 * 60))
        assertTrue(WidgetVisibility.inWindow(5 * 60, 22 * 60, 6 * 60))
        assertFalse(WidgetVisibility.inWindow(12 * 60, 22 * 60, 6 * 60))
        assertTrue(WidgetVisibility.holds(WidgetCondition(AutomationTrigger.SUNRISE), env(minute = 12 * 60)))
        assertTrue(WidgetVisibility.holds(WidgetCondition(AutomationTrigger.SUNSET), env(minute = 23 * 60)))
        assertTrue(WidgetVisibility.holds(WidgetCondition(AutomationTrigger.MUSIC_ON), env(music = true)))
    }

    @Test
    fun specialPlacementsHaveOneWidgetAndTheFirstWins() {
        val rules = mapOf(
            1 to WidgetRule(),
            2 to WidgetRule(WidgetPlacement.REPLACE_CLOCK),
            3 to WidgetRule(WidgetPlacement.REPLACE_CLOCK),
            4 to headphones.copy(placement = WidgetPlacement.BESIDE_CLOCK),
        )
        val off = WidgetVisibility.resolve(listOf(1, 2, 3, 4), rules, env())
        assertEquals(setOf(1), off.below)
        assertEquals(2, off.replace)
        assertNull(off.beside)
        val on = WidgetVisibility.resolve(listOf(3, 2, 1, 4), rules, env(headphones = true))
        assertEquals(3, on.replace)
        assertEquals(4, on.beside)
    }

    private val automation = Automation(id = "a", name = "Cuffie", trigger = AutomationTrigger.HEADPHONES_ON)

    @Test
    fun automationsShowAndHideThroughOverrides() {
        val saved = Settings(widgetRules = mapOf(5 to WidgetRule(always = false, conditions = listOf(WidgetCondition(AutomationTrigger.CHARGING_ON)))))
        val show = WidgetVisibility.change(GestureAction.WIDGET_SHOW, WidgetVisibility.showArg(5, WidgetPlacement.BESIDE_CLOCK, restore = true))!!
        val hide = WidgetVisibility.change(GestureAction.WIDGET_HIDE, WidgetVisibility.hideArg(6, restore = true))!!
        val started = AutomationOverrides.start(AutomationOverrides.start(saved, automation, show, 1L), automation, hide, 2L)
        // Le due sostituzioni della stessa automazione restano entrambe (widget diversi)
        assertEquals(2, started.automationOverrides.size)
        // Le impostazioni salvate non cambiano
        assertEquals(saved.widgetRules, started.widgetRules)
        val effective = AutomationOverrides.effective(started)
        val shown = WidgetVisibility.resolve(listOf(5, 6), effective.widgetRules, env())
        assertEquals(5, shown.beside)
        assertFalse(6 in shown.below)
        // Ripristino al termine: scollegando le cuffie le sostituzioni cadono e torna la regola salvata
        val ended = AutomationOverrides.prune(started.copy(automations = listOf(automation)), env(headphones = false), 10L)
        assertTrue(ended.automationOverrides.isEmpty())
        val back = WidgetVisibility.resolve(listOf(5, 6), AutomationOverrides.effective(ended).widgetRules, env())
        assertNull(back.beside)
        assertEquals(setOf(6), back.below)
    }

    @Test
    fun expiredOverrideFalls() {
        val show = WidgetVisibility.change(GestureAction.WIDGET_SHOW, WidgetVisibility.showArg(5, WidgetPlacement.REPLACE_CLOCK, restore = true))!!
        val timed = automation.copy(trigger = AutomationTrigger.TIME)
        val s = AutomationOverrides.start(Settings(automations = listOf(timed)), timed, show, now = 1L, until = 100L)
        assertEquals(1, AutomationOverrides.prune(s, env(), 50L).automationOverrides.size)
        assertTrue(AutomationOverrides.prune(s, env(), 100L).automationOverrides.isEmpty())
    }

    @Test
    fun argRoundTrip() {
        val a = WidgetVisibility.parseArg(WidgetVisibility.showArg(12, WidgetPlacement.BESIDE_CLOCK, restore = false))!!
        assertEquals(12, a.id)
        assertEquals(WidgetPlacement.BESIDE_CLOCK, a.placement)
        assertFalse(a.restore)
        assertTrue(WidgetVisibility.needsEnv(Settings(widgetRules = mapOf(1 to headphones))))
        assertFalse(WidgetVisibility.needsEnv(Settings(widgetRules = mapOf(1 to WidgetRule()))))
    }
}
