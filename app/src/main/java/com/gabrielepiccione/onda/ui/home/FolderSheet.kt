// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.home

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.ActionChoice
import com.gabrielepiccione.onda.data.Actions
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.data.Folder
import com.gabrielepiccione.onda.data.FolderIcons
import com.gabrielepiccione.onda.data.WidgetManager
import com.gabrielepiccione.onda.data.WidgetSize
import com.gabrielepiccione.onda.data.WidgetSizing
import com.gabrielepiccione.onda.ui.components.ActionPickerDialog
import com.gabrielepiccione.onda.ui.components.ActionRow
import com.gabrielepiccione.onda.ui.components.DestructiveRow
import com.gabrielepiccione.onda.ui.components.GroupCard
import com.gabrielepiccione.onda.ui.components.LocalWidgetSizeSetter
import com.gabrielepiccione.onda.ui.components.LocalWidgetSizes
import com.gabrielepiccione.onda.ui.components.SheetScaffold
import com.gabrielepiccione.onda.ui.components.WidgetSizeControls
import com.gabrielepiccione.onda.ui.components.actionSummary

/**
 * Menu della cartella: intestazione con icona (tocco = cambia) e nome (tocco = rinomina); gruppo «Cartella»
 * (Rinomina, Icona, Azione dello swipe, Sposta su/giù), gruppo «Widget nella cartella» con una riga per widget
 * (tocco = il suo menu) e «Aggiungi widget»; «Elimina cartella» in fondo.
 */
@Composable
fun FolderSheet(
    folder: Folder,
    widgets: WidgetManager,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRename: (String) -> Unit,
    onMove: (Int) -> Unit,
    onAddWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onChangeIcon: () -> Unit = {},
    onWidgetHeight: (Int?) -> Unit = {},
    allApps: List<AppInfo> = emptyList(),
    shortcutsOf: (AppInfo) -> List<ShortcutInfo> = { emptyList() },
    themedIcons: Boolean = false,
    accessibilityEnabled: Boolean = true,
    onAccessibility: () -> Unit = {},
    onSwipe: (ActionChoice) -> Unit = {},
    apps: List<AppInfo> = emptyList(),
) {
    var renaming by remember { mutableStateOf(false) }
    var pickingSwipe by remember { mutableStateOf(false) }
    var widgetMenu by remember { mutableStateOf<Int?>(null) }
    val pm = LocalContext.current.packageManager
    val iconLabel = stringResource(R.string.folder_icon)
    fun labelOf(id: Int): String? = widgets.infoFor(id)?.let { runCatching { it.loadLabel(pm) }.getOrNull() }

    SheetScaffold(
        title = folder.name,
        onDismiss = onDismiss,
        // Tocco sull'icona: la cambia
        icon = {
            FolderIcon(
                folder,
                apps,
                40.dp,
                themedIcons,
                Modifier.clickable { onChangeIcon(); onDismiss() }.semantics { contentDescription = iconLabel },
            )
        },
    ) {
        GroupCard(stringResource(R.string.folder_group_folder), container = MaterialTheme.colorScheme.surfaceContainer) {
            ActionRow(Icons.Filled.Edit, stringResource(R.string.rename), folder.name) { renaming = true }
            ActionRow(Icons.Filled.Palette, stringResource(R.string.folder_icon)) { onChangeIcon(); onDismiss() }
            ActionRow(
                Icons.Filled.PlayArrow,
                stringResource(R.string.folder_swipe),
                actionSummary(Actions.decode(folder.swipe), allApps, stringResource(R.string.folder_swipe_open)),
            ) { pickingSwipe = true }
            if (canMoveUp || canMoveDown) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    if (canMoveUp) {
                        FilledTonalButton(onClick = { onMove(-1) }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Icon(Icons.Filled.KeyboardArrowUp, null)
                            Text(stringResource(R.string.move_up), modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                    if (canMoveDown) {
                        FilledTonalButton(onClick = { onMove(1) }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Icon(Icons.Filled.KeyboardArrowDown, null)
                            Text(stringResource(R.string.move_down), modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
        }
        GroupCard(stringResource(R.string.folder_group_widgets), container = MaterialTheme.colorScheme.surfaceContainer) {
            // Una riga per widget: il tocco apre il suo menu (dimensioni, rimozione)
            folder.widgets.forEach { id ->
                ActionRow(Icons.Filled.Widgets, labelOf(id) ?: stringResource(R.string.widget_number, id)) { widgetMenu = id }
            }
            ActionRow(Icons.Filled.Add, stringResource(R.string.folder_add_widget)) { onAddWidget(); onDismiss() }
        }
        DestructiveRow(Icons.Filled.Delete, stringResource(R.string.folder_delete), stringResource(R.string.folder_delete_confirm, folder.name)) {
            onDelete(); onDismiss()
        }
    }

    widgetMenu?.let { id ->
        FolderWidgetSheet(
            folder = folder,
            id = id,
            label = labelOf(id) ?: stringResource(R.string.widget_number, id),
            widgets = widgets,
            onWidgetHeight = onWidgetHeight,
            onRemove = { onRemoveWidget(id); widgetMenu = null },
            onDismiss = { widgetMenu = null },
        )
    }
    if (pickingSwipe) {
        ActionPickerDialog(
            title = stringResource(R.string.folder_swipe),
            current = Actions.decode(folder.swipe),
            allApps = allApps,
            shortcutsOf = shortcutsOf,
            themedIcons = themedIcons,
            accessibilityEnabled = accessibilityEnabled,
            onAccessibility = onAccessibility,
            onPick = onSwipe,
            onDismiss = { pickingSwipe = false },
            noneLabel = stringResource(R.string.folder_swipe_open),
        )
    }
    if (renaming) {
        NameDialog(stringResource(R.string.folder_rename), folder.name, onDismiss = { renaming = false }) { onRename(it); renaming = false }
    }
}

/** Menu di un widget della cartella: le dimensioni (uguali per i widget della cartella) e la rimozione. */
@Composable
private fun FolderWidgetSheet(
    folder: Folder,
    id: Int,
    label: String,
    widgets: WidgetManager,
    onWidgetHeight: (Int?) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    SheetScaffold(title = label, onDismiss = onDismiss) {
        GroupCard(stringResource(R.string.widget_group_size), container = MaterialTheme.colorScheme.surfaceContainer) {
            val defaultHeight = remember(folder.widgets) {
                folder.widgets.mapNotNull { widgets.infoFor(it) }
                    .maxOfOrNull { with(density) { it.minHeight.toDp().value.toInt() } }
                    ?.coerceIn(FolderIcons.HEIGHT_RANGE.first, FolderIcons.HEIGHT_RANGE.last) ?: 160
            }
            val key = WidgetSizing.folderKey(folder.id)
            val setSize = LocalWidgetSizeSetter.current
            WidgetSizeControls(
                size = LocalWidgetSizes.current[key] ?: WidgetSize(),
                height = folder.widgetHeightDp ?: defaultHeight,
                defaultHeight = defaultHeight,
                maxWidth = LocalConfiguration.current.screenWidthDp - 48,
                onSize = { size -> setSize(key, size) },
                onHeight = { h -> onWidgetHeight(h) },
            )
            if (folder.widgets.size > 1) {
                Text(stringResource(R.string.folder_widget_size_note), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
        DestructiveRow(Icons.Filled.Delete, stringResource(R.string.widget_remove), stringResource(R.string.widget_remove_confirm_named, label)) {
            onRemove()
        }
    }
}
