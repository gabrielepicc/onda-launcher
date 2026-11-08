// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationOverridesTest {
    private val carOn = Automation("car", "Auto", AutomationTrigger.DEVICE_ON, device = "Auto", actions = listOf("X"))
    private val night = Automation("night", "Notte", AutomationTrigger.TIME, minute = 23 * 60, actions = listOf("X"))
    private val big = LauncherChange(LauncherSetting.ICON_SIZE, "1.25", restore = true)

    private fun env(devices: Set<String> = emptySet(), atHome: Boolean = false) = EnvSnapshot(atHome, false, false, devices, 600, 2)

    private fun base() = Settings(automations = listOf(carOn, night))

    @Test
    fun savedSettingsAreNeverTouched() {
        val s = AutomationOverrides.start(base(), carOn, big, now = 10)
        assertEquals(1f, s.iconSize, 0f)
        assertEquals(1.25f, AutomationOverrides.effective(s).iconSize, 0f)
    }

    @Test
    fun theLatestRuleWins() {
        var s = AutomationOverrides.start(base(), carOn, big, now = 10)
        s = AutomationOverrides.start(s, night, LauncherChange(LauncherSetting.ICON_SIZE, "0.85", restore = false), now = 20)
        assertEquals(0.85f, AutomationOverrides.effective(s).iconSize, 0f)
    }

    @Test
    fun overridesOnDifferentSettingsStack() {
        var s = AutomationOverrides.start(base(), carOn, big, now = 10)
        s = AutomationOverrides.start(s, carOn, LauncherChange(LauncherSetting.TEXT_SIZE, "1.3", true), now = 11)
        val e = AutomationOverrides.effective(s)
        assertEquals(1.25f, e.iconSize, 0f)
        assertEquals(1.3f, e.textScale, 0f)
    }

    @Test
    fun theSameRuleRenewsInsteadOfDuplicating() {
        var s = AutomationOverrides.start(base(), carOn, big, now = 10)
        s = AutomationOverrides.start(s, carOn, big.copy(value = "1.45"), now = 30)
        assertEquals(1, s.automationOverrides.size)
        assertEquals(1.45f, AutomationOverrides.effective(s).iconSize, 0f)
    }

    @Test
    fun restoreDropsTheOverrideWhenTheConditionEndsEvenAfterARestart() {
        val s = AutomationOverrides.start(base(), carOn, big, now = 10)
        // Ancora collegata: resta
        assertSame(s, AutomationOverrides.prune(s, env(devices = setOf("auto")), now = 20))
        // Scollegata (anche se il processo è ripartito e non ha visto il momento esatto): cade
        assertTrue(AutomationOverrides.prune(s, env(), now = 20).automationOverrides.isEmpty())
    }

    @Test
    fun withoutRestoreTheOverrideStays() {
        val s = AutomationOverrides.start(base(), carOn, big.copy(restore = false), now = 10)
        assertEquals(1, AutomationOverrides.prune(s, env(), now = 20).automationOverrides.size)
    }

    @Test
    fun unknownStateKeepsTheOverride() {
        val s = AutomationOverrides.start(base(), carOn, big, now = 10)
        assertEquals(1, AutomationOverrides.prune(s, null, now = 20).automationOverrides.size)
    }

    @Test
    fun overridesExpire() {
        val s = AutomationOverrides.start(base(), night, big, now = 10, until = 100)
        assertEquals(1, AutomationOverrides.prune(s, env(), now = 99).automationOverrides.size)
        assertTrue(AutomationOverrides.prune(s, env(), now = 100).automationOverrides.isEmpty())
    }

    @Test
    fun aManualChoiceWins() {
        val s = AutomationOverrides.start(base(), carOn, big, now = 10)
        // L'utente sposta lo slider delle icone mentre la sostituzione è attiva
        val manual = s.copy(iconSize = 1.1f)
        val pruned = AutomationOverrides.prune(manual, env(devices = setOf("Auto")), now = 20)
        assertTrue(pruned.automationOverrides.isEmpty())
        assertEquals(1.1f, AutomationOverrides.effective(pruned).iconSize, 0f)
    }

    @Test
    fun deletedOrDisabledAutomationsLoseTheirOverrides() {
        val s = AutomationOverrides.start(base(), carOn, big, now = 10)
        assertTrue(AutomationOverrides.prune(s.copy(automations = listOf(night)), null, 20).automationOverrides.isEmpty())
        assertTrue(AutomationOverrides.prune(s.copy(automations = listOf(carOn.copy(enabled = false), night)), null, 20).automationOverrides.isEmpty())
    }

    @Test
    fun everySettingRoundTripsThroughReadAndApply() {
        LauncherSetting.entries.forEach { setting ->
            AutomationOverrides.presets(setting).forEach { p ->
                val applied = AutomationOverrides.apply(Settings(), setting, p.value)
                assertEquals("$setting=${p.value}", p.value.toFloatOrNull() ?: p.value, AutomationOverrides.read(applied, setting).let { it.toFloatOrNull() ?: it })
            }
        }
    }

    @Test
    fun invalidValuesChangeNothing() {
        assertEquals(Settings(), AutomationOverrides.apply(Settings(), LauncherSetting.ICON_SIZE, "big"))
        assertEquals(Settings(), AutomationOverrides.apply(Settings(), LauncherSetting.THEME, "PURPLE"))
    }

    @Test
    fun grayscaleOverrideTurnsItOn() {
        val e = AutomationOverrides.apply(Settings(), LauncherSetting.GRAYSCALE, "1")
        assertTrue(GrayscaleSchedule.active(e.grayscaleMode, 0, 0, 0, "", null))
    }

    @Test
    fun changeEncodingRoundTrips() {
        val c = LauncherChange(LauncherSetting.BLOCK_SPACING, "16", restore = false)
        assertEquals(c, LauncherChange.decode(c.encode()))
        assertEquals(LauncherChange(LauncherSetting.PROFILE, "", true), LauncherChange.decode("PROFILE=;1"))
        assertNull(LauncherChange.decode("NOPE=1;1"))
    }

    @Test
    fun restoreIsOnByDefaultOnlyForPairs() {
        assertTrue(AutomationOverrides.defaultRestore(AutomationTrigger.DEVICE_ON))
        assertTrue(AutomationOverrides.defaultRestore(AutomationTrigger.CHARGING_ON))
        assertFalse(AutomationOverrides.defaultRestore(AutomationTrigger.TIME))
    }

    @Test
    fun conditionsReadFromState() {
        assertEquals(true, AutomationOverrides.conditionHolds(AutomationTrigger.HOME_ARRIVE, "", env(atHome = true)))
        assertEquals(false, AutomationOverrides.conditionHolds(AutomationTrigger.DEVICE_ON, "Auto", env(devices = setOf("Cuffie"))))
        assertEquals(true, AutomationOverrides.conditionHolds(AutomationTrigger.DEVICE_OFF, "Auto", env()))
        assertNull(AutomationOverrides.conditionHolds(AutomationTrigger.SUNSET, "", env()))
    }
}
