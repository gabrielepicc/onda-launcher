// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

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
}
