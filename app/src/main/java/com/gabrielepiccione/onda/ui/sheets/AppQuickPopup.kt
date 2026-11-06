// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import com.gabrielepiccione.onda.data.Actions
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.ui.components.actionIcon
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
    /** Azioni personalizzate del popup (catalogo unico): testo salvato e nome da mostrare. */
    customActions: List<Pair<String, String>> = emptyList(),
    onCustomAction: (String) -> Unit = {},
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
    /** Con notifiche, scorciatoie e widget si piegano in una riga in fondo. */
    collapseExtras: Boolean = true,
) {
    val colors = LocalOndaColors.current
    val ondaHaptics = LocalOndaHaptics.current
    LaunchedEffect(app.key) { ondaHaptics?.perform(HapticEvent.OPEN) }
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val bars = WindowInsets.systemBars.asPaddingValues()
    // Movimento di Material 3 Expressive: il contenitore cresce dall'icona con una molla spaziale appena
    // elastica, la sua opacità segue una molla "effetto" senza rimbalzi, e il contenuto entra a cascata
    val enter = remember(app.key) { Animatable(0f) }
    val opacity = remember(app.key) { Animatable(0f) }
    val content = remember(app.key) { Animatable(0f) }
    val exit = remember(app.key) { Animatable(0f) }
    LaunchedEffect(app.key) {
        launch { enter.animateTo(1f, spring(dampingRatio = SPATIAL_DAMPING, stiffness = SPATIAL_STIFFNESS)) }
        launch { opacity.animateTo(1f, spring(dampingRatio = 1f, stiffness = EFFECTS_STIFFNESS)) }
        content.animateTo(1f, tween(CONTENT_MILLIS, easing = Motion.EmphasizedDecelerate))
    }
    val scope = rememberCoroutineScope()
    val closing = remember(app.key) { booleanArrayOf(false) }
    // Uscita: breve e accelerata, il popup sfuma rimpicciolendosi verso l'icona da cui era uscito
    val leave: suspend () -> Unit = {
        exit.animateTo(1f, tween(EXIT_MILLIS, easing = Motion.EmphasizedAccelerate))
        onDismiss()
    }
    val dismiss: () -> Unit = {
        if (!closing[0]) {
            closing[0] = true
            scope.launch { leave() }
        }
    }
    BackHandler(onBack = dismiss)

    // Tirare il popup oltre il bordo del contenuto (o quando non c'è nulla da scorrere) lo rimpicciolisce e lo
    // schiarisce, come il gesto Indietro predittivo: lasciato abbastanza lontano, o con un colpo deciso, si chiude
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
                    leave()
                } else {
                    animate(from, 0f, available.y, spring(dampingRatio = SPATIAL_DAMPING, stiffness = SPATIAL_STIFFNESS)) { v, _ -> pull = v }
                }
                return available
            }
        }
    }

    // Punto da cui cresce il popup, in frazioni della sua misura: l'icona della riga
    val origin = remember(app.key) { floatArrayOf(0.15f, 0f) }
    val screenWidth = config.screenWidthDp.dp
    val popupWidth = min(320.dp, screenWidth - 32.dp)
    val maxHeight = config.screenHeightDp.dp * 0.7f

    // Tutto il testo del popup parte dal colore leggibile sul pannello, anche quello senza colore esplicito
    CompositionLocalProvider(LocalContentColor provides colors.onWallpaper) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = opacity.value.coerceIn(0f, 1f) * (1f - exit.value) }
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
                    val iconX = anchor?.let { it.left + with(density) { ICON_CENTER.toPx() } } ?: (spot.x + placeable.width / 2f)
                    val iconY = anchor?.center?.y ?: (spot.y + placeable.height / 2f)
                    origin[0] = ((iconX - spot.x) / placeable.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    origin[1] = ((iconY - spot.y) / placeable.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(spot.x, spot.y) }
                }
                .width(popupWidth)
                .heightIn(max = maxHeight)
                .graphicsLayer {
                    val pulled = (abs(pull) / thresholdPx).coerceIn(0f, 1f)
                    val scale = (ENTER_SCALE + (1f - ENTER_SCALE) * enter.value) *
                        (1f - PULL_SHRINK * pulled) * (1f - (1f - EXIT_SCALE) * exit.value)
                    scaleX = scale
                    scaleY = scale
                    alpha = opacity.value.coerceIn(0f, 1f) * (1f - PULL_FADE * pulled) * (1f - exit.value)
                    translationY = pull * PULL_FOLLOW
                    transformOrigin = TransformOrigin(origin[0], origin[1])
                    shadowElevation = 6.dp.toPx() * alpha
                    shape = RoundedCornerShape(POPUP_CORNER)
                    clip = true
                }
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
                modifier = Modifier.cascade(content, 0).fillMaxWidth().padding(start = 8.dp, end = 4.dp),
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

            if (notifications.isNotEmpty()) Column(Modifier.cascade(content, 1)) {
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

            // Con notifiche (e l'opzione attiva) le scorciatoie e il widget restano piegati in fondo
            val folded = collapseExtras && notifications.isNotEmpty() && (shortcuts.isNotEmpty() || widgetIds.isNotEmpty())
            var extrasOpen by remember(app.key) { mutableStateOf(false) }
            val shortcutsBlock: @Composable () -> Unit = {
            if (shortcuts.isNotEmpty()) Column(Modifier.cascade(content, 2)) {
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
            }
            if (!folded) shortcutsBlock()

            if (relatedApps.isNotEmpty()) Column(Modifier.cascade(content, 3)) {
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

            if (customActions.isNotEmpty()) Column(Modifier.cascade(content, 3)) {
                SectionLabel(R.string.popup_actions)
                customActions.forEach { (encoded, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onCustomAction(encoded); dismiss() }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Icon(actionIcon(Actions.decode(encoded).action), null, tint = colors.accent, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(label, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // Più widget si sfogliano in orizzontale, come le pile della home
            val widgetsBlock: @Composable () -> Unit = {
            if (widgetIds.isNotEmpty()) Column(Modifier.cascade(content, 4)) {
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
            if (!folded) {
                widgetsBlock()
            } else {
                FoldedExtras(open = extrasOpen, onToggle = { extrasOpen = !extrasOpen })
                AnimatedVisibility(
                    visible = extrasOpen,
                    enter = expandVertically(Motion.enter()) + fadeIn(Motion.enter()),
                    exit = shrinkVertically(Motion.exit()) + fadeOut(Motion.exit()),
                ) {
                    Column {
                        shortcutsBlock()
                        widgetsBlock()
                    }
                }
            }
        }
    }
    }
}

/**
 * Entrata a cascata delle sezioni: ognuna parte un poco dopo la precedente, sale di 12dp e compare.
 * Si legge nella fase di disegno, così l'animazione non ricompone il contenuto.
 */
private fun Modifier.cascade(progress: Animatable<Float, *>, index: Int): Modifier = graphicsLayer {
    val p = ((progress.value - index * CASCADE_STEP) / (1f - CASCADE_STEP * CASCADE_SECTIONS)).coerceIn(0f, 1f)
    alpha = p
    translationY = (1f - p) * 12.dp.toPx()
}

/** Riga "Scorciatoie" che apre e chiude sul posto scorciatoie e widget, senza chiudere il popup. */
@Composable
private fun FoldedExtras(open: Boolean, onToggle: () -> Unit) {
    val colors = LocalOndaColors.current
    val turn by animateFloatAsState(if (open) 180f else 0f, Motion.bouncy(), label = "foldedExtras")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(Icons.Filled.Bolt, null, tint = colors.accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(stringResource(R.string.popup_section_shortcuts), color = colors.onWallpaper, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.KeyboardArrowDown, null, tint = colors.onWallpaperMuted, modifier = Modifier.graphicsLayer { rotationZ = turn })
    }
}

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

/**
 * Valori del movimento, dai token di Material 3: molla spaziale "default" dello schema Expressive
 * (smorzamento 0,8, rigidità 380), molla degli effetti (1600), uscita Short4 (200 ms), contenuto Medium4 (400 ms).
 */
private const val SPATIAL_DAMPING = 0.8f
private const val SPATIAL_STIFFNESS = 380f
private const val EFFECTS_STIFFNESS = 1600f
private const val EXIT_MILLIS = 200
private const val CONTENT_MILLIS = 400
private const val ENTER_SCALE = 0.8f
private const val EXIT_SCALE = 0.9f
private const val CASCADE_STEP = 0.08f
private const val CASCADE_SECTIONS = 4

/** Tirandolo: quanto segue il dito, quanto si rimpicciolisce e quanto si schiarisce al massimo. */
private const val PULL_FOLLOW = 0.3f
private const val PULL_SHRINK = 0.08f
private const val PULL_FADE = 0.3f
private val POPUP_CORNER = 28.dp

/** Centro dell'icona rispetto al bordo sinistro della riga. */
private val ICON_CENTER = 36.dp
