// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.sheets

import androidx.compose.animation.core.tween
import androidx.compose.runtime.rememberCoroutineScope
import com.gabrielepiccione.onda.ui.theme.Motion
import kotlinx.coroutines.launch
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import com.gabrielepiccione.onda.util.HapticEvent
import com.gabrielepiccione.onda.util.LocalOndaHaptics
import android.app.Notification
import android.content.pm.ShortcutInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import com.gabrielepiccione.onda.util.PullToDismiss
import kotlin.math.abs
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.Edit
import com.gabrielepiccione.onda.data.WidgetStack
import com.gabrielepiccione.onda.ui.components.WidgetStackCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VisibilityOff
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
    widgetIds: List<Int>,
    relatedApps: List<AppInfo>,
    onRelatedApp: (AppInfo) -> Unit,
    onEdit: () -> Unit,
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
    /** Solo per le app suggerite in home: non proporla più. */
    onStopSuggesting: (() -> Unit)? = null,
) {
    val colors = LocalOndaColors.current
    val ondaHaptics = LocalOndaHaptics.current
    LaunchedEffect(app.key) { ondaHaptics?.perform(HapticEvent.OPEN) }
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val bars = WindowInsets.systemBars.asPaddingValues()
    val enter = remember(app.key) { Animatable(0f) }
    // Si apre come una bolla che si gonfia (supera un poco la misura e si assesta) e si chiude come se scoppiasse
    LaunchedEffect(app.key) { enter.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 420f)) }
    val burst = remember(app.key) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val closing = remember(app.key) { booleanArrayOf(false) }
    val pop: suspend () -> Unit = {
        ondaHaptics?.perform(HapticEvent.CONFIRM)
        burst.animateTo(1f, tween(POP_MILLIS, easing = Motion.EmphasizedDecelerate))
        onDismiss()
    }
    val dismiss: () -> Unit = {
        if (!closing[0]) {
            closing[0] = true
            scope.launch { pop() }
        }
    }
    BackHandler(onBack = dismiss)

    // Tirare il popup oltre il bordo del contenuto (o quando non c'è nulla da scorrere) lo tende come una bolla:
    // lasciandolo abbastanza teso, o con un colpo deciso, scoppia; altrimenti torna com'era con un rimbalzo
    var pull by remember(app.key) { mutableFloatStateOf(0f) }
    val thresholdPx = with(density) { PullToDismiss.THRESHOLD_DP.dp.toPx() }
    val flingPx = with(density) { PullToDismiss.FLING_DP.dp.toPx() }
    val pullConnection = remember(app.key, thresholdPx, flingPx) {
        object : NestedScrollConnection {
            // Se è già spostato, il dito che torna indietro riporta prima a posto il popup
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (closing[0] || pull == 0f || source != NestedScrollSource.UserInput) return Offset.Zero
                val moved = PullToDismiss.next(pull, available.y)
                val used = (moved - pull) / PullToDismiss.RESISTANCE
                pull = moved
                return Offset(0f, used)
            }

            // Quello che il contenuto non può più scorrere sposta il popup
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (closing[0] || source != NestedScrollSource.UserInput || available.y == 0f) return Offset.Zero
                pull = PullToDismiss.next(pull, available.y)
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (closing[0] || pull == 0f) return Velocity.Zero
                val from = pull
                if (PullToDismiss.shouldDismiss(from, available.y, thresholdPx, flingPx)) {
                    closing[0] = true
                    pop()
                } else {
                    animate(from, 0f, available.y, spring(dampingRatio = 0.4f, stiffness = 350f)) { v, _ -> pull = v }
                }
                return available
            }
        }
    }

    val above = remember { booleanArrayOf(false) }
    val screenWidth = config.screenWidthDp.dp
    val popupWidth = min(320.dp, screenWidth - 32.dp)
    val maxHeight = config.screenHeightDp.dp * 0.7f

    // Tutto il testo del popup parte dal colore leggibile sul pannello, anche quello senza colore esplicito
    CompositionLocalProvider(LocalContentColor provides colors.onWallpaper) {
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = enter.value.coerceIn(0f, 1f) * (1f - burst.value) }
            .background(Color.Black.copy(alpha = 0.25f))
            .pointerInput(Unit) { detectTapGestures { dismiss() } }
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
                    // Bolla: parte piccola, si gonfia oltre la misura e si assesta; tirata si allunga; scoppiando si dilata
                    val base = (0.25f + 0.75f * enter.value) * (1f + POP_GROWTH * burst.value)
                    val stretch = (abs(pull) / 1400f).coerceAtMost(0.12f)
                    scaleX = base * (1f - stretch / 2f)
                    scaleY = base * (1f + stretch)
                    transformOrigin = TransformOrigin(0.15f, if (above[0]) 1f else 0f)
                }
                .clip(RoundedCornerShape(24.dp))
                .background(colors.panel)
                // I tocchi dentro il popup non devono chiuderlo
                .pointerInput(Unit) { detectTapGestures { } }
                .nestedScroll(pullConnection)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenApp(); dismiss() }
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
                    onOpen = { onNotification(n); dismiss() },
                    onSelectReply = { onReply(n); dismiss() },
                    onAction = { a -> onNotificationAction(n, a) },
                    onDismiss = { onDismissNotification(n) },
                )
            }

            if (relatedApps.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    relatedApps.forEach { other ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(68.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onRelatedApp(other); dismiss() }
                                .padding(vertical = 8.dp),
                        ) {
                            AppIcon(other, 40.dp, themedIcons)
                            Text(
                                other.label,
                                color = colors.onWallpaper,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }

            // Più widget si sfogliano in orizzontale, come le pile della home
            when (widgetIds.size) {
                0 -> Unit
                1 -> Box(Modifier.padding(horizontal = 8.dp)) { HostedWidget(widgetIds.single(), widgets) }
                else -> WidgetStackCard(
                    stack = WidgetStack("popup_" + app.key, widgetIds),
                    widgets = widgets,
                    highlighted = false,
                    onLongPress = null,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }

            shortcuts.forEach { sc ->
                val icon = remember(sc.id) { shortcutIcon(sc) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShortcut(sc); dismiss() }
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

            onStopSuggesting?.let { stop ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { stop(); dismiss() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Icon(Icons.Filled.VisibilityOff, null, tint = colors.accent, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(stringResource(R.string.suggested_stop), color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                QuickAction(
                    if (app.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    stringResource(if (app.isFavorite) R.string.favorite_remove else R.string.favorite_add),
                ) { onToggleFavorite(); dismiss() }
                QuickAction(Icons.Filled.Info, stringResource(R.string.app_info)) { onInfo(); dismiss() }
                QuickAction(Icons.Filled.Delete, stringResource(R.string.uninstall)) { onUninstall(); dismiss() }
                QuickAction(Icons.Filled.Edit, stringResource(R.string.popup_edit)) { onEdit(); dismiss() }
                QuickAction(Icons.Filled.MoreVert, stringResource(R.string.more_options)) { onMoreOptions(); dismiss() }
            }
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

private const val POP_MILLIS = 170
private const val POP_GROWTH = 0.16f
