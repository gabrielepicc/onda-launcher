// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/** App suggerite sotto i preferiti, senza dipendenze Android (testabile su JVM). */
object HomeSuggestions {
    const val MAX = 3

    /** Sotto questo numero di aperture un'app non è "usata spesso". */
    const val MIN_LAUNCHES = 3

    /**
     * Le app più aperte tra quelle che non stanno già in home. [eligible] esclude ciò che non si può
     * mostrare (nascoste, Spazio privato, disinstallate); a parità di aperture l'ordine è stabile.
     */
    fun pick(s: Settings, eligible: Set<String>): List<String> {
        if (!s.suggestInHome) return emptyList()
        val inHome = s.favorites.toSet() + s.folders.values.flatMap { it.apps }
        return s.launchCounts.entries
            .filter { (key, count) ->
                count >= MIN_LAUNCHES && key in eligible && key !in inHome && key !in s.suggestionsDismissed
            }
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(MAX)
            .map { it.key }
    }

    fun dismiss(s: Settings, key: String): Settings = s.copy(suggestionsDismissed = s.suggestionsDismissed + key)

    fun restore(s: Settings, key: String): Settings = s.copy(suggestionsDismissed = s.suggestionsDismissed - key)
}
