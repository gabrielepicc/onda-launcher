// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoBackupTest {

    private val day = 24 * 60 * 60 * 1000L

    @Test
    fun notDueWithoutFolder() {
        assertFalse(AutoBackup.isDue(Settings(), now = 10 * day))
    }

    @Test
    fun dueOncePerDay() {
        val s = Settings(autoBackupUri = "content://x", lastAutoBackup = 10 * day)
        assertFalse(AutoBackup.isDue(s, now = 10 * day + day / 2))
        assertTrue(AutoBackup.isDue(s, now = 11 * day))
        assertTrue(AutoBackup.isDue(s.copy(lastAutoBackup = 0), now = 10 * day))
    }
}
