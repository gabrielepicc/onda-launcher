// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import com.gabrielepiccione.onda.data.ClockStyle
import com.gabrielepiccione.onda.data.ClockText
import com.gabrielepiccione.onda.ui.theme.LocalClockFont
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import com.gabrielepiccione.onda.ui.theme.LocalTextShadow
import java.time.LocalDateTime
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Gli stili dell'orologio aggiunti dopo i primi otto: Material You (pillola, blocchi, biscotto, doppio colore,
 * analogico M3, data in grande, verticale) e classici/creativi (segmenti, matrice a punti, serif, neon, romani,
 * flip, tacche). Disegno semplice: nessuna anteprima costosa, le forme si calcolano una volta sola.
 */
@Composable
fun ExtraClockFace(style: ClockStyle, now: LocalDateTime, scale: Float, textAlign: TextAlign, seconds: Boolean) {
    val colors = LocalOndaColors.current
    val scheme = MaterialTheme.colorScheme
    val clockFont = LocalClockFont.current
    val base = MaterialTheme.typography.displayLarge.copy(color = colors.onWallpaper, shadow = LocalTextShadow.current)
        .let { if (clockFont != null) it.copy(fontFamily = clockFont) else it }
    val text = ClockText.digits(now.hour, now.minute, now.second, seconds)
    val hh = "%02d".format(now.hour)
    val mm = "%02d".format(now.minute)
    when (style) {
        ClockStyle.PILL -> Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(scheme.primaryContainer)
                .padding(horizontal = 28.dp * scale, vertical = 6.dp * scale),
        ) { AnimatedDigits(text, base.copy(fontSize = 56.sp * scale, fontWeight = FontWeight.Medium, color = scheme.onPrimaryContainer, shadow = null)) }

        ClockStyle.BLOCKS -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp * scale), verticalAlignment = Alignment.CenterVertically) {
            text.forEachIndexed { i, ch ->
                if (ch == ':') {
                    Text(":", style = base.copy(fontSize = 40.sp * scale, shadow = null), color = colors.onWallpaperMuted)
                } else {
                    val hours = i < 2
                    Box(
                        Modifier
                            .size(width = 48.dp * scale, height = 68.dp * scale)
                            .clip(RoundedCornerShape(16.dp * scale))
                            .background(if (hours) scheme.primaryContainer else scheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            ch.toString(),
                            style = base.copy(fontSize = 42.sp * scale, fontWeight = FontWeight.Medium, shadow = null),
                            color = if (hours) scheme.onPrimaryContainer else scheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }

        ClockStyle.COOKIE -> {
            val shape = remember { cookiePath(9, 0.82f, 0.45f) }
            Box(Modifier.size(172.dp * scale), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(172.dp * scale)) { drawUnitPath(shape, scheme.primaryContainer) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(hh, style = base.copy(fontSize = 54.sp * scale, fontWeight = FontWeight.Bold, lineHeight = 52.sp * scale, shadow = null), color = scheme.onPrimaryContainer)
                    Text(mm, style = base.copy(fontSize = 54.sp * scale, fontWeight = FontWeight.Light, lineHeight = 52.sp * scale, shadow = null), color = scheme.onPrimaryContainer)
                    if (seconds) Text("%02d".format(now.second), style = base.copy(fontSize = 16.sp * scale, shadow = null), color = scheme.onPrimaryContainer.copy(alpha = 0.7f))
                }
            }
        }

        ClockStyle.DUAL -> Row(verticalAlignment = Alignment.Bottom) {
            AnimatedDigits(hh, base.copy(fontSize = 84.sp * scale, fontWeight = FontWeight.Black, color = scheme.primary))
            AnimatedDigits(mm, base.copy(fontSize = 84.sp * scale, fontWeight = FontWeight.ExtraLight, color = scheme.tertiary))
            if (seconds) AnimatedDigits("%02d".format(now.second), base.copy(fontSize = 28.sp * scale, fontWeight = FontWeight.Medium, color = colors.onWallpaperMuted))
        }

        ClockStyle.ANALOG_M3 -> {
            val dial = remember { cookiePath(12, 0.9f, 0.4f) }
            Box(Modifier.size(152.dp * scale), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(152.dp * scale)) {
                    drawUnitPath(dial, scheme.secondaryContainer)
                    val r = size.minDimension / 2f
                    hand(this, center, now.hour % 12 * 30f + now.minute * 0.5f, r * 0.5f, r * 0.12f, scheme.primary)
                    hand(this, center, now.minute * 6f, r * 0.72f, r * 0.08f, scheme.tertiary)
                    if (seconds) hand(this, center, now.second * 6f, r * 0.8f, r * 0.025f, scheme.onSecondaryContainer)
                    drawCircle(scheme.onSecondaryContainer, radius = r * 0.07f, center = center)
                }
            }
        }

        ClockStyle.BIG_DATE -> Column(horizontalAlignment = if (textAlign == TextAlign.End) Alignment.End else if (textAlign == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start) {
            Text(now.dayOfMonth.toString(), style = base.copy(fontSize = 104.sp * scale, fontWeight = FontWeight.Black, lineHeight = 96.sp * scale, color = scheme.primary))
            Text(
                now.dayOfWeek.getDisplayName(DateTextStyle.FULL, Locale.getDefault()) + " · " + now.month.getDisplayName(DateTextStyle.SHORT, Locale.getDefault()),
                style = base.copy(fontSize = 20.sp * scale, fontWeight = FontWeight.Medium),
            )
            AnimatedDigits(text, base.copy(fontSize = 28.sp * scale, fontWeight = FontWeight.Light, color = colors.onWallpaperMuted))
        }

        ClockStyle.VERTICAL -> {
            // Ombra tonale piena (senza sfocatura): sembra un rilievo del colore primario
            val shadow = Shadow(scheme.primary.copy(alpha = 0.4f), Offset(6f * scale, 6f * scale), 0f)
            Column(horizontalAlignment = if (textAlign == TextAlign.End) Alignment.End else if (textAlign == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start) {
                AnimatedDigits(hh, base.copy(fontSize = 88.sp * scale, fontWeight = FontWeight.Bold, lineHeight = 80.sp * scale, shadow = shadow))
                AnimatedDigits(mm, base.copy(fontSize = 88.sp * scale, fontWeight = FontWeight.Light, lineHeight = 80.sp * scale, shadow = shadow))
            }
        }

        ClockStyle.SEGMENTS -> SegmentClock(text, scale, colors.accent, colors.onWallpaper.copy(alpha = 0.1f))
        ClockStyle.DOTS -> DotClock(text, scale, colors.accent, colors.onWallpaper.copy(alpha = 0.12f))

        ClockStyle.SERIF -> AnimatedDigits(text, base.copy(fontSize = 80.sp * scale, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontStyle = FontStyle.Italic))

        ClockStyle.NEON -> Box {
            // Alone: lo stesso testo con un'ombra colorata molto sfocata dietro, poi il testo chiaro sopra
            Text(text, style = base.copy(fontSize = 68.sp * scale, fontWeight = FontWeight.Bold, color = colors.accent, shadow = Shadow(colors.accent, Offset.Zero, 42f * scale)))
            Text(text, style = base.copy(fontSize = 68.sp * scale, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.9f), shadow = Shadow(colors.accent, Offset.Zero, 12f * scale)))
        }

        ClockStyle.ROMAN -> Text(
            ClockText.romanTime(now.hour, now.minute),
            style = base.copy(fontSize = 52.sp * scale, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, letterSpacing = 2.sp),
            textAlign = textAlign,
        )

        ClockStyle.FLIP -> Row(horizontalArrangement = Arrangement.spacedBy(5.dp * scale), verticalAlignment = Alignment.CenterVertically) {
            text.forEach { ch ->
                if (ch == ':') Text(":", style = base.copy(fontSize = 34.sp * scale, shadow = null), color = colors.onWallpaperMuted)
                else FlipDigit(ch, scale)
            }
        }

        ClockStyle.TICKS -> Box(Modifier.size(148.dp * scale), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(148.dp * scale).padding(4.dp)) {
                val r = size.minDimension / 2f
                for (i in 0 until 60) {
                    val major = i % 5 == 0
                    val a = Math.toRadians(i * 6.0 - 90)
                    val outer = r
                    val inner = if (major) r * 0.84f else r * 0.92f
                    drawLine(
                        if (major) colors.onWallpaper else colors.onWallpaperMuted,
                        Offset(center.x + inner * cos(a).toFloat(), center.y + inner * sin(a).toFloat()),
                        Offset(center.x + outer * cos(a).toFloat(), center.y + outer * sin(a).toFloat()),
                        strokeWidth = if (major) r * 0.035f else r * 0.015f,
                        cap = StrokeCap.Round,
                    )
                }
                hand(this, center, now.hour % 12 * 30f + now.minute * 0.5f, r * 0.5f, r * 0.045f, colors.onWallpaper)
                hand(this, center, now.minute * 6f, r * 0.76f, r * 0.03f, colors.onWallpaper)
                if (seconds) hand(this, center, now.second * 6f, r * 0.82f, r * 0.015f, colors.accent)
                drawCircle(colors.accent, radius = r * 0.045f, center = center)
            }
        }

        // Gli stili di base non passano di qui
        else -> Text(text, style = base.copy(fontSize = 76.sp * scale))
    }
}

/** Il contorno di una forma Material «a stella» arrotondata, nello spazio unitario (raggio 1, centro 0,0). */
private fun cookiePath(points: Int, inner: Float, rounding: Float): Path =
    RoundedPolygon.star(points, radius = 1f, innerRadius = inner, rounding = CornerRounding(rounding)).toPath().asComposePath()

private fun DrawScope.drawUnitPath(path: Path, color: Color) {
    val r = size.minDimension / 2f
    withTransform({
        translate(center.x, center.y)
        scale(r, r, Offset.Zero)
    }) { drawPath(path, color) }
}

private fun hand(scope: DrawScope, center: Offset, angleDeg: Float, length: Float, width: Float, color: Color) {
    val a = Math.toRadians(angleDeg - 90.0)
    scope.drawLine(color, center, Offset(center.x + length * cos(a).toFloat(), center.y + length * sin(a).toFloat()), strokeWidth = width, cap = StrokeCap.Round)
}

/** Cifre a sette segmenti: i segmenti spenti restano appena visibili. */
@Composable
private fun SegmentClock(text: String, scale: Float, lit: Color, unlit: Color) {
    val digitW = 40.dp * scale
    val digitH = 72.dp * scale
    val colonW = 14.dp * scale
    val gap = 8.dp * scale
    val total = text.sumOf { if (it == ':') colonW.value.toDouble() else digitW.value.toDouble() }.toFloat().dp + gap * (text.length - 1)
    Canvas(Modifier.size(total, digitH)) {
        var x = 0f
        text.forEach { ch ->
            if (ch == ':') {
                val d = digitW.toPx() * 0.16f
                drawCircle(lit, d / 2f, Offset(x + colonW.toPx() / 2f, digitH.toPx() * 0.33f))
                drawCircle(lit, d / 2f, Offset(x + colonW.toPx() / 2f, digitH.toPx() * 0.67f))
                x += colonW.toPx() + gap.toPx()
            } else {
                drawDigit(ClockText.segments(ch), x, digitW.toPx(), digitH.toPx(), lit, unlit)
                x += digitW.toPx() + gap.toPx()
            }
        }
    }
}

private fun DrawScope.drawDigit(on: List<Boolean>, x: Float, w: Float, h: Float, lit: Color, unlit: Color) {
    val t = w * 0.17f
    val vertical = (h - 3 * t) / 2f
    val corner = CornerRadius(t / 2f)
    fun bar(index: Int, topLeft: Offset, size: Size) = drawRoundRect(if (on[index]) lit else unlit, Offset(x + topLeft.x, topLeft.y), size, corner)
    bar(0, Offset(t * 0.6f, 0f), Size(w - 1.2f * t, t))                      // a
    bar(1, Offset(w - t, t), Size(t, vertical))                               // b
    bar(2, Offset(w - t, 2 * t + vertical), Size(t, vertical))                // c
    bar(3, Offset(t * 0.6f, h - t), Size(w - 1.2f * t, t))                    // d
    bar(4, Offset(0f, 2 * t + vertical), Size(t, vertical))                   // e
    bar(5, Offset(0f, t), Size(t, vertical))                                  // f
    bar(6, Offset(t * 0.6f, t + vertical), Size(w - 1.2f * t, t))             // g
}

/** Matrice a punti 5×7 per cifra: punti accesi nel colore d'accento, gli altri appena accennati. */
@Composable
private fun DotClock(text: String, scale: Float, lit: Color, unlit: Color) {
    val cell = 7.dp * scale
    val columns = text.length * (ClockText.DOT_COLUMNS + 1) - 1
    Canvas(Modifier.size(cell * columns, cell * ClockText.DOT_ROWS)) {
        val c = cell.toPx()
        text.forEachIndexed { i, ch ->
            val glyph = ClockText.dotGlyph(ch)
            val left = i * (ClockText.DOT_COLUMNS + 1) * c
            glyph.forEachIndexed { row, bits ->
                bits.forEachIndexed { col, bit ->
                    drawCircle(if (bit == '1') lit else unlit, c * 0.38f, Offset(left + col * c + c / 2f, row * c + c / 2f))
                }
            }
        }
    }
}

/** Una cifra su una tessera che scatta: si piega a metà e mostra la nuova (solo quando cambia). */
@Composable
private fun FlipDigit(ch: Char, scale: Float) {
    val scheme = MaterialTheme.colorScheme
    var shown by remember { mutableStateOf(ch) }
    val turn = remember { Animatable(0f) }
    LaunchedEffect(ch) {
        if (ch != shown) {
            turn.animateTo(90f, tween(110))
            shown = ch
            turn.snapTo(-90f)
            turn.animateTo(0f, tween(110))
        }
    }
    Box(
        Modifier
            .size(width = 44.dp * scale, height = 64.dp * scale)
            .graphicsLayer { rotationX = turn.value; cameraDistance = 14f * density }
            .clip(RoundedCornerShape(10.dp * scale))
            .background(scheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Text(shown.toString(), style = TextStyle(fontSize = 44.sp * scale, fontWeight = FontWeight.Bold, color = scheme.onSurface))
        // La fessura al centro della tessera
        Box(Modifier.size(width = 44.dp * scale, height = 1.5.dp).background(scheme.surface.copy(alpha = 0.7f)))
    }
}
