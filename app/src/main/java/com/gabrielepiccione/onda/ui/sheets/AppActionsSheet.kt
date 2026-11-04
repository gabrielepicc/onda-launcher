// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(ExperimentalMaterial3Api::class)

package com.gabrielepiccione.onda.ui.sheets

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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabrielepiccione.onda.data.AppInfo

data class AppActions(
    val shortcuts: List<ShortcutInfo>,
    val shortcutIcon: (ShortcutInfo) -> androidx.compose.ui.graphics.ImageBitmap?,
    val swipeTarget: AppInfo?,
    val allApps: List<AppInfo>,
    val themedIcons: Boolean,
    val onSetSwipe: (AppInfo?) -> Unit,
    val folderName: String?,
    val onChangeIcon: () -> Unit,
    val isProtected: Boolean = false,
    val onToggleProtected: () -> Unit = {},
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
)

@Composable
fun AppActionsSheet(app: AppInfo, actions: AppActions, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var renaming by remember { mutableStateOf(false) }
    var pickingSwipe by remember { mutableStateOf(false) }
    val colors = LocalOndaColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.panel,
        contentColor = colors.onWallpaper,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                AppIcon(app, 48.dp, actions.themedIcons)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(app.label, style = MaterialTheme.typography.headlineSmall)
                    if (app.label != app.originalLabel) {
                        Text(app.originalLabel, fontSize = 13.sp, color = colors.onWallpaperMuted)
                    }
                }
            }

            if (actions.shortcuts.isNotEmpty()) {
                actions.shortcuts.forEach { sc ->
                    val icon = remember(sc.id) { actions.shortcutIcon(sc) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                            .background(colors.panelHigh)
                            .clickable { actions.onShortcut(sc); onDismiss() }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        if (icon != null) Image(icon, null, Modifier.size(24.dp))
                        else Icon(Icons.Filled.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(16.dp))
                        Text((sc.shortLabel ?: sc.longLabel ?: "").toString(), style = MaterialTheme.typography.bodyLarge, color = colors.onWallpaper)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp, horizontal = 24.dp), color = colors.panelBorder)
            }

            ActionRow(Icons.Filled.Star, if (app.isFavorite) stringResource(R.string.favorite_remove) else stringResource(R.string.favorite_add)) {
                actions.onToggleFavorite(); onDismiss()
            }
            if (app.isFavorite && actions.favoriteIndex > 0) {
                ActionRow(Icons.Filled.KeyboardArrowUp, stringResource(R.string.move_up)) { actions.onMove(-1) }
            }
            if (app.isFavorite && actions.favoriteIndex in 0 until actions.favoriteCount - 1) {
                ActionRow(Icons.Filled.KeyboardArrowDown, stringResource(R.string.move_down)) { actions.onMove(1) }
            }
            ActionRow(Icons.Filled.Add, actions.folderName?.let { stringResource(R.string.in_folder_move, it) } ?: stringResource(R.string.move_to_folder_ellipsis)) {
                actions.onMoveToFolder(); onDismiss()
            }
            ActionRow(Icons.Filled.Edit, stringResource(R.string.rename)) { renaming = true }
            ActionRow(Icons.Filled.Face, stringResource(R.string.icon_change)) { actions.onChangeIcon(); onDismiss() }
            ActionRow(Icons.Filled.Lock, stringResource(if (actions.isProtected) R.string.protect_off else R.string.protect_on)) {
                actions.onToggleProtected(); onDismiss()
            }
            ActionRow(Icons.Filled.Lock, if (app.isHidden) stringResource(R.string.show_in_list) else stringResource(R.string.hide)) {
                actions.onToggleHidden(); onDismiss()
            }
            ActionRow(Icons.Filled.Notifications, if (actions.isMuted) stringResource(R.string.notif_show_home) else stringResource(R.string.notif_hide_home)) {
                actions.onToggleMuted(); onDismiss()
            }
            ActionRow(Icons.Filled.Add, stringResource(R.string.app_add_widget_this)) {
                actions.onAddWidget(); onDismiss()
            }
            ActionRow(Icons.Filled.Info, stringResource(R.string.app_info)) { actions.onInfo(); onDismiss() }
            ActionRow(Icons.Filled.Delete, stringResource(R.string.uninstall)) { actions.onUninstall(); onDismiss() }
        }
    }

    if (pickingSwipe) {
        AlertDialog(
            onDismissRequest = { pickingSwipe = false },
            title = { Text(stringResource(R.string.swipe_dialog_title, app.label)) },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    item {
                        Text(
                            stringResource(R.string.app_panel_default),
                            color = colors.onWallpaperMuted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { actions.onSetSwipe(null); pickingSwipe = false }
                                .padding(vertical = 12.dp),
                        )
                    }
                    items(actions.allApps.size) { i ->
                        val target = actions.allApps[i]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { actions.onSetSwipe(target); pickingSwipe = false }
                                .padding(vertical = 8.dp)
                        ) {
                            AppIcon(target, 30.dp, actions.themedIcons)
                            Spacer(Modifier.width(12.dp))
                            Text(target.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickingSwipe = false }) { Text(stringResource(R.string.close)) } },
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

@Composable
private fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = LocalOndaColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(18.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.onWallpaper)
    }
}
