// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/** App installate di recente, per la voce in fondo all'elenco. */
object RecentInstalls {
    const val LIMIT = 20

    /**
     * Le [limit] voci installate più di recente, dalla più nuova. Le voci senza data (0)
     * restano fuori; a parità di data resta l'ordine di partenza (alfabetico).
     */
    fun <T> pick(items: List<T>, limit: Int = LIMIT, installedAt: (T) -> Long): List<T> =
        items.filter { installedAt(it) > 0L }
            .sortedByDescending(installedAt)
            .take(limit)
}
