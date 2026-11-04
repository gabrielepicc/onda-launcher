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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.data.NotificationItem
import com.gabrielepiccione.onda.data.WidgetManager

/**
 * Pannello rapido di un'app, aperto con lo swipe sulla riga:
 * scorciatoie, tutte le notifiche e un widget dell'app a scelta.
 */
@Composable
fun AppPanelSheet(
    app: AppInfo,
    shortcuts: List<ShortcutInfo>,
    shortcutIcon: (ShortcutInfo) -> ImageBitmap?,
    notifications: List<NotificationItem>,
    widgetId: Int?,
    widgets: WidgetManager,
    themedIcons: Boolean,
    onOpenApp: () -> Unit,
    onShortcut: (ShortcutInfo) -> Unit,
    onNotification: (NotificationItem) -> Unit,
    onDismissNotification: (NotificationItem) -> Unit,
    onReplyNotification: (NotificationItem, String) -> Boolean,
    onNotificationAction: (NotificationItem, android.app.Notification.Action) -> Unit,
    onAddWidget: () -> Unit,
    onRemoveWidget: () -> Unit,
    onMoreOptions: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalOndaColors.current
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Intestazione: tocco = apri l'app
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onOpenApp(); onDismiss() }
                    .padding(8.dp)
            ) {
                AppIcon(app, 52.dp, themedIcons)
                Spacer(Modifier.width(16.dp))
                Text(app.label, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                androidx.compose.foundation.layout.Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(50)).clickable { onDismiss(); onMoreOptions() }
                ) {
                    Icon(Icons.Filled.MoreVert, stringResource(R.string.more_options), tint = colors.onWallpaperMuted)
                }
            }

            // Widget dell'app
            if (widgetId != null) {
                HostedWidget(widgetId, widgets)
                TextButton(onClick = onRemoveWidget) { Text(stringResource(R.string.remove_widget)) }
            }

            // Scorciatoie
            shortcuts.forEach { sc ->
                val icon = remember(sc.id) { shortcutIcon(sc) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.panelHigh)
                        .clickable { onShortcut(sc); onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    if (icon != null) Image(icon, null, Modifier.size(24.dp))
                    else Icon(Icons.Filled.PlayArrow, null, tint = colors.accent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(16.dp))
                    Text((sc.shortLabel ?: sc.longLabel ?: "").toString(), style = MaterialTheme.typography.bodyLarge)
                }
            }

            // Notifiche
            if (notifications.isNotEmpty()) {
                Text(stringResource(R.string.notifications), color = colors.accent, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp, top = 8.dp))
                notifications.forEach { n ->
                    androidx.compose.runtime.key(n.key) {
                        NotificationCard(
                            item = n,
                            extraCount = 0,
                            onClick = { onNotification(n); onDismiss() },
                            onDismiss = { onDismissNotification(n) },
                            onToggleExpand = {},
                            onReply = { text -> onReplyNotification(n, text) },
                            onAction = { a -> onNotificationAction(n, a) },
                        )
                    }
                }
            }

            if (widgetId == null) {
                FilledTonalButton(onClick = { onAddWidget(); onDismiss() }, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.app_panel_add_widget, app.label))
                }
            }
            if (shortcuts.isEmpty() && notifications.isEmpty() && widgetId == null) {
                Text(
                    stringResource(R.string.no_shortcuts),
                    color = colors.onWallpaperMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}
