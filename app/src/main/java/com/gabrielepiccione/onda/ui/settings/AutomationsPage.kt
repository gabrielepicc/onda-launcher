// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.settings

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.ActionChoice
import com.gabrielepiccione.onda.data.Actions
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.data.Automation
import com.gabrielepiccione.onda.data.AutomationTrigger
import com.gabrielepiccione.onda.data.Automations
import com.gabrielepiccione.onda.data.GestureAction
import com.gabrielepiccione.onda.data.Settings
import com.gabrielepiccione.onda.ui.components.ActionPickerDialog
import com.gabrielepiccione.onda.ui.components.ValueSlider
import com.gabrielepiccione.onda.ui.components.actionSummary
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Pagina delle automazioni «se… allora»: elenco, esempi pronti ed editor. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AutomationsPage(
    settings: Settings,
    allApps: List<AppInfo>,
    connectedDevices: () -> Set<String>,
    shortcutsOf: (AppInfo) -> List<ShortcutInfo>,
    accessibilityEnabled: Boolean,
    onAccessibility: () -> Unit,
    onEdit: ((Settings) -> Settings) -> Unit,
) {
    val colors = LocalOndaColors.current
    var editing by remember { mutableStateOf<Automation?>(null) }
    val newName = stringResource(R.string.auto_new)

    SettingsSection(stringResource(R.string.page_automations)) {
        Text(stringResource(R.string.auto_intro), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
        SettingsSupporting(stringResource(R.string.auto_limits))
    }

    SettingsSection(null) {
        if (settings.automations.isEmpty()) {
            Text(stringResource(R.string.auto_empty), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
        }
        settings.automations.forEach { a ->
            SettingsItem(
                headline = a.name,
                supporting = stringResource(R.string.auto_summary, stringResource(a.trigger.labelRes), a.actions.size),
                onClick = { editing = a },
                trailing = {
                    Switch(checked = a.enabled, onCheckedChange = { on -> onEdit { s -> s.copy(automations = Automations.upsert(s.automations, a.copy(enabled = on))) } })
                },
            )
        }
        FilledTonalButton(
            onClick = { editing = Automation(id = "a" + System.currentTimeMillis().toString(36), name = newName, trigger = AutomationTrigger.HOME_ARRIVE) },
            modifier = Modifier.padding(top = 8.dp).heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.auto_new)) }
    }

    SettingsSection(stringResource(R.string.auto_templates)) {
        Automations.TEMPLATES.forEach { t ->
            val name = stringResource(t.nameRes)
            val reminder = t.reminderRes?.let { stringResource(it) }.orEmpty()
            SettingsItem(
                headline = name,
                supporting = stringResource(t.subRes),
                icon = Icons.Filled.Add,
                onClick = {
                    val created = Automations.fromTemplate(t, "a" + System.currentTimeMillis().toString(36), name, reminder)
                    onEdit { s -> s.copy(automations = Automations.upsert(s.automations, created)) }
                    editing = created
                },
            )
        }
    }

    editing?.let { current ->
        AutomationEditor(
            initial = current,
            settings = settings,
            allApps = allApps,
            connectedDevices = connectedDevices,
            shortcutsOf = shortcutsOf,
            accessibilityEnabled = accessibilityEnabled,
            onAccessibility = onAccessibility,
            onSave = { saved ->
                onEdit { s -> s.copy(automations = Automations.upsert(s.automations, saved)) }
                editing = null
            },
            onDelete = {
                onEdit { s -> s.copy(automations = Automations.remove(s.automations, current.id)) }
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AutomationEditor(
    initial: Automation,
    settings: Settings,
    allApps: List<AppInfo>,
    connectedDevices: () -> Set<String>,
    shortcutsOf: (AppInfo) -> List<ShortcutInfo>,
    accessibilityEnabled: Boolean,
    onAccessibility: () -> Unit,
    onSave: (Automation) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalOndaColors.current
    var a by remember { mutableStateOf(initial) }
    var pickingAction by remember { mutableStateOf(false) }
    val devices = remember { connectedDevices().toList().sorted() }
    val locale = Locale.getDefault()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.auto_edit_title)) },
        text = {
            androidx.compose.foundation.layout.Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedTextField(
                    value = a.name,
                    onValueChange = { a = a.copy(name = it) },
                    label = { Text(stringResource(R.string.auto_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )

                SettingsLabel(stringResource(R.string.auto_when))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AutomationTrigger.entries.forEach { t ->
                        FilterChip(selected = a.trigger == t, onClick = { a = a.copy(trigger = t) }, label = { Text(stringResource(t.labelRes)) })
                    }
                }
                if (a.trigger == AutomationTrigger.DEVICE_ON || a.trigger == AutomationTrigger.DEVICE_OFF) {
                    OutlinedTextField(
                        value = a.device,
                        onValueChange = { a = a.copy(device = it) },
                        label = { Text(stringResource(R.string.auto_device)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (devices.isNotEmpty()) {
                        SettingsSupporting(stringResource(R.string.auto_device_pick))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            devices.forEach { d -> FilterChip(selected = a.device == d, onClick = { a = a.copy(device = d) }, label = { Text(d) }) }
                        }
                    }
                }
                if (a.trigger == AutomationTrigger.TIME) {
                    ValueSlider(
                        stringResource(R.string.auto_hour),
                        "%02d".format(a.minute / 60),
                        (a.minute / 60).toFloat(),
                        22f,
                        0f..23f,
                        steps = 22,
                    ) { v -> a = a.copy(minute = v.toInt() * 60 + a.minute % 60) }
                    ValueSlider(
                        stringResource(R.string.auto_minute),
                        "%02d".format(a.minute % 60),
                        (a.minute % 60).toFloat(),
                        0f,
                        0f..55f,
                        steps = 10,
                    ) { v -> a = a.copy(minute = a.minute / 60 * 60 + (v / 5).toInt() * 5) }
                    SettingsSupporting(stringResource(R.string.auto_time, "%02d:%02d".format(a.minute / 60, a.minute % 60)))
                }
                if (a.trigger.isTimed) {
                    SettingsSupporting(stringResource(R.string.auto_days_label))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..7).forEach { d ->
                            FilterChip(
                                selected = d in a.days,
                                onClick = { a = a.copy(days = if (d in a.days) a.days - d else a.days + d) },
                                label = { Text(DayOfWeek.of(d).getDisplayName(TextStyle.SHORT, locale)) },
                            )
                        }
                    }
                }

                SettingsLabel(stringResource(R.string.auto_do))
                if (a.actions.isEmpty()) {
                    Text(stringResource(R.string.auto_actions_none), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
                }
                a.actions.forEach { encoded ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(
                            actionSummary(Actions.decode(encoded), allApps, ""),
                            color = colors.onWallpaper,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { a = a.copy(actions = a.actions - encoded) }) { Text(stringResource(R.string.remove)) }
                    }
                }
                TextButton(onClick = { pickingAction = true }) { Text(stringResource(R.string.auto_add_action)) }

                if (settings.contextProfiles.isNotEmpty()) {
                    SettingsSupporting(stringResource(R.string.auto_profile))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = a.profile.isBlank(), onClick = { a = a.copy(profile = "") }, label = { Text(stringResource(R.string.auto_profile_none)) })
                        settings.contextProfiles.forEach { p ->
                            FilterChip(selected = a.profile == p.id, onClick = { a = a.copy(profile = p.id) }, label = { Text(p.name) })
                        }
                    }
                }
                OutlinedTextField(
                    value = a.reminder,
                    onValueChange = { a = a.copy(reminder = it) },
                    label = { Text(stringResource(R.string.auto_reminder)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(a.copy(name = a.name.trim().ifBlank { initial.name })) }) { Text(stringResource(R.string.auto_save)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text(stringResource(R.string.auto_delete)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )

    if (pickingAction) {
        ActionPickerDialog(
            title = stringResource(R.string.auto_add_action),
            current = ActionChoice(GestureAction.NONE),
            allApps = allApps,
            shortcutsOf = shortcutsOf,
            themedIcons = settings.themedIcons,
            accessibilityEnabled = accessibilityEnabled,
            onAccessibility = onAccessibility,
            onPick = { choice -> a = a.copy(actions = a.actions + Actions.encode(choice)) },
            onDismiss = { pickingAction = false },
            automatable = true,
        )
    }
}
