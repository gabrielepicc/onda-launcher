// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionsTest {
    @Test
    fun `encode e decode sono inversi`() {
        val scelte = listOf(
            ActionChoice(GestureAction.FLASHLIGHT),
            ActionChoice(GestureAction.OPEN_APP, "com.x/com.x.Main#0"),
            ActionChoice(GestureAction.OPEN_SHORTCUT, Actions.shortcutArg("com.x", "chat/1")),
        )
        scelte.forEach { assertEquals(it, Actions.decode(Actions.encode(it))) }
    }

    @Test
    fun `nessuna azione è il testo vuoto`() {
        assertEquals("", Actions.encode(ActionChoice(GestureAction.NONE, "x")))
        assertEquals(GestureAction.NONE, Actions.decode("").action)
        assertEquals(GestureAction.NONE, Actions.decode(null).action)
    }

    @Test
    fun `una chiave d'app vecchia diventa Apri un'app`() {
        val c = Actions.decode("org.mozilla.firefox/org.mozilla.fenix.App#0")
        assertEquals(GestureAction.OPEN_APP, c.action)
        assertEquals("org.mozilla.firefox/org.mozilla.fenix.App#0", c.arg)
    }

    @Test
    fun `le scorciatoie si separano al primo slash`() {
        assertEquals("com.x" to "chat/1", Actions.parseShortcut("com.x/chat/1"))
        assertNull(Actions.parseShortcut("senza-slash"))
        assertNull(Actions.parseShortcut("com.x/"))
    }

    @Test
    fun `le azioni di sistema dichiarano il permesso`() {
        assertEquals(ActionNeed.ACCESSIBILITY, Actions.need(GestureAction.LOCK_SCREEN))
        assertEquals(ActionNeed.ACCESSIBILITY, Actions.need(GestureAction.SCREENSHOT))
        assertEquals(ActionNeed.POLICY_ACCESS, Actions.need(GestureAction.SILENT))
        assertEquals(ActionNeed.NONE, Actions.need(GestureAction.FLASHLIGHT))
    }

    @Test
    fun `il catalogo contiene ogni azione una volta sola e non offre Nessuna`() {
        val tutte = Actions.catalog.flatMap { it.second }
        assertEquals(tutte.size, tutte.toSet().size)
        assertFalse(GestureAction.NONE in tutte)
        assertEquals(GestureAction.entries.toSet() - GestureAction.NONE, tutte.toSet())
    }

    @Test
    fun `una scelta con argomento è completa solo se l'argomento c'è`() {
        assertFalse(Actions.isComplete(ActionChoice(GestureAction.OPEN_APP)))
        assertTrue(Actions.isComplete(ActionChoice(GestureAction.OPEN_APP, "k")))
        assertTrue(Actions.isComplete(ActionChoice(GestureAction.SEARCH)))
        assertFalse(Actions.isComplete(ActionChoice(GestureAction.NONE)))
    }

    @Test
    fun `la ricerca trova le azioni senza maiuscole né accenti e salta quelle con argomento`() {
        val nomi = mapOf(GestureAction.FLASHLIGHT to "Torcia", GestureAction.SILENT to "Silenzioso", GestureAction.OPEN_APP to "Apri un'app")
        val label = { a: GestureAction -> nomi[a] ?: a.name }
        assertEquals(listOf(GestureAction.FLASHLIGHT), Actions.matching("TORC", label))
        assertEquals(listOf(GestureAction.SILENT), Actions.matching("silenziós", label))
        assertTrue(Actions.matching("t", label).isEmpty())
        assertFalse(GestureAction.OPEN_APP in Actions.matching("apri", label))
    }
}
