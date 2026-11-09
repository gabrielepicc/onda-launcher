// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DesignRulesTest {
    @Test
    fun segmentedOnlyWithFewShortLabels() {
        val seg = DesignRules.ChoiceStyle.SEGMENTED
        val list = DesignRules.ChoiceStyle.LIST
        assertEquals(seg, DesignRules.choiceStyle(listOf("Spento", "Colori")))
        assertEquals(seg, DesignRules.choiceStyle(listOf("Uno", "Due", "Tre")))
        // Quattro opzioni o etichette lunghe: elenco
        assertEquals(list, DesignRules.choiceStyle(listOf("A", "B", "C", "D")))
        assertEquals(list, DesignRules.choiceStyle(listOf("Off", "To the bottom")))
        assertEquals(list, DesignRules.choiceStyle(emptyList()))
        // Dieci caratteri vanno bene fino alla scala 1,3, oltre no
        assertEquals(seg, DesignRules.choiceStyle(listOf("Automatica", "Manuale"), fontScale = 1.3f))
        assertEquals(list, DesignRules.choiceStyle(listOf("Automatica", "Manuale"), fontScale = 1.6f))
        assertEquals(seg, DesignRules.choiceStyle(listOf("Sì", "No"), fontScale = 2f))
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
