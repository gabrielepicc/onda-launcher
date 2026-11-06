// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(ExperimentalMaterial3Api::class)

package com.gabrielepiccione.onda.ui.home

import com.gabrielepiccione.onda.data.WidgetSize
import com.gabrielepiccione.onda.data.WidgetSizing
import com.gabrielepiccione.onda.ui.components.WidgetSizeControls
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import com.gabrielepiccione.onda.ui.settings.AlignTiles
import com.gabrielepiccione.onda.ui.settings.ClockStyleTiles
import com.gabrielepiccione.onda.ui.settings.MediaStyleTiles
import com.gabrielepiccione.onda.ui.settings.TextChoices
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gabrielepiccione.onda.ui.components.ConfirmDeleteWidgetDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.HomeBlocks
import com.gabrielepiccione.onda.data.Settings
import com.gabrielepiccione.onda.ui.components.ValueSlider
import com.gabrielepiccione.onda.ui.components.percentText
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import kotlin.math.roundToInt

/** Azioni del foglio di modifica di un blocco della home. */
interface HomeEditActions {
    fun edit(transform: (Settings) -> Settings)
    fun move(block: String, delta: Int)
    fun setWidgetHeight(id: Int, dp: Int)
    fun replaceWidget(id: Int)
    fun replaceClock()
    fun addToStack(block: String)
    fun setStackHeight(stackId: String, dp: Int)
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
    widgetLabel: (Int) -> String?,
    actions: HomeEditActions,
) {
    val colors = LocalOndaColors.current
    val index = blocks.indexOf(block)
    var widgetToDelete by remember { mutableStateOf<Int?>(null) }
    widgetToDelete?.let { id ->
        ConfirmDeleteWidgetDialog(
            onConfirm = {
                widgetToDelete = null
                actions.deleteWidget(id)
                // Un widget singolo sparisce con il suo blocco; in una pila il foglio resta aperto
                if (HomeBlocks.widgetId(block) != null) actions.done()
            },
            onDismiss = { widgetToDelete = null },
        )
    }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val widgetId = HomeBlocks.widgetId(block)
            val stack = settings.widgetStacks.firstOrNull { it.id == HomeBlocks.stackId(block) }
            Text(
                stringResource(
                    when {
                        widgetId != null -> R.string.edit_widget
                        stack != null -> R.string.edit_stack
                        block == HomeBlocks.MEDIA -> R.string.edit_media
                        block == HomeBlocks.AGENDA -> R.string.agenda_title
                        block == HomeBlocks.NOTIFICATIONS -> R.string.summary_title
                        else -> R.string.edit_clock
                    }
                ),
                style = MaterialTheme.typography.headlineSmall,
            )

            if (block == HomeBlocks.CLOCK) {
                SectionTitle(R.string.edit_style)
                ClockStyleTiles(settings.clockStyle, compact = true) { st -> actions.edit { it.copy(clockStyle = st) } }
                ValueSlider(stringResource(R.string.edit_size), percentText(settings.clockScale), settings.clockScale, Settings().clockScale, 0.6f..1.6f, accentLabel = true) { v ->
                    actions.edit { it.copy(clockScale = v) }
                }
                SectionTitle(R.string.edit_alignment)
                AlignTiles(settings.clockAlign) { a -> actions.edit { it.copy(clockAlign = a) } }
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
                MediaStyleTiles(settings.mediaCompact) { compact -> actions.edit { it.copy(mediaCompact = compact) } }
                ValueSlider(stringResource(R.string.edit_size), percentText(settings.mediaScale), settings.mediaScale, Settings().mediaScale, 0.75f..1.25f, accentLabel = true) { v ->
                    actions.edit { it.copy(mediaScale = v) }
                }
            }

            val maxWidth = LocalConfiguration.current.screenWidthDp - 32
            if (widgetId != null) {
                val key = WidgetSizing.widgetKey(widgetId)
                WidgetSizeControls(
                    size = settings.widgetSizes[key] ?: WidgetSize(),
                    height = settings.widgetHeights[widgetId] ?: defaultWidgetHeight(widgetId),
                    defaultHeight = defaultWidgetHeight(widgetId),
                    maxWidth = maxWidth,
                    onSize = { size -> actions.edit { WidgetSizing.set(it, key, size) } },
                    onHeight = { h -> actions.setWidgetHeight(widgetId, h ?: defaultWidgetHeight(widgetId)) },
                )
            }

            if (stack != null) {
                val stackDefault = stack.widgets.maxOfOrNull { defaultWidgetHeight(it) } ?: 160
                val key = WidgetSizing.stackKey(stack.id)
                WidgetSizeControls(
                    size = settings.widgetSizes[key] ?: WidgetSize(),
                    height = stack.heightDp ?: stackDefault,
                    defaultHeight = stackDefault,
                    maxWidth = maxWidth,
                    onSize = { size -> actions.edit { WidgetSizing.set(it, key, size) } },
                    onHeight = { h -> actions.setStackHeight(stack.id, h ?: stackDefault) },
                )
                SectionTitle(R.string.stack_widgets)
                stack.widgets.forEach { id ->
                    val label = widgetLabel(id) ?: stringResource(R.string.widget_number, id)
                    SheetRow(Icons.Filled.Delete, stringResource(R.string.widget_remove_named, label)) { widgetToDelete = id }
                }
                SheetRow(Icons.Filled.Add, stringResource(R.string.stack_add)) { actions.addToStack(block) }
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

            SectionTitle(R.string.home_position)
            HomePositionControl(settings) { actions.edit(it) }

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
            if (block == HomeBlocks.NOTIFICATIONS) {
                SheetRow(Icons.Filled.Delete, stringResource(R.string.summary_hide)) {
                    actions.edit { it.copy(showNotificationSummary = false) }
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
                SheetRow(Icons.Filled.Add, stringResource(R.string.stack_make)) { actions.addToStack(block) }
                SheetRow(Icons.Filled.Refresh, stringResource(R.string.edit_replace_widget)) { actions.replaceWidget(widgetId) }
                SheetRow(Icons.Filled.Delete, stringResource(R.string.edit_delete_widget)) { widgetToDelete = widgetId }
            }

            Button(onClick = { actions.done() }, modifier = Modifier.padding(top = 8.dp).align(Alignment.End)) {
                Text(stringResource(R.string.edit_done))
            }
        }
    }
}

/**
 * Sceglie dove inizia il primo blocco della home: ancorato in basso (automatico) oppure in un
 * punto preciso dello schermo. I blocchi successivi, preferiti compresi, seguono il primo.
 */
@Composable
fun HomePositionControl(settings: Settings, edit: ((Settings) -> Settings) -> Unit) {
    val manual = settings.homeTopFraction >= 0f
    TextChoices(
        listOf(false to stringResource(R.string.home_position_auto), true to stringResource(R.string.home_position_manual)),
        manual,
        columns = 2,
    ) { wantManual ->
        if (wantManual) { if (!manual) edit { it.copy(homeTopFraction = DEFAULT_HOME_TOP) } } else edit { it.copy(homeTopFraction = -1f) }
    }
    if (manual) {
        Slider(
            value = settings.homeTopFraction,
            onValueChange = { v -> edit { it.copy(homeTopFraction = v) } },
            valueRange = 0f..MAX_HOME_TOP,
        )
        Text(
            stringResource(R.string.home_position_hint),
            color = LocalOndaColors.current.onWallpaperMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private const val DEFAULT_HOME_TOP = 0.25f
const val MAX_HOME_TOP = 0.8f

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
