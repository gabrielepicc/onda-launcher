// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.sheets

import android.app.Notification
import android.content.pm.ShortcutInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.data.NotificationItem
import com.gabrielepiccione.onda.data.WidgetManager
import com.gabrielepiccione.onda.ui.components.AppIcon
import com.gabrielepiccione.onda.ui.components.HostedWidget
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors

/** Dove disegnare il popup, in pixel; [above] = sopra la riga da cui è stato aperto. */
data class QuickPopupPlacement(val x: Int, val y: Int, val above: Boolean)

/**
 * Sceglie la posizione del popup: sotto la riga se c'è spazio, altrimenti sopra, altrimenti dove
 * entra meglio; sempre dentro lo schermo. Senza riga di riferimento resta al centro.
 */
fun placeQuickPopup(
    anchorLeft: Int?,
    anchorTop: Int?,
    anchorBottom: Int?,
    popupWidth: Int,
    popupHeight: Int,
    screenWidth: Int,
    screenHeight: Int,
    topLimit: Int,
    bottomLimit: Int,
    margin: Int,
): QuickPopupPlacement {
    val maxX = (screenWidth - popupWidth - margin).coerceAtLeast(margin)
    val minY = topLimit + margin
    val maxY = (screenHeight - bottomLimit - margin - popupHeight).coerceAtLeast(minY)
    if (anchorLeft == null || anchorTop == null || anchorBottom == null) {
        return QuickPopupPlacement((screenWidth - popupWidth) / 2, ((screenHeight - popupHeight) / 2).coerceIn(minY, maxY), above = false)
    }
    val x = anchorLeft.coerceIn(margin, maxX)
    val below = anchorBottom + margin
    val aboveY = anchorTop - margin - popupHeight
    return when {
        below <= maxY -> QuickPopupPlacement(x, below, above = false)
        aboveY >= minY -> QuickPopupPlacement(x, aboveY, above = true)
        else -> QuickPopupPlacement(x, maxY, above = true)
    }
}

/**
 * Popup dell'app all'altezza della sua riga: notifiche, scorciatoie e azioni rapide.
 * La risposta alle notifiche si apre nel pannello, così il campo resta sopra la tastiera.
 */
@Composable
fun AppQuickPopup(
    app: AppInfo,
    anchor: Rect?,
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
    onNotificationAction: (NotificationItem, Notification.Action) -> Unit,
    onReply: (NotificationItem) -> Unit,
    onToggleFavorite: () -> Unit,
    onInfo: () -> Unit,
    onUninstall: () -> Unit,
    onMoreOptions: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalOndaColors.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val bars = WindowInsets.systemBars.asPaddingValues()
    val enter = remember(app.key) { Animatable(0f) }
    LaunchedEffect(app.key) { enter.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 700f)) }
    BackHandler(onBack = onDismiss)

    val above = remember { booleanArrayOf(false) }
    val screenWidth = config.screenWidthDp.dp
    val popupWidth = min(320.dp, screenWidth - 32.dp)
    val maxHeight = config.screenHeightDp.dp * 0.7f

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = enter.value.coerceIn(0f, 1f) }
            .background(Color.Black.copy(alpha = 0.25f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
    ) {
        Column(
            Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                    val spot = placeQuickPopup(
                        anchorLeft = anchor?.left?.toInt(),
                        anchorTop = anchor?.top?.toInt(),
                        anchorBottom = anchor?.bottom?.toInt(),
                        popupWidth = placeable.width,
                        popupHeight = placeable.height,
                        screenWidth = constraints.maxWidth,
                        screenHeight = constraints.maxHeight,
                        topLimit = with(density) { bars.calculateTopPadding().roundToPx() },
                        bottomLimit = with(density) { bars.calculateBottomPadding().roundToPx() },
                        margin = with(density) { 8.dp.roundToPx() },
                    )
                    above[0] = spot.above
                    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(spot.x, spot.y) }
                }
                .width(popupWidth)
                .heightIn(max = maxHeight)
                .graphicsLayer {
                    val s = 0.85f + 0.15f * enter.value
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(0.15f, if (above[0]) 1f else 0f)
                }
                .clip(RoundedCornerShape(24.dp))
                .background(colors.panel)
                // I tocchi dentro il popup non devono chiuderlo
                .pointerInput(Unit) { detectTapGestures { } }
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenApp(); onDismiss() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                AppIcon(app, 36.dp, themedIcons)
                Spacer(Modifier.width(12.dp))
                Text(app.label, color = colors.onWallpaper, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            notifications.forEach { n ->
                Conversation(
                    item = n,
                    selectedForReply = false,
                    onOpen = { onNotification(n); onDismiss() },
                    onSelectReply = { onReply(n); onDismiss() },
                    onAction = { a -> onNotificationAction(n, a) },
                    onDismiss = { onDismissNotification(n) },
                )
            }

            if (widgetId != null) {
                Box(Modifier.padding(horizontal = 8.dp)) { HostedWidget(widgetId, widgets) }
            }

            shortcuts.forEach { sc ->
                val icon = remember(sc.id) { shortcutIcon(sc) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShortcut(sc); onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (icon != null) {
                        Image(icon, null, Modifier.size(24.dp).clip(CircleShape))
                    } else {
                        Icon(Icons.Filled.PlayArrow, null, tint = colors.accent, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Text((sc.shortLabel ?: sc.longLabel ?: "").toString(), color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                QuickAction(
                    if (app.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    stringResource(if (app.isFavorite) R.string.favorite_remove else R.string.favorite_add),
                ) { onToggleFavorite(); onDismiss() }
                QuickAction(Icons.Filled.Info, stringResource(R.string.app_info)) { onInfo(); onDismiss() }
                QuickAction(Icons.Filled.Delete, stringResource(R.string.uninstall)) { onUninstall(); onDismiss() }
                QuickAction(Icons.Filled.MoreVert, stringResource(R.string.more_options)) { onMoreOptions(); onDismiss() }
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .semantics { contentDescription = description }
            .clickable(onClick = onClick),
    ) {
        Icon(icon, null, tint = LocalOndaColors.current.onWallpaper)
    }
}
