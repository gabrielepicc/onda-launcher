// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.components

import com.gabrielepiccione.onda.util.HapticEvent
import com.gabrielepiccione.onda.util.LocalOndaHaptics
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.ui.theme.LocalHapticsEnabled
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Gesto orizzontale standard di Onda sulle righe delle app:
 * - destra → [onRight] (pannello dell'app), sempre disponibile;
 * - sinistra → [onLeft] (elimina le notifiche), solo se [canDismiss]; altrimenti la riga
 *   si sposta appena e torna indietro.
 * Lo sfondo rivela l'azione e vibra quando si supera la soglia.
 */
@Composable
fun SwipeActions(
    canDismiss: Boolean,
    onRight: () -> Unit,
    onLeft: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = LocalOndaColors.current
    val haptics = LocalHapticFeedback.current
    val ondaHaptics = LocalOndaHaptics.current
    val hapticsOn = LocalHapticsEnabled.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val offsetX = remember { Animatable(0f) }
    // Poco scorrimento: la riga si tende appena e al suo posto cresce una bolla
    val triggerPx = with(density) { 44.dp.toPx() }
    val maxPx = with(density) { 64.dp.toPx() }
    var armed by remember { mutableStateOf(false) }
    val currentRight by rememberUpdatedState(onRight)
    val currentLeft by rememberUpdatedState(onLeft)
    val currentCanDismiss by rememberUpdatedState(canDismiss)

    Box(modifier.fillMaxWidth()) {
        val x = offsetX.value
        if (abs(x) > 1f) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (x > 0f) Arrangement.Start else Arrangement.End,
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 12.dp),
            ) {
                val p = (abs(x) / triggerPx).coerceIn(0f, 1f)
                val bubble by animateFloatAsState(if (armed) 1.25f else 1f, spring(dampingRatio = 0.4f, stiffness = 500f), label = "swipeBubble")
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .graphicsLayer { alpha = p; scaleX = (0.3f + 0.7f * p) * bubble; scaleY = (0.3f + 0.7f * p) * bubble }
                        .clip(CircleShape)
                        .background(if (armed) colors.accentContainer else colors.panel),
                ) {
                    if (x > 0f) Icon(Icons.Filled.KeyboardArrowRight, null, tint = colors.onAccentContainer)
                    else if (canDismiss) Icon(Icons.Filled.Close, null, tint = colors.onAccentContainer)
                }
            }
        }
        Box(
            Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .then(if (abs(x) > 1f) Modifier.clip(RoundedCornerShape(24.dp)).background(colors.scrim) else Modifier)
                .draggable(
                    enabled = enabled,
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val resist = if (abs(offsetX.value) > triggerPx) 0.35f else 1f
                            val min = if (currentCanDismiss) -maxPx else -maxPx * 0.2f
                            val next = (offsetX.value + delta * resist).coerceIn(min, maxPx)
                            offsetX.snapTo(next)
                            val nowArmed = next >= triggerPx || (currentCanDismiss && next <= -triggerPx)
                            if (nowArmed != armed) {
                                armed = nowArmed
                                ondaHaptics?.perform(if (nowArmed) HapticEvent.THRESHOLD_ON else HapticEvent.THRESHOLD_OFF)
                            }
                        }
                    },
                    onDragStopped = {
                        if (armed) {
                            if (offsetX.value > 0f) currentRight() else currentLeft()
                        }
                        armed = false
                        offsetX.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 500f))
                    },
                )
        ) { content() }
    }
}
