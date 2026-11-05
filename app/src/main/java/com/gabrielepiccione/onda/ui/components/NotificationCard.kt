// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(ExperimentalMaterial3Api::class)

package com.gabrielepiccione.onda.ui.components

import com.gabrielepiccione.onda.data.ConversationSummary
import com.gabrielepiccione.onda.data.NotificationImages
import com.gabrielepiccione.onda.ui.theme.LocalNotificationImages
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import com.gabrielepiccione.onda.util.HapticEvent
import com.gabrielepiccione.onda.util.LocalOndaHaptics
import android.app.Notification
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.NotificationItem
import com.gabrielepiccione.onda.ui.theme.LocalHapticsEnabled
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import com.gabrielepiccione.onda.ui.theme.Motion

/**
 * Notifica mostrata in home: conversazione (se è una chat), azioni dell'app,
 * risposta diretta e swipe laterale per eliminarla.
 */
@Composable
fun NotificationCard(
    item: NotificationItem,
    extraCount: Int,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    onToggleExpand: () -> Unit,
    onReply: (String) -> Boolean,
    onAction: (Notification.Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalOndaColors.current
    val haptics = LocalHapticFeedback.current
    val ondaHaptics = LocalOndaHaptics.current
    val hapticsOn = LocalHapticsEnabled.current
    var replying by rememberSaveable(item.key) { mutableStateOf(false) }
    var draft by rememberSaveable(item.key) { mutableStateOf("") }

    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled && item.clearable) {
                onDismiss(); true
            } else false
        }
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = item.clearable && !replying,
        enableDismissFromEndToStart = item.clearable && !replying,
        backgroundContent = {},
        modifier = modifier,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.panel.copy(alpha = 0.9f))
                .clickable(onClick = onClick)
                .padding(start = 14.dp, top = 10.dp, bottom = 8.dp, end = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    if (item.title.isNotBlank()) {
                        Text(
                            item.title,
                            color = colors.onWallpaper,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (item.messages.size > 1) {
                        // Conversazione: ultime righe con il mittente
                        item.messages.forEach { m ->
                            Text(
                                buildString {
                                    if (m.sender.isNotBlank() && m.sender != item.title) append(m.sender).append(": ")
                                    append(m.text)
                                },
                                color = colors.onWallpaperMuted,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else if (item.text.isNotBlank()) {
                        Text(
                            item.text,
                            color = colors.onWallpaperMuted,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (extraCount > 0) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(50))
                            .background(colors.accentContainer)
                            .clickable(onClick = onToggleExpand)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("+$extraCount", color = colors.onAccentContainer, style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (item.clearable) {
                    val cd = stringResource(R.string.cd_dismiss_notification)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .semantics { contentDescription = cd }
                            .clickable(onClick = onDismiss)
                    ) {
                        Icon(Icons.Filled.Close, null, tint = colors.onWallpaperMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Azioni: Rispondi + azioni dell'app
            if (item.canReply || item.actions.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    if (item.canReply && !replying) {
                        ActionChip(stringResource(R.string.notif_reply), primary = true) { replying = true }
                    }
                    item.actions.forEach { a ->
                        ActionChip(a.title?.toString().orEmpty()) { onAction(a) }
                    }
                }
            }

            // Campo di risposta diretta
            AnimatedVisibility(
                visible = replying,
                enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
                exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit()),
            ) {
                val focus = remember { FocusRequester() }
                LaunchedEffect(Unit) { focus.requestFocus() }
                fun send() {
                    if (draft.isNotBlank() && onReply(draft)) {
                        ondaHaptics?.perform(HapticEvent.LONG_PRESS)
                        draft = ""
                        replying = false
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 8.dp, end = 6.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(colors.panelHigh)
                        .padding(start = 16.dp, end = 4.dp)
                ) {
                    Box(Modifier.weight(1f).heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
                        if (draft.isEmpty()) {
                            Text(stringResource(R.string.notif_reply_hint), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
                        }
                        BasicTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onWallpaper),
                            cursorBrush = SolidColor(colors.accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { send() }),
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).focusRequester(focus),
                        )
                    }
                    val cdSend = stringResource(R.string.notif_send)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (draft.isBlank()) colors.panel else colors.accent)
                            .semantics { contentDescription = cdSend }
                            .clickable(enabled = draft.isNotBlank()) { send() }
                    ) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Filled.Send, null, tint = if (draft.isBlank()) colors.onWallpaperMuted else colors.onAccent, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(label: String, primary: Boolean = false, onClick: () -> Unit) {
    val colors = LocalOndaColors.current
    Text(
        label,
        color = if (primary) colors.onAccentContainer else colors.accent,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .background(if (primary) colors.accentContainer else colors.panelHigh)
            .clickable(onClick = onClick)
            .heightIn(min = 36.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/** Immagine, GIF o sticker della notifica, intera e senza tagli; nulla se non c'è o se è spenta. */
@Composable
fun NotificationImage(item: NotificationItem, maxHeight: Dp, modifier: Modifier = Modifier) {
    if (LocalNotificationImages.current == NotificationImages.OFF) return
    val image = item.image ?: return
    Image(
        image,
        contentDescription = stringResource(R.string.notif_image),
        contentScale = ContentScale.Fit,
        modifier = modifier.padding(top = 6.dp).heightIn(max = maxHeight).clip(RoundedCornerShape(12.dp)),
    )
}

/** Tempo trascorso breve e localizzato ("2 min", "1 h"…). */
@Composable
fun relativeTime(postTime: Long): String {
    val minutes = ((System.currentTimeMillis() - postTime) / 60_000L).coerceAtLeast(0)
    return when {
        minutes < 1 -> stringResource(R.string.time_now)
        minutes < 60 -> stringResource(R.string.time_minutes, minutes.toInt())
        minutes < 24 * 60 -> stringResource(R.string.time_hours, (minutes / 60).toInt())
        else -> android.text.format.DateFormat.getMediumDateFormat(androidx.compose.ui.platform.LocalContext.current)
            .format(java.util.Date(postTime))
    }
}

/**
 * Anteprima della notifica sotto l'app in home: mittente · tempo, testo su più righe e freccia.
 * Con più messaggi diventa un riepilogo ("5 messaggi da 2 chat") con l'ultimo messaggio sotto.
 * Un'immagine allegata compare come miniatura a destra, senza allungare la riga.
 * Il tocco apre il pannello dell'app (conversazione completa e risposta); lo swipe la elimina.
 */
@Composable
fun NotificationPreview(
    item: NotificationItem,
    extraCount: Int,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    summary: ConversationSummary? = null,
) {
    val colors = LocalOndaColors.current
    val shown = summary?.latest ?: item
    val title = when {
        summary == null -> item.replyTo.ifBlank { item.title }
        summary.chats > 1 -> stringResource(R.string.notif_summary_chats, summary.messages, summary.chats)
        else -> stringResource(R.string.notif_summary_chat, summary.messages, summary.chatName)
    }
    val body = summary?.lastLine ?: (item.messages.lastOrNull()?.text ?: item.text)
    Box(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onOpen)
                .padding(vertical = 2.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        color = colors.onWallpaper,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        "  " + relativeTime(shown.postTime),
                        color = colors.onWallpaperMuted,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                    if (summary == null && extraCount > 0) {
                        Text(
                            "+$extraCount",
                            color = colors.onAccentContainer,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(colors.accentContainer)
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
                if (body.isNotBlank()) {
                    Text(
                        body,
                        color = colors.onWallpaperMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (summary != null) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            val thumbnail = shown.image
            if (thumbnail != null && LocalNotificationImages.current == NotificationImages.THUMBNAIL) {
                Image(
                    thumbnail,
                    contentDescription = stringResource(R.string.notif_image),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.padding(start = 8.dp).size(THUMBNAIL_SIZE),
                )
            }
            Icon(
                Icons.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.notif_open_panel),
                tint = colors.onWallpaperMuted,
                modifier = Modifier.padding(start = 4.dp).size(20.dp),
            )
        }
    }
}

/** Lato della miniatura accanto al testo: sta nell'altezza di titolo e due righe, così la riga non cresce. */
private val THUMBNAIL_SIZE = 48.dp
