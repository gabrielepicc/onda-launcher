// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import com.gabrielepiccione.onda.util.SwipeFeedback

/** Raggio dei bordi della traccia e della velatura: come le righe delle app. */
private val SWIPE_CORNER = 28.dp

/**
 * Stato condiviso degli swipe sulle righe: mentre una riga si sposta, il pulsante di ricerca si fa da
 * parte e non copre il testo. Una sola riga alla volta si trascina, quindi basta un valore.
 */
object RowSwipe {
    var active by mutableStateOf(false)
}

/**
 * Traccia dietro una riga che si sposta: nasce invisibile e si colora d'accento man mano che si trascina
 * (fino a ~22%), superata la soglia diventa il colore contenitore al 35%. L'icona dell'azione appare
 * con una dissolvenza e si ingrandisce alla soglia. Colori del tema, nessun colore fisso.
 * Il valore dello spostamento si legge nella fase di disegno: niente ricomposizioni mentre si trascina.
 */
@Composable
fun BoxScope.SwipeTrack(offset: () -> Float, triggerPx: Float, armed: Boolean, canDismiss: Boolean) {
    val colors = LocalOndaColors.current
    val armedScale by animateFloatAsState(if (armed) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = 500f), label = "swipeArmed")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (offset() >= 0f) Arrangement.Start else Arrangement.End,
        modifier = Modifier
            .matchParentSize()
            .clip(RoundedCornerShape(SWIPE_CORNER))
            .drawBehind {
                val p = SwipeFeedback.progress(offset(), triggerPx)
                val color = if (armed) colors.accentContainer else colors.accent
                drawRoundRect(color.copy(alpha = SwipeFeedback.trackAlpha(p, armed)), cornerRadius = CornerRadius(SWIPE_CORNER.toPx()))
            }
            .padding(horizontal = 24.dp),
    ) {
        val toRight = offset() >= 0f
        if (toRight || canDismiss) {
            androidx.compose.foundation.layout.Box(
                Modifier.graphicsLayer {
                    val p = SwipeFeedback.progress(offset(), triggerPx)
                    alpha = SwipeFeedback.iconAlpha(p)
                    val s = SwipeFeedback.iconScale(p, false) * (1f + (SwipeFeedback.ARMED_SCALE - 1f) * armedScale)
                    scaleX = s
                    scaleY = s
                },
            ) {
                if (toRight) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.onWallpaper)
                else Icon(Icons.Filled.Close, null, tint = colors.onWallpaper)
            }
        }
    }
}

/** Velatura leggerissima (colore del testo al 7%) sulla riga che si sposta, con gli angoli arrotondati. */
@Composable
fun Modifier.swipeVeil(offset: () -> Float, triggerPx: Float): Modifier {
    val color = LocalOndaColors.current.onWallpaper
    return drawBehind {
        val p = SwipeFeedback.progress(offset(), triggerPx)
        if (p > 0f) drawRoundRect(color.copy(alpha = SwipeFeedback.veilAlpha(p)), cornerRadius = CornerRadius(SWIPE_CORNER.toPx()))
    }
}

/** Segnala agli altri elementi (il pulsante di ricerca) che una riga si sta trascinando. */
@Composable
fun SwipeReporter(swiping: Boolean) {
    LaunchedEffect(swiping) { RowSwipe.active = swiping }
    DisposableEffect(Unit) { onDispose { RowSwipe.active = false } }
}
