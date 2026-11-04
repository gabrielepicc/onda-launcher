// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

/**
 * Ora "a parole", arrotondata ai 5 minuti più vicini.
 * Italiano: "Sono le dieci e un quarto", "È l'una meno venti", "È mezzogiorno".
 * Inglese:  "It's quarter past ten", "It's twenty to one", "It's noon".
 */
object TimeInWords {

    fun format(hour24: Int, minute: Int, language: String): String =
        if (language.startsWith("it")) italian(hour24, minute) else english(hour24, minute)

    private fun rounded(hour24: Int, minute: Int): Pair<Int, Int> {
        val total = hour24 * 60 + minute
        val r = ((total + 2) / 5) * 5 % (24 * 60)
        return r / 60 to r % 60
    }

    // ---------- Italiano ----------
    private val IT_HOURS = listOf(
        "mezzanotte", "l'una", "le due", "le tre", "le quattro", "le cinque", "le sei",
        "le sette", "le otto", "le nove", "le dieci", "le undici", "mezzogiorno",
    )
    private val IT_MIN = mapOf(
        5 to "cinque", 10 to "dieci", 15 to "un quarto", 20 to "venti", 25 to "venticinque", 30 to "mezza",
    )

    private fun italian(h24: Int, m0: Int): String {
        val (h, m) = rounded(h24, m0)
        val past = m <= 30
        val hourRef = if (past) h else (h + 1) % 24
        val name = itHour(hourRef)
        val verb = if (name.startsWith("le ")) "Sono" else "È"
        return when {
            m == 0 -> "$verb $name"
            past -> "$verb $name e ${IT_MIN.getValue(m)}"
            else -> "$verb $name meno ${IT_MIN.getValue(60 - m)}"
        }
    }

    private fun itHour(h: Int): String = when (h) {
        0 -> IT_HOURS[0]
        12 -> IT_HOURS[12]
        else -> IT_HOURS[if (h > 12) h - 12 else h]
    }

    // ---------- English ----------
    private val EN_HOURS = listOf(
        "midnight", "one", "two", "three", "four", "five", "six",
        "seven", "eight", "nine", "ten", "eleven", "noon",
    )
    private val EN_MIN = mapOf(
        5 to "five", 10 to "ten", 15 to "quarter", 20 to "twenty", 25 to "twenty-five", 30 to "half",
    )

    private fun english(h24: Int, m0: Int): String {
        val (h, m) = rounded(h24, m0)
        val past = m <= 30
        val hourRef = if (past) h else (h + 1) % 24
        val name = when (hourRef) {
            0 -> EN_HOURS[0]
            12 -> EN_HOURS[12]
            else -> EN_HOURS[if (hourRef > 12) hourRef - 12 else hourRef]
        }
        val oClock = if (hourRef == 0 || hourRef == 12) name else "$name o'clock"
        return when {
            m == 0 -> "It's $oClock"
            past -> "It's ${EN_MIN.getValue(m)} past $name"
            else -> "It's ${EN_MIN.getValue(60 - m)} to $name"
        }
    }
}
