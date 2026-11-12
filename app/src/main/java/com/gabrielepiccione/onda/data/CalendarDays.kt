// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Un evento di tutto il giorno in un certo giorno: «giorno N di M» se dura più giorni. */
data class DayLabel(val event: CalendarEvent, val dayIndex: Int, val dayCount: Int) {
    val multiDay: Boolean get() = dayCount > 1
}

/** Il contenuto di un giorno dell'agenda: prima gli eventi di tutto il giorno (etichette), poi quelli con l'ora. */
data class DayGroup(val date: LocalDate, val allDay: List<DayLabel>, val timed: List<CalendarEvent>)

/** Cosa mostrare sotto l'orologio: il prossimo evento con l'ora e le etichette di tutto il giorno di oggi. */
data class DayStrip(val timed: CalendarEvent?, val labels: List<DayLabel>) {
    val isEmpty: Boolean get() = timed == null && labels.isEmpty()
}

/**
 * Eventi di tutto il giorno e giorni locali, senza Android (testabile su JVM). Gli eventi di tutto il giorno arrivano
 * già a mezzanotte locale ([CalendarLogic.allDayLocal]): qui si ragiona sulle date, mai sui millisecondi, così un
 * evento non scivola di giorno in nessun fuso orario e con l'ora legale.
 */
object CalendarDays {
    /** Quante etichette stanno sotto l'orologio prima del «+N». */
    const val MAX_STRIP_LABELS = 2

    fun firstDay(e: CalendarEvent, zone: ZoneId): LocalDate = Instant.ofEpochMilli(e.begin).atZone(zone).toLocalDate()

    /** Giorni occupati da un evento di tutto il giorno (la fine è esclusa: a mezzanotte del giorno dopo). */
    fun dayCount(e: CalendarEvent, zone: ZoneId): Int {
        val first = firstDay(e, zone)
        val endExclusive = Instant.ofEpochMilli(e.end).atZone(zone).toLocalDate()
        return java.time.temporal.ChronoUnit.DAYS.between(first, endExclusive).toInt().coerceAtLeast(1)
    }

    /** Le etichette di tutto il giorno che cadono in [date], con «giorno N di M», per inizio e poi per titolo. */
    fun labelsOn(date: LocalDate, events: List<CalendarEvent>, zone: ZoneId): List<DayLabel> =
        events.filter { it.allDay }.mapNotNull { e ->
            val first = firstDay(e, zone)
            val count = dayCount(e, zone)
            val index = java.time.temporal.ChronoUnit.DAYS.between(first, date).toInt()
            if (index in 0 until count) DayLabel(e, index + 1, count) else null
        }.sortedWith(compareBy({ it.event.begin }, { it.event.title.lowercase() }))

    /** Le etichette di oggi e quelle che non stanno sotto l'orologio («+N»). */
    fun splitForStrip(labels: List<DayLabel>): Pair<List<DayLabel>, Int> =
        labels.take(MAX_STRIP_LABELS) to (labels.size - MAX_STRIP_LABELS).coerceAtLeast(0)

    /**
     * La riga sotto l'orologio: il prossimo evento con l'ora (il primo non ancora finito) e gli eventi di tutto il giorno
     * di oggi, secondo [mode]. Con solo eventi di tutto il giorno oggi, la riga mostra quelli.
     */
    fun strip(events: List<CalendarEvent>, nowMs: Long, zone: ZoneId, mode: AllDayMode): DayStrip {
        val timed = CalendarLogic.sorted(events).firstOrNull { !it.allDay && !CalendarLogic.isOver(it, nowMs) }
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val labels = when (mode) {
            AllDayMode.HIDE -> emptyList()
            AllDayMode.ONLY_IF_NONE -> if (timed == null) labelsOn(today, events, zone) else emptyList()
            AllDayMode.ALWAYS -> labelsOn(today, events, zone)
        }
        return DayStrip(timed, labels)
    }

    /**
     * L'agenda per giorno, da [from] per [days] giorni: in cima le etichette di tutto il giorno (un evento su più giorni
     * compare in ognuno), poi gli eventi con l'ora (nel giorno in cui iniziano, già finiti esclusi). Solo i giorni con qualcosa.
     */
    fun groups(events: List<CalendarEvent>, mode: AllDayMode, nowMs: Long, zone: ZoneId, from: LocalDate, days: Int): List<DayGroup> {
        val timedAll = CalendarLogic.sorted(events).filter { !it.allDay && !CalendarLogic.isOver(it, nowMs) }
        return (0 until days.coerceAtLeast(0)).mapNotNull { offset ->
            val date = from.plusDays(offset.toLong())
            val timed = timedAll.filter { firstDay(it, zone) == date }
            val labels = when (mode) {
                AllDayMode.HIDE -> emptyList()
                AllDayMode.ONLY_IF_NONE -> if (timed.isEmpty()) labelsOn(date, events, zone) else emptyList()
                AllDayMode.ALWAYS -> labelsOn(date, events, zone)
            }
            if (labels.isEmpty() && timed.isEmpty()) null else DayGroup(date, labels, timed)
        }
    }
}
