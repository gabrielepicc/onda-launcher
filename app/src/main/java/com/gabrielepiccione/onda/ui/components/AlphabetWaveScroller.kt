// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.components

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
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.BoxWithConstraints
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
    letters: List<Char>,
    leftSide: Boolean,
    modifier: Modifier = Modifier,
    showLetters: Boolean = true,
    onTouchStart: () -> Unit = {},
    /** Lettera scelta e altezza del dito nella finestra (px), per allineare l'elenco al dito. */
    onLetter: (Char, Float) -> Unit,
    onTouchEnd: () -> Unit = {},
    /** Doppio tocco sulla colonna (es. blocco schermo); null = disattivato. */
    onDoubleTap: (() -> Unit)? = null,
) {
    if (letters.isEmpty()) return
    val colors = LocalOndaColors.current
    val haptics = LocalHapticFeedback.current
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
    // Bordo superiore della colonna nella finestra: serve per passare l'altezza del dito
    val originY = remember { floatArrayOf(0f) }

    val letterStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.onWallpaperMuted)
    val bubbleStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold, color = colors.onAccentContainer)
    // Misuriamo le lettere una sola volta
    val measured = remember(letters, letterStyle) { letters.map { measurer.measure(it.toString(), letterStyle) } }
    val bubbleLayouts = remember(letters, bubbleStyle) { letters.map { measurer.measure(it.toString(), bubbleStyle) } }

    // La colonna è alta quanto le lettere (passo massimo [MAX_LETTER_STEP]) e ancorata in basso,
    // vicino al pollice: con poche lettere è più corta, con molte usa al massimo lo spazio disponibile.
    BoxWithConstraints(modifier.fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
    val columnHeight = minOf(maxHeight, MAX_LETTER_STEP * letters.size)
    Canvas(
        Modifier
            .height(columnHeight)
            .width(if (showLetters) 36.dp else 28.dp)
            .onGloballyPositioned { originY[0] = it.positionInRoot().y }
            .pointerInput(letters.size) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val step = size.height.toFloat() / currentLetters.size
                    currentOnStart()
                    scope.launch { intensity.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)) }

                    fun update(y: Float) {
                        touchY.floatValue = y.coerceIn(0f, size.height.toFloat())
                        val idx = (y / step).toInt().coerceIn(0, currentLetters.lastIndex)
                        if (idx != selected.intValue) {
                            selected.intValue = idx
                            if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentOnLetter(currentLetters[idx], originY[0] + touchY.floatValue)
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
                    scope.launch {
                        intensity.animateTo(0f, tween(300, easing = Motion.Standard))
                        selected.intValue = -1
                    }
                }
            }
    ) {
        val n = letters.size
        val step = size.height / n
        val k = intensity.value
        val center = touchY.floatValue / step - 0.5f
        val maxShift = 52.dp.toPx()
        val sigma = 2.4f
        val dir = if (leftSide) 1f else -1f

        if (showLetters) {
            for (i in 0 until n) {
                val d = i - center
                val wave = if (k > 0f) exp(-(d * d) / (2 * sigma * sigma)) * k else 0f
                val layout = measured[i]
                val cx = size.width / 2f + dir * maxShift * wave
                val cy = i * step + step / 2f
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
}

/** Distanza massima tra due lettere: oltre, la colonna smette di allungarsi. */
private val MAX_LETTER_STEP = 19.dp
