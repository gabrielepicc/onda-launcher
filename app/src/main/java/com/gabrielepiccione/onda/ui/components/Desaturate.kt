// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

/**
 * Toglie il colore a tutto ciò che sta dentro, widget compresi: [saturation] da 1 (colori) a 0 (bianco e nero).
 * Il valore si legge nella fase di disegno, così il passaggio morbido non ricompone nulla.
 */
fun Modifier.desaturate(saturation: () -> Float): Modifier = drawWithContent {
    val value = saturation()
    if (value >= 1f) {
        drawContent()
    } else {
        val paint = Paint().apply { colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(value) }) }
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
            drawContent()
            canvas.restore()
        }
    }
}
