// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.WidgetGroupRef
import com.gabrielepiccione.onda.data.WidgetOrder
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import com.gabrielepiccione.onda.ui.theme.LocalOndaTokens
import com.gabrielepiccione.onda.util.HapticEvent
import com.gabrielepiccione.onda.util.LocalOndaHaptics

/**
 * «Ordine nel carosello»: le pagine di un gruppo di widget (della home o di una cartella) con maniglia per
 * trascinare, pulsanti Su e Giù, e per ogni voce «Sposta in…» e «Rimuovi». Lo stesso componente serve i caroselli
 * della home e quelli delle cartelle; l'ordine si salva a ogni spostamento.
 */
@Composable
fun WidgetOrderSheet(
    widgets: List<Int>,
    labelOf: (Int) -> String,
    iconOf: (Int) -> ImageBitmap?,
    targets: List<WidgetGroupRef>,
    targetName: (WidgetGroupRef) -> String,
    onReorder: (from: Int, to: Int) -> Unit,
    onMoveTo: (widgetId: Int, target: WidgetGroupRef) -> Unit,
    onRemove: (widgetId: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var removing by remember { mutableStateOf<Int?>(null) }
    removing?.let { id ->
        ConfirmDeleteWidgetDialog(onConfirm = { removing = null; onRemove(id) }, onDismiss = { removing = null })
    }
    val haptics = LocalOndaHaptics.current
    val rowPx = with(LocalDensity.current) { ROW_HEIGHT.dp.toPx() }
    var dragging by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val list by rememberUpdatedState(widgets)
    SheetScaffold(title = stringResource(R.string.order_title), onDismiss = onDismiss) {
        Text(stringResource(R.string.order_hint), color = LocalOndaColors.current.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
        widgets.forEachIndexed { index, id ->
            key(id) {
                val isDragged = dragging == id
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ROW_HEIGHT.dp)
                        .graphicsLayer { translationY = if (isDragged) dragOffset else 0f }
                        .then(if (isDragged) Modifier.shadow(4.dp, LocalOndaTokens.current.shapes.medium) else Modifier)
                        .clip(LocalOndaTokens.current.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(start = 4.dp),
                ) {
                    // Maniglia: si trascina per spostare la pagina; ogni riga attraversata è una vibrazione
                    Box(
                        Modifier
                            .size(48.dp)
                            .pointerInput(id) {
                                detectDragGestures(
                                    onDragStart = { dragging = id; dragOffset = 0f },
                                    onDragEnd = { dragging = null; dragOffset = 0f },
                                    onDragCancel = { dragging = null; dragOffset = 0f },
                                ) { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                    val current = list.indexOf(id)
                                    val target = WidgetOrder.targetIndex(current, dragOffset, rowPx, list.size)
                                    if (target != current) {
                                        haptics?.perform(HapticEvent.THRESHOLD_ON)
                                        onReorder(current, target)
                                        dragOffset -= (target - current) * rowPx
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.DragHandle, stringResource(R.string.order_drag), tint = LocalOndaColors.current.onWallpaperMuted) }
                    val icon = remember(id) { iconOf(id) }
                    if (icon != null) Image(icon, null, Modifier.size(32.dp).clip(LocalOndaTokens.current.shapes.small))
                    else Icon(Icons.Filled.Widgets, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        labelOf(id),
                        color = LocalOndaColors.current.onWallpaper,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    )
                    IconButton(onClick = { onReorder(index, index - 1) }, enabled = index > 0) {
                        Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.move_up))
                    }
                    IconButton(onClick = { onReorder(index, index + 1) }, enabled = index < widgets.lastIndex) {
                        Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.move_down))
                    }
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.more_options)) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = LocalOndaTokens.current.shapes.medium) {
                            targets.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.order_move_to, targetName(t))) },
                                    leadingIcon = { Icon(Icons.Filled.Widgets, null) },
                                    onClick = { menu = false; onMoveTo(id, t) },
                                )
                            }
                            if (targets.isNotEmpty()) HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menu = false; removing = id },
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val ROW_HEIGHT = 64
