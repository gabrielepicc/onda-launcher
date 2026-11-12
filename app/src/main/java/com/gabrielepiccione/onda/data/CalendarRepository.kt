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

    /**
     * Permesso, calendari con i conteggi per sorgente (Instances e tabella Events, separati), come è andata la lettura e
     * la prova di lettura in tre passi. Nessun testo personale: solo conteggi, tipi di account, date ed errori.
     */
    suspend fun diagnostics(): CalendarDiagnostics = withContext(Dispatchers.IO) {
        val device = "${android.os.Build.MANUFACTURER} Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})"
        if (!hasPermission()) return@withContext CalendarDiagnostics(false, emptyList(), device = device)
        val zone = java.time.ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val today = java.time.LocalDate.now()
        val startOfToday = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfToday = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end14 = CalendarLogic.windowEnd(now, CalendarLogic.DIAGNOSTIC_DAYS)

        // La lettura vera (unione delle sorgenti, calendari spenti esclusi dalle impostazioni non si conoscono qui: nessun filtro)
        val result = read(now, end14, emptySet())
        val (calendars, _) = readCalendars()

        // Prova di lettura: tre passi separati, ognuno con righe ed eccezione
        val steps = mutableListOf<ProbeResult>()
        fun instancesStep(step: ProbeStep, start: Long, end: Long): List<CalendarEvent> {
            val reports = mutableListOf<SourceReport>()
            val events = readInstances(start, end, zone, reports)
            val r = reports.last()
            steps += ProbeResult(step, r.rows, r.error)
            return events
        }
        instancesStep(ProbeStep.INSTANCES_TODAY, startOfToday, endOfToday)
        val instances14 = instancesStep(ProbeStep.INSTANCES_14, now, end14)
        val eventsReports = mutableListOf<SourceReport>()
        val events14 = readEvents(null, now, end14, zone, eventsReports)
        steps += ProbeResult(ProbeStep.EVENTS_14_NO_FILTER, eventsReports.last().rows, eventsReports.last().error)
        val table = eventsTableInfo(now)

        val byInstances = instances14.groupBy { it.calendarId }
        val byEvents = events14.groupBy { it.calendarId }
        val merged = result.events.groupBy { it.calendarId }
        val info = calendars.map { c ->
            c.copy(
                events14 = merged[c.id].orEmpty().size,
                allDay14 = merged[c.id].orEmpty().count { it.allDay },
                instances = CalendarLogic.counts(byInstances[c.id].orEmpty(), today, zone),
                events = CalendarLogic.counts(byEvents[c.id].orEmpty(), today, zone),
            )
        }
        CalendarDiagnostics(true, info, CalendarReadInfo(result.path, result.sources), CalendarProbe(steps, table.first, table.second, table.third), device)
    }

    /** Righe totali della tabella Events e giorno dell'evento futuro più vicino (solo la data, mai il titolo). */
    private fun eventsTableInfo(now: Long): Triple<Int, String?, String?> {
        var total = 0
        var nearest: String? = null
        var error: String? = null
        try {
            context.contentResolver.query(CalendarContract.Events.CONTENT_URI, arrayOf(CalendarContract.Events._ID), null, null, null)?.use { total = it.count }
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI, arrayOf(CalendarContract.Events.DTSTART),
                "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DELETED} = 0", arrayOf(now.toString()), "${CalendarContract.Events.DTSTART} ASC",
            )?.use { c ->
                if (c.moveToFirst()) nearest = java.time.Instant.ofEpochMilli(c.getLong(0)).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
            }
        } catch (t: Throwable) {
            error = CalendarReading.describeError(t)
        }
        return Triple(total, nearest, error)
    }

    private fun readCalendars(): Pair<List<CalendarInfo>, SourceReport> {
        val basic = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.SYNC_EVENTS,
            CalendarContract.Calendars.CALENDAR_COLOR,
        )
        // Tipo di account, livello di accesso e «principale» sono in più: se il telefono li rifiuta si riparte dalla base
        val extended = basic + arrayOf(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, "isPrimary")
        fun query(projection: Array<String>, wide: Boolean): List<CalendarInfo> {
            val out = mutableListOf<CalendarInfo>()
            context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC")?.use { c ->
                while (c.moveToNext()) {
                    out += CalendarInfo(
                        c.getLong(0), c.getString(1).orEmpty(), c.getString(2).orEmpty(), c.getInt(3) == 1, c.getInt(4) == 1, c.getInt(5),
                        accountType = if (wide) c.getString(6).orEmpty() else "",
                        access = if (wide) c.getInt(7) else -1,
                        primary = if (wide && !c.isNull(8)) c.getInt(8) == 1 else null,
                    )
                }
            }
            return out
        }
        return try {
            val list = query(extended, true)
            list to SourceReport(CalendarSource.CALENDARS, list.size)
        } catch (first: Throwable) {
            try {
                val list = query(basic, false)
                list to SourceReport(CalendarSource.CALENDARS, list.size, CalendarReading.describeError(first) + " (riprovato con meno colonne)")
            } catch (second: Throwable) {
                emptyList<CalendarInfo>() to SourceReport(CalendarSource.CALENDARS, 0, CalendarReading.describeError(second))
            }
        }
    }

    /**
     * L'UNICA lettura degli eventi tra [start] e [end], dei calendari non spenti in [excluded]. Le due sorgenti si UNISCONO
     * (non una al posto dell'altra): Instances ha le occorrenze già calcolate, la tabella Events i calendari che Instances
     * non include (e gli eventi che non ha) con le ripetizioni calcolate da [Recurrence]; [CalendarReading.merge] toglie i
     * doppioni. Il resoconto dice da dove vengono e se qualche query ha dato errore (nessuna eccezione viene inghiottita).
     * Il colore viene dalla tabella dei calendari; gli eventi di tutto il giorno seguono il giorno locale.
     */
    fun read(start: Long, end: Long, excluded: Set<Long>): CalendarRead {
        val (calendars, calendarsReport) = readCalendars()
        val colors = calendars.associate { it.id to it.color }
        val zone = java.time.ZoneId.systemDefault()
        val reports = mutableListOf(calendarsReport)
        val instances = readInstances(start, end, zone, reports)
        // Tutti i calendari tranne quelli spenti a mano; se l'elenco dei calendari non si legge, nessun filtro
        val ids = if (calendars.isEmpty()) null else calendars.filter { CalendarLogic.isIncluded(it.id, excluded) }.map { it.id }
        val eventRows = if (ids == null || ids.isNotEmpty()) readEvents(ids, start, end, zone, reports) else emptyList()
        val merged = CalendarReading.merge(instances, eventRows)
        val events = CalendarReading.withColors(CalendarLogic.filter(merged, excluded), colors)
        val path = CalendarReading.path(
            reports.last { it.source == CalendarSource.INSTANCES }.rows,
            reports.lastOrNull { it.source == CalendarSource.EVENTS }?.rows ?: 0,
        )
        return CalendarRead(events, reports, path)
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

    private fun readEvents(ids: List<Long>?, start: Long, end: Long, zone: java.time.ZoneId, reports: MutableList<SourceReport>): List<CalendarEvent> {
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
        val calendarFilter = ids?.let { "${CalendarContract.Events.CALENDAR_ID} IN (${it.joinToString(",")}) AND " }.orEmpty()
        val selection = calendarFilter + "${CalendarContract.Events.DELETED} = 0 AND " +
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
