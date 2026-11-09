// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/**
 * Regole di impaginazione delle card di notifica (popup, riepilogo e card in home), senza dipendenze Android
 * (testabili su JVM): angoli morbidi, margini che tengono il testo lontano dal bordo, titoli su più righe.
 */
object NotificationLayout {
    /** Margine orizzontale minimo tra il bordo della card e il testo, in dp. */
    const val MIN_HORIZONTAL_PADDING_DP = 16

    /** Righe del titolo prima del troncamento: occupa tutto lo spazio, riservando solo ora e pulsante. */
    const val TITLE_MAX_LINES = 2

    /** Il titolo prende tutto lo spazio libero della riga (peso 1, riempie) e non si accorcia a metà. */
    const val TITLE_FILLS_ROW = true

    /** Distanza (dp) oltre la quale lo swipe elimina la notifica. */
    const val SWIPE_TRIGGER_DP = 72

    /** Durata in ms della card che scivola via e della lista che si richiude. */
    const val SLIDE_OUT_MS = 180
    const val COLLAPSE_MS = 160

    /** Il margine applicato alla card, mai sotto il minimo. */
    fun horizontalPaddingDp(requested: Int): Int = maxOf(requested, MIN_HORIZONTAL_PADDING_DP)

    /** Solo le notifiche che il sistema permette di chiudere si eliminano con lo swipe. */
    fun canSwipeAway(clearable: Boolean): Boolean = clearable
}
