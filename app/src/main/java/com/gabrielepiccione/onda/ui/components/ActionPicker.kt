// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.components

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.ActionArg
import com.gabrielepiccione.onda.data.ActionChoice
import com.gabrielepiccione.onda.data.ActionNeed
import com.gabrielepiccione.onda.data.Actions
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.data.GestureAction
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors

/** Riga cliccabile di un elenco di scelte: alta almeno 48 dp. */
@Composable
private fun PickRow(label: String, selected: Boolean = false, content: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    val colors = LocalOndaColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        content?.let { it(); Spacer(Modifier.width(12.dp)) }
        Text(
            label,
            color = if (selected) colors.accent else colors.onWallpaper,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/**
 * Selettore UNICO delle azioni (gesti, swipe, popup, cartelle, ricerca): mostra il catalogo [Actions.catalog]
 * e, per "Apri un'app" e "Apri una scorciatoia", i passi successivi per sceglierle.
 * [noneLabel], se c'è, è la prima voce e produce [GestureAction.NONE] (es. "Apri la cartella", "Predefinita").
 */
@Composable
fun ActionPickerDialog(
    title: String,
    current: ActionChoice,
    allApps: List<AppInfo>,
    shortcutsOf: (AppInfo) -> List<ShortcutInfo>,
    themedIcons: Boolean,
    accessibilityEnabled: Boolean,
    onAccessibility: () -> Unit,
    onPick: (ActionChoice) -> Unit,
    onDismiss: () -> Unit,
    noneLabel: String? = null,
    /** Parte direttamente dalla scelta dell'app o della scorciatoia (quando l'azione è già decisa). */
    startAt: ActionArg? = null,
    /** Solo azioni eseguibili senza schermate (per le automazioni). */
    automatable: Boolean = false,
) {
    var step by remember { mutableStateOf(startAt) }
    var shortcutApp by remember { mutableStateOf<AppInfo?>(null) }
    // L'azione che attende un'app (aprire, bloccare, sbloccare)
    var pendingAction by remember { mutableStateOf<GestureAction?>(null) }
    val shortcuts = remember(shortcutApp) { shortcutApp?.let(shortcutsOf).orEmpty() }
    fun finish(choice: ActionChoice) {
        // Permesso just-in-time: l'azione si salva comunque, e si apre la guida per concederlo
        if (Actions.need(choice.action) == ActionNeed.ACCESSIBILITY && !accessibilityEnabled) onAccessibility()
        onPick(choice)
        onDismiss()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                val kind = step
                when {
                    kind == null -> {
                        if (noneLabel != null) {
                            item { PickRow(noneLabel, current.action == GestureAction.NONE) { finish(ActionChoice(GestureAction.NONE)) } }
                        }
                        Actions.catalog.forEach { (group, all) ->
                            val list = all.filter { if (automatable) Actions.isAutomatable(it) else !Actions.isAutomationOnly(it) }
                            item(key = "g_" + group.name) {
                                Text(
                                    stringResource(group.labelRes),
                                    color = LocalOndaColors.current.accent,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                                )
                            }
                            items(list, key = { it.name }) { action ->
                                PickRow(stringResource(action.labelRes), current.action == action) {
                                    when (Actions.argOf(action)) {
                                        ActionArg.NONE -> finish(ActionChoice(action))
                                        else -> {
                                            pendingAction = action
                                            step = Actions.argOf(action)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    kind == ActionArg.APP || (kind == ActionArg.SHORTCUT && shortcutApp == null) -> {
                        items(allApps, key = { it.key }) { app ->
                            PickRow(app.label, content = { AppIcon(app, 30.dp, themedIcons) }) {
                                if (kind == ActionArg.APP) finish(ActionChoice(pendingAction ?: GestureAction.OPEN_APP, app.key))
                                else shortcutApp = app
                            }
                        }
                    }
                    else -> {
                        if (shortcuts.isEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.action_shortcut_none),
                                    color = LocalOndaColors.current.onWallpaperMuted,
                                    modifier = Modifier.padding(vertical = 12.dp),
                                )
                            }
                        }
                        items(shortcuts, key = { it.id }) { sc ->
                            PickRow(sc.shortLabel?.toString() ?: sc.id) {
                                finish(ActionChoice(GestureAction.OPEN_SHORTCUT, Actions.shortcutArg(sc.`package`, sc.id)))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (step != null) {
                    TextButton(onClick = { if (shortcutApp != null) shortcutApp = null else step = null }) { Text(stringResource(R.string.back)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
            }
        },
    )
}

/** Nome leggibile di una scelta, per mostrarla accanto al titolo dell'impostazione. */
@Composable
fun actionSummary(choice: ActionChoice, allApps: List<AppInfo>, noneLabel: String): String = when (choice.action) {
    GestureAction.NONE -> noneLabel
    GestureAction.OPEN_APP -> allApps.firstOrNull { it.key == choice.arg }?.label ?: stringResource(choice.action.labelRes)
    GestureAction.BLOCK_APP, GestureAction.UNBLOCK_APP ->
        stringResource(choice.action.labelRes) + ": " + (allApps.firstOrNull { it.key == choice.arg }?.label ?: "…")
    GestureAction.OPEN_SHORTCUT -> {
        val pkg = Actions.parseShortcut(choice.arg)?.first
        val app = allApps.firstOrNull { it.packageName == pkg }?.label
        if (app != null) stringResource(R.string.action_shortcut_of, app) else stringResource(choice.action.labelRes)
    }
    else -> stringResource(choice.action.labelRes)
}

/** Icona di un'azione del catalogo (per il pulsante dei gesti, le righe del popup e la ricerca). */
fun actionIcon(action: GestureAction): ImageVector = when (action) {
    GestureAction.SEARCH -> Icons.Filled.Search
    GestureAction.NOTIFICATIONS -> Icons.Filled.Notifications
    GestureAction.ALL_APPS -> Icons.Filled.List
    GestureAction.LOCK_SCREEN -> Icons.Filled.Lock
    GestureAction.SETTINGS, GestureAction.QUICK_SETTINGS -> Icons.Filled.Settings
    GestureAction.OPEN_APP -> Icons.Filled.Apps
    GestureAction.BLOCK_APP -> Icons.Filled.Lock
    GestureAction.UNBLOCK_APP -> Icons.Filled.LockOpen
    GestureAction.FLASHLIGHT -> Icons.Filled.FlashOn
    GestureAction.SILENT, GestureAction.SILENT_ON -> Icons.Filled.NotificationsOff
    GestureAction.SOUND_ON -> Icons.Filled.Notifications
    GestureAction.SCREENSHOT -> Icons.Filled.PhotoCamera
    GestureAction.RECENTS -> Icons.Filled.History
    GestureAction.OPEN_SHORTCUT -> Icons.Filled.Star
    GestureAction.NONE -> Icons.Filled.Close
}
