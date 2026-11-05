// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(ExperimentalFoundationApi::class)

package com.gabrielepiccione.onda.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.data.GestureAction
import com.gabrielepiccione.onda.data.SearchButton
import com.gabrielepiccione.onda.data.Settings
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import com.gabrielepiccione.onda.util.HapticEvent
import com.gabrielepiccione.onda.util.LocalOndaHaptics
import kotlin.math.abs

/** Disegno di ogni icona scelta per il pulsante (nomi in [SearchButton.ICONS]). */
fun searchButtonIcon(name: String): ImageVector = when (SearchButton.iconOrDefault(name)) {
    "apps" -> Icons.Filled.Apps
    "notifications" -> Icons.Filled.Notifications
    "lock" -> Icons.Filled.Lock
    "settings" -> Icons.Filled.Settings
    "star" -> Icons.Filled.Star
    "home" -> Icons.Filled.Home
    "bolt" -> Icons.Filled.Bolt
    "phone" -> Icons.Filled.Phone
    "mail" -> Icons.Filled.Mail
    "music" -> Icons.Filled.MusicNote
    "camera" -> Icons.Filled.PhotoCamera
    else -> Icons.Filled.Search
}

/** Colore di sfondo e dell'icona del pulsante: quelli del tema, oppure il colore scelto. */
@Composable
fun searchButtonColors(settings: Settings): Pair<Color, Color> {
    val colors = LocalOndaColors.current
    if (SearchButton.isAutoColor(settings.searchButtonColor)) return colors.accentContainer.copy(alpha = 0.6f) to colors.onAccentContainer
    val base = Color(settings.searchButtonColor)
    return base.copy(alpha = 0.85f) to if (base.luminance() > 0.5f) Color.Black else Color.White
}

/**
 * Pulsante fluttuante accanto all'alfabeto. Tocco, pressione lunga e scorrimento verso l'alto
 * eseguono le azioni scelte dall'utente, le stesse dei gesti della home.
 */
@Composable
fun SearchButton(
    settings: Settings,
    onAction: (GestureAction, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalOndaColors.current
    val haptics = LocalOndaHaptics.current
    val (container, content) = searchButtonColors(settings)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.6f, stiffness = 600f), label = "searchButtonPress")
    val swipeThreshold = with(LocalDensity.current) { SWIPE_DISTANCE.toPx() }
    var dragged by remember { mutableFloatStateOf(0f) }
    val description = stringResource(settings.searchButtonTap.labelRes)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .size(56.dp)
            .clip(CircleShape)
            .background(container)
            .border(1.dp, colors.onWallpaper.copy(alpha = 0.15f), CircleShape)
            .semantics { contentDescription = description }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta -> dragged += delta },
                onDragStarted = { dragged = 0f },
                onDragStopped = {
                    if (-dragged > swipeThreshold && settings.searchButtonSwipe != GestureAction.NONE) {
                        haptics?.perform(HapticEvent.CONFIRM)
                        onAction(settings.searchButtonSwipe, settings.searchButtonSwipeApp)
                    }
                    dragged = 0f
                },
            )
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = { onAction(settings.searchButtonTap, settings.searchButtonTapApp) },
                onLongClick = {
                    if (settings.searchButtonLong != GestureAction.NONE) {
                        haptics?.perform(HapticEvent.LONG_PRESS)
                        onAction(settings.searchButtonLong, settings.searchButtonLongApp)
                    }
                },
            ),
    ) {
        Icon(searchButtonIcon(settings.searchButtonIcon), contentDescription = null, tint = content)
    }
}

/** Quanto si deve scorrere verso l'alto perché valga come swipe. */
private val SWIPE_DISTANCE = 36.dp

/** Altezza dal bordo inferiore, come nel riferimento: poco sopra la barra dei gesti. */
val SEARCH_BUTTON_BOTTOM = 36.dp
