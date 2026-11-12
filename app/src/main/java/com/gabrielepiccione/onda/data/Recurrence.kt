// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Occorrenze di un evento ricorrente letto dalla tabella Events (per i calendari che Android non espande in Instances,
 * cioè quelli non marcati visibili). Supporta FREQ=DAILY/WEEKLY(BYDAY)/MONTHLY/YEARLY con INTERVAL, COUNT e UNTIL;
 * le regole più complesse (BYDAY con ordinale, BYSETPOS, eccezioni) si leggono come la versione semplice.
 * Logica pura, testabile su JVM.
 */
object Recurrence {
    private const val MAX_STEPS = 5_000
    private val DAYS = mapOf("MO" to DayOfWeek.MONDAY, "TU" to DayOfWeek.TUESDAY, "WE" to DayOfWeek.WEDNESDAY, "TH" to DayOfWeek.THURSDAY, "FR" to DayOfWeek.FRIDAY, "SA" to DayOfWeek.SATURDAY, "SU" to DayOfWeek.SUNDAY)

    private class Rule(val freq: String, val interval: Int, val count: Int?, val untilMs: Long?, val byDay: List<DayOfWeek>)

    private fun parse(rrule: String): Rule? {
        val parts = rrule.removePrefix("RRULE:").split(';').mapNotNull { p -> p.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0].trim().uppercase() to it[1].trim() } }.toMap()
        val freq = parts["FREQ"]?.uppercase() ?: return null
        if (freq !in setOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")) return null
        val byDay = parts["BYDAY"]?.split(',')?.mapNotNull { DAYS[it.trim().takeLast(2).uppercase()] }.orEmpty()
        return Rule(freq, (parts["INTERVAL"]?.toIntOrNull() ?: 1).coerceAtLeast(1), parts["COUNT"]?.toIntOrNull(), parts["UNTIL"]?.let(::parseUntil), byDay)
    }

    private fun parseUntil(text: String): Long? = runCatching {
        if (text.length >= 15) LocalDateTime.parse(text.removeSuffix("Z"), DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")).toInstant(ZoneOffset.UTC).toEpochMilli()
        else LocalDate.parse(text.take(8), DateTimeFormatter.BASIC_ISO_DATE).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() - 1
    }.getOrNull()

    /**
     * Gli inizi delle occorrenze (ms) che toccano [windowStartMs, windowEndMs): [startMs] è il primo inizio,
     * [durationMs] la durata, [zone] il fuso in cui l'ora resta uguale (per gli eventi di tutto il giorno: UTC).
     */
    fun occurrences(startMs: Long, durationMs: Long, rrule: String?, zone: ZoneId, windowStartMs: Long, windowEndMs: Long, max: Int = 200): List<Long> {
        fun touches(begin: Long) = begin < windowEndMs && begin + durationMs > windowStartMs
        if (rrule.isNullOrBlank()) return if (touches(startMs)) listOf(startMs) else emptyList()
        val rule = parse(rrule) ?: return if (touches(startMs)) listOf(startMs) else emptyList()
        val first = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(startMs), zone)
        val time: LocalTime = first.toLocalTime()
        val out = mutableListOf<Long>()
        var produced = 0
        var step = 0
        fun at(date: LocalDate) = ZonedDateTime.of(date, time, zone).toInstant().toEpochMilli()
        // Le date candidate di ogni passo, in ordine; si ferma a COUNT, UNTIL, fine finestra o tetto di sicurezza
        while (step < MAX_STEPS && out.size < max) {
            val dates: List<LocalDate> = when (rule.freq) {
                "DAILY" -> listOf(first.toLocalDate().plusDays(step.toLong() * rule.interval))
                "WEEKLY" -> {
                    val monday = first.toLocalDate().minusDays((first.dayOfWeek.value - 1).toLong()).plusWeeks(step.toLong() * rule.interval)
                    (rule.byDay.ifEmpty { listOf(first.dayOfWeek) }).sortedBy { it.value }.map { monday.plusDays((it.value - 1).toLong()) }
                }
                "MONTHLY" -> {
                    val month = first.toLocalDate().withDayOfMonth(1).plusMonths(step.toLong() * rule.interval)
                    if (first.dayOfMonth <= month.lengthOfMonth()) listOf(month.withDayOfMonth(first.dayOfMonth)) else emptyList()
                }
                else -> {
                    val year = first.year + step * rule.interval
                    runCatching { listOf(LocalDate.of(year, first.monthValue, first.dayOfMonth)) }.getOrDefault(emptyList())
                }
            }
            step += 1
            for (date in dates) {
                val begin = at(date)
                if (begin < startMs) continue
                if (rule.untilMs != null && begin > rule.untilMs) return out
                produced += 1
                if (rule.count != null && produced > rule.count) return out
                if (begin >= windowEndMs) return out
                if (touches(begin)) out += begin
            }
        }
        return out
    }
}
