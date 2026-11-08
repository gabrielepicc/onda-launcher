// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppOptionsTest {
    @Test
    fun moveUpAndDownOnlyForFavoritesThatCanMove() {
        val first = AppOptions.visible(isFavorite = true, favoriteIndex = 0, favoriteCount = 3, hasShortcuts = false)
        assertFalse(AppOption.MOVE_UP in first)
        assertTrue(AppOption.MOVE_DOWN in first)
        val notFav = AppOptions.visible(isFavorite = false, favoriteIndex = -1, favoriteCount = 3, hasShortcuts = false)
        assertFalse(AppOption.MOVE_UP in notFav)
        assertFalse(AppOption.MOVE_DOWN in notFav)
    }

    @Test
    fun pinShortcutOnlyWhenTheAppHasShortcuts() {
        assertFalse(AppOption.PIN_SHORTCUT in AppOptions.visible(false, -1, 0, hasShortcuts = false))
        assertTrue(AppOption.PIN_SHORTCUT in AppOptions.visible(false, -1, 0, hasShortcuts = true))
    }

    @Test
    fun groupsFollowTheStandardOrderWithUninstallLast() {
        val groups = AppOptions.groups(AppOptions.visible(true, 1, 3, true))
        assertEquals(AppOptionGroup.entries.toList(), groups.map { it.first })
        assertEquals(AppOption.UNINSTALL, groups.last().second.last())
    }

    @Test
    fun emptyGroupsDisappear() {
        val groups = AppOptions.groups(AppOptions.visible(false, -1, 0, hasShortcuts = false, canUninstall = false))
        assertFalse(groups.any { (g, _) -> g == AppOptionGroup.SYSTEM && AppOption.UNINSTALL in g.options && false })
        assertTrue(groups.first { it.first == AppOptionGroup.SYSTEM }.second == listOf(AppOption.INFO))
        assertFalse(groups.first { it.first == AppOptionGroup.ADD }.second.contains(AppOption.PIN_SHORTCUT))
    }
}
