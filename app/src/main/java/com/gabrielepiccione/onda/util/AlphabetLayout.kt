// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

/**
 * Geometria dell'alfabeto: la zona di tocco occupa tutta l'altezza del bordo, mentre la colonna
 * delle lettere è compatta e ancorata in basso. Tutte le misure sono in pixel. Testabile su JVM.
 */
object AlphabetLayout {

    /** Distanza tra due lettere: quella massima, o meno se non ci stanno tutte. */
    fun step(zoneHeight: Float, count: Int, maxStep: Float): Float =
        if (count <= 0) 0f else minOf(maxStep, zoneHeight / count)

    /** Dove inizia la colonna dentro la zona di tocco. */
    fun top(zoneHeight: Float, count: Int, maxStep: Float): Float =
        (zoneHeight - step(zoneHeight, count, maxStep) * count).coerceAtLeast(0f)

    /** Lettera sotto il dito: sopra la colonna vale la prima, sotto l'ultima. */
    fun indexAt(y: Float, zoneHeight: Float, count: Int, maxStep: Float): Int {
        if (count <= 0) return -1
        val step = step(zoneHeight, count, maxStep)
        if (step <= 0f) return 0
        val top = top(zoneHeight, count, maxStep)
        return ((y - top) / step).toInt().coerceIn(0, count - 1)
    }
}
