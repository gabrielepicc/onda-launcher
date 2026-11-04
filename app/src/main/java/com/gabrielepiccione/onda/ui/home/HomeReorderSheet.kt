// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(ExperimentalMaterial3Api::class)

package com.gabrielepiccione.onda.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.Reorder
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors

private val ROW_HEIGHT = 56.dp

/** Riordino trascinando: voce = chiave da salvare + nome da mostrare. */
@Composable
fun HomeReorderSheet(
    blocks: List<Pair<String, String>>,
    entries: List<Pair<String, String>>,
    onBlocks: (List<String>) -> Unit,
    onEntries: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalOndaColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.panel,
        contentColor = colors.onWallpaper,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(stringResource(R.string.reorder_home), style = MaterialTheme.typography.headlineSmall)
            ReorderTitle(R.string.reorder_blocks)
            ReorderableColumn(blocks, onBlocks)
            if (entries.isNotEmpty()) {
                ReorderTitle(R.string.reorder_favorites)
                ReorderableColumn(entries, onEntries)
            }
            Button(onClick = onDismiss, modifier = Modifier.padding(top = 12.dp).align(Alignment.End)) {
                Text(stringResource(R.string.edit_done))
            }
        }
    }
}

@Composable
private fun ReorderTitle(res: Int) {
    Text(
        stringResource(res),
        color = LocalOndaColors.current.accent,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
    )
}

@Composable
private fun ReorderableColumn(items: List<Pair<String, String>>, onOrder: (List<String>) -> Unit) {
    val colors = LocalOndaColors.current
    val rowPx = with(LocalDensity.current) { ROW_HEIGHT.toPx() }
    val currentItems by rememberUpdatedState(items)
    val currentOnOrder by rememberUpdatedState(onOrder)
    var dragged by remember { mutableStateOf<String?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    val from = items.indexOfFirst { it.first == dragged }
    val target = if (from >= 0) Reorder.target(from, offset, rowPx, items.size) else -1
    val dragDescription = stringResource(R.string.reorder_handle)

    Column {
        items.forEachIndexed { index, (id, label) ->
            key(id) {
                val isDragged = id == dragged
                val shift by animateFloatAsState(
                    Reorder.shift(index, from, target) * rowPx,
                    // Al rilascio la lista è già nel nuovo ordine: niente rimbalzo
                    if (from >= 0) spring(stiffness = 700f) else snap(),
                    label = "reorderShift",
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ROW_HEIGHT)
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragged) offset else shift }
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDragged) colors.panelHigh else Color.Transparent)
                        .padding(start = 12.dp),
                ) {
                    Text(
                        label,
                        color = colors.onWallpaper,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .pointerInput(id) {
                                detectDragGestures(
                                    onDragStart = {
                                        dragged = id
                                        offset = 0f
                                    },
                                    onDragEnd = {
                                        val list = currentItems
                                        val start = list.indexOfFirst { it.first == id }
                                        if (start >= 0) {
                                            val end = Reorder.target(start, offset, rowPx, list.size)
                                            if (end != start) currentOnOrder(Reorder.moved(list, start, end).map { it.first })
                                        }
                                        dragged = null
                                        offset = 0f
                                    },
                                    onDragCancel = {
                                        dragged = null
                                        offset = 0f
                                    },
                                ) { change, amount ->
                                    change.consume()
                                    offset += amount.y
                                }
                            },
                    ) {
                        Icon(Icons.Filled.DragHandle, dragDescription, tint = colors.onWallpaperMuted)
                    }
                }
            }
        }
    }
}
