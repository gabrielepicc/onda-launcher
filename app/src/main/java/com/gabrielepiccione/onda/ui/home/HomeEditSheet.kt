// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(ExperimentalMaterial3Api::class)

package com.gabrielepiccione.onda.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.ClockStyle
import com.gabrielepiccione.onda.data.HAlign
import com.gabrielepiccione.onda.data.HomeBlocks
import com.gabrielepiccione.onda.data.Settings
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import kotlin.math.roundToInt

/** Azioni del foglio di modifica di un blocco della home. */
interface HomeEditActions {
    fun edit(transform: (Settings) -> Settings)
    fun move(block: String, delta: Int)
    fun setWidgetHeight(id: Int, dp: Int)
    fun replaceWidget(id: Int)
    fun replaceClock()
    fun deleteWidget(id: Int)
    fun done()
}

/**
 * Foglio di modifica per orologio o widget. Lo scrim è trasparente, così le modifiche
 * (stile, dimensione, altezza, posizione) si vedono dal vivo sulla home.
 */
@Composable
fun HomeEditSheet(
    block: String,
    settings: Settings,
    blocks: List<String>,
    defaultWidgetHeight: (Int) -> Int,
    actions: HomeEditActions,
) {
    val colors = LocalOndaColors.current
    val index = blocks.indexOf(block)
    ModalBottomSheet(
        onDismissRequest = { actions.done() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.panel,
        contentColor = colors.onWallpaper,
        scrimColor = Color.Transparent,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val widgetId = HomeBlocks.widgetId(block)
            Text(
                stringResource(
                    when {
                        widgetId != null -> R.string.edit_widget
                        block == HomeBlocks.MEDIA -> R.string.edit_media
                        block == HomeBlocks.AGENDA -> R.string.agenda_title
                        else -> R.string.edit_clock
                    }
                ),
                style = MaterialTheme.typography.headlineSmall,
            )

            if (block == HomeBlocks.CLOCK) {
                SectionTitle(R.string.edit_style)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClockStyle.entries.forEach { st ->
                        FilterChip(
                            selected = settings.clockStyle == st,
                            onClick = { actions.edit { it.copy(clockStyle = st) } },
                            label = { Text(stringResource(st.labelRes)) },
                        )
                    }
                }
                SectionTitle(R.string.edit_size)
                Slider(
                    value = settings.clockScale,
                    onValueChange = { v -> actions.edit { it.copy(clockScale = v) } },
                    valueRange = 0.6f..1.6f,
                )
                SectionTitle(R.string.edit_alignment)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HAlign.entries.forEach { a ->
                        FilterChip(
                            selected = settings.clockAlign == a,
                            onClick = { actions.edit { it.copy(clockAlign = a) } },
                            label = { Text(stringResource(a.labelRes)) },
                        )
                    }
                }
            }

            if (block == HomeBlocks.AGENDA) {
                SectionTitle(R.string.agenda_days)
                Slider(
                    value = settings.agendaDays.toFloat(),
                    onValueChange = { v -> actions.edit { it.copy(agendaDays = v.roundToInt().coerceIn(1, 7)) } },
                    valueRange = 1f..7f,
                    steps = 5,
                )
                Text(
                    pluralStringResourceCompat(settings.agendaDays),
                    color = LocalOndaColors.current.onWallpaperMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (block == HomeBlocks.MEDIA) {
                SectionTitle(R.string.media_style)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = settings.mediaCompact, onClick = { actions.edit { it.copy(mediaCompact = true) } }, label = { Text(stringResource(R.string.media_compact)) })
                    FilterChip(selected = !settings.mediaCompact, onClick = { actions.edit { it.copy(mediaCompact = false) } }, label = { Text(stringResource(R.string.media_full)) })
                }
                SectionTitle(R.string.edit_size)
                Slider(
                    value = settings.mediaScale,
                    onValueChange = { v -> actions.edit { it.copy(mediaScale = v) } },
                    valueRange = 0.75f..1.25f,
                )
            }

            if (widgetId != null) {
                SectionTitle(R.string.edit_height)
                val current = settings.widgetHeights[widgetId] ?: defaultWidgetHeight(widgetId)
                Slider(
                    value = current.toFloat(),
                    onValueChange = { v -> actions.setWidgetHeight(widgetId, v.roundToInt()) },
                    valueRange = 64f..520f,
                )
            }

            SectionTitle(R.string.edit_position)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = { actions.move(block, -1) }, enabled = index > 0) {
                    Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.move_up))
                }
                FilledTonalIconButton(onClick = { actions.move(block, 1) }, enabled = index in 0 until blocks.lastIndex) {
                    Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.move_down))
                }
            }

            if (block == HomeBlocks.CLOCK) {
                SheetRow(Icons.Filled.Refresh, stringResource(R.string.edit_replace_clock)) { actions.replaceClock() }
                SheetRow(Icons.Filled.Delete, stringResource(R.string.edit_hide_clock)) {
                    actions.edit { it.copy(showClock = false) }
                    actions.done()
                }
            }
            if (block == HomeBlocks.AGENDA) {
                SheetRow(Icons.Filled.Delete, stringResource(R.string.agenda_hide)) {
                    actions.edit { it.copy(showAgenda = false) }
                    actions.done()
                }
            }
            if (block == HomeBlocks.MEDIA) {
                SheetRow(Icons.Filled.Delete, stringResource(R.string.edit_hide_media)) {
                    actions.edit { it.copy(showMedia = false) }
                    actions.done()
                }
            }
            if (widgetId != null) {
                SheetRow(Icons.Filled.Refresh, stringResource(R.string.edit_replace_widget)) { actions.replaceWidget(widgetId) }
                SheetRow(Icons.Filled.Delete, stringResource(R.string.edit_delete_widget)) {
                    actions.deleteWidget(widgetId)
                    actions.done()
                }
            }

            Button(onClick = { actions.done() }, modifier = Modifier.padding(top = 8.dp).align(Alignment.End)) {
                Text(stringResource(R.string.edit_done))
            }
        }
    }
}

@Composable
private fun pluralStringResourceCompat(days: Int): String = stringResource(R.string.agenda_days_value, days)

@Composable
private fun SectionTitle(res: Int) {
    Text(
        stringResource(res),
        color = LocalOndaColors.current.accent,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 10.dp),
    )
}
