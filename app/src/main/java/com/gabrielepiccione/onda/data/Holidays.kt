// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import java.time.LocalDate
import java.time.Month

/**
 * Giorni festivi, senza dipendenze Android (testabili su JVM). La fonte è il calendario «Festività in Italia» di
 * Google, se è sul telefono e il permesso del calendario c'è; altrimenti vale l'elenco delle festività nazionali
 * calcolato qui (date fisse più Pasquetta).
 */
object Holidays {
    /** Festività nazionali a data fissa: (mese, giorno). */
    private val FIXED = listOf(
        Month.JANUARY to 1, Month.JANUARY to 6, Month.APRIL to 25, Month.MAY to 1, Month.JUNE to 2,
        Month.AUGUST to 15, Month.NOVEMBER to 1, Month.DECEMBER to 8, Month.DECEMBER to 25, Month.DECEMBER to 26,
    )

    /** Domenica di Pasqua (algoritmo di Gauss/«computus» anonimo gregoriano). */
    fun easter(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }

    /** Festività nazionali italiane: date fisse e lunedì dell'Angelo (Pasquetta). */
    fun builtInItalian(date: LocalDate): Boolean =
        FIXED.any { (m, d) -> date.month == m && date.dayOfMonth == d } || date == easter(date.year).plusDays(1)

    /**
     * Il giorno è festivo? [fromCalendar] sono le date che il calendario delle festività segna (null = calendario
     * non trovato o permesso mancante: si usa l'elenco interno).
     */
    fun isHoliday(date: LocalDate, fromCalendar: Set<LocalDate>?): Boolean =
        if (fromCalendar != null) date in fromCalendar else builtInItalian(date)

    /** Riconosce il calendario delle festività italiane di Google dal proprietario o dal nome. */
    fun isItalianHolidayCalendar(ownerAccount: String?, displayName: String?): Boolean {
        val owner = ownerAccount.orEmpty().lowercase()
        val name = displayName.orEmpty().trim().lowercase()
        val googleHoliday = owner.endsWith("#holiday@group.v.calendar.google.com") && (owner.startsWith("it.") || owner.contains("italian"))
        return googleHoliday || name == "festività in italia" || name == "holidays in italy"
    }
}
