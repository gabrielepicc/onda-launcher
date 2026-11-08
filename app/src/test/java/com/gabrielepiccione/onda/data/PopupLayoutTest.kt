// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PopupLayoutTest {
    @Test
    fun withNotificationsOnlyNotifications() {
        assertFalse(PopupLayout.showShortcuts(hasNotifications = true, collapseWithNotifications = true))
        assertTrue(PopupLayout.showShortcuts(hasNotifications = false, collapseWithNotifications = true))
        assertTrue(PopupLayout.showShortcuts(hasNotifications = true, collapseWithNotifications = false))
    }

    @Test
    fun atMostFourShortcutsThenMore() {
        assertEquals(4, PopupLayout.visibleShortcuts(7, expanded = false))
        assertEquals(7, PopupLayout.visibleShortcuts(7, expanded = true))
        assertEquals(3, PopupLayout.visibleShortcuts(3, expanded = false))
        assertTrue(PopupLayout.hasMoreShortcuts(5, expanded = false))
        assertFalse(PopupLayout.hasMoreShortcuts(4, expanded = false))
        assertFalse(PopupLayout.hasMoreShortcuts(9, expanded = true))
    }
}
