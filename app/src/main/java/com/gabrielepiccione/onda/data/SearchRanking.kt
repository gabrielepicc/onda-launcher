// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import kotlin.math.pow

/** Quante volte un'app è stata aperta dalla ricerca e quando l'ultima volta. Niente testo digitato. */
data class SearchStat(val count: Int, val lastAt: Long)

/** Classifica delle app aperte dalla ricerca, con un decadimento: chi non si cerca più scende. */
object SearchRanking {
    const val HALF_LIFE_MS = 14L * 24 * 60 * 60 * 1000
    /** Sotto questo numero di ricerche un'app non entra in «Cercate spesso». */
    const val MIN_SEARCHES = 2
    const val DEFAULT_SHOWN = 4
    const val MAX_SHOWN = 8
    /** Con meno app di così sopra la soglia si mostrano le più usate. */
    const val MIN_FOR_SECTION = 2

    fun record(history: Map<String, SearchStat>, packageName: String, now: Long): Map<String, SearchStat> {
        val old = history[packageName]
        return history + (packageName to SearchStat((old?.count ?: 0) + 1, now))
    }

    /** Punteggio con emivita di 14 giorni dall'ultima ricerca. */
    fun score(stat: SearchStat, now: Long): Double {
        val age = (now - stat.lastAt).coerceAtLeast(0L).toDouble()
        return stat.count * 0.5.pow(age / HALF_LIFE_MS)
    }

    /**
     * Pacchetti da mostrare in «Cercate spesso», in ordine: punteggio, poi la più recente.
     * [excluded] = app nascoste, protette con sblocco o dello Spazio privato.
     */
    fun top(history: Map<String, SearchStat>, now: Long, limit: Int, excluded: Set<String> = emptySet()): List<String> {
        if (limit <= 0) return emptyList()
        return history.entries
            .filter { it.value.count >= MIN_SEARCHES && it.key !in excluded }
            .sortedWith(compareByDescending<Map.Entry<String, SearchStat>> { score(it.value, now) }.thenByDescending { it.value.lastAt })
            .take(limit)
            .map { it.key }
    }

    /** Spareggio fra risultati con la stessa pertinenza: più alto = prima. */
    fun tieBreak(history: Map<String, SearchStat>, packageName: String, now: Long): Double =
        history[packageName]?.let { score(it, now) } ?: 0.0

    /** Cosa mostrare a ricerca vuota: le cercate spesso, oppure (ripiego) le più usate. */
    data class Section(val packages: List<String>, val frequentSearches: Boolean)

    /**
     * Le app da mostrare a ricerca vuota, dalle [visible] (senza nascoste e Spazio privato):
     * escluse anche quelle protette con sblocco. Il secondo valore dice se sono «Cercate spesso».
     */
    fun <T> pick(
        history: Map<String, SearchStat>,
        now: Long,
        limit: Int,
        visible: List<T>,
        mostUsed: List<T>,
        protectedPackages: Set<String>,
        packageOf: (T) -> String,
    ): Pair<List<T>, Boolean> {
        val byPackage = visible.associateBy(packageOf)
        val excluded = history.keys - byPackage.keys + protectedPackages
        val top = top(history, now, MAX_SHOWN, excluded).mapNotNull { byPackage[it] }
        val usable = mostUsed.filter { packageOf(it) !in protectedPackages }
        val s = section(top.map(packageOf), usable.map(packageOf), limit)
        val all = (top + usable).associateBy(packageOf)
        return s.packages.mapNotNull { all[it] } to s.frequentSearches
    }

    fun section(top: List<String>, mostUsed: List<String>, limit: Int): Section = when {
        limit <= 0 -> Section(emptyList(), true)
        top.size >= MIN_FOR_SECTION -> Section(top.take(limit), true)
        else -> Section(mostUsed.take(limit), false)
    }
}
