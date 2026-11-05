// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupArchiveTest {

    @Test
    fun settingsAndFilesSurviveTheRoundTrip() {
        val icon = File.createTempFile("icona", ".png").apply { writeBytes(byteArrayOf(1, 2, 3)); deleteOnExit() }
        val out = ByteArrayOutputStream()
        BackupArchive.write(out, """{"a":"città"}""", mapOf("icons/a.png" to icon))

        val written = HashMap<String, ByteArrayOutputStream>()
        val json = BackupArchive.read(ByteArrayInputStream(out.toByteArray())) { path ->
            ByteArrayOutputStream().also { written[path] = it }
        }
        assertEquals("""{"a":"città"}""", json)
        assertArrayEquals(byteArrayOf(1, 2, 3), written.getValue("icons/a.png").toByteArray())
    }

    @Test
    fun recognizesArchivesAndOldBackups() {
        val out = ByteArrayOutputStream()
        BackupArchive.write(out, "{}", emptyMap())
        assertTrue(BackupArchive.isArchive(out.toByteArray()))
        assertFalse(BackupArchive.isArchive("{}".toByteArray()))
        assertFalse(BackupArchive.isArchive(ByteArray(0)))
    }

    @Test
    fun pathsMoveBetweenPhones() {
        val portable = BackupArchive.toPortable("""{"f":"/data/user/0/onda/files/fonts/a.ttf"}""", "/data/user/0/onda/files")
        assertFalse(portable.contains("/data/user"))
        assertEquals("""{"f":"/data/user/10/onda/files/fonts/a.ttf"}""", BackupArchive.toLocal(portable, "/data/user/10/onda/files"))
    }

    @Test
    fun onlyKnownFoldersCanBeWritten() {
        assertEquals("icons/a.png", BackupArchive.safeResourcePath("files/icons/a.png"))
        assertEquals("fonts/b.ttf", BackupArchive.safeResourcePath("files/fonts/b.ttf"))
        assertNull(BackupArchive.safeResourcePath("files/../../shared_prefs/x.xml"))
        assertNull(BackupArchive.safeResourcePath("files/icons/../../x"))
        assertNull(BackupArchive.safeResourcePath("files/icons/.."))
        assertNull(BackupArchive.safeResourcePath("files/altro/a.png"))
        assertNull(BackupArchive.safeResourcePath("settings.xml"))
        assertNull(BackupArchive.safeResourcePath("files/icons/a\\b"))
        assertNull(BackupArchive.safeResourcePath("files/icons/"))
    }

    @Test
    fun maliciousEntriesAreIgnoredAndMissingSettingsGiveNothing() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("files/../../evil"))
            zip.write(1)
            zip.closeEntry()
        }
        val opened = ArrayList<String>()
        val json = BackupArchive.read(ByteArrayInputStream(out.toByteArray())) { path ->
            opened += path
            ByteArrayOutputStream()
        }
        assertNull(json)
        assertTrue(opened.isEmpty())
    }
}
