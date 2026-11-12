// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/** Le schede del foglio di modifica di un blocco della home. */
enum class EditTab { STYLE, SIZE, DETAILS }

/** Misure e schede del foglio di modifica (orologio, player, widget…), senza dipendenze Android. */
object EditSheetLayout {
    /** Il foglio occupa al massimo questa parte dell'altezza: la home resta visibile sopra. */
    const val MAX_HEIGHT_FRACTION = 0.55f

    /** Il foglio è quasi opaco: icone e testi della home non passano sotto il suo testo. */
    const val SHEET_ALPHA = 0.95f

    /** Mentre si trascina uno slider il foglio si fa traslucido per vedere il blocco cambiare. */
    const val DRAG_ALPHA = 0.35f

    /** Quanto si alza il contenuto della home (frazione dell'altezza) per far vedere il blocco sopra il foglio. */
    const val CONTENT_SHIFT_FRACTION = 0.5f

    /** Una scheda «Stile» c'è solo per i blocchi che hanno stili (orologio, player). */
    fun tabs(hasStyle: Boolean): List<EditTab> = if (hasStyle) listOf(EditTab.STYLE, EditTab.SIZE, EditTab.DETAILS) else listOf(EditTab.SIZE, EditTab.DETAILS)

    /** Se la scheda salvata non esiste per questo blocco si parte dalla prima. */
    fun tabOrFirst(saved: EditTab?, tabs: List<EditTab>): EditTab = saved?.takeIf { it in tabs } ?: tabs.first()

    fun maxHeightDp(screenHeightDp: Int): Int = (screenHeightDp * MAX_HEIGHT_FRACTION).toInt()

    fun contentShiftDp(screenHeightDp: Int, editing: Boolean): Int = if (editing) (screenHeightDp * CONTENT_SHIFT_FRACTION).toInt() else 0

    fun sheetAlpha(dragging: Boolean): Float = if (dragging) DRAG_ALPHA else SHEET_ALPHA
}
