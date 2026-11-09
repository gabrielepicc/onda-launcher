// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.settings

import com.gabrielepiccione.onda.data.HomeBlocks
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.content.pm.ShortcutInfo
import androidx.compose.foundation.layout.Arrangement
import com.gabrielepiccione.onda.ui.components.TonalIcon
import com.gabrielepiccione.onda.ui.components.actionIcon
import com.gabrielepiccione.onda.data.AutomationOverrides
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Surface
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    /** Nome di un widget della home, per «Mostra widget» e «Nascondi widget». */
    widgetLabel: (Int) -> String? = { null },
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
            widgetLabel = widgetLabel,
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

/** Gruppi di condizioni nell'editor, ognuno con le sue scelte. */
private val TRIGGER_GROUPS: List<Pair<Int, List<AutomationTrigger>>> = listOf(
    R.string.auto_group_home to listOf(AutomationTrigger.HOME_ARRIVE, AutomationTrigger.HOME_LEAVE),
    R.string.auto_group_device to listOf(
        AutomationTrigger.DEVICE_ON, AutomationTrigger.DEVICE_OFF, AutomationTrigger.HEADPHONES_ON, AutomationTrigger.HEADPHONES_OFF,
    ),
    R.string.auto_group_charging to listOf(AutomationTrigger.CHARGING_ON, AutomationTrigger.CHARGING_OFF),
    R.string.auto_group_time to listOf(AutomationTrigger.TIME),
    R.string.auto_group_sun to listOf(AutomationTrigger.SUNSET, AutomationTrigger.SUNRISE),
    R.string.auto_group_music to listOf(AutomationTrigger.MUSIC_ON, AutomationTrigger.MUSIC_OFF),
)

/** Scheda tonale dell'editor: titolo nel colore primario e contenuto. */
@Composable
private fun EditorCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        content()
    }
}

/** Editor a tutta altezza: «Quando», «Allora», «Promemoria» e la barra delle azioni in basso. */
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
    widgetLabel: (Int) -> String?,
    onSave: (Automation) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalOndaColors.current
    val scheme = MaterialTheme.colorScheme
    var a by remember { mutableStateOf(initial) }
    var pickingAction by remember { mutableStateOf(false) }
    val devices = remember { connectedDevices().toList().sorted() }
    val locale = Locale.getDefault()
    val profileName: (String) -> String? = { id -> settings.contextProfiles.firstOrNull { it.id == id }?.name }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = scheme.surface, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.auto_edit_title), style = MaterialTheme.typography.headlineSmall, color = colors.onWallpaper)
                    OutlinedTextField(
                        value = a.name,
                        onValueChange = { a = a.copy(name = it) },
                        label = { Text(stringResource(R.string.auto_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    EditorCard(stringResource(R.string.auto_when)) {
                        TRIGGER_GROUPS.forEach { (groupRes, triggers) ->
                            Text(stringResource(groupRes), style = MaterialTheme.typography.labelMedium, color = colors.onWallpaperMuted)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                triggers.forEach { t ->
                                    FilterChip(selected = a.trigger == t, onClick = { a = a.copy(trigger = t) }, label = { Text(stringResource(t.labelRes)) })
                                }
                            }
                        }
                        if (a.trigger == AutomationTrigger.DEVICE_ON || a.trigger == AutomationTrigger.DEVICE_OFF) {
                            OutlinedTextField(
                                value = a.device,
                                onValueChange = { a = a.copy(device = it) },
                                label = { Text(stringResource(R.string.auto_device)) },
                                singleLine = true,
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
                            ValueSlider(stringResource(R.string.auto_hour), "%02d".format(a.minute / 60), (a.minute / 60).toFloat(), 22f, 0f..23f, steps = 22) { v ->
                                a = a.copy(minute = v.toInt() * 60 + a.minute % 60)
                            }
                            ValueSlider(stringResource(R.string.auto_minute), "%02d".format(a.minute % 60), (a.minute % 60).toFloat(), 0f, 0f..55f, steps = 10) { v ->
                                a = a.copy(minute = a.minute / 60 * 60 + (v / 5).toInt() * 5)
                            }
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
                    }

                    EditorCard(stringResource(R.string.auto_do)) {
                        if (a.actions.isEmpty()) {
                            Text(stringResource(R.string.auto_actions_none), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
                        }
                        a.actions.forEach { encoded ->
                            val choice = Actions.decode(encoded)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                                TonalIcon(actionIcon(choice.action))
                                Text(
                                    actionSummary(choice, allApps, "", profileName),
                                    color = colors.onWallpaper,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                )
                                IconButton(onClick = { a = a.copy(actions = a.actions - encoded) }) {
                                    Icon(Icons.Filled.Delete, stringResource(R.string.remove), tint = colors.onWallpaperMuted)
                                }
                            }
                        }
                        FilledTonalButton(onClick = { pickingAction = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.auto_add_action), modifier = Modifier.padding(start = 8.dp))
                        }
                        if (settings.contextProfiles.isNotEmpty()) {
                            SettingsSupporting(stringResource(R.string.auto_profile))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = a.profile.isBlank(), onClick = { a = a.copy(profile = "") }, label = { Text(stringResource(R.string.auto_profile_none)) })
                                settings.contextProfiles.forEach { p ->
                                    FilterChip(selected = a.profile == p.id, onClick = { a = a.copy(profile = p.id) }, label = { Text(p.name) })
                                }
                            }
                        }
                    }

                    EditorCard(stringResource(R.string.auto_reminder_title)) {
                        OutlinedTextField(
                            value = a.reminder,
                            onValueChange = { a = a.copy(reminder = it) },
                            label = { Text(stringResource(R.string.auto_reminder)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Barra in basso: Elimina a sinistra (colore d'errore), Annulla e Salva a destra, ben separati
                HorizontalDivider(color = scheme.outlineVariant)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = scheme.error)) {
                        Text(stringResource(R.string.auto_delete))
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { onSave(a.copy(name = a.name.trim().ifBlank { initial.name })) }) { Text(stringResource(R.string.auto_save)) }
                }
            }
        }
    }

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
            profiles = settings.contextProfiles.map { it.id to it.name },
            defaultRestore = AutomationOverrides.defaultRestore(a.trigger),
            homeWidgets = settings.homeLayout.mapNotNull { HomeBlocks.widgetId(it) }.map { id ->
                id to (widgetLabel(id) ?: stringResource(R.string.wrule_widget_n, id))
            },
        )
    }
}
