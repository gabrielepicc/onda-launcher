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
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
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
import kotlin.math.max
import kotlin.math.min
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipPath
import com.gabrielepiccione.onda.ui.components.bubbleClip
import com.gabrielepiccione.onda.ui.components.drawBubbleBurst
import com.gabrielepiccione.onda.ui.components.drawBubbleFilm
import com.gabrielepiccione.onda.ui.components.puncturePoint
import com.gabrielepiccione.onda.util.BubbleMotion
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
    // Si apre come una bolla che esce dall'icona. Si chiude come una bolla vera: si raccoglie in un cerchio,
    // la pellicola si fora in un punto, si ritira dal foro e il bordo si rompe in goccioline
    LaunchedEffect(app.key) { enter.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 260f)) }
    val gather = remember(app.key) { Animatable(0f) }
    val burst = remember(app.key) { Animatable(0f) }
    val droplets = remember(app.key) { BubbleMotion.droplets(app.key.hashCode(), DROPLETS) }
    val scope = rememberCoroutineScope()
    val closing = remember(app.key) { booleanArrayOf(false) }
    val pop: suspend () -> Unit = {
        gather.animateTo(1f, tween(GATHER_MILLIS, easing = Motion.EmphasizedAccelerate))
        ondaHaptics?.perform(HapticEvent.CONFIRM)
        burst.animateTo(1f, tween(BURST_MILLIS, easing = LinearEasing))
        onDismiss()
    }
    val dismiss: () -> Unit = {
        if (!closing[0]) {
            closing[0] = true
            scope.launch { pop() }
        }
    }
    BackHandler(onBack = dismiss)

    // Tirare il popup oltre il bordo del contenuto (o quando non c'è nulla da scorrere) lo arrotonda verso la bolla:
    // lasciandolo abbastanza teso, o con un colpo deciso, scoppia; altrimenti torna com'era con un rimbalzo
    var pull by remember(app.key) { mutableFloatStateOf(0f) }
    val thresholdPx = with(density) { PullToDismiss.THRESHOLD_DP.dp.toPx() }
    val flingPx = with(density) { PullToDismiss.FLING_DP.dp.toPx() }
    val cornerPx = with(density) { POPUP_CORNER.toPx() }
    val rimPx = with(density) { 1.5.dp.toPx() }
    val dropletPx = with(density) { 3.5.dp.toPx() }
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
                    gather.snapTo((abs(from) / thresholdPx * PULL_GATHER).coerceIn(0f, PULL_GATHER))
                    pop()
                } else {
                    animate(from, 0f, available.y, spring(dampingRatio = 0.4f, stiffness = 350f)) { v, _ -> pull = v }
                }
                return available
            }
        }
    }

    val bubble = remember(app.key) { BubbleGeometry() }
    val screenWidth = config.screenWidthDp.dp
    val popupWidth = min(320.dp, screenWidth - 32.dp)
    val maxHeight = config.screenHeightDp.dp * 0.7f

    // Tutto il testo del popup parte dal colore leggibile sul pannello, anche quello senza colore esplicito
    CompositionLocalProvider(LocalContentColor provides colors.onWallpaper) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = enter.value.coerceIn(0f, 1f) * (1f - burst.value) }
                .background(Color.Black.copy(alpha = 0.25f))
                .pointerInput(Unit) { detectTapGestures { dismiss() } }
        )
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
                    // La bolla nasce dall'icona della riga, non da un angolo del popup
                    val iconX = anchor?.let { it.left + with(density) { ICON_CENTER.toPx() } } ?: (spot.x + placeable.width / 2f)
                    val iconY = anchor?.center?.y ?: (spot.y + placeable.height / 2f)
                    bubble.left = spot.x.toFloat()
                    bubble.top = spot.y.toFloat()
                    bubble.originX = iconX - spot.x
                    bubble.originY = iconY - spot.y
                    bubble.width = placeable.width.toFloat()
                    bubble.height = placeable.height.toFloat()
                    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(spot.x, spot.y) }
                }
                .width(popupWidth)
                .heightIn(max = maxHeight)
                .graphicsLayer { translationY = pull * PULL_FOLLOW }
                .drawWithContent {
                    if (burst.value >= 1f) return@drawWithContent
                    val state = bubble.state(enter.value, gather.value, pull, thresholdPx, burst.value)
                    if (state == null) {
                        drawContent()
                    } else {
                        clipPath(bubbleClip(size, cornerPx, state.center, state.radius, state.hole, state.holeRadius)) {
                            this@drawWithContent.drawContent()
                        }
                    }
                }
                .clip(RoundedCornerShape(POPUP_CORNER))
                .background(colors.panel)
                // I tocchi dentro il popup non devono chiuderlo
                .pointerInput(Unit) { detectTapGestures { } }
                .nestedScroll(pullConnection)
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp, bottom = 12.dp),
        ) {
            var menu by remember(app.key) { mutableStateOf(false) }

            // Intestazione: l'app (si apre toccandola) e, in alto a destra, il menu con le altre azioni
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenApp(); dismiss() }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    AppIcon(app, 40.dp, themedIcons)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(app.label, color = colors.onWallpaper, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(stringResource(R.string.popup_open_app), color = colors.onWallpaperMuted, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Box {
                    QuickAction(Icons.Filled.MoreVert, stringResource(R.string.more_options)) { menu = true }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        MenuEntry(
                            if (app.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            stringResource(if (app.isFavorite) R.string.favorite_remove else R.string.favorite_add),
                        ) { menu = false; onToggleFavorite(); dismiss() }
                        MenuEntry(Icons.Filled.Info, stringResource(R.string.app_info)) { menu = false; onInfo(); dismiss() }
                        MenuEntry(Icons.Filled.Edit, stringResource(R.string.popup_edit)) { menu = false; onEdit(); dismiss() }
                        onStopSuggesting?.let { stop ->
                            MenuEntry(Icons.Filled.VisibilityOff, stringResource(R.string.suggested_stop)) { menu = false; stop(); dismiss() }
                        }
                        MenuEntry(Icons.Filled.Delete, stringResource(R.string.uninstall)) { menu = false; onUninstall(); dismiss() }
                        MenuEntry(Icons.Filled.MoreVert, stringResource(R.string.more_options)) { menu = false; onMoreOptions(); dismiss() }
                    }
                }
            }

            if (notifications.isNotEmpty()) {
                SectionLabel(R.string.popup_section_notifications)
                Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    notifications.forEach { n ->
                        CompactConversation(
                            item = n,
                            onOpen = { onNotification(n); dismiss() },
                            onSelectReply = { onReply(n); dismiss() },
                            onAction = { a -> onNotificationAction(n, a) },
                            onDismiss = { onDismissNotification(n) },
                        )
                    }
                }
            }

            if (shortcuts.isNotEmpty()) {
                SectionLabel(R.string.popup_section_shortcuts)
                Column(Modifier.padding(horizontal = 8.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.panelHigh.copy(alpha = 0.5f))) {
                    shortcuts.forEach { sc ->
                        val icon = remember(sc.id) { shortcutIcon(sc) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onShortcut(sc); dismiss() }
                                .heightIn(min = 48.dp)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            if (icon != null) {
                                Image(icon, null, Modifier.size(24.dp).clip(CircleShape))
                            } else {
                                Icon(Icons.Filled.PlayArrow, null, tint = colors.accent, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text((sc.shortLabel ?: sc.longLabel ?: "").toString(), color = colors.onWallpaper, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            if (relatedApps.isNotEmpty()) {
                SectionLabel(R.string.popup_section_related)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
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
            if (widgetIds.isNotEmpty()) {
                SectionLabel(R.string.popup_section_widgets)
                when (widgetIds.size) {
                    1 -> Box(Modifier.padding(horizontal = 8.dp)) { HostedWidget(widgetIds.single(), widgets) }
                    else -> WidgetStackCard(
                        stack = WidgetStack("popup_" + app.key, widgetIds),
                        widgets = widgets,
                        highlighted = false,
                        onLongPress = null,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        }
        // Sopra tutto, senza prendere i tocchi: la pellicola e le gocce escono dai bordi del popup
        Canvas(Modifier.fillMaxSize()) {
            val state = bubble.state(enter.value, gather.value, pull, thresholdPx, burst.value) ?: return@Canvas
            val center = Offset(bubble.left + state.center.x, bubble.top + state.center.y + pull * PULL_FOLLOW)
            if (burst.value <= 0f) {
                drawBubbleFilm(center, state.radius, state.filmAlpha, rimPx)
            } else {
                val rimFade = 1f - (burst.value / 0.3f).coerceIn(0f, 1f)
                drawBubbleFilm(center, state.radius, rimFade * state.filmAlpha, rimPx)
                drawBubbleBurst(center, state.radius, burst.value, droplets, dropletPx, rimPx, colors.accent)
            }
        }
    }
    }
}

/**
 * Dove sta la bolla rispetto al popup. La posizione si conosce solo misurando, così si aggiorna in
 * fase di layout e si legge disegnando.
 */
private class BubbleGeometry {
    var left = 0f
    var top = 0f
    var width = 0f
    var height = 0f
    var originX = 0f
    var originY = 0f

    /** Lo stato da disegnare, o null se il popup è fermo e intero: niente ritaglio, niente lavoro in più. */
    fun state(enter: Float, gather: Float, pull: Float, thresholdPx: Float, burst: Float): BubbleState? {
        val pulled = (abs(pull) / thresholdPx * PULL_GATHER).coerceIn(0f, PULL_GATHER)
        val closing = max(gather, pulled)
        if (enter >= 0.995f && closing <= 0f && burst <= 0f) return null
        val origin = Offset(originX, originY)
        val covering = BubbleMotion.coveringRadius(width, height, originX, originY)
        val middle = Offset(width / 2f, height / 2f)
        if (closing > 0f || burst > 0f) {
            // Si raccoglie al centro del popup, in modo continuo da dove si trovava il cerchio di apertura
            val center = Offset(lerp(origin.x, middle.x, closing), lerp(origin.y, middle.y, closing))
            // Se si chiude mentre si sta ancora aprendo, parte da quanto era già cresciuta
            val reached = min(covering, BubbleMotion.openRadius(enter, min(width, height) * 0.12f, covering))
            val radius = BubbleMotion.gatherRadius(closing, reached, min(reached, BubbleMotion.roundRadius(width, height)))
            val hole = if (burst > 0f) puncturePoint(center, radius) else null
            return BubbleState(center, radius, hole, BubbleMotion.holeRadius(burst, radius), closing.coerceIn(0f, 1f))
        }
        val start = min(width, height) * 0.12f
        val radius = BubbleMotion.openRadius(enter, start, covering)
        // Si vede la pellicola mentre si apre, poi il cerchio si scioglie nel popup
        val film = (1f - enter / 0.85f).coerceIn(0f, 1f)
        return BubbleState(origin, radius, null, 0f, film)
    }
}

private class BubbleState(val center: Offset, val radius: Float, val hole: Offset?, val holeRadius: Float, val filmAlpha: Float)

private fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t

/** Titolo di una sezione del popup, così si distingue cosa è l'app, cosa sono le notifiche e cosa le scorciatoie. */
@Composable
private fun SectionLabel(@StringRes res: Int) {
    Text(
        stringResource(res),
        color = LocalOndaColors.current.accent,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun MenuEntry(icon: ImageVector, text: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        leadingIcon = { Icon(icon, null) },
        onClick = onClick,
    )
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

/** Raccogliersi è rapido e scoppiare ancora di più: Material chiede uscite brevi (token Short3 e Medium2). */
private const val GATHER_MILLIS = 150
private const val BURST_MILLIS = 300
private const val DROPLETS = 22

/** Quanto il popup segue il dito quando lo si tira, e quanto si arrotonda al massimo prima di scoppiare. */
private const val PULL_FOLLOW = 0.5f
private const val PULL_GATHER = 0.55f
private val POPUP_CORNER = 28.dp

/** Centro dell'icona rispetto al bordo sinistro della riga. */
private val ICON_CENTER = 36.dp
