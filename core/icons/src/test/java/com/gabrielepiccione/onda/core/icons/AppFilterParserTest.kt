// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.core.icons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFilterParserTest {

    private val sample = """
        <?xml version="1.0" encoding="utf-8"?>
        <resources>
            <!-- commento con <item component="ComponentInfo{finto/finto}" drawable="no"/> -->
            <iconback img1="back_a" img2="back_b" />
            <iconmask img1="mask" />
            <iconupon img1="upon" />
            <scale factor="0.75" />
            <item component="ComponentInfo{com.example.mail/com.example.mail.MainActivity}" drawable="mail" />
            <item component="ComponentInfo{com.example.mail/com.example.mail.Compose}" drawable="mail_compose" />
            <item component="ComponentInfo{com.example.rel/.Start}" drawable="rel" />
            <item component="ComponentInfo{broken}" drawable="x" />
            <calendar component="ComponentInfo{com.example.cal/com.example.cal.Main}" prefix="cal_" />
        </resources>
    """.trimIndent()

    @Test
    fun parsesItemsBacksMaskUponScale() {
        val f = AppFilterParser.parseText(sample)
        assertEquals("mail", f.components["com.example.mail/com.example.mail.MainActivity"])
        assertEquals(listOf("back_a", "back_b"), f.iconBacks)
        assertEquals("mask", f.iconMask)
        assertEquals("upon", f.iconUpon)
        assertEquals(0.75f, f.scale, 0.0001f)
        assertTrue(f.hasFallback)
    }

    @Test
    fun ignoresCommentsAndInvalidComponents() {
        val f = AppFilterParser.parseText(sample)
        assertNull(f.components["finto/finto"])
        assertFalse(f.components.keys.any { it.startsWith("broken") })
    }

    @Test
    fun expandsRelativeClassNames() {
        val f = AppFilterParser.parseText(sample)
        assertEquals("rel", f.components["com.example.rel/com.example.rel.Start"])
    }

    @Test
    fun packageFallbackUsesFirstEntry() {
        val f = AppFilterParser.parseText(sample)
        assertEquals("mail", f.drawableFor("com.example.mail", "com.example.mail.Other", 1))
    }

    @Test
    fun calendarIconsUseDayOfMonth() {
        val f = AppFilterParser.parseText(sample)
        assertEquals("cal_17", f.drawableFor("com.example.cal", "com.example.cal.Main", 17))
    }

    @Test
    fun iconBackIsStablePerPackage() {
        val f = AppFilterParser.parseText(sample)
        assertEquals(f.iconBackFor("com.a.b"), f.iconBackFor("com.a.b"))
    }

    @Test
    fun invalidScaleIsIgnored() {
        val f = AppFilterParser.parseText("""<resources><scale factor="99"/></resources>""")
        assertEquals(1f, f.scale, 0.0001f)
    }
}
