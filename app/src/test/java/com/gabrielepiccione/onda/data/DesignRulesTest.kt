// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DesignRulesTest {
    @Test
    fun fewOptionsBecomeSegmentedButtons() {
        assertEquals(DesignRules.ChoiceStyle.SEGMENTED, DesignRules.choiceStyle(2))
        assertEquals(DesignRules.ChoiceStyle.SEGMENTED, DesignRules.choiceStyle(4))
        assertEquals(DesignRules.ChoiceStyle.LIST, DesignRules.choiceStyle(5))
        assertEquals(DesignRules.ChoiceStyle.LIST, DesignRules.choiceStyle(0))
    }

    @Test
    fun groupsFollowTheStandardOrderWithDangerLast() {
        val groups = listOf("Pericolo" to DesignRules.MenuGroup.DANGER, "Aspetto" to DesignRules.MenuGroup.APPEARANCE, "Nome" to DesignRules.MenuGroup.IDENTITY)
        assertEquals(listOf("Nome", "Aspetto", "Pericolo"), DesignRules.ordered(groups) { it.second }.map { it.first })
    }

    @Test
    fun sliderStepsForDiscreteValues() {
        // 0..32 a passi di 4: 9 valori, 7 tacche intermedie
        assertEquals(7, DesignRules.sliderSteps(0f, 32f, 4f))
        // Piena / ¾ / ½: 0.5..1 a passi di 0.25 → 3 valori, 1 tacca
        assertEquals(1, DesignRules.sliderSteps(0.5f, 1f, 0.25f))
        assertEquals(0, DesignRules.sliderSteps(0f, 1f, 0f))
    }
}
