// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import java.text.Normalizer

/** Ricerca nelle impostazioni: confronto senza maiuscole e senza accenti, per parole. */
object SettingsSearch {

    /** Minuscolo e senza accenti, così "Orologio" trova "orologio" e "lunghezza" trova "lunghèzza". */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\p{Mn}+"), "")
            .lowercase()
            .trim()

    /** Vero se ogni parola della ricerca compare nel testo (l'ordine non conta). Ricerca vuota: nessun risultato. */
    fun matches(query: String, vararg texts: String): Boolean {
        val words = normalize(query).split(Regex("\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return false
        val haystack = texts.joinToString(" ") { normalize(it) }
        return words.all { it in haystack }
    }
}
