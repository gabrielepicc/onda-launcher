// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.gabrielepiccione.onda.ui.components

import com.gabrielepiccione.onda.data.ConversationSummary
import com.gabrielepiccione.onda.util.HapticEvent
import com.gabrielepiccione.onda.util.LocalOndaHaptics
import com.gabrielepiccione.onda.ui.theme.*
import com.gabrielepiccione.onda.ui.home.*
import com.gabrielepiccione.onda.ui.drawer.*
import com.gabrielepiccione.onda.ui.search.*
import com.gabrielepiccione.onda.ui.settings.*
import com.gabrielepiccione.onda.ui.sheets.*

import androidx.compose.ui.res.stringResource
import com.gabrielepiccione.onda.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.data.HAlign
import com.gabrielepiccione.onda.data.NotificationItem
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Icona dell'app: normale oppure "a tema" (monocromatica sui colori Material You). */
@Composable
fun AppIcon(app: AppInfo, size: Dp, themed: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalOndaColors.current
    val mono = app.mono
    val style = LocalIconStyle.current
    if (style in GENERATED_ICON_STYLES && !app.hasCustomIcon) {
        GeneratedIcon(app, size, style, modifier)
    } else if (themed && mono != null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(colors.accentContainer)
        ) {
            Image(
                bitmap = mono,
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.onAccentContainer),
                modifier = Modifier.size(size),
            )
        }
    } else {
        Image(bitmap = app.icon, contentDescription = null, modifier = modifier.size(size))
    }
}

/** Modifier che rimpicciolisce leggermente l'elemento mentre è premuto (feedback tattile visivo). */
@Composable
private fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(dampingRatio = 0.6f, stiffness = 600f), label = "press")
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * Riga grande della home: icona, nome e anteprima delle notifiche.
 * Scorrendo verso destra si apre l'app associata (azione di scorrimento) oppure si espandono le notifiche.
 */
@Composable
fun FavoriteRow(
    app: AppInfo,
    swipeTarget: AppInfo?,
    notifications: List<NotificationItem>,
    showIcon: Boolean,
    themedIcons: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSwipe: () -> Unit,
    hideName: Boolean = false,
    /** Apre il pannello dell'app (swipe a sinistra, o a destra se non c'è un'app associata). */
    onOpenPanel: () -> Unit = onSwipe,
    /** Swipe a sinistra: elimina le notifiche dell'app (se ce ne sono). */
    onSwipeLeft: () -> Unit = {},
    iconScale: Float = 1f,
    onNotificationClick: (NotificationItem) -> Unit,
    onNotificationDismiss: (NotificationItem) -> Unit,
    modifier: Modifier = Modifier,
    onNotificationReply: (NotificationItem, String) -> Boolean = { _, _ -> false },
    onNotificationAction: (NotificationItem, android.app.Notification.Action) -> Unit = { _, _ -> },
) {
    val colors = LocalOndaColors.current
    val haptics = LocalHapticFeedback.current
    val ondaHaptics = LocalOndaHaptics.current
    val hapticsOn = LocalHapticsEnabled.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val interaction = remember { MutableInteractionSource() }
    var expanded by rememberSaveable(app.key) { mutableStateOf(false) }

    val offsetX = remember { Animatable(0f) }
    val triggerPx = with(density) { 72.dp.toPx() }
    val maxPx = with(density) { 140.dp.toPx() }
    var armed by remember { mutableStateOf(false) }

    val latest = notifications.firstOrNull()
    val canSwipe = true
    val canDismiss = notifications.any { it.clearable }

    Box(modifier.fillMaxWidth()) {
        // Sfondo rivelato dallo scorrimento
        if (kotlin.math.abs(offsetX.value) > 1f) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (armed) colors.accentContainer else colors.panel)
                    .padding(horizontal = 20.dp),
                horizontalArrangement = if (offsetX.value > 0f) Arrangement.Start else Arrangement.End,
            ) {
                val p = (kotlin.math.abs(offsetX.value) / triggerPx).coerceIn(0f, 1f)
                Box(Modifier.graphicsLayer { alpha = p; scaleX = 0.6f + 0.4f * p; scaleY = 0.6f + 0.4f * p }) {
                    if (offsetX.value > 0f) {
                        Icon(Icons.Filled.KeyboardArrowRight, null, tint = colors.onAccentContainer)
                    } else if (canDismiss) {
                        Icon(Icons.Filled.Close, null, tint = colors.onAccentContainer)
                    }
                }
            }
        }

        Column(
            Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .pressScale(interaction)
                .clip(RoundedCornerShape(28.dp))
                .then(if (kotlin.math.abs(offsetX.value) > 1f) Modifier.background(colors.scrim) else Modifier)
                .draggable(
                    enabled = canSwipe,
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val resist = if (kotlin.math.abs(offsetX.value) > triggerPx) 0.35f else 1f
                            // A sinistra si "tira" davvero solo se c'è qualcosa da eliminare
                            val next = (offsetX.value + delta * resist).coerceIn(if (canDismiss) -maxPx else -maxPx * 0.2f, maxPx)
                            offsetX.snapTo(next)
                            val nowArmed = next >= triggerPx || (canDismiss && next <= -triggerPx)
                            if (nowArmed != armed) {
                                armed = nowArmed
                                ondaHaptics?.perform(HapticEvent.LONG_PRESS)
                            }
                        }
                    },
                    onDragStopped = {
                        if (armed) {
                            if (offsetX.value > 0f) onOpenPanel() else onSwipeLeft()
                        }
                        armed = false
                        offsetX.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 500f))
                    },
                )
                .combinedClickable(
                    interactionSource = interaction,
                    indication = androidx.compose.material3.ripple(),
                    onClick = onClick,
                    onLongClick = {
                        ondaHaptics?.perform(HapticEvent.LONG_PRESS)
                        onLongClick()
                    },
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .animateContentSize(Motion.bouncy())
        ) {
            val listStyle = LocalListStyle.current
            val listFont = LocalListFont.current
            val shadow = LocalTextShadow.current
            val rowDirection = if (listStyle.align == HAlign.END) androidx.compose.ui.unit.LayoutDirection.Rtl
            else androidx.compose.ui.platform.LocalLayoutDirection.current
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides rowDirection) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (listStyle.align == HAlign.CENTER) Arrangement.Center else Arrangement.Start,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (showIcon) {
                    Box {
                        AppIcon(app, 44.dp * iconScale, themedIcons)
                        val dot by animateFloatAsState(if (latest != null) 1f else 0f, Motion.bouncy(), label = "dot")
                        if (dot > 0.01f) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .graphicsLayer { scaleX = dot; scaleY = dot; alpha = dot.coerceIn(0f, 1f) }
                                    .size(13.dp)
                                    .clip(CircleShape)
                                    .background(colors.scrim)
                                    .padding(2.5.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent)
                            )
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                }
                // Nome dell'app e, accanto all'icona, l'anteprima dell'ultima notifica
                Column(
                    if (listStyle.align == HAlign.CENTER) Modifier.weight(1f, fill = false) else Modifier.weight(1f),
                    horizontalAlignment = if (listStyle.align == HAlign.CENTER) Alignment.CenterHorizontally else Alignment.Start,
                ) {
                    if (!(hideName && showIcon)) {
                        Text(
                            text = app.label,
                            color = colors.onWallpaper,
                            style = (if (latest != null) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall).copy(
                                fontWeight = FontWeight(listStyle.weight.coerceIn(100, 900)),
                                letterSpacing = listStyle.letterSpacingEm.em,
                                shadow = shadow,
                            ).let { st -> if (listFont != null) st.copy(fontFamily = listFont) else st },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    AnimatedVisibility(
                        visible = latest != null,
                        enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
                        exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit()),
                    ) {
                        notifications.firstOrNull()?.let { n ->
                            key(n.key) {
                                NotificationPreview(
                                    item = n,
                                    extraCount = notifications.size - 1,
                                    summary = ConversationSummary.of(notifications),
                                    onOpen = { onNotificationClick(n) },
                                    onDismiss = { onNotificationDismiss(n) },
                                )
                            }
                        }
                    }
                }
                if (!showIcon && latest != null) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(colors.accent))
                }
            }
            }
        }
    }
}

/** Scorciatoia aggiunta alla home da un'altra app: stesso aspetto di un preferito, senza notifiche. */
@Composable
fun PinnedShortcutRow(
    label: String,
    icon: ImageBitmap?,
    showIcon: Boolean,
    hideName: Boolean,
    iconScale: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalOndaColors.current
    val ondaHaptics = LocalOndaHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val listStyle = LocalListStyle.current
    val listFont = LocalListFont.current
    val shadow = LocalTextShadow.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (listStyle.align == HAlign.CENTER) Arrangement.Center else Arrangement.Start,
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clip(RoundedCornerShape(28.dp))
            .combinedClickable(
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick,
                onLongClick = {
                    ondaHaptics?.perform(HapticEvent.LONG_PRESS)
                    onLongClick()
                },
            )
            .semantics { contentDescription = label }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        if (showIcon) {
            if (icon != null) Image(icon, null, Modifier.size(44.dp * iconScale))
            else Box(Modifier.size(44.dp * iconScale).clip(CircleShape).background(colors.accentContainer))
            Spacer(Modifier.width(18.dp))
        }
        if (!(hideName && showIcon)) {
            Text(
                text = label,
                color = colors.onWallpaper,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight(listStyle.weight.coerceIn(100, 900)),
                    letterSpacing = listStyle.letterSpacingEm.em,
                    shadow = shadow,
                ).let { st -> if (listFont != null) st.copy(fontFamily = listFont) else st },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Riga compatta per l'elenco completo e i risultati di ricerca. */
@Composable
fun AppListRow(
    app: AppInfo,
    hasNotification: Boolean,
    showIcon: Boolean,
    themedIcons: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    /** Ultima notifica dell'app, mostrata sotto il nome (tocco = pannello dell'app). */
    notification: NotificationItem? = null,
    notificationCount: Int = 0,
    /** Riepilogo "N messaggi da M chat" quando ci sono più messaggi. */
    summary: ConversationSummary? = null,
    onOpenPanel: () -> Unit = {},
    /** Swipe come in home: destra = pannello, sinistra = elimina notifiche. */
    swipeEnabled: Boolean = false,
    onDismissNotifications: () -> Unit = {},
) {
    val colors = LocalOndaColors.current
    val haptics = LocalHapticFeedback.current
    val ondaHaptics = LocalOndaHaptics.current
    val hapticsOn = LocalHapticsEnabled.current
    val interaction = remember { MutableInteractionSource() }
    SwipeActions(
        canDismiss = notification?.clearable == true,
        onRight = onOpenPanel,
        onLeft = { ondaHaptics?.perform(HapticEvent.CONFIRM); onDismissNotifications() },
        enabled = swipeEnabled,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(interaction)
                .clip(RoundedCornerShape(24.dp))
                .background(if (highlighted) colors.accentContainer.copy(alpha = 0.6f) else Color.Transparent)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = androidx.compose.material3.ripple(),
                    onClick = onClick,
                    onLongClick = {
                        ondaHaptics?.perform(HapticEvent.LONG_PRESS)
                        onLongClick()
                    },
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (showIcon) {
                AppIcon(app, 36.dp, themedIcons)
                Spacer(Modifier.width(16.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    app.label,
                    color = colors.onWallpaper,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (notification != null) {
                    NotificationPreview(
                        item = notification,
                        extraCount = (notificationCount - 1).coerceAtLeast(0),
                        summary = summary,
                        onOpen = onOpenPanel,
                        onDismiss = {},
                    )
                }
            }
            if (app.isWorkProfile) {
                Text(
                    stringResource(R.string.work),
                    color = colors.onAccentContainer,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.accentContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            if (hasNotification && notification == null) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(colors.accent))
            }
        }
    }
}
