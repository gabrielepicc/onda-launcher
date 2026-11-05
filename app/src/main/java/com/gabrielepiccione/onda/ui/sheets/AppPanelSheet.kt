// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(ExperimentalMaterial3Api::class)

package com.gabrielepiccione.onda.ui.sheets

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.ui.unit.Dp
import com.gabrielepiccione.onda.ui.components.NotificationImage
import com.gabrielepiccione.onda.util.HapticEvent
import com.gabrielepiccione.onda.util.LocalOndaHaptics
import android.app.Notification
import android.content.pm.ShortcutInfo
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.data.NotificationItem
import com.gabrielepiccione.onda.data.WidgetManager
import com.gabrielepiccione.onda.ui.components.AppIcon
import com.gabrielepiccione.onda.ui.components.HostedWidget
import com.gabrielepiccione.onda.ui.components.relativeTime
import com.gabrielepiccione.onda.ui.theme.LocalHapticsEnabled
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors

/**
 * Pannello dell'app (swipe a destra sulla riga o tocco sull'anteprima della notifica):
 * conversazioni con foto, azioni, scorciatoie, widget dell'app e barra di risposta fissa
 * in basso che sale sopra la tastiera.
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
    onNotificationAction: (NotificationItem, Notification.Action) -> Unit,
    onAddWidget: () -> Unit,
    onRemoveWidget: () -> Unit,
    onMoreOptions: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalOndaColors.current
    val haptics = LocalHapticFeedback.current
    val ondaHaptics = LocalOndaHaptics.current
    val hapticsOn = LocalHapticsEnabled.current

    // Conversazione a cui si risponde: di default la più recente che lo consente
    var replyKey by rememberSaveable(app.key) { mutableStateOf(notifications.firstOrNull { it.canReply }?.key) }
    val replyTarget = notifications.firstOrNull { it.key == replyKey && it.canReply }
        ?: notifications.firstOrNull { it.canReply }
    var draft by rememberSaveable(app.key) { mutableStateOf("") }
    var sentTo by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.panel,
        contentColor = colors.onWallpaper,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Intestazione: icona e nome (tocco = apri l'app), elimina tutte, altre opzioni
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, bottom = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onOpenApp(); onDismiss() }
                        .padding(8.dp)
                ) {
                    AppIcon(app, 36.dp, themedIcons)
                    Spacer(Modifier.width(14.dp))
                    Text(app.label, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (notifications.any { it.clearable }) {
                    RoundAction(Icons.Filled.Clear, stringResource(R.string.notif_clear_all)) {
                        notifications.filter { it.clearable }.forEach(onDismissNotification)
                    }
                }
                RoundAction(Icons.Filled.MoreVert, stringResource(R.string.more_options)) { onDismiss(); onMoreOptions() }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 620.dp),
            ) {
                items(notifications, key = { it.key }) { n ->
                    Conversation(
                        item = n,
                        selectedForReply = replyTarget?.key == n.key && notifications.count { it.canReply } > 1,
                        onOpen = { onNotification(n); onDismiss() },
                        onSelectReply = { replyKey = n.key },
                        onAction = { a -> onNotificationAction(n, a) },
                        onDismiss = { onDismissNotification(n) },
                    )
                }

                if (widgetId != null) {
                    item(key = "widget") {
                        Column(Modifier.padding(horizontal = 12.dp)) {
                            HostedWidget(widgetId, widgets)
                            TextButton(onClick = onRemoveWidget) { Text(stringResource(R.string.remove_widget)) }
                        }
                    }
                }

                items(shortcuts, key = { "sc_" + it.id }) { sc ->
                    val icon = remember(sc.id) { shortcutIcon(sc) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onShortcut(sc); onDismiss() }
                            .padding(horizontal = 24.dp, vertical = 14.dp)
                    ) {
                        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            if (icon != null) Image(icon, null, Modifier.size(32.dp).clip(CircleShape))
                            else Icon(Icons.Filled.PlayArrow, null, tint = colors.accent)
                        }
                        Spacer(Modifier.width(18.dp))
                        Text((sc.shortLabel ?: sc.longLabel ?: "").toString(), style = MaterialTheme.typography.titleMedium)
                    }
                }

                if (widgetId == null) {
                    item(key = "addWidget") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAddWidget(); onDismiss() }
                                .padding(horizontal = 24.dp, vertical = 14.dp)
                        ) {
                            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Add, null, tint = colors.accent)
                            }
                            Spacer(Modifier.width(18.dp))
                            Text(stringResource(R.string.app_panel_add_widget, app.label), style = MaterialTheme.typography.titleMedium, color = colors.onWallpaperMuted)
                        }
                    }
                }
            }

            // Barra di risposta fissa in basso
            if (replyTarget != null) {
                fun send() {
                    if (draft.isNotBlank() && onReplyNotification(replyTarget, draft)) {
                        ondaHaptics?.perform(HapticEvent.LONG_PRESS)
                        sentTo = replyTarget.replyTo
                        draft = ""
                    }
                }
                sentTo?.let {
                    Text(
                        stringResource(R.string.notif_sent_to, it),
                        color = colors.accent,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 28.dp, top = 6.dp),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .border(2.dp, colors.accent, RoundedCornerShape(28.dp))
                        .background(colors.panelHigh)
                        .padding(start = 20.dp, end = 6.dp)
                        .animateContentSize()
                ) {
                    Box(Modifier.weight(1f).heightIn(min = 56.dp), contentAlignment = Alignment.CenterStart) {
                        if (draft.isEmpty()) {
                            Text(
                                stringResource(R.string.notif_reply_to, replyTarget.replyTo),
                                color = colors.onWallpaperMuted,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        BasicTextField(
                            value = draft,
                            onValueChange = { draft = it; sentTo = null },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onWallpaper),
                            cursorBrush = SolidColor(colors.accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { send() }),
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        )
                    }
                    val cdSend = stringResource(R.string.notif_send)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (draft.isBlank()) colors.panelHigh else colors.accent)
                            .semantics { contentDescription = cdSend }
                            .clickable(enabled = draft.isNotBlank()) { send() }
                    ) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Filled.Send, null, tint = if (draft.isBlank()) colors.onWallpaperMuted else colors.onAccent)
                    }
                }
            }
        }
    }
}

/** Una conversazione/notifica nel pannello: foto, mittente · tempo, messaggi, azioni. */
@Composable
internal fun Conversation(
    item: NotificationItem,
    selectedForReply: Boolean,
    onOpen: () -> Unit,
    onSelectReply: () -> Unit,
    onAction: (Notification.Action) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalOndaColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (selectedForReply) colors.accentContainer.copy(alpha = 0.35f) else colors.panelHigh.copy(alpha = 0.5f))
            .clickable(onClick = onOpen)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(item)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.replyTo.ifBlank { item.title },
                    // Colore esplicito: dentro il popup il colore ereditato può essere quello di sistema (nero)
                    color = colors.onWallpaper,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(relativeTime(item.postTime), style = MaterialTheme.typography.bodySmall, color = colors.onWallpaperMuted)
            }
            if (item.clearable) {
                RoundAction(Icons.Filled.Clear, stringResource(R.string.cd_dismiss_notification), onClick = onDismiss)
            }
        }
        Spacer(Modifier.size(10.dp))
        if (item.messages.isNotEmpty()) {
            item.messages.forEach { m ->
                val showSender = m.sender.isNotBlank() && m.sender != item.title && m.sender != item.replyTo
                Text(
                    buildString {
                        if (showSender) append(m.sender).append(": ")
                        append(m.text)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onWallpaper,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        } else if (item.text.isNotBlank()) {
            Text(item.text, style = MaterialTheme.typography.bodyLarge, color = colors.onWallpaper)
        }
        NotificationImage(item, 240.dp)
        if (item.actions.isNotEmpty() || (item.canReply && !selectedForReply)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(top = 12.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                item.actions.forEach { a -> Chip(a.title?.toString().orEmpty()) { onAction(a) } }
                if (item.canReply) Chip(stringResource(R.string.notif_reply)) { onSelectReply() }
            }
        }
    }
}

/**
 * La stessa notifica nel popup, dove divide lo spazio con il resto: testo a 14sp come le scorciatoie,
 * al massimo 4 righe (toccandola si apre per intero) e azioni che vanno a capo invece di tagliarsi.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CompactConversation(
    item: NotificationItem,
    onOpen: () -> Unit,
    onSelectReply: () -> Unit,
    onAction: (Notification.Action) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalOndaColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.panelHigh.copy(alpha = 0.5f))
            .clickable(onClick = onOpen)
            .padding(start = 12.dp, top = 4.dp, end = 0.dp, bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(item, 32.dp, compact = true)
            Spacer(Modifier.width(10.dp))
            Text(
                item.replyTo.ifBlank { item.title },
                color = colors.onWallpaper,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(6.dp))
            Text(relativeTime(item.postTime), style = MaterialTheme.typography.labelSmall, color = colors.onWallpaperMuted)
            Spacer(Modifier.weight(1f))
            if (item.clearable) {
                RoundAction(Icons.Filled.Clear, stringResource(R.string.cd_dismiss_notification), iconSize = 18.dp, onClick = onDismiss)
            }
        }
        Column(Modifier.padding(end = 12.dp)) {
            val text = if (item.messages.isNotEmpty()) {
                item.messages.takeLast(COMPACT_MESSAGES).joinToString("\n") { m ->
                    val showSender = m.sender.isNotBlank() && m.sender != item.title && m.sender != item.replyTo
                    if (showSender) m.sender + ": " + m.text else m.text
                }
            } else {
                item.text
            }
            if (text.isNotBlank()) {
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onWallpaper,
                    maxLines = COMPACT_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            NotificationImage(item, 160.dp)
            if (item.actions.isNotEmpty() || item.canReply) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    item.actions.forEach { a -> ActionChip(a.title?.toString().orEmpty()) { onAction(a) } }
                    if (item.canReply) ActionChip(stringResource(R.string.notif_reply)) { onSelectReply() }
                }
            }
        }
    }
}

private const val COMPACT_LINES = 4
private const val COMPACT_MESSAGES = 3

/** Azione di una notifica: il chip di Material, con il suo bersaglio di tocco da 48dp. */
@Composable
private fun ActionChip(label: String, onClick: () -> Unit) {
    val colors = LocalOndaColors.current
    AssistChip(
        onClick = onClick,
        label = { Text(label, color = colors.onWallpaper, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        border = null,
        colors = AssistChipDefaults.assistChipColors(containerColor = colors.panel),
    )
}

@Composable
private fun Avatar(item: NotificationItem, size: Dp = 48.dp, compact: Boolean = false) {
    val colors = LocalOndaColors.current
    val icon = item.largeIcon
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.accentContainer)
    ) {
        if (icon != null) {
            Image(icon, null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            Text(
                item.replyTo.trim().take(1).uppercase(),
                color = colors.onAccentContainer,
                style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    val colors = LocalOndaColors.current
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = colors.onWallpaper,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.panel)
            .clickable(onClick = onClick)
            .heightIn(min = 40.dp)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    )
}

@Composable
private fun RoundAction(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, iconSize: Dp = 24.dp, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .semantics { contentDescription = description }
            .clickable(onClick = onClick)
    ) {
        Icon(icon, null, tint = LocalOndaColors.current.onWallpaperMuted, modifier = Modifier.size(iconSize))
    }
}
