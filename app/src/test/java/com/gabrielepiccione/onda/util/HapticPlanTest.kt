// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

import android.os.VibrationEffect
import android.view.HapticFeedbackConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticPlanTest {

    private val all = setOf(1, 2, 3, 4, 5, 6, 7, 8)

    @Test
    fun constantsMatchAndroid() {
        assertEquals(VibrationEffect.Composition.PRIMITIVE_CLICK, HapticPlan.PRIMITIVE_CLICK)
        assertEquals(VibrationEffect.Composition.PRIMITIVE_TICK, HapticPlan.PRIMITIVE_TICK)
        assertEquals(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, HapticPlan.PRIMITIVE_LOW_TICK)
        assertEquals(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, HapticPlan.PRIMITIVE_QUICK_RISE)
        assertEquals(HapticFeedbackConstants.CLOCK_TICK, HapticPlan.FB_CLOCK_TICK)
        assertEquals(HapticFeedbackConstants.CONFIRM, HapticPlan.FB_CONFIRM)
        assertEquals(HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE, HapticPlan.FB_GESTURE_THRESHOLD_ACTIVATE)
        assertEquals(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK, HapticPlan.FB_SEGMENT_FREQUENT_TICK)
    }

    @Test
    fun letterIsTheLightestTap() {
        val letter = HapticPlan.composition(HapticEvent.LETTER, all)!!.single()
        assertEquals(HapticPlan.PRIMITIVE_LOW_TICK, letter.primitive)
        val longPress = HapticPlan.composition(HapticEvent.LONG_PRESS, all)!!.single()
        assertTrue(letter.scale < longPress.scale)
    }

    @Test
    fun letterFallsBackToSoftTickWithoutLowTick() {
        val letter = HapticPlan.composition(HapticEvent.LETTER, setOf(HapticPlan.PRIMITIVE_TICK, HapticPlan.PRIMITIVE_CLICK))!!.single()
        assertEquals(HapticPlan.PRIMITIVE_TICK, letter.primitive)
        assertTrue(letter.scale <= 0.4f)
    }

    @Test
    fun missingPrimitiveMeansSystemFallback() {
        assertNull(HapticPlan.composition(HapticEvent.OPEN, setOf(HapticPlan.PRIMITIVE_TICK)))
        assertNotNull(HapticPlan.composition(HapticEvent.OPEN, all))
        assertNull(HapticPlan.composition(HapticEvent.LETTER, emptySet()))
    }

    @Test
    fun fallbackUsesNewerConstantsOnlyWhenAvailable() {
        assertEquals(HapticPlan.FB_SEGMENT_FREQUENT_TICK, HapticPlan.fallback(HapticEvent.LETTER, 34))
        assertEquals(HapticPlan.FB_CLOCK_TICK, HapticPlan.fallback(HapticEvent.LETTER, 29))
        assertEquals(HapticPlan.FB_CONFIRM, HapticPlan.fallback(HapticEvent.CONFIRM, 30))
        assertEquals(HapticPlan.FB_VIRTUAL_KEY, HapticPlan.fallback(HapticEvent.CONFIRM, 26))
    }

    @Test
    fun fastLettersAreThrottled() {
        assertFalse(HapticPlan.shouldPlay(HapticEvent.LETTER, 1010, 1000))
        assertTrue(HapticPlan.shouldPlay(HapticEvent.LETTER, 1040, 1000))
        assertTrue(HapticPlan.shouldPlay(HapticEvent.EDGE, 1001, 1000))
    }
}
