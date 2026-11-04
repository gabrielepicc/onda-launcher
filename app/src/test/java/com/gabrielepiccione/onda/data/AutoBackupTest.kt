// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoBackupTest {

    @Test
    fun runsOncePerDayAndOnlyWithAFolder() {
        assertFalse(AutoBackup.isDue(Settings(), "2026-11-05"))
        assertTrue(AutoBackup.isDue(Settings(autoBackupUri = "content://x", autoBackupLastDay = "2026-11-04"), "2026-11-05"))
        assertFalse(AutoBackup.isDue(Settings(autoBackupUri = "content://x", autoBackupLastDay = "2026-11-05"), "2026-11-05"))
    }

    @Test
    fun deletesOnlyOldOndaFiles() {
        val names = listOf("foto.jpg", "onda-backup-2026-01-01.json") + (1..9).map { AutoBackup.fileName("2026-11-0$it") }
        assertEquals(
            listOf(AutoBackup.fileName("2026-11-02"), AutoBackup.fileName("2026-11-01")),
            AutoBackup.toDelete(names),
        )
    }

    @Test
    fun deviceSpecificBackupFolderIsNotInTheDefaults() {
        assertEquals("", Settings().autoBackupUri)
    }
}
