// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/**
 * Misure del player, senza dipendenze Android (testabili su JVM): angoli concentrici tra contenitore e copertina,
 * stesso margine attorno alla copertina, copertina alta quanto il contenitore meno i due margini.
 */
object PlayerLayout {
    /** Raggio del contenitore del player (dp), uguale in tutte le varianti. */
    const val CONTAINER_RADIUS = 28f

    /** Margine attorno alla copertina nel player compatto e in quello grande (dp). */
    const val COMPACT_PADDING = 8f
    const val FULL_PADDING = 10f

    /** Titolo e artista del brano (sp), uguali in tutte le varianti del player (abbassati: 17 e 13). */
    const val TITLE_SP = 17f
    const val ARTIST_SP = 13f

    /** Altezza della riga del player compatto (dp): i comandi sono alti 48. */
    const val COMPACT_HEIGHT = 64f

    /** Angoli concentrici: il raggio interno è quello esterno meno il margine, mai negativo. */
    fun concentricRadius(outer: Float, padding: Float): Float = (outer - padding).coerceAtLeast(0f)

    /** La copertina riempie l'altezza meno i due margini. */
    fun artSize(height: Float, padding: Float): Float = (height - 2 * padding).coerceAtLeast(0f)
}
