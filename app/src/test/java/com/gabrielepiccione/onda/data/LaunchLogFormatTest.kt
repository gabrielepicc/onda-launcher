// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LaunchLogFormatTest {
    @Test
    fun roundTripWithContext() {
        val r = LaunchRecord("com.a/.Main#0", 1_700_000_000_000L, headphones = true, charging = true, wifiHash = "abc123")
        assertEquals(r, LaunchLogFormat.decode(LaunchLogFormat.encode(r)))
    }

    @Test
    fun plainRecordsStayShort() {
        val r = LaunchRecord("app", 5L)
        assertEquals("5\tapp", LaunchLogFormat.encode(r))
        assertEquals(r, LaunchLogFormat.decode("5\tapp"))
    }

    @Test
    fun badLinesAreSkipped() {
        assertNull(LaunchLogFormat.decode(""))
        assertNull(LaunchLogFormat.decode("x\tapp"))
        assertNull(LaunchLogFormat.decode("5"))
        assertNull(LaunchLogFormat.decode("5\t"))
    }

    @Test
    fun wifiIsOnlyACode() {
        val code = LaunchLogFormat.wifiCode("CasaMia")!!
        assertEquals(12, code.length)
        assertEquals(code, LaunchLogFormat.wifiCode(" CasaMia "))
        assertNotEquals(code, LaunchLogFormat.wifiCode("Ufficio"))
        assertNull(LaunchLogFormat.wifiCode(null))
        assertNull(LaunchLogFormat.wifiCode("  "))
    }
}
