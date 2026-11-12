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

    /** Primo evento non ancora concluso nei prossimi [days] giorni, dai calendari non spenti in [excluded]. */
    suspend fun nextEvent(days: Int = 1, excluded: Set<Long> = emptySet()): CalendarEvent? = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext null
        val now = System.currentTimeMillis()
        CalendarLogic.next(events(CalendarLogic.windowStart(now), CalendarLogic.windowEnd(now, days), excluded), now)
    }

    /** Eventi dei prossimi [days] giorni (da oggi), con il colore del calendario. */
    suspend fun upcoming(days: Int, limit: Int = 12, excluded: Set<Long> = emptySet()): List<CalendarEvent> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val now = System.currentTimeMillis()
        val startOfDay = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = startOfDay + days.coerceIn(1, CalendarLogic.DIAGNOSTIC_DAYS) * 24 * 60 * 60 * 1000L
        CalendarLogic.sorted(events(startOfDay, end, excluded)).filterNot { CalendarLogic.isOver(it, now) }.take(limit)
    }

    /** I calendari del telefono con quanti eventi hanno nei prossimi 14 giorni (nessun testo personale, solo conteggi). */
    suspend fun diagnostics(): CalendarDiagnostics = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext CalendarDiagnostics(false, emptyList())
        val now = System.currentTimeMillis()
        val counts = events(now, CalendarLogic.windowEnd(now, CalendarLogic.DIAGNOSTIC_DAYS), emptySet()).groupingBy { it.calendarId }.eachCount()
        CalendarDiagnostics(true, readCalendars().map { it.copy(events14 = counts[it.id] ?: 0) })
    }

    private fun readCalendars(): List<CalendarInfo> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.SYNC_EVENTS,
            CalendarContract.Calendars.CALENDAR_COLOR,
        )
        val out = mutableListOf<CalendarInfo>()
        runCatching {
            context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC")?.use { c ->
                while (c.moveToNext()) {
                    out += CalendarInfo(c.getLong(0), c.getString(1).orEmpty(), c.getString(2).orEmpty(), c.getInt(3) == 1, c.getInt(4) == 1, c.getInt(5))
                }
            }
        }
        return out
    }

    /**
     * Gli eventi tra [start] e [end] di tutti i calendari tranne [excluded]. Instances contiene solo i calendari marcati
     * visibili: quelli non visibili si leggono dalla tabella Events e le ricorrenze si calcolano con [Recurrence].
     */
    private fun events(start: Long, end: Long, excluded: Set<Long>): List<CalendarEvent> {
        val calendars = readCalendars()
        val hidden = calendars.filter { !it.visible && it.id !in excluded }.map { it.id }
        val zone = java.time.ZoneId.systemDefault()
        val out = mutableListOf<CalendarEvent>()
        // Calendari visibili: Instances ha già le occorrenze
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, start)
            ContentUris.appendId(it, end)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.CALENDAR_ID,
        )
        runCatching {
            context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
                while (c.moveToNext()) {
                    val title = c.getString(0)?.takeIf { it.isNotBlank() } ?: continue
                    var begin = c.getLong(1)
                    var finish = c.getLong(2)
                    val allDay = c.getInt(3) == 1
                    if (allDay) CalendarLogic.allDayLocal(begin, finish, zone).let { (b, e) -> begin = b; finish = e }
                    out += CalendarEvent(title, begin, allDay, c.getLong(4), finish, c.getInt(5), c.getLong(6))
                }
            }
        }
        if (hidden.isNotEmpty()) out += hiddenEvents(hidden, calendars, start, end, zone)
        return CalendarLogic.filter(out, excluded)
    }

    private fun hiddenEvents(ids: List<Long>, calendars: List<CalendarInfo>, start: Long, end: Long, zone: java.time.ZoneId): List<CalendarEvent> {
        val colors = calendars.associate { it.id to it.color }
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.DURATION,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.RRULE,
            CalendarContract.Events.EVENT_TIMEZONE,
        )
        val selection = "${CalendarContract.Events.CALENDAR_ID} IN (${ids.joinToString(",")}) AND ${CalendarContract.Events.DELETED} = 0 AND " +
            "${CalendarContract.Events.DTSTART} < ? AND (${CalendarContract.Events.RRULE} IS NOT NULL OR ${CalendarContract.Events.DTEND} >= ?)"
        val out = mutableListOf<CalendarEvent>()
        runCatching {
            context.contentResolver.query(CalendarContract.Events.CONTENT_URI, projection, selection, arrayOf(end.toString(), start.toString()), null)?.use { c ->
                while (c.moveToNext()) {
                    val title = c.getString(2)?.takeIf { it.isNotBlank() } ?: continue
                    val dtStart = c.getLong(3)
                    val allDay = c.getInt(6) == 1
                    val duration = if (!c.isNull(4) && c.getLong(4) > dtStart) c.getLong(4) - dtStart
                    else CalendarLogic.parseDurationMs(c.getString(5)) ?: if (allDay) 86_400_000L else 3_600_000L
                    // Eventi di tutto il giorno: l'ora resta uguale in UTC; gli altri nel fuso dell'evento
                    val eventZone = if (allDay) java.time.ZoneOffset.UTC else runCatching { java.time.ZoneId.of(c.getString(8)) }.getOrDefault(zone)
                    val calendarId = c.getLong(1)
                    // Per gli eventi di tutto il giorno la finestra si allarga di un giorno: i bordi sono a mezzanotte UTC
                    val margin = if (allDay) 86_400_000L else 0L
                    Recurrence.occurrences(dtStart, duration, c.getString(7), eventZone, start - margin, end + margin).forEach { begin ->
                        var b = begin
                        var e = begin + duration
                        if (allDay) CalendarLogic.allDayLocal(b, e, zone).let { (lb, le) -> b = lb; e = le }
                        if (e >= start && b < end) out += CalendarEvent(title, b, allDay, c.getLong(0), e, colors[calendarId] ?: 0, calendarId)
                    }
                }
            }
        }
        return out
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
