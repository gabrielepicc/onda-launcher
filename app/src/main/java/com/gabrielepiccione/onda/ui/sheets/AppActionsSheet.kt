// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(ExperimentalMaterial3Api::class)

package com.gabrielepiccione.onda.ui.sheets

import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.FilledTonalButton
import com.gabrielepiccione.onda.ui.components.SwitchRow
import com.gabrielepiccione.onda.ui.components.SheetScaffold
import com.gabrielepiccione.onda.ui.components.GroupCard
import com.gabrielepiccione.onda.ui.components.DestructiveRow
import com.gabrielepiccione.onda.ui.components.ActionRow
import com.gabrielepiccione.onda.data.AppOptions
import com.gabrielepiccione.onda.data.AppOptionGroup
import com.gabrielepiccione.onda.data.AppOption
import com.gabrielepiccione.onda.ui.theme.*
import com.gabrielepiccione.onda.ui.components.*
import com.gabrielepiccione.onda.ui.home.*
import com.gabrielepiccione.onda.ui.drawer.*
import com.gabrielepiccione.onda.ui.search.*
import com.gabrielepiccione.onda.ui.settings.*

import androidx.compose.ui.res.stringResource
import com.gabrielepiccione.onda.R
import android.content.pm.ShortcutInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.data.ActionChoice
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.ui.components.ActionPickerDialog
import com.gabrielepiccione.onda.ui.components.actionSummary

data class AppActions(
    val shortcuts: List<ShortcutInfo>,
    val shortcutIcon: (ShortcutInfo) -> androidx.compose.ui.graphics.ImageBitmap?,
    /** Azione dello swipe a destra sulla riga (catalogo unico, vedi [Actions]); Nessuna = il pannello dell'app. */
    val swipe: ActionChoice,
    val allApps: List<AppInfo>,
    val themedIcons: Boolean,
    val onSetSwipe: (ActionChoice) -> Unit,
    val shortcutsOf: (AppInfo) -> List<ShortcutInfo>,
    val accessibilityEnabled: Boolean,
    val onAccessibility: () -> Unit,
    val folderName: String?,
    val onChangeIcon: () -> Unit,
    val isProtected: Boolean = false,
    val onToggleProtected: () -> Unit = {},
    val hasBreaker: Boolean = false,
    val onToggleBreaker: () -> Unit = {},
    val hasReminder: Boolean = false,
    val onToggleReminder: () -> Unit = {},
    val onMoveToFolder: () -> Unit,
    val isMuted: Boolean,
    val favoriteIndex: Int,
    val favoriteCount: Int,
    val onShortcut: (ShortcutInfo) -> Unit,
    val onToggleFavorite: () -> Unit,
    val onMove: (Int) -> Unit,
    val onRename: (String?) -> Unit,
    val onToggleHidden: () -> Unit,
    val onToggleMuted: () -> Unit,
    val onAddWidget: () -> Unit,
    val onInfo: () -> Unit,
    val onUninstall: () -> Unit,
    /** Apre la personalizzazione del popup dell'app. */
    val onEditPopup: () -> Unit = {},
    /** Fissa una scorciatoia dell'app sulla home. */
    val onPinShortcut: (ShortcutInfo) -> Unit = {},
)

@Composable
fun AppActionsSheet(app: AppInfo, actions: AppActions, onDismiss: () -> Unit) {
    var renaming by remember { mutableStateOf(false) }
    var pickingSwipe by remember { mutableStateOf(false) }
    var pinning by remember { mutableStateOf(false) }
    val colors = LocalOndaColors.current
    val visible = AppOptions.visible(app.isFavorite, actions.favoriteIndex, actions.favoriteCount, actions.shortcuts.isNotEmpty())

    // Foglio a gruppi: Organizza, Comportamento, Privacy e benessere, Aggiungi, Sistema (Disinstalla in fondo)
    SheetScaffold(
        title = app.label,
        onDismiss = onDismiss,
        icon = { AppIcon(app, 40.dp, actions.themedIcons) },
    ) {
        if (app.label != app.originalLabel) {
            Text(app.originalLabel, style = MaterialTheme.typography.bodySmall, color = colors.onWallpaperMuted, modifier = Modifier.padding(horizontal = 8.dp))
        }
        AppOptions.groups(visible).forEach { (group, options) ->
            GroupCard(stringResource(groupTitle(group)), container = MaterialTheme.colorScheme.surfaceContainer) {
                options.forEach { option ->
                    when (option) {
                        AppOption.FAVORITE -> SwitchRow(stringResource(R.string.opt_favorite), null, app.isFavorite, icon = Icons.Filled.Star) { actions.onToggleFavorite() }
                        AppOption.MOVE_UP -> Unit
                        AppOption.MOVE_DOWN -> Unit
                        AppOption.FOLDER -> ActionRow(Icons.Filled.Folder, stringResource(R.string.opt_folder), actions.folderName ?: stringResource(R.string.opt_folder_none)) {
                            actions.onMoveToFolder(); onDismiss()
                        }
                        AppOption.RENAME -> ActionRow(Icons.Filled.Edit, stringResource(R.string.rename), app.label) { renaming = true }
                        AppOption.ICON -> ActionRow(Icons.Filled.Palette, stringResource(R.string.icon_change)) { actions.onChangeIcon(); onDismiss() }
                        AppOption.EDIT_POPUP -> ActionRow(Icons.Filled.Tune, stringResource(R.string.opt_edit_popup)) { actions.onEditPopup(); onDismiss() }
                        AppOption.SWIPE -> ActionRow(
                            Icons.Filled.PlayArrow,
                            stringResource(R.string.action_swipe_title),
                            actionSummary(actions.swipe, actions.allApps, stringResource(R.string.action_default)),
                        ) { pickingSwipe = true }
                        AppOption.NOTIFICATIONS_HOME -> SwitchRow(stringResource(R.string.opt_notifications_home), stringResource(R.string.opt_notifications_home_sub), !actions.isMuted, icon = Icons.Filled.Notifications) {
                            actions.onToggleMuted()
                        }
                        AppOption.REMINDER -> SwitchRow(stringResource(R.string.opt_reminder), stringResource(R.string.opt_reminder_sub), actions.hasReminder, icon = Icons.Filled.Schedule) {
                            actions.onToggleReminder()
                        }
                        AppOption.PROTECT -> SwitchRow(stringResource(R.string.opt_protect), null, actions.isProtected, icon = Icons.Filled.Lock) { actions.onToggleProtected() }
                        AppOption.BREAKER -> SwitchRow(stringResource(R.string.opt_breaker), stringResource(R.string.opt_breaker_sub), actions.hasBreaker, icon = Icons.Filled.HourglassEmpty) {
                            actions.onToggleBreaker()
                        }
                        AppOption.VISIBLE -> SwitchRow(stringResource(R.string.opt_visible), null, !app.isHidden, icon = Icons.Filled.Visibility) { actions.onToggleHidden() }
                        AppOption.ADD_WIDGET -> ActionRow(Icons.Filled.Widgets, stringResource(R.string.app_add_widget_this)) { actions.onAddWidget(); onDismiss() }
                        AppOption.PIN_SHORTCUT -> ActionRow(Icons.Filled.Add, stringResource(R.string.opt_pin_shortcut)) { pinning = true }
                        AppOption.INFO -> ActionRow(Icons.Filled.Info, stringResource(R.string.app_info)) { actions.onInfo(); onDismiss() }
                        AppOption.UNINSTALL -> DestructiveRow(Icons.Filled.Delete, stringResource(R.string.uninstall), stringResource(R.string.opt_uninstall_confirm, app.label)) {
                            actions.onUninstall(); onDismiss()
                        }
                    }
                }
                if (AppOption.MOVE_UP in options || AppOption.MOVE_DOWN in options) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        if (AppOption.MOVE_UP in options) {
                            FilledTonalButton(onClick = { actions.onMove(-1) }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Icon(Icons.Filled.KeyboardArrowUp, null)
                                Text(stringResource(R.string.move_up), modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                        if (AppOption.MOVE_DOWN in options) {
                            FilledTonalButton(onClick = { actions.onMove(1) }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Icon(Icons.Filled.KeyboardArrowDown, null)
                                Text(stringResource(R.string.move_down), modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (pinning) {
        AlertDialog(
            onDismissRequest = { pinning = false },
            title = { Text(stringResource(R.string.opt_pin_shortcut)) },
            text = {
                Column {
                    actions.shortcuts.forEach { sc ->
                        val icon = remember(sc.id) { actions.shortcutIcon(sc) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { actions.onPinShortcut(sc); pinning = false; onDismiss() }
                                .padding(vertical = 8.dp),
                        ) {
                            if (icon != null) Image(icon, null, Modifier.size(24.dp)) else Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(16.dp))
                            Text((sc.shortLabel ?: sc.longLabel ?: "").toString(), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pinning = false }) { Text(stringResource(R.string.close)) } },
        )
    }

    if (pickingSwipe) {
        ActionPickerDialog(
            title = stringResource(R.string.swipe_dialog_title, app.label),
            current = actions.swipe,
            allApps = actions.allApps,
            shortcutsOf = actions.shortcutsOf,
            themedIcons = actions.themedIcons,
            accessibilityEnabled = actions.accessibilityEnabled,
            onAccessibility = actions.onAccessibility,
            onPick = actions.onSetSwipe,
            onDismiss = { pickingSwipe = false },
            noneLabel = stringResource(R.string.app_panel_default),
        )
    }

    if (renaming) {
        var text by remember { mutableStateOf(app.label) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text(stringResource(R.string.rename)) },
            text = {
                OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, label = { Text(app.originalLabel) })
            },
            confirmButton = {
                TextButton(onClick = { actions.onRename(text); renaming = false }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { actions.onRename(null); renaming = false }) { Text(stringResource(R.string.restore)) }
            },
        )
    }
}

@androidx.annotation.StringRes
private fun groupTitle(group: AppOptionGroup): Int = when (group) {
    AppOptionGroup.ORGANIZE -> R.string.opt_group_organize
    AppOptionGroup.BEHAVIOR -> R.string.opt_group_behavior
    AppOptionGroup.PRIVACY -> R.string.opt_group_privacy
    AppOptionGroup.ADD -> R.string.opt_group_add
    AppOptionGroup.SYSTEM -> R.string.opt_group_system
}
