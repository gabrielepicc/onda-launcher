// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Come mostrare un tempo trascorso. */
enum class AgeKind { NOW, MINUTES, HOURS, DATE }

/** Un tempo trascorso già classificato: «ora», «N min», «N h» o la data. */
data class Age(val kind: AgeKind, val value: Int = 0)

/**
 * Il tempo dell'interfaccia, senza Android (testabile su JVM): tempi relativi, prossimo tick, minuto del giorno,
 * cambio di giorno e di fuso. Tutto parte dall'ora reale di sistema, mai dall'uptime: dopo una sospensione o un
 * congelamento l'ora è comunque giusta.
 */
object UiTime {
    const val MINUTE_MS = 60_000L
    private const val HOUR_MINUTES = 60L
    private const val DAY_MINUTES = 24 * 60L

    /** Minuti trascorsi da [postTime] a [nowMs]; se l'orologio è stato spostato indietro non sono mai negativi. */
    fun elapsedMinutes(postTime: Long, nowMs: Long): Long = ((nowMs - postTime) / MINUTE_MS).coerceAtLeast(0L)

    /** «ora» sotto il minuto, poi minuti, poi ore fino a un giorno, poi la data. */
    fun age(postTime: Long, nowMs: Long): Age {
        val minutes = elapsedMinutes(postTime, nowMs)
        return when {
            minutes < 1 -> Age(AgeKind.NOW)
            minutes < HOUR_MINUTES -> Age(AgeKind.MINUTES, minutes.toInt())
            minutes < DAY_MINUTES -> Age(AgeKind.HOURS, (minutes / HOUR_MINUTES).toInt())
            else -> Age(AgeKind.DATE)
        }
    }

    /**
     * Quanto aspettare, dall'ora di sistema [nowMs], per il prossimo tick di periodo [periodMs] (un minuto, o un secondo
     * con i secondi accesi): fino al prossimo multiplo più un piccolo margine, mai più di un periodo. Si ricalcola a ogni giro.
     */
    fun nextTickDelayMs(nowMs: Long, periodMs: Long = MINUTE_MS, marginMs: Long = 30L): Long {
        val period = periodMs.coerceAtLeast(1L)
        val untilBoundary = period - Math.floorMod(nowMs, period)
        return (untilBoundary + marginMs).coerceAtMost(period)
    }

    fun localDateTime(nowMs: Long, zone: ZoneId): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMs), zone)

    fun localDate(nowMs: Long, zone: ZoneId): LocalDate = localDateTime(nowMs, zone).toLocalDate()

    /** Minuti dalla mezzanotte locale. */
    fun minuteOfDay(nowMs: Long, zone: ZoneId): Int = localDateTime(nowMs, zone).let { it.hour * 60 + it.minute }

    /** Il giorno locale è cambiato tra [prevMs] e [nowMs] (anche per un cambio di fuso o di data a mano). */
    fun dayChanged(prevMs: Long, nowMs: Long, zone: ZoneId): Boolean = localDate(prevMs, zone) != localDate(nowMs, zone)

    /** L'orologio di sistema è stato spostato: indietro, o avanti di molto più del tempo di un tick normale. */
    fun clockJumped(prevMs: Long, nowMs: Long, maxNormalGapMs: Long = 2 * MINUTE_MS): Boolean =
        nowMs < prevMs || nowMs - prevMs > maxNormalGapMs
}
