// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.settings

import com.gabrielepiccione.onda.ui.components.OndaTextField
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.AppBlock
import com.gabrielepiccione.onda.data.AppBlocks
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.data.HolidayRepository
import com.gabrielepiccione.onda.data.Settings
import com.gabrielepiccione.onda.ui.components.ValueSlider
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Blocchi delle app a fasce orarie e giorni, con la sospensione nei giorni festivi, più le app bloccate a mano. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppBlocksSection(
    settings: Settings,
    allApps: List<AppInfo>,
    calendarPermission: Boolean,
    onCalendarPermission: () -> Unit,
    onEdit: ((Settings) -> Settings) -> Unit,
) {
    val colors = LocalOndaColors.current
    val context = LocalContext.current
    var editing by remember { mutableStateOf<AppBlock?>(null) }
    val newName = stringResource(R.string.blocks_new_name)
    // Da dove vengono i giorni festivi: il calendario di Google, o l'elenco interno
    var fromCalendar by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(calendarPermission) { fromCalendar = HolidayRepository(context).around(LocalDate.now()) != null }
    val byKey = remember(allApps) { allApps.associateBy { it.key } }

    SettingsSection(stringResource(R.string.blocks_title)) {
        Text(stringResource(R.string.blocks_intro), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
        if (settings.manualBlocks.isNotEmpty()) {
            SettingsLabel(stringResource(R.string.blocks_manual))
            settings.manualBlocks.forEach { key ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(byKey[key]?.label ?: key, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onEdit { s -> AppBlocks.unblock(s, key) } }) { Text(stringResource(R.string.blocks_unblock)) }
                }
            }
        }
        settings.appBlocks.forEach { b ->
            SettingsItem(
                headline = b.name,
                supporting = stringResource(
                    R.string.blocks_summary,
                    AppBlocks.timeText(b.startMinute),
                    AppBlocks.timeText(b.endMinute),
                    b.apps.size,
                ),
                onClick = { editing = b },
                trailing = {
                    Switch(checked = b.enabled, onCheckedChange = { on -> onEdit { s -> s.copy(appBlocks = AppBlocks.upsert(s.appBlocks, b.copy(enabled = on))) } })
                },
            )
        }
        FilledTonalButton(
            onClick = { editing = AppBlock(id = "b" + System.currentTimeMillis().toString(36), name = newName) },
            modifier = Modifier.padding(top = 8.dp).heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.blocks_add)) }
        SettingsSupporting(
            stringResource(
                when (fromCalendar) {
                    true -> R.string.blocks_holidays_calendar
                    else -> R.string.blocks_holidays_builtin
                },
            ),
        )
    }

    editing?.let { current ->
        BlockEditor(
            initial = current,
            allApps = allApps,
            onCalendarPermission = { if (!calendarPermission) onCalendarPermission() },
            onSave = { saved ->
                onEdit { s -> s.copy(appBlocks = AppBlocks.upsert(s.appBlocks, saved)) }
                editing = null
            },
            onDelete = {
                onEdit { s -> s.copy(appBlocks = AppBlocks.remove(s.appBlocks, current.id)) }
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlockEditor(
    initial: AppBlock,
    allApps: List<AppInfo>,
    onCalendarPermission: () -> Unit,
    onSave: (AppBlock) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalOndaColors.current
    var b by remember { mutableStateOf(initial) }
    var pickingApps by remember { mutableStateOf(false) }
    val locale = Locale.getDefault()
    val names = remember(allApps, b.apps) { allApps.filter { it.key in b.apps }.joinToString(", ") { it.label } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.blocks_edit_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OndaTextField(
                    value = b.name,
                    onValueChange = { b = b.copy(name = it) },
                    label = { Text(stringResource(R.string.auto_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                SettingsItem(
                    headline = stringResource(R.string.blocks_apps, b.apps.size),
                    supporting = names.ifBlank { stringResource(R.string.blocks_apps_none) },
                    onClick = { pickingApps = true },
                )
                SettingsLabel(stringResource(R.string.blocks_days))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..7).forEach { d ->
                        FilterChip(
                            selected = d in b.days,
                            onClick = { b = b.copy(days = if (d in b.days) b.days - d else b.days + d) },
                            label = { Text(DayOfWeek.of(d).getDisplayName(TextStyle.SHORT, locale)) },
                        )
                    }
                }
                SettingsSupporting(stringResource(R.string.blocks_days_hint))
                ValueSlider(
                    stringResource(R.string.blocks_from),
                    AppBlocks.timeText(b.startMinute),
                    (b.startMinute / 15).toFloat(),
                    88f,
                    0f..95f,
                    steps = 94,
                ) { v -> b = b.copy(startMinute = v.toInt() * 15) }
                ValueSlider(
                    stringResource(R.string.blocks_to),
                    AppBlocks.timeText(b.endMinute),
                    (b.endMinute / 15).toFloat(),
                    28f,
                    0f..95f,
                    steps = 94,
                ) { v -> b = b.copy(endMinute = v.toInt() * 15) }
                SettingsSupporting(stringResource(if (b.startMinute == b.endMinute) R.string.blocks_all_day else R.string.blocks_overnight_hint))
                SettingsSwitch(stringResource(R.string.blocks_skip_holidays), stringResource(R.string.blocks_skip_holidays_sub), b.skipHolidays) { on ->
                    b = b.copy(skipHolidays = on)
                    if (on) onCalendarPermission()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(b.copy(name = b.name.trim().ifBlank { initial.name })) }) { Text(stringResource(R.string.auto_save)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text(stringResource(R.string.auto_delete)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )

    if (pickingApps) {
        AlertDialog(
            onDismissRequest = { pickingApps = false },
            title = { Text(stringResource(R.string.blocks_pick_apps)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(allApps, key = { it.key }) { app ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { b = b.copy(apps = if (app.key in b.apps) b.apps - app.key else b.apps + app.key) },
                        ) {
                            Checkbox(checked = app.key in b.apps, onCheckedChange = null)
                            Text(app.label, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickingApps = false }) { Text(stringResource(R.string.ok)) } },
        )
    }
}
