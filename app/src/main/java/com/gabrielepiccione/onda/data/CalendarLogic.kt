// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Quanti eventi ha trovato una sorgente per un calendario: oggi, nei prossimi 14 giorni e, di questi, di tutto il giorno. */
data class SourceCounts(val today: Int = 0, val days14: Int = 0, val allDay14: Int = 0)

/** Un calendario del telefono, con quanti eventi ha nei prossimi 14 giorni. */
data class CalendarInfo(
    val id: Long,
    val name: String,
    val account: String,
    val visible: Boolean,
    val synced: Boolean,
    val color: Int,
    /** Eventi trovati uniti dalle due sorgenti (quelli che si vedono davvero). */
    val events14: Int = 0,
    /** Di questi, quanti di tutto il giorno. */
    val allDay14: Int = 0,
    /** Tipo di account (com.google, com.microsoft.exchange, LOCAL…): non è un dato personale, a differenza dell'account. */
    val accountType: String = "",
    /** Livello di accesso di Android (0 nessuno … 700 proprietario), -1 se non si legge. */
    val access: Int = -1,
    /** È il calendario principale dell'account; null se il telefono non lo dice. */
    val primary: Boolean? = null,
    /** Cosa ha trovato Instances e cosa la tabella Events, separatamente: così si vede dove si perdono gli eventi. */
    val instances: SourceCounts = SourceCounts(),
    val events: SourceCounts = SourceCounts(),
)

/** Stato della lettura del calendario: permesso e calendari trovati. */
data class CalendarDiagnostics(
    val permission: Boolean,
    val calendars: List<CalendarInfo>,
    val read: CalendarReadInfo? = null,
    val probe: CalendarProbe? = null,
    /** Produttore e versione di Android, per le particolarità del calendario di certi telefoni. */
    val device: String = "",
)

/** Perché non compaiono eventi, nell'ordine in cui conviene sistemarlo. */
enum class CalendarProblem { NO_PERMISSION, NO_CALENDARS, NONE_SELECTED, NONE_VISIBLE, NOT_SYNCED, NO_EVENTS }

/** Finestra temporale, filtro per calendari, ordinamento ed eventi di tutto il giorno: senza Android (testabile su JVM). */
object CalendarLogic {
    private const val HOUR_MS = 3_600_000L
    private const val DAY_MS = 24 * HOUR_MS

    /** Giorni scelti per l'evento sotto l'orologio. */
    val NEXT_EVENT_CHOICES = listOf(1, 3, 7)
    const val DIAGNOSTIC_DAYS = 14

    /** Nome del livello di accesso (CALENDAR_ACCESS_LEVEL di Android). */
    fun accessName(level: Int): String = when {
        level < 0 -> "?"
        level >= 800 -> "root"
        level >= 700 -> "owner"
        level >= 600 -> "editor"
        level >= 500 -> "contributor"
        level >= 400 -> "override"
        level >= 300 -> "respond"
        level >= 200 -> "read"
        level >= 100 -> "freebusy"
        else -> "none"
    }

    /** Quanti eventi ha una sorgente: oggi (con l'ora e di tutto il giorno che cadono oggi), nella finestra e di tutto il giorno. */
    fun counts(events: List<CalendarEvent>, today: java.time.LocalDate, zone: ZoneId): SourceCounts {
        val todayCount = events.count { e ->
            if (e.allDay) CalendarDays.labelsOn(today, listOf(e), zone).isNotEmpty() else CalendarDays.firstDay(e, zone) == today
        }
        return SourceCounts(todayCount, events.size, events.count { it.allDay })
    }

    /** Quanti calendari sono inclusi: tutti, tranne quelli che l'utente ha spento a mano (il valore di partenza è «tutti»). */
    fun includedCount(calendars: List<CalendarInfo>, excluded: Set<Long>): Int = calendars.count { it.id !in excluded }

    fun isIncluded(calendarId: Long, excluded: Set<Long>): Boolean = calendarId !in excluded

    /** Gli eventi tra la finestra [startMs, endMs) : un evento che finisce prima o inizia dopo non conta. */
    fun inRange(events: List<CalendarEvent>, startMs: Long, endMs: Long): List<CalendarEvent> =
        events.filter { it.begin < endMs && it.end >= startMs }

    /** Gli eventi già iniziati da poco restano in vista: la finestra parte qualche ora prima di adesso. */
    const val PAST_MARGIN_MS = 6 * HOUR_MS

    fun nextEventDays(value: Int): Int = if (value in NEXT_EVENT_CHOICES) value else NEXT_EVENT_CHOICES.first()

    fun windowStart(nowMs: Long): Long = nowMs - PAST_MARGIN_MS
    fun windowEnd(nowMs: Long, days: Int): Long = nowMs + days.coerceIn(1, DIAGNOSTIC_DAYS) * DAY_MS

    /** Toglie gli eventi dei calendari spenti (un evento senza calendario noto resta). */
    fun filter(events: List<CalendarEvent>, excluded: Set<Long>): List<CalendarEvent> =
        if (excluded.isEmpty()) events else events.filter { it.calendarId == 0L || it.calendarId !in excluded }

    /** Per inizio; a pari inizio prima gli eventi con l'ora, poi quelli di tutto il giorno; poi per titolo. */
    fun sorted(events: List<CalendarEvent>): List<CalendarEvent> =
        events.sortedWith(compareBy<CalendarEvent>({ it.begin }, { it.allDay }, { it.title.lowercase() }))

    /** Un evento è finito quando la sua fine è passata (quelli di tutto il giorno finiscono a mezzanotte, ora locale). */
    fun isOver(event: CalendarEvent, nowMs: Long): Boolean = if (event.allDay) event.end <= nowMs else event.end < nowMs

    /** Il prossimo evento: il primo con l'ora non ancora finito, altrimenti il primo di tutto il giorno. */
    fun next(events: List<CalendarEvent>, nowMs: Long): CalendarEvent? {
        val open = sorted(events).filterNot { isOver(it, nowMs) }
        return open.firstOrNull { !it.allDay } ?: open.firstOrNull()
    }

    /**
     * Gli eventi di tutto il giorno arrivano con inizio e fine a mezzanotte UTC: nel fuso locale cadrebbero nel giorno
     * sbagliato (o a metà giornata). Qui diventano la mezzanotte locale dello stesso giorno di calendario.
     */
    fun allDayLocal(beginUtcMs: Long, endUtcMs: Long, zone: ZoneId): Pair<Long, Long> {
        fun local(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        return local(beginUtcMs) to local(endUtcMs)
    }

    /** Durata di un evento ricorrente dal campo DURATION di Android («PT1H», «P3600S», «P1D»); null se non si legge. */
    fun parseDurationMs(text: String?): Long? {
        val raw = text?.trim()?.takeIf { it.startsWith("P") } ?: return null
        // Android scrive anche «P3600S» (senza T): java.time vuole «PT3600S»
        val normalized = if (raw.contains('T') || !raw.endsWith("S")) raw else "PT" + raw.removePrefix("P")
        return runCatching { java.time.Duration.parse(normalized).toMillis() }.getOrNull()
    }

    /**
     * Il motivo per cui non si vede nulla, o null se gli eventi ci sono. [calendars] ha i conteggi a 14 giorni;
     * [excluded] sono i calendari spenti dall'utente.
     */
    fun diagnose(permission: Boolean, calendars: List<CalendarInfo>, excluded: Set<Long>): CalendarProblem? {
        if (!permission) return CalendarProblem.NO_PERMISSION
        if (calendars.isEmpty()) return CalendarProblem.NO_CALENDARS
        val chosen = calendars.filter { it.id !in excluded }
        if (chosen.isEmpty()) return CalendarProblem.NONE_SELECTED
        if (chosen.sumOf { it.events14 } > 0) return null
        return when {
            chosen.none { it.visible } -> CalendarProblem.NONE_VISIBLE
            chosen.none { it.synced } -> CalendarProblem.NOT_SYNCED
            else -> CalendarProblem.NO_EVENTS
        }
    }
}
