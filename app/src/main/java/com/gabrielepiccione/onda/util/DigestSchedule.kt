// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

/**
 * Orari del riepilogo notifiche, espressi in minuti dall'inizio della giornata (0–1439).
 * Funzioni pure, senza Android: testabili su JVM.
 */
object DigestSchedule {

    const val DAY = 24 * 60

    /** Orari proposti nelle impostazioni. */
    val PRESETS = listOf(7 * 60, 9 * 60, 12 * 60, 13 * 60 + 30, 18 * 60, 19 * 60, 21 * 60)

    /** Minuti che mancano al prossimo orario (sempre > 0), o null se non ci sono orari. */
    fun minutesUntilNext(nowMinute: Int, slots: Collection<Int>): Int? {
        val valid = slots.filter { it in 0 until DAY }.distinct()
        if (valid.isEmpty()) return null
        // Un orario che coincide con adesso conta come "domani" (tra 24 ore)
        return valid.minOf { slot -> (((slot - nowMinute) % DAY + DAY) % DAY).let { if (it == 0) DAY else it } }
    }

    /** Prossimo orario (minuti del giorno), o null se non ci sono orari. */
    fun nextSlot(nowMinute: Int, slots: Collection<Int>): Int? =
        minutesUntilNext(nowMinute, slots)?.let { (nowMinute + it) % DAY }

    /** Minuti trascorsi dall'ultimo orario passato (utile per non riposticipare ciò che esce ora). */
    fun minutesSinceLast(nowMinute: Int, slots: Collection<Int>): Int? {
        val valid = slots.filter { it in 0 until DAY }.distinct()
        if (valid.isEmpty()) return null
        return valid.minOf { slot -> ((nowMinute - slot) % DAY + DAY) % DAY }
    }

    fun format(minute: Int): String = String.format(java.util.Locale.ROOT, "%d:%02d", minute / 60, minute % 60)
}
