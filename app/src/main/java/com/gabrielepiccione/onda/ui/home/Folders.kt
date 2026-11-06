// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.gabrielepiccione.onda.ui.home

import com.gabrielepiccione.onda.data.WidgetSize
import com.gabrielepiccione.onda.data.WidgetSizing
import com.gabrielepiccione.onda.ui.components.WidgetSizeControls
import com.gabrielepiccione.onda.ui.components.LocalWidgetSizes
import com.gabrielepiccione.onda.ui.components.LocalWidgetSizeSetter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.filled.Palette
import androidx.compose.ui.platform.LocalDensity
import com.gabrielepiccione.onda.data.FolderIcons
import com.gabrielepiccione.onda.data.WidgetStack
import com.gabrielepiccione.onda.util.HapticEvent
import com.gabrielepiccione.onda.util.LocalOndaHaptics
import com.gabrielepiccione.onda.ui.theme.*
import com.gabrielepiccione.onda.ui.components.*
import com.gabrielepiccione.onda.ui.drawer.*
import com.gabrielepiccione.onda.ui.search.*
import com.gabrielepiccione.onda.ui.settings.*
import com.gabrielepiccione.onda.ui.sheets.*

import androidx.compose.ui.res.stringResource
import com.gabrielepiccione.onda.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.pm.ShortcutInfo
import androidx.compose.material.icons.filled.PlayArrow
import com.gabrielepiccione.onda.data.ActionChoice
import com.gabrielepiccione.onda.data.Actions
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.ui.components.ActionPickerDialog
import com.gabrielepiccione.onda.ui.components.SwipeActions
import com.gabrielepiccione.onda.ui.components.actionSummary
import com.gabrielepiccione.onda.data.Folder
import com.gabrielepiccione.onda.data.NotificationItem
import com.gabrielepiccione.onda.data.WidgetManager

/** Cartella in home: chiusa mostra nome e mini-icone; toccandola si espande con app e widget. */
@Composable
fun FolderRow(
    folder: Folder,
    apps: List<AppInfo>,
    widgets: WidgetManager,
    notifications: Map<String, List<NotificationItem>>,
    showIcons: Boolean,
    themedIcons: Boolean,
    iconScale: Float,
    onApp: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Nasconde il nome; senza icone resta visibile, altrimenti la riga sarebbe vuota. */
    hideName: Boolean = false,
    /** Esegue l'azione dello swipe a destra (formato di [Actions.encode]); se la cartella non ne ha una, lo swipe la apre o la chiude. */
    onPerform: (String) -> Unit = {},
) {
    val nameHidden = hideName && showIcons
    val colors = LocalOndaColors.current
    val haptics = LocalHapticFeedback.current
    val ondaHaptics = LocalOndaHaptics.current
    val hapticsOn = LocalHapticsEnabled.current
    var open by rememberSaveable(folder.id) { mutableStateOf(false) }
    val toggleLabel = stringResource(if (open) R.string.cd_folder_close else R.string.cd_folder_open)
    val unread = apps.count { notifications[it.packageName].orEmpty().isNotEmpty() }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(if (open) colors.panel.copy(alpha = 0.85f) else androidx.compose.ui.graphics.Color.Transparent)
            .animateContentSize(Motion.bouncy())
    ) {
        SwipeActions(
            canDismiss = false,
            onRight = { if (folder.swipe.isBlank()) open = !open else onPerform(folder.swipe) },
            onLeft = {},
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .combinedClickable(
                        onClick = { open = !open },
                        onClickLabel = toggleLabel,
                        onLongClick = {
                            ondaHaptics?.perform(HapticEvent.LONG_PRESS)
                            onLongClick()
                        },
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Icona scelta dall'utente, oppure le prime app sovrapposte
                if (showIcons) {
                    FolderIcon(folder, apps, 44.dp * iconScale, themedIcons)
                    Spacer(Modifier.width(18.dp))
                }
                if (nameHidden) {
                    // Il nome resta disponibile per TalkBack
                    Spacer(Modifier.weight(1f).semantics { contentDescription = folder.name })
                } else {
                    Text(
                        folder.name,
                        color = colors.onWallpaper,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (unread > 0 && !open) {
                    Text(
                        unread.toString(),
                        color = colors.onAccentContainer,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(colors.accentContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = open,
            enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
            exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit()),
        ) {
            Column(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 10.dp)) {
                // Più widget: un carosello orizzontale; tutti con l'altezza scelta per la cartella
                if (folder.widgets.size > 1) {
                    WidgetStackCard(
                        stack = WidgetStack("f_" + folder.id, folder.widgets, folder.widgetHeightDp),
                        widgets = widgets,
                        highlighted = false,
                        onLongPress = onLongClick,
                        size = LocalWidgetSizes.current[WidgetSizing.folderKey(folder.id)],
                    )
                } else {
                    folder.widgets.forEach { id ->
                        HostedWidget(id, widgets, heightDp = folder.widgetHeightDp, onLongPress = onLongClick, size = LocalWidgetSizes.current[WidgetSizing.folderKey(folder.id)])
                    }
                }
                apps.forEach { app ->
                    AppListRow(
                        app = app,
                        hasNotification = notifications[app.packageName].orEmpty().isNotEmpty(),
                        showIcon = showIcons,
                        themedIcons = themedIcons,
                        onClick = { onApp(app) },
                        onLongClick = { onAppLongClick(app) },
                    )
                }
                if (apps.isEmpty() && folder.widgets.isEmpty()) {
                    Text(
                        stringResource(R.string.folder_empty),
                        color = colors.onWallpaperMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

/** Opzioni della cartella: rinomina, sposta, aggiungi/rimuovi widget, elimina. */
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
) {
    val colors = LocalOndaColors.current
    val density = LocalDensity.current
    var renaming by remember { mutableStateOf(false) }
    var pickingSwipe by remember { mutableStateOf(false) }
    val pm = androidx.compose.ui.platform.LocalContext.current.packageManager
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.panel,
        contentColor = colors.onWallpaper,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            Text(folder.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            SheetRow(Icons.Filled.Edit, stringResource(R.string.rename)) { renaming = true }
            SheetRow(Icons.Filled.Palette, stringResource(R.string.folder_icon)) { onChangeIcon(); onDismiss() }
            SheetRow(
                Icons.Filled.PlayArrow,
                stringResource(R.string.folder_swipe) + ": " + actionSummary(Actions.decode(folder.swipe), allApps, stringResource(R.string.folder_swipe_open)),
            ) { pickingSwipe = true }
            if (canMoveUp) SheetRow(Icons.Filled.KeyboardArrowUp, stringResource(R.string.move_up)) { onMove(-1) }
            if (canMoveDown) SheetRow(Icons.Filled.KeyboardArrowDown, stringResource(R.string.move_down)) { onMove(1) }
            SheetRow(Icons.Filled.Add, stringResource(R.string.folder_add_widget)) { onAddWidget(); onDismiss() }
            if (folder.widgets.isNotEmpty()) {
                // Altezza dei widget nella cartella, dal vivo; il predefinito è la più alta tra quelle dei widget
                val defaultHeight = remember(folder.widgets) {
                    folder.widgets.mapNotNull { widgets.infoFor(it) }
                        .maxOfOrNull { with(density) { it.minHeight.toDp().value.toInt() } }
                        ?.coerceIn(FolderIcons.HEIGHT_RANGE.first, FolderIcons.HEIGHT_RANGE.last) ?: 160
                }
                val current = folder.widgetHeightDp ?: defaultHeight
                Column(Modifier.padding(horizontal = 24.dp)) {
                    val key = WidgetSizing.folderKey(folder.id)
                    val setSize = LocalWidgetSizeSetter.current
                    WidgetSizeControls(
                        size = LocalWidgetSizes.current[key] ?: WidgetSize(),
                        height = current,
                        defaultHeight = defaultHeight,
                        maxWidth = LocalConfiguration.current.screenWidthDp - 48,
                        onSize = { size -> setSize(key, size) },
                        onHeight = { h -> onWidgetHeight(h) },
                    )
                }
            }
            folder.widgets.forEach { id ->
                val label = widgets.infoFor(id)?.let { runCatching { it.loadLabel(pm) }.getOrNull() } ?: stringResource(R.string.section_widgets)
                SheetRow(Icons.Filled.Delete, stringResource(R.string.widget_remove_named, label)) { onRemoveWidget(id) }
            }
            SheetRow(Icons.Filled.Delete, stringResource(R.string.folder_delete)) { onDelete(); onDismiss() }
        }
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

/** Scelta della cartella di destinazione per un'app. */
@Composable
fun MoveToFolderDialog(
    folders: List<Folder>,
    currentFolderId: String?,
    onPick: (String?) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.move_to_folder)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                item {
                    DialogChoice(stringResource(R.string.folder_new_ellipsis), false) { creating = true }
                }
                if (currentFolderId != null) {
                    item { DialogChoice(stringResource(R.string.folder_outside), false) { onPick(null) } }
                }
                items(folders.size) { i ->
                    val f = folders[i]
                    DialogChoice(f.name, f.id == currentFolderId) { onPick(f.id) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
    if (creating) {
        NameDialog(stringResource(R.string.new_folder), "", onDismiss = { creating = false }) { onCreate(it); creating = false }
    }
}

@Composable
fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, label = { Text(stringResource(R.string.name)) }) },
        confirmButton = { TextButton(onClick = { onConfirm(text.trim()) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun DialogChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalOndaColors.current
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge,
        color = if (selected) colors.accent else colors.onWallpaper,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
    )
}

@Composable
fun SheetRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val colors = LocalOndaColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Icon(icon, null, tint = colors.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(18.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.onWallpaper)
    }
}
