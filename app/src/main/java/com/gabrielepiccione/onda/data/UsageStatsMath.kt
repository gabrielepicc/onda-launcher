// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Tipo di evento letto dai dati di utilizzo di Android, già tradotto. */
enum class UsageKind { FOREGROUND, BACKGROUND, SCREEN_ON, SCREEN_OFF, UNLOCK }

/** Un evento dei dati di utilizzo: quando, quale app (vuoto per lo schermo) e cosa è successo. */
data class UsageRecord(val time: Long, val packageName: String, val kind: UsageKind)

/** Uso di un'app in un giorno. */
data class AppDayUsage(
    val packageName: String,
    val foregroundMs: Long,
    val launches: Int,
    val sessions: Int,
    val longestSessionMs: Long,
)

/** Una giornata di uso del telefono, già riassunta. */
data class DayUsage(
    val dayStart: Long,
    val screenMs: Long,
    val unlocks: Int,
    val launches: Int,
    val apps: List<AppDayUsage>,
    /** Millisecondi di uso in ciascuna delle 24 ore. */
    val hourlyMs: List<Long>,
    /** Primo uso della giornata (sblocco o app), se c'è stato. */
    val firstUse: Long?,
    /** Durata di ogni sessione d'uso di un'app, per mediana e controlli rapidi. */
    val sessionLengths: List<Long>,
    /** Uso tra mezzanotte e le 6. */
    val nightMs: Long,
) {
    val shortChecks: Int get() = sessionLengths.count { it < UsageStatsMath.SHORT_CHECK_MS }
}

/** Riassunto di più giorni e confronto con il periodo prima. */
data class PeriodSummary(
    val days: Int,
    val averageMs: Long,
    val totalMs: Long,
    val medianSessionMs: Long,
    val busiestDayStart: Long?,
    val quietestDayStart: Long?,
    /** Variazione percentuale della media rispetto al periodo prima; null se non ci sono dati per confrontare. */
    val changePercent: Int?,
    val averageUnlocks: Int,
    val averageLaunches: Int,
)

/**
 * Statistiche d'uso del telefono, senza dipendenze Android (testabili su JVM). Le regole sono
 * quelle che si aspetta chi usa il benessere digitale di sistema: il launcher stesso non conta,
 * un'app che cambia schermata non è una nuova apertura, lo schermo spento chiude la sessione.
 */
object UsageStatsMath {
    const val HOUR_MS = 3_600_000L
    const val DAY_MS = 24 * HOUR_MS

    /** Sotto il minuto una sessione è un "controllo rapido": aprire, guardare, richiudere. */
    const val SHORT_CHECK_MS = 60_000L

    /** Due sessioni della stessa app separate da meno di così sono una sola (cambio di schermata). */
    const val MERGE_MS = 2_000L

    /** Fino a che ora conta come uso notturno. */
    const val NIGHT_END_HOUR = 6

    /**
     * Riassume una giornata da [dayStart] a [dayStart] + 24 ore (o fino a [now], se è oggi). Gli eventi
     * possono iniziare prima: servono a sapere quale app era aperta a mezzanotte.
     */
    fun day(records: List<UsageRecord>, dayStart: Long, now: Long, ignored: Set<String>): DayUsage {
        val dayEnd = min(dayStart + DAY_MS, now)
        val perApp = HashMap<String, MutableAppUsage>()
        val hourly = LongArray(24)
        val sessions = ArrayList<Long>()
        var unlocks = 0
        var firstUse: Long? = null
        var nightMs = 0L

        var openPkg: String? = null
        var openSince = 0L
        // Ultima sessione chiusa: se la stessa app torna subito, è la stessa sessione
        var lastPkg: String? = null
        var lastEnd = Long.MIN_VALUE
        var lastSessionStart = 0L

        fun account(pkg: String, from: Long, to: Long) {
            val start = max(from, dayStart)
            val end = min(to, dayEnd)
            if (end <= start) return
            val usage = perApp.getOrPut(pkg) { MutableAppUsage() }
            usage.ms += end - start
            var cursor = start
            while (cursor < end) {
                val hour = ((cursor - dayStart) / HOUR_MS).toInt().coerceIn(0, 23)
                val hourEnd = min(end, dayStart + (hour + 1) * HOUR_MS)
                hourly[hour] += hourEnd - cursor
                if (hour < NIGHT_END_HOUR) nightMs += hourEnd - cursor
                cursor = hourEnd
            }
        }

        fun close(at: Long) {
            val pkg = openPkg ?: return
            account(pkg, openSince, at)
            lastPkg = pkg
            lastEnd = at
            openPkg = null
        }

        fun finishSession() {
            val pkg = lastPkg ?: return
            val length = min(lastEnd, dayEnd) - max(lastSessionStart, dayStart)
            if (length > 0) {
                sessions += length
                perApp.getOrPut(pkg) { MutableAppUsage() }.apply {
                    this.sessions += 1
                    longest = max(longest, length)
                }
            }
            lastPkg = null
        }

        for (r in records.sortedBy { it.time }) {
            if (r.time > dayEnd) break
            val counted = r.packageName !in ignored
            when (r.kind) {
                UsageKind.FOREGROUND -> {
                    if (openPkg == r.packageName) continue
                    close(r.time)
                    if (!counted) {
                        finishSession()
                        continue
                    }
                    val sameSession = lastPkg == r.packageName && r.time - lastEnd < MERGE_MS
                    if (!sameSession) {
                        finishSession()
                        lastSessionStart = r.time
                        if (r.time >= dayStart) perApp.getOrPut(r.packageName) { MutableAppUsage() }.launches += 1
                    }
                    openPkg = r.packageName
                    openSince = r.time
                    if (r.time >= dayStart && firstUse == null) firstUse = r.time
                }
                UsageKind.BACKGROUND -> if (openPkg == r.packageName) close(r.time)
                UsageKind.SCREEN_OFF -> {
                    close(r.time)
                    finishSession()
                }
                UsageKind.SCREEN_ON -> Unit
                UsageKind.UNLOCK -> if (r.time >= dayStart) {
                    unlocks += 1
                    if (firstUse == null) firstUse = r.time
                }
            }
        }
        // Ciò che è ancora aperto conta fino a fine giornata (o adesso)
        if (openPkg != null) close(dayEnd)
        finishSession()

        val apps = perApp.map { (pkg, u) -> AppDayUsage(pkg, u.ms, u.launches, u.sessions, u.longest) }
            .filter { it.foregroundMs > 0 || it.launches > 0 }
            .sortedByDescending { it.foregroundMs }
        return DayUsage(
            dayStart = dayStart,
            screenMs = apps.sumOf { it.foregroundMs },
            unlocks = unlocks,
            launches = apps.sumOf { it.launches },
            apps = apps,
            hourlyMs = hourly.toList(),
            firstUse = firstUse,
            sessionLengths = sessions,
            nightMs = nightMs,
        )
    }

    /** Riassume [days] (dal più vecchio al più recente) confrontandoli con [previous], il periodo subito prima. */
    fun summary(days: List<DayUsage>, previous: List<DayUsage> = emptyList()): PeriodSummary {
        val total = days.sumOf { it.screenMs }
        val average = if (days.isEmpty()) 0L else total / days.size
        val withData = days.filter { it.screenMs > 0 }
        val previousWithData = previous.filter { it.screenMs > 0 }
        val previousAverage = if (previousWithData.isEmpty()) null else previousWithData.sumOf { it.screenMs } / previousWithData.size
        val currentAverage = if (withData.isEmpty()) 0L else withData.sumOf { it.screenMs } / withData.size
        return PeriodSummary(
            days = days.size,
            averageMs = average,
            totalMs = total,
            medianSessionMs = median(days.flatMap { it.sessionLengths }),
            busiestDayStart = withData.maxByOrNull { it.screenMs }?.dayStart,
            quietestDayStart = withData.minByOrNull { it.screenMs }?.dayStart,
            changePercent = previousAverage?.let { percentChange(currentAverage, it) },
            averageUnlocks = if (days.isEmpty()) 0 else days.sumOf { it.unlocks } / days.size,
            averageLaunches = if (days.isEmpty()) 0 else days.sumOf { it.launches } / days.size,
        )
    }

    /** Variazione percentuale arrotondata; null se non c'è una base da cui partire. */
    fun percentChange(current: Long, base: Long): Int? =
        if (base <= 0L) null else ((current - base) * 100.0 / base).roundToInt()

    fun median(values: List<Long>): Long {
        if (values.isEmpty()) return 0L
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
    }

    /** Media di uso per ora del giorno su più giorni, per vedere le abitudini e non un giorno solo. */
    fun averageHourly(days: List<DayUsage>): List<Long> =
        if (days.isEmpty()) List(24) { 0L } else List(24) { h -> days.sumOf { it.hourlyMs[h] } / days.size }

    /** Uso di un'app per giorno, nello stesso ordine di [days]. */
    fun appTrend(days: List<DayUsage>, packageName: String): List<Long> =
        days.map { d -> d.apps.firstOrNull { it.packageName == packageName }?.foregroundMs ?: 0L }

    /** Quota del totale, da 0 a 1. */
    fun share(part: Long, total: Long): Float = if (total <= 0L) 0f else (part.toFloat() / total).coerceIn(0f, 1f)

    /** Vero se l'app ha superato il limite di [limitMinutes] (0 = nessun limite). */
    fun overLimit(usedMs: Long, limitMinutes: Int): Boolean = limitMinutes > 0 && usedMs >= limitMinutes * 60_000L

    private class MutableAppUsage {
        var ms = 0L
        var launches = 0
        var sessions = 0
        var longest = 0L
    }
}
