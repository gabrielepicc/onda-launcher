// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/** Sezione per numeri, simboli e tutto ciò che non ha una lettera. */
const val OTHER_SECTION = "#"

/** Decide sotto quale lettera dell'alfabeto finisce un nome e in che ordine stanno le lettere. */
interface SectionIndex {
    fun sectionOf(label: String): String

    /** Posizione della sezione nell'alfabeto: [OTHER_SECTION] viene sempre per prima. */
    fun rank(section: String): Int
}

/** Alfabeto semplice A–Z, senza dipendenze Android: è il ripiego se quello di sistema non è disponibile. */
object PlainSectionIndex : SectionIndex {
    override fun sectionOf(label: String): String {
        val letter = com.gabrielepiccione.onda.data.sectionOf(label)
        return if (letter == '#') OTHER_SECTION else letter.toString()
    }

    override fun rank(section: String): Int =
        if (section == OTHER_SECTION) -1 else section.firstOrNull()?.code ?: Int.MAX_VALUE
}
