// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalendarRepository(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** Primo evento non ancora concluso nelle prossime 24 ore. */
    suspend fun nextEvent(): CalendarEvent? = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext null
        val now = System.currentTimeMillis()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, now - 6 * 60 * 60 * 1000L)
            ContentUris.appendId(it, now + 24 * 60 * 60 * 1000L)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_ID,
        )
        runCatching {
            context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
                var allDayFallback: CalendarEvent? = null
                while (c.moveToNext()) {
                    val title = c.getString(0)?.takeIf { it.isNotBlank() } ?: continue
                    val begin = c.getLong(1)
                    val end = c.getLong(2)
                    val allDay = c.getInt(3) == 1
                    if (end < now) continue
                    val event = CalendarEvent(title, begin, allDay, c.getLong(4))
                    if (allDay) {
                        if (allDayFallback == null) allDayFallback = event
                    } else {
                        return@use event
                    }
                }
                allDayFallback
            }
        }.getOrNull()
    }

    /** Eventi dei prossimi [days] giorni (da oggi), con il colore del calendario. */
    suspend fun upcoming(days: Int, limit: Int = 12): List<CalendarEvent> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val now = System.currentTimeMillis()
        val startOfDay = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, startOfDay)
            ContentUris.appendId(it, startOfDay + days.coerceIn(1, 14) * 24 * 60 * 60 * 1000L)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.DISPLAY_COLOR,
        )
        val out = mutableListOf<CalendarEvent>()
        runCatching {
            context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
                while (c.moveToNext() && out.size < limit) {
                    val title = c.getString(0)?.takeIf { it.isNotBlank() } ?: continue
                    val end = c.getLong(2)
                    if (end < now) continue
                    out += CalendarEvent(title, c.getLong(1), c.getInt(3) == 1, c.getLong(4), end, c.getInt(5))
                }
            }
        }
        out
    }

    /** Titolo, luogo e descrizione degli eventi in corso adesso (non quelli di tutto il giorno), per i profili. */
    suspend fun currentEventTexts(): List<String> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val now = System.currentTimeMillis()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, now - 12 * 60 * 60 * 1000L)
            ContentUris.appendId(it, now + 60 * 1000L)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
        )
        val out = mutableListOf<String>()
        runCatching {
            context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    if (c.getInt(5) == 1 || c.getLong(3) > now || c.getLong(4) <= now) continue
                    out += listOf(c.getString(0), c.getString(1), c.getString(2)).filterNotNull().joinToString("\n")
                }
            }
        }
        out
    }

    fun open(event: CalendarEvent) {
        val intent = Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.eventId))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
