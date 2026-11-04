// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.core.icons

import java.text.Normalizer

/**
 * Confronto "fuzzy" tra il nome di un'app e i nomi dei drawable di un icon pack
 * (es. "Google Maps" ↔ "google_maps", "maps_alt", "gmaps"). Puro Kotlin, testabile su JVM.
 */
object IconMatcher {

    private fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()

    /** Parole significative: separa su spazi, trattini, underscore, punti e cambi maiuscola. */
    fun tokens(s: String): List<String> =
        normalize(s.replace(Regex("([a-z])([A-Z])"), "$1 $2"))
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 2 && it !in STOP }

    private val STOP = setOf("app", "the", "for", "by", "ic", "icon", "alt", "lite", "beta", "pro")

    /**
     * Punteggio 0..100: 100 = identico, alto se tutte le parole del nome compaiono,
     * medio per prefissi comuni o parole condivise.
     */
    fun score(appLabel: String, packageName: String, drawable: String): Int {
        val d = normalize(drawable).replace(Regex("[^a-z0-9]"), "")
        val label = normalize(appLabel).replace(Regex("[^a-z0-9]"), "")
        if (d.isEmpty() || label.isEmpty()) return 0
        if (d == label) return 100
        val lt = tokens(appLabel)
        val dt = tokens(drawable)
        var score = 0
        if (lt.isNotEmpty() && lt.all { t -> dt.any { it == t } }) score = maxOf(score, 90)
        if (d.startsWith(label) || label.startsWith(d)) score = maxOf(score, 80)
        val shared = lt.count { t -> dt.any { it == t || (t.length >= 4 && it.startsWith(t)) } }
        if (shared > 0) score = maxOf(score, 50 + 20 * shared / lt.size.coerceAtLeast(1))
        // Ultima parola del pacchetto (es. com.spotify.music -> "spotify" / "music")
        val pkgTokens = packageName.split('.').filter { it.length >= 3 && it !in setOf("com", "org", "net", "android", "app") }
        if (pkgTokens.any { p -> dt.any { it == p } }) score = maxOf(score, 70)
        if (d.contains(label) && label.length >= 4) score = maxOf(score, 75)
        return score.coerceAtMost(99)
    }

    /** I migliori [limit] drawable per un'app, dal più pertinente. */
    fun suggest(appLabel: String, packageName: String, drawables: Collection<String>, limit: Int = 12): List<String> =
        drawables.asSequence()
            .map { it to score(appLabel, packageName, it) }
            .filter { it.second >= 50 }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first.length })
            .map { it.first }
            .distinct()
            .take(limit)
            .toList()
}
