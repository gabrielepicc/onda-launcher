// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditSheetLayoutTest {
    @Test
    fun tabsDependOnWhetherTheBlockHasStyles() {
        assertEquals(listOf(EditTab.STYLE, EditTab.SIZE, EditTab.DETAILS), EditSheetLayout.tabs(true))
        assertEquals(listOf(EditTab.SIZE, EditTab.DETAILS), EditSheetLayout.tabs(false))
        // Una scheda salvata che non esiste più per il blocco: si riparte dalla prima
        assertEquals(EditTab.SIZE, EditSheetLayout.tabOrFirst(EditTab.STYLE, EditSheetLayout.tabs(false)))
        assertEquals(EditTab.DETAILS, EditSheetLayout.tabOrFirst(EditTab.DETAILS, EditSheetLayout.tabs(true)))
        assertEquals(EditTab.STYLE, EditSheetLayout.tabOrFirst(null, EditSheetLayout.tabs(true)))
    }

    @Test
    fun sheetStaysBelowAboutHalfTheScreenAndNearlyOpaque() {
        assertEquals(440, EditSheetLayout.maxHeightDp(800))
        assertTrue(EditSheetLayout.SHEET_ALPHA >= 0.95f)
        assertEquals(0.35f, EditSheetLayout.sheetAlpha(true), 0f)
        assertEquals(0.95f, EditSheetLayout.sheetAlpha(false), 0f)
        // La home sale solo mentre si modifica
        assertEquals(400, EditSheetLayout.contentShiftDp(800, true))
        assertEquals(0, EditSheetLayout.contentShiftDp(800, false))
    }
}
