// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.gabrielepiccione.onda.data.NotificationLayout
import com.gabrielepiccione.onda.util.HapticEvent
import com.gabrielepiccione.onda.util.LocalOndaHaptics
import com.gabrielepiccione.onda.util.SwipeFeedback
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Card che si elimina scorrendola a destra o a sinistra oltre la soglia (la stessa degli swipe sulle righe):
 * vibrazione quando si supera la soglia e quando si torna indietro, conferma a eliminazione fatta.
 * La card scivola via, la lista sotto si richiude senza salti, poi [onDismiss] la toglie davvero.
 */
@Composable
fun SwipeAwayCard(
    enabled: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val ondaHaptics = LocalOndaHaptics.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val trigger = with(density) { NotificationLayout.SWIPE_TRIGGER_DP.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    val collapse = remember { Animatable(1f) }
    var width by remember { mutableStateOf(0) }
    var armed by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    val currentDismiss by rememberUpdatedState(onDismiss)

    Box(
        modifier
            .clipToBounds()
            .layout { measurable, constraints ->
                val p = measurable.measure(constraints)
                width = p.width
                layout(p.width, (p.height * collapse.value).roundToInt()) { p.place(0, 0) }
            }
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .draggable(
                enabled = enabled && !leaving,
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    scope.launch {
                        val next = offsetX.value + delta * SwipeFeedback.resistance(offsetX.value, trigger)
                        offsetX.snapTo(next)
                        val now = SwipeFeedback.isArmed(next, trigger)
                        if (now != armed) {
                            armed = now
                            ondaHaptics?.perform(if (now) HapticEvent.THRESHOLD_ON else HapticEvent.THRESHOLD_OFF)
                        }
                    }
                },
                onDragStopped = {
                    if (armed) {
                        leaving = true
                        ondaHaptics?.perform(HapticEvent.CONFIRM)
                        val away = (if (offsetX.value > 0f) 1f else -1f) * maxOf(width, 1).toFloat()
                        offsetX.animateTo(away, tween(NotificationLayout.SLIDE_OUT_MS))
                        collapse.animateTo(0f, tween(NotificationLayout.COLLAPSE_MS))
                        currentDismiss()
                    } else {
                        armed = false
                        offsetX.animateTo(0f, tween(NotificationLayout.SLIDE_OUT_MS))
                    }
                },
            )
    ) { content() }
}
