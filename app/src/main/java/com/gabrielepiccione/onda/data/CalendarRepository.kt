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

    /** La riga sotto l'orologio: il prossimo evento con l'ora nei prossimi [days] giorni e gli eventi di tutto il giorno di oggi. */
    suspend fun strip(days: Int, excluded: Set<Long>, mode: AllDayMode): DayStrip = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext DayStrip(null, emptyList())
        val now = System.currentTimeMillis()
        // La finestra parte qualche ora prima di adesso e dall'inizio di oggi: gli eventi di tutto il giorno di oggi ci sono
        val startOfDay = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val read = read(minOf(CalendarLogic.windowStart(now), startOfDay), CalendarLogic.windowEnd(now, days), excluded)
        CalendarDays.strip(read.events, now, java.time.ZoneId.systemDefault(), mode)
    }

    /** Eventi dei prossimi [days] giorni (da oggi), con il colore del calendario (stessa lettura di [read]). */
    suspend fun upcoming(days: Int, limit: Int = 60, excluded: Set<Long> = emptySet()): List<CalendarEvent> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val now = System.currentTimeMillis()
        val startOfDay = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = startOfDay + days.coerceIn(1, CalendarLogic.DIAGNOSTIC_DAYS) * 24 * 60 * 60 * 1000L
        CalendarLogic.sorted(read(startOfDay, end, excluded).events).filterNot { CalendarLogic.isOver(it, now) }.take(limit)
    }

    /** Permesso, calendari con i conteggi dei prossimi 14 giorni e come è andata la lettura (nessun testo personale). */
    suspend fun diagnostics(): CalendarDiagnostics = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext CalendarDiagnostics(false, emptyList())
        val now = System.currentTimeMillis()
        val result = read(now, CalendarLogic.windowEnd(now, CalendarLogic.DIAGNOSTIC_DAYS), emptySet())
        val all = result.events.groupingBy { it.calendarId }.eachCount()
        val allDay = result.events.filter { it.allDay }.groupingBy { it.calendarId }.eachCount()
        val calendars = readCalendars().first
        CalendarDiagnostics(true, calendars.map { it.copy(events14 = all[it.id] ?: 0, allDay14 = allDay[it.id] ?: 0) }, CalendarReadInfo(result.path, result.sources))
    }

    private fun readCalendars(): Pair<List<CalendarInfo>, SourceReport> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.SYNC_EVENTS,
            CalendarContract.Calendars.CALENDAR_COLOR,
        )
        val out = mutableListOf<CalendarInfo>()
        val report = try {
            context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC")?.use { c ->
                while (c.moveToNext()) {
                    out += CalendarInfo(c.getLong(0), c.getString(1).orEmpty(), c.getString(2).orEmpty(), c.getInt(3) == 1, c.getInt(4) == 1, c.getInt(5))
                }
            }
            SourceReport(CalendarSource.CALENDARS, out.size)
        } catch (t: Throwable) {
            SourceReport(CalendarSource.CALENDARS, out.size, CalendarReading.describeError(t))
        }
        return out to report
    }

    /**
     * L'UNICA lettura degli eventi tra [start] e [end], dei calendari non spenti in [excluded]; il resoconto dice da dove
     * vengono e se qualche query ha dato errore (nessuna eccezione viene inghiottita). Instances ha le occorrenze già
     * calcolate ma solo per i calendari visibili: per gli altri (e per tutti, se Instances dà errore o nessuna riga) si
     * legge la tabella Events e le ripetizioni si calcolano con [Recurrence]. Il colore viene dalla tabella dei calendari.
     */
    fun read(start: Long, end: Long, excluded: Set<Long>): CalendarRead {
        val (calendars, calendarsReport) = readCalendars()
        val colors = calendars.associate { it.id to it.color }
        val zone = java.time.ZoneId.systemDefault()
        val reports = mutableListOf(calendarsReport)
        val chosen = calendars.filter { it.id !in excluded }
        val visible = chosen.filter { it.visible }
        val instances = readInstances(start, end, zone, reports)
        val instancesReport = reports.lastOrNull { it.source == CalendarSource.INSTANCES }
        val fellBack = CalendarReading.needsFallback(visible.size, instancesReport)
        // Gli altri calendari, e quelli visibili se Instances non ha funzionato, dalla tabella Events
        val fromEvents = chosen.filter { !it.visible || fellBack }.map { it.id }
        val eventRows = if (fromEvents.isNotEmpty()) readEvents(fromEvents, start, end, zone, reports) else emptyList()
        val merged = CalendarReading.merge(instances, eventRows)
        val events = CalendarReading.withColors(CalendarLogic.filter(merged, excluded), colors)
        return CalendarRead(events, reports, CalendarReading.path(visible.size, instancesReport, fellBack))
    }

    /** Instances con la proiezione minima; se il provider rifiuta CALENDAR_ID si riprova senza e l'errore resta nel resoconto. */
    private fun readInstances(start: Long, end: Long, zone: java.time.ZoneId, reports: MutableList<SourceReport>): List<CalendarEvent> {
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, start)
            ContentUris.appendId(it, end)
        }.build()
        val base = arrayOf(
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_ID,
        )
        fun query(projection: Array<String>, withCalendar: Boolean): List<CalendarEvent> {
            val out = mutableListOf<CalendarEvent>()
            context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
                while (c.moveToNext()) {
                    val title = c.getString(2)?.takeIf { it.isNotBlank() } ?: continue
                    var begin = c.getLong(0)
                    var finish = c.getLong(1)
                    val allDay = c.getInt(3) == 1
                    if (allDay) CalendarLogic.allDayLocal(begin, finish, zone).let { (b, e) -> begin = b; finish = e }
                    out += CalendarEvent(title, begin, allDay, c.getLong(4), finish, 0, if (withCalendar) c.getLong(5) else 0L)
                }
            }
            return out
        }
        return try {
            query(base + CalendarContract.Instances.CALENDAR_ID, true).also { reports += SourceReport(CalendarSource.INSTANCES, it.size) }
        } catch (first: Throwable) {
            // Senza CALENDAR_ID gli eventi perdono il calendario (e il filtro per calendario): meglio di niente
            try {
                query(base, false).also { reports += SourceReport(CalendarSource.INSTANCES, it.size, CalendarReading.describeError(first) + " (riprovato senza CALENDAR_ID)") }
            } catch (second: Throwable) {
                reports += SourceReport(CalendarSource.INSTANCES, 0, CalendarReading.describeError(second))
                emptyList()
            }
        }
    }

    private fun readEvents(ids: List<Long>, start: Long, end: Long, zone: java.time.ZoneId, reports: MutableList<SourceReport>): List<CalendarEvent> {
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
        var rows = 0
        try {
            context.contentResolver.query(CalendarContract.Events.CONTENT_URI, projection, selection, arrayOf(end.toString(), start.toString()), null)?.use { c ->
                while (c.moveToNext()) {
                    rows += 1
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
                        if (e >= start && b < end) out += CalendarEvent(title, b, allDay, c.getLong(0), e, 0, calendarId)
                    }
                }
            }
            reports += SourceReport(CalendarSource.EVENTS, rows)
        } catch (t: Throwable) {
            reports += SourceReport(CalendarSource.EVENTS, rows, CalendarReading.describeError(t))
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
