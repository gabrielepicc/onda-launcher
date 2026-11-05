// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FolderIconsTest {

    private val base = Settings(folders = mapOf("f1" to Folder("f1", "Giochi")))

    @Test
    fun parsesEveryKind() {
        assertEquals(FolderIcons.Kind.Auto, FolderIcons.parse(""))
        assertEquals(FolderIcons.Kind.App("com.x/.A#0"), FolderIcons.parse(FolderIcons.app("com.x/.A#0")))
        assertEquals(FolderIcons.Kind.Symbol("games"), FolderIcons.parse(FolderIcons.symbol("games")))
        assertEquals(FolderIcons.Kind.Image("/data/i.png"), FolderIcons.parse(FolderIcons.image("/data/i.png")))
    }

    @Test
    fun unknownSymbolFallsBackToAuto() {
        assertEquals(FolderIcons.Kind.Auto, FolderIcons.parse("sym:does-not-exist"))
    }

    @Test
    fun setIconAndHeightOnlyTouchTheFolder() {
        val s = FolderIcons.setWidgetHeight(FolderIcons.setIcon(base, "f1", FolderIcons.symbol("star")), "f1", 1000)
        assertEquals("sym:star", s.folders["f1"]?.icon)
        assertEquals(FolderIcons.HEIGHT_RANGE.last, s.folders["f1"]?.widgetHeightDp)
        assertEquals(s, FolderIcons.setIcon(s, "missing", "sym:star"))
        assertNull(FolderIcons.setWidgetHeight(s, "f1", null).folders["f1"]?.widgetHeightDp)
    }

    @Test
    fun imageKeyIdentifiesTheFolder() {
        assertEquals("f1", FolderIcons.folderOfImageKey(FolderIcons.IMAGE_KEY_PREFIX + "f1"))
        assertNull(FolderIcons.folderOfImageKey("com.x/.A#0"))
    }

    @Test
    fun oldFoldersWithoutNewFieldsStillDecode() {
        val s = SettingsCodec.decode("""{"schemaVersion":4,"folders":{"f1":{"id":"f1","name":"Vecchia","apps":["a"]}}}""")
        assertEquals("", s.folders["f1"]?.icon)
        assertNull(s.folders["f1"]?.widgetHeightDp)
    }
}
