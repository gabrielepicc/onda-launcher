// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.components

import com.gabrielepiccione.onda.util.AlphabetLayout
import com.gabrielepiccione.onda.util.HapticEvent
import com.gabrielepiccione.onda.util.LocalOndaHaptics
import com.gabrielepiccione.onda.ui.theme.*
import com.gabrielepiccione.onda.ui.home.*
import com.gabrielepiccione.onda.ui.drawer.*
import com.gabrielepiccione.onda.ui.search.*
import com.gabrielepiccione.onda.ui.settings.*
import com.gabrielepiccione.onda.ui.sheets.*

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.exp

/**
 * Scroller alfabetico "a onda".
 *
 * Tutto viene disegnato in una sola Canvas e lo stato del dito viene letto solo in fase di
 * disegno: muovere il dito non ricompone nulla, quindi resta fluido anche con molte lettere.
 *
 * @param showLetters false = zona invisibile (lato opposto): si vede solo la bolla con la lettera.
 */
@Composable
fun AlphabetWaveScroller(
    letters: List<String>,
    leftSide: Boolean,
    modifier: Modifier = Modifier,
    showLetters: Boolean = true,
    /** Dimensione delle lettere rispetto a quella normale. */
    letterScale: Float = 1f,
    /** Distanza massima tra due lettere: oltre, la colonna smette di allungarsi. */
    maxLetterStep: Dp = 19.dp,
    onTouchStart: () -> Unit = {},
    onLetter: (String) -> Unit,
    onTouchEnd: () -> Unit = {},
    /** Doppio tocco sulla colonna (es. blocco schermo); null = disattivato. */
    onDoubleTap: (() -> Unit)? = null,
) {
    if (letters.isEmpty()) return
    val colors = LocalOndaColors.current
    val haptics = LocalHapticFeedback.current
    val ondaHaptics = LocalOndaHaptics.current
    val hapticsOn = LocalHapticsEnabled.current
    val scope = rememberCoroutineScope()
    val measurer = rememberTextMeasurer(cacheSize = 64)

    val touchY = remember { mutableFloatStateOf(0f) }
    val selected = remember { mutableIntStateOf(-1) }
    val intensity = remember { Animatable(0f) }

    val currentLetters by rememberUpdatedState(letters)
    val currentOnLetter by rememberUpdatedState(onLetter)
    val currentOnStart by rememberUpdatedState(onTouchStart)
    val currentOnEnd by rememberUpdatedState(onTouchEnd)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val lastTapUp = remember { longArrayOf(0L) }

    val letterStyle = TextStyle(fontSize = 13.sp * letterScale, fontWeight = FontWeight.Medium, color = colors.onWallpaperMuted)
    val bubbleStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold, color = colors.onAccentContainer)
    // Misuriamo le lettere una sola volta
    val measured = remember(letters, letterStyle) { letters.map { measurer.measure(it, letterStyle) } }
    val bubbleLayouts = remember(letters, bubbleStyle) { letters.map { measurer.measure(it, bubbleStyle) } }

    // Di quanto la colonna è salita rispetto a dove riposa (px, negativo = verso l'alto)
    val columnShift = remember { Animatable(0f) }

    // La zona di tocco è alta tutto il bordo: si può iniziare a scorrere ovunque. La colonna visibile
    // riposa in basso, alta quanto le lettere (passo massimo [maxLetterStep]); toccando più in alto
    // sale incontro al dito e torna giù quando lo si alza.
    Canvas(
        modifier
            .fillMaxHeight()
            .width(if (showLetters) 36.dp else 28.dp)
            .pointerInput(letters.size, maxLetterStep) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val maxStepPx = maxLetterStep.toPx()
                    currentOnStart()
                    scope.launch { intensity.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)) }

                    // La colonna si fissa al primo tocco: la stella resta sopra il dito, che parte dalla prima lettera
                    val zone = size.height.toFloat()
                    val count = currentLetters.size
                    val anchor = if (currentLetters.firstOrNull() == FAVORITES_MARK) 1 else 0
                    val restTop = AlphabetLayout.top(zone, count, maxStepPx)
                    val gestureTop = AlphabetLayout.anchoredTop(down.position.y, zone, count, maxStepPx, anchor)
                    scope.launch { columnShift.animateTo(gestureTop - restTop, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)) }

                    fun update(y: Float) {
                        touchY.floatValue = y.coerceIn(0f, zone)
                        val idx = AlphabetLayout.indexAt(y, zone, currentLetters.size, maxStepPx, gestureTop)
                        if (idx != selected.intValue) {
                            selected.intValue = idx
                            ondaHaptics?.perform(if (idx == 0 || idx == currentLetters.lastIndex) HapticEvent.EDGE else HapticEvent.LETTER)
                            currentOnLetter(currentLetters[idx])
                        }
                    }
                    update(down.position.y)

                    var moved = 0f
                    var upTime = down.uptimeMillis
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        upTime = change.uptimeMillis
                        if (!change.pressed) break
                        moved = maxOf(moved, kotlin.math.abs(change.position.y - down.position.y))
                        update(change.position.y)
                        change.consume()
                    }
                    currentOnEnd()

                    // Riconoscimento del doppio tocco (tocchi brevi e fermi, ravvicinati)
                    val isTap = upTime - down.uptimeMillis < viewConfiguration.longPressTimeoutMillis &&
                        moved < viewConfiguration.touchSlop
                    if (isTap) {
                        val gap = down.uptimeMillis - lastTapUp[0]
                        if (gap in 1 until viewConfiguration.doubleTapTimeoutMillis) {
                            lastTapUp[0] = 0L
                            currentOnDoubleTap?.invoke()
                        } else {
                            lastTapUp[0] = upTime
                        }
                    } else {
                        lastTapUp[0] = 0L
                    }
                    scope.launch { columnShift.animateTo(0f, tween(300, easing = Motion.Standard)) }
                    scope.launch {
                        intensity.animateTo(0f, tween(300, easing = Motion.Standard))
                        selected.intValue = -1
                    }
                }
            }
    ) {
        val n = letters.size
        val maxStepPx = maxLetterStep.toPx()
        val step = AlphabetLayout.step(size.height, n, maxStepPx)
        val columnTop = AlphabetLayout.top(size.height, n, maxStepPx) + columnShift.value
        val k = intensity.value
        val center = (touchY.floatValue - columnTop) / step - 0.5f
        val maxShift = 52.dp.toPx()
        val sigma = 2.4f
        val dir = if (leftSide) 1f else -1f

        if (showLetters) {
            for (i in 0 until n) {
                val d = i - center
                val wave = if (k > 0f) exp(-(d * d) / (2 * sigma * sigma)) * k else 0f
                val layout = measured[i]
                val cx = size.width / 2f + dir * maxShift * wave
                val cy = columnTop + i * step + step / 2f
                val s = 1f + 1.15f * wave
                scale(s, s, pivot = Offset(cx, cy)) {
                    drawText(
                        layout,
                        topLeft = Offset(cx - layout.size.width / 2f, cy - layout.size.height / 2f),
                        alpha = 0.65f + 0.35f * wave,
                    )
                }
            }
        }

        // Bolla con la lettera corrente, accanto al dito
        val sel = selected.intValue
        if (sel in 0 until n && k > 0.02f) {
            val bubble = 64.dp.toPx() * (0.6f + 0.4f * k)
            val bx = size.width / 2f + dir * (if (showLetters) 104.dp.toPx() else 88.dp.toPx())
            val by = touchY.floatValue
            drawRoundRect(
                color = colors.accentContainer.copy(alpha = k.coerceIn(0f, 1f)),
                topLeft = Offset(bx - bubble / 2f, by - bubble / 2f),
                size = Size(bubble, bubble),
                cornerRadius = CornerRadius(bubble / 2f),
            )
            val t = bubbleLayouts[sel]
            drawText(
                t,
                topLeft = Offset(bx - t.size.width / 2f, by - t.size.height / 2f),
                alpha = k.coerceIn(0f, 1f),
            )
        }
    }
}
