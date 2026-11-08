// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Blocco delle app a fasce orarie: nei giorni e nell'orario scelti le app elencate non si aprono.
 * [startMinute] e [endMinute] sono minuti dalla mezzanotte; se la fine precede l'inizio la fascia passa la
 * mezzanotte (la parte dopo mezzanotte conta per il giorno in cui la fascia è iniziata); se coincidono vale tutto il giorno.
 */
@Serializable
data class AppBlock(
    val id: String,
    val name: String,
    /** Chiavi delle app bloccate. */
    val apps: Set<String> = emptySet(),
    /** Giorni (1 = lunedì … 7 = domenica); vuoto = tutti. */
    val days: Set<Int> = emptySet(),
    val startMinute: Int = 22 * 60,
    val endMinute: Int = 7 * 60,
    /** Non si applica nei giorni festivi (calendario «Festività in Italia», o l'elenco interno). */
    val skipHolidays: Boolean = true,
    val enabled: Boolean = true,
)

/** Perché un'app è bloccata adesso: [until] è il minuto di fine della fascia (null = finché non la sblocchi). */
data class BlockHit(val name: String, val until: Int?)

/** Regole dei blocchi, senza dipendenze Android (testabili su JVM). */
object AppBlocks {
    private const val DAY = 24 * 60

    /** La fascia [b] è attiva alle [at], dato quali giorni sono festivi. */
    fun isActive(b: AppBlock, at: LocalDateTime, isHoliday: (LocalDate) -> Boolean): Boolean {
        if (!b.enabled || b.apps.isEmpty()) return false
        val minute = at.hour * 60 + at.minute
        val start = b.startMinute.coerceIn(0, DAY - 1)
        val end = b.endMinute.coerceIn(0, DAY - 1)
        val startDay: LocalDate = when {
            start == end -> at.toLocalDate()
            start < end -> if (minute in start until end) at.toLocalDate() else return false
            minute >= start -> at.toLocalDate()
            minute < end -> at.toLocalDate().minusDays(1)
            else -> return false
        }
        if (b.days.isNotEmpty() && startDay.dayOfWeek.value !in b.days) return false
        if (b.skipHolidays && isHoliday(startDay)) return false
        return true
    }

    /** Il blocco che vale adesso per l'app [appKey] (a mano o a fascia), o null. */
    fun hit(s: Settings, appKey: String, at: LocalDateTime, isHoliday: (LocalDate) -> Boolean): BlockHit? {
        if (appKey in s.manualBlocks) return BlockHit(name = "", until = null)
        val pkg = UsageReminders.packageOf(appKey)
        return s.appBlocks
            .firstOrNull { b -> b.apps.any { it == appKey || UsageReminders.packageOf(it) == pkg } && isActive(b, at, isHoliday) }
            ?.let { BlockHit(it.name, if (it.startMinute == it.endMinute) null else it.endMinute.coerceIn(0, DAY - 1)) }
    }

    /** Come [hit], ma da un nome di pacchetto (il servizio di accessibilità conosce solo quello). */
    fun hitForPackage(s: Settings, packageName: String, at: LocalDateTime, isHoliday: (LocalDate) -> Boolean): BlockHit? {
        s.manualBlocks.firstOrNull { UsageReminders.packageOf(it) == packageName }?.let { return BlockHit("", null) }
        return s.appBlocks
            .firstOrNull { b -> b.apps.any { UsageReminders.packageOf(it) == packageName } && isActive(b, at, isHoliday) }
            ?.let { BlockHit(it.name, if (it.startMinute == it.endMinute) null else it.endMinute.coerceIn(0, DAY - 1)) }
    }

    /** C'è almeno un blocco (a mano o a fascia acceso) da controllare. */
    fun any(s: Settings): Boolean = s.manualBlocks.isNotEmpty() || s.appBlocks.any { it.enabled && it.apps.isNotEmpty() }

    fun block(s: Settings, appKey: String): Settings = s.copy(manualBlocks = s.manualBlocks + appKey)

    fun unblock(s: Settings, appKey: String): Settings {
        val pkg = UsageReminders.packageOf(appKey)
        return s.copy(manualBlocks = s.manualBlocks.filterNot { it == appKey || UsageReminders.packageOf(it) == pkg }.toSet())
    }

    fun upsert(list: List<AppBlock>, b: AppBlock): List<AppBlock> =
        if (list.any { it.id == b.id }) list.map { if (it.id == b.id) b else it } else list + b

    fun remove(list: List<AppBlock>, id: String): List<AppBlock> = list.filterNot { it.id == id }

    /** Orario "HH:MM" da minuti dalla mezzanotte. */
    fun timeText(minute: Int): String = "%02d:%02d".format(minute.coerceIn(0, DAY - 1) / 60, minute.coerceIn(0, DAY - 1) % 60)
}
