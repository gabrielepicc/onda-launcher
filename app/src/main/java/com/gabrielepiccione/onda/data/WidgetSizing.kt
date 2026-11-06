// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import kotlinx.serialization.Serializable

/**
 * Larghezza scelta per un widget e opzione "Automatica". L'altezza resta dove era già (widgetHeights,
 * WidgetStack.heightDp, Folder.widgetHeightDp), così i file salvati prima restano validi.
 * [widthDp] = 0 vuol dire tutta la larghezza disponibile.
 */
@Serializable
data class WidgetSize(val widthDp: Int = 0, val auto: Boolean = false)

/** Misure finali di un widget, in dp; [width] null = tutta la larghezza. */
data class WidgetBox(val width: Int?, val height: Int)

/** Regole delle misure dei widget, senza dipendenze Android (testabili su JVM). */
object WidgetSizing {
    const val MIN_WIDTH = 120
    const val MIN_HEIGHT = 48
    const val MAX_HEIGHT = 520

    /** Chiavi delle misure: widget singolo (anche quello delle luci), pila, cartella. */
    fun widgetKey(id: Int) = "w$id"
    fun stackKey(stackId: String) = "s$stackId"
    fun folderKey(folderId: String) = "f$folderId"

    /**
     * Le misure da usare. Con "Automatica" valgono quelle preferite dal widget; altrimenti la larghezza
     * scelta (o tutta) e l'altezza scelta (o quella preferita). Mai oltre lo spazio disponibile.
     */
    fun resolve(size: WidgetSize?, chosenHeight: Int?, preferredWidth: Int, preferredHeight: Int, available: Int): WidgetBox {
        val prefH = preferredHeight.coerceIn(MIN_HEIGHT, MAX_HEIGHT)
        if (size?.auto == true) {
            val w = preferredWidth.coerceIn(MIN_WIDTH, available.coerceAtLeast(MIN_WIDTH))
            return WidgetBox(if (w >= available) null else w, prefH)
        }
        val h = (chosenHeight ?: prefH).coerceIn(MIN_HEIGHT, MAX_HEIGHT)
        val w = size?.widthDp?.takeIf { it > 0 }?.coerceIn(MIN_WIDTH, available.coerceAtLeast(MIN_WIDTH))
        return WidgetBox(if (w == null || w >= available) null else w, h)
    }

    fun set(s: Settings, key: String, size: WidgetSize): Settings =
        s.copy(widgetSizes = if (size == WidgetSize()) s.widgetSizes - key else s.widgetSizes + (key to size))
}
