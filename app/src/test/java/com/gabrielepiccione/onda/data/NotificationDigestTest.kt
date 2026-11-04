// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationDigestTest {

    private fun item(key: String, pkg: String, time: Long) =
        NotificationItem(key, pkg, "titolo", "testo", time, null, autoCancel = false, clearable = true)

    @Test
    fun countsAndOrdersAppsByMostRecent() {
        val digest = NotificationDigest.of(
            mapOf(
                "a" to listOf(item("1", "a", 10), item("2", "a", 20)),
                "b" to listOf(item("3", "b", 30)),
            )
        )
        assertEquals(3, digest.total)
        assertEquals(listOf("b", "a"), digest.packages)
    }

    @Test
    fun ignoresAppsWithoutNotifications() {
        val digest = NotificationDigest.of(mapOf("a" to emptyList()))
        assertEquals(0, digest.total)
        assertEquals(emptyList<String>(), digest.packages)
    }
}
