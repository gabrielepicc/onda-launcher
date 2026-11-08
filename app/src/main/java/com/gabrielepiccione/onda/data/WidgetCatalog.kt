// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import kotlin.math.ceil

/** Un widget nel selettore, senza dipendenze Android: chiave del provider, nomi, pacchetto e misure minime (dp). */
data class WidgetEntry(
    val key: String,
    val label: String,
    val appLabel: String,
    val packageName: String,
    val minWidthDp: Int,
    val minHeightDp: Int,
)

/** Le app con i loro widget, per l'elenco raggruppato. */
data class WidgetGroup(val appLabel: String, val packageName: String, val letter: Char, val widgets: List<WidgetEntry>)

/** Regole del selettore dei widget, testabili su JVM. */
object WidgetCatalog {
    const val MAX_RECENT = 8
    const val MAX_SUGGESTED = 8

    /** Celle della griglia di Android per una misura minima: n celle valgono 70·n − 30 dp. */
    fun cells(dp: Int): Int = ceil((dp + 30) / 70.0).toInt().coerceAtLeast(1)

    /** Misura leggibile, es. «4×2». */
    fun sizeText(e: WidgetEntry): String = "${cells(e.minWidthDp)}×${cells(e.minHeightDp)}"

    /** Ricerca per nome del widget o dell'app, senza maiuscole né accenti. */
    fun filter(all: List<WidgetEntry>, query: String): List<WidgetEntry> {
        val q = query.normalizedForSearch().trim()
        if (q.isEmpty()) return all
        return all.filter { it.label.normalizedForSearch().contains(q) || it.appLabel.normalizedForSearch().contains(q) }
    }

    /** Raggruppati per app, in ordine alfabetico dell'app; dentro, per nome. */
    fun groups(entries: List<WidgetEntry>): List<WidgetGroup> =
        entries.groupBy { it.packageName }
            .map { (pkg, list) ->
                val app = list.first().appLabel
                WidgetGroup(app, pkg, sectionOf(app), list.sortedBy { it.label.normalizedForSearch() })
            }
            .sortedBy { it.appLabel.normalizedForSearch() }

    /** Le lettere della barra dell'alfabeto, una per gruppo, senza doppioni e nell'ordine dell'elenco. */
    fun letters(groups: List<WidgetGroup>): List<String> = groups.map { it.letter.toString() }.distinct()

    /** Indice del primo gruppo di una lettera. */
    fun indexOfLetter(groups: List<WidgetGroup>, letter: String): Int = groups.indexOfFirst { it.letter.toString() == letter }

    /** Usati di recente: i più recenti prima, solo quelli ancora installati. */
    fun recent(all: List<WidgetEntry>, recentKeys: List<String>): List<WidgetEntry> {
        val byKey = all.associateBy { it.key }
        return recentKeys.mapNotNull { byKey[it] }.take(MAX_RECENT)
    }

    /** Suggeriti: i widget delle app preferite, esclusi quelli già nei recenti. */
    fun suggested(all: List<WidgetEntry>, favoritePackages: List<String>, exclude: Set<String>): List<WidgetEntry> =
        favoritePackages.distinct().flatMap { pkg -> all.filter { it.packageName == pkg && it.key !in exclude } }.take(MAX_SUGGESTED)

    /** Dopo una scelta: in testa ai recenti, senza doppioni, al più [MAX_RECENT]. */
    fun rememberPick(recentKeys: List<String>, key: String): List<String> = (listOf(key) + recentKeys.filter { it != key }).take(MAX_RECENT)
}
