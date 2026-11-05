// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.gabrielepiccione.onda.util.BubbleMotion
import kotlin.math.cos
import kotlin.math.sin

/** Riflessi della pellicola, come quelli di una bolla di sapone: sono luce, non colori del tema. */
private val FILM_COLORS = listOf(
    Color(0x00FFFFFF),
    Color(0xCC7DE8FF),
    Color(0x99C9A0FF),
    Color(0xCCFF9EDB),
    Color(0x00FFFFFF),
    Color(0x99FFE9A8),
    Color(0xCC8DFFD2),
    Color(0x00FFFFFF),
)

/**
 * Forma del popup in un istante: l'intersezione tra il suo rettangolo arrotondato e il cerchio della
 * bolla, meno il foro che si allarga quando scoppia.
 */
fun bubbleClip(
    size: Size,
    corner: Float,
    center: Offset,
    radius: Float,
    hole: Offset? = null,
    holeRadius: Float = 0f,
): Path {
    val card = Path().apply {
        addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(corner)))
    }
    val circle = Path().apply { addOval(Rect(center, radius)) }
    val body = Path().apply { op(card, circle, PathOperation.Intersect) }
    if (hole == null || holeRadius <= 0f) return body
    val punched = Path().apply { addOval(Rect(hole, holeRadius)) }
    return Path().apply { op(body, punched, PathOperation.Difference) }
}

/** Punto del bordo della bolla in cui la pellicola si fora. */
fun puncturePoint(center: Offset, radius: Float): Offset = Offset(
    center.x + cos(BubbleMotion.PUNCTURE_ANGLE) * radius,
    center.y + sin(BubbleMotion.PUNCTURE_ANGLE) * radius,
)

/** Il bordo iridescente e i due riflessi che fanno sembrare il cerchio una bolla vera. */
fun DrawScope.drawBubbleFilm(center: Offset, radius: Float, alpha: Float, rimWidth: Float) {
    if (alpha <= 0.01f || radius <= 0f) return
    // Interno appena lattiginoso, più denso verso il bordo
    drawCircle(
        Brush.radialGradient(
            0.72f to Color.Transparent,
            0.96f to Color.White.copy(alpha = 0.10f * alpha),
            1f to Color.White.copy(alpha = 0.26f * alpha),
            center = center,
            radius = radius,
        ),
        radius,
        center,
    )
    drawCircle(
        Brush.sweepGradient(FILM_COLORS, center),
        radius,
        center,
        alpha = alpha,
        style = Stroke(rimWidth),
    )
    // Riflesso della luce in alto a sinistra e il suo piccolo controriflesso in basso a destra
    val light = radius * 0.78f
    drawArc(
        Color.White.copy(alpha = 0.75f * alpha),
        startAngle = 200f,
        sweepAngle = 48f,
        useCenter = false,
        topLeft = Offset(center.x - light, center.y - light),
        size = Size(light * 2f, light * 2f),
        style = Stroke(rimWidth * 2f, cap = StrokeCap.Round),
    )
    drawArc(
        Color.White.copy(alpha = 0.35f * alpha),
        startAngle = 30f,
        sweepAngle = 22f,
        useCenter = false,
        topLeft = Offset(center.x - light, center.y - light),
        size = Size(light * 2f, light * 2f),
        style = Stroke(rimWidth * 1.4f, cap = StrokeCap.Round),
    )
}

/** Lo scoppio: l'orlo del foro che corre, l'onda che si allarga e le goccioline lanciate dal bordo. */
fun DrawScope.drawBubbleBurst(
    center: Offset,
    radius: Float,
    burst: Float,
    droplets: List<BubbleMotion.Droplet>,
    dropletRadius: Float,
    rimWidth: Float,
    tint: Color,
) {
    if (burst <= 0f) return
    if (!BubbleMotion.filmGone(burst)) {
        // L'orlo della pellicola che si ritira: il foro è la cosa più luminosa in quegli istanti
        val hole = puncturePoint(center, radius)
        val holeRadius = BubbleMotion.holeRadius(burst, radius)
        val rim = Path().apply {
            val inside = Path().apply { addOval(Rect(center, radius)) }
            val ring = Path().apply { addOval(Rect(hole, holeRadius)) }
            op(ring, inside, PathOperation.Intersect)
        }
        drawPath(rim, Color.White.copy(alpha = 0.85f), style = Stroke(rimWidth * 2.2f))
    }
    // Onda d'urto sottile: dura poco e si spegne allargandosi
    val wave = (burst / 0.7f).coerceIn(0f, 1f)
    if (wave < 1f) {
        drawCircle(
            tint.copy(alpha = 0.4f * (1f - wave) * (1f - wave)),
            radius * (1f + 0.45f * wave),
            center,
            style = Stroke(rimWidth * (1.6f - wave)),
        )
    }
    droplets.forEach { droplet ->
        val state = BubbleMotion.dropletAt(droplet, burst, radius, dropletRadius)
        if (state.alpha > 0.01f && state.radius > 0.2f) {
            val at = Offset(center.x + state.x, center.y + state.y)
            drawCircle(tint.copy(alpha = state.alpha * 0.9f), state.radius, at)
            // Un punto di luce su ogni goccia: basta questo a farle sembrare d'acqua
            drawCircle(
                Color.White.copy(alpha = state.alpha),
                state.radius * 0.38f,
                Offset(at.x - state.radius * 0.3f, at.y - state.radius * 0.3f),
            )
        }
    }
}
