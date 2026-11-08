// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.data.CalendarEvent
import com.gabrielepiccione.onda.data.CardPage
import com.gabrielepiccione.onda.data.HomeCarousel
import com.gabrielepiccione.onda.data.MediaAction
import com.gabrielepiccione.onda.data.MediaInfo
import com.gabrielepiccione.onda.data.NotificationDigest
import com.gabrielepiccione.onda.data.PlayerLayout
import com.gabrielepiccione.onda.ui.components.AppIcon
import com.gabrielepiccione.onda.ui.components.MediaCard
import com.gabrielepiccione.onda.ui.components.TonalIcon
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import com.gabrielepiccione.onda.ui.theme.LocalOndaTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** Quello che serve al carosello per disegnare le schede e reagire ai tocchi. */
class CardsCallbacks(
    val onMediaOpen: () -> Unit,
    val onPlayPause: () -> Unit,
    val onPrev: () -> Unit,
    val onNext: () -> Unit,
    val onMediaAction: (MediaAction) -> Unit,
    val onEvent: (CalendarEvent) -> Unit,
    val onAgendaEmpty: () -> Unit,
    val onSummary: () -> Unit,
    /** Pressione lunga su una scheda: la modifica del blocco, come prima. */
    val onLongPress: (String) -> Unit,
)

/**
 * Player, agenda e riepilogo delle notifiche in un solo blocco a scorrimento orizzontale, con l'accenno della
 * scheda successiva e i puntini. Tutte le schede hanno la stessa forma e la stessa altezza del player compatto.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeCardsCarousel(
    pages: List<CardPage>,
    media: MediaInfo?,
    events: List<CalendarEvent>,
    digest: NotificationDigest,
    appOf: (String) -> AppInfo?,
    labelOf: (String) -> String,
    themedIcons: Boolean,
    highlighted: String?,
    callbacks: CardsCallbacks,
    modifier: Modifier = Modifier,
) {
    if (pages.isEmpty()) return
    val colors = LocalOndaColors.current
    val pager = rememberPagerState(pageCount = { pages.size })
    val dragging by pager.interactionSource.collectIsDraggedAsState()
    // Quando parte la musica si va al player, senza rubare lo scorrimento a chi sta toccando
    val playing = media?.playing == true
    val wasPlaying = remember { booleanArrayOf(playing) }
    LaunchedEffect(playing, pages) {
        HomeCarousel.musicTarget(pages, wasPlaying[0], playing, dragging)?.let { pager.animateScrollToPage(it) }
        wasPlaying[0] = playing
    }
    Column(modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pager,
            // Accenno della scheda successiva
            contentPadding = PaddingValues(end = if (pages.size > 1) PEEK.dp else 0.dp),
            pageSpacing = 8.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { index ->
            val page = pages[index]
            Box(Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
                when (page) {
                    CardPage.MEDIA -> if (media != null) {
                        MediaCard(
                            media = media,
                            onOpen = callbacks.onMediaOpen,
                            onPlayPause = callbacks.onPlayPause,
                            onPrev = callbacks.onPrev,
                            onNext = callbacks.onNext,
                            onAction = callbacks.onMediaAction,
                            compact = true,
                            highlighted = highlighted == page.block,
                            onLongPress = { callbacks.onLongPress(page.block) },
                        )
                    }
                    CardPage.AGENDA -> AgendaPage(events, highlighted == page.block, callbacks)
                    CardPage.NOTIFICATIONS -> SummaryPage(digest, appOf, labelOf, themedIcons, highlighted == page.block, callbacks)
                }
            }
        }
        if (HomeCarousel.showDots(pages.size)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                repeat(pages.size) { i ->
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (i == pager.currentPage) colors.accent else colors.onWallpaperMuted.copy(alpha = 0.4f)),
                    )
                }
            }
        }
    }
}

/** Il guscio comune: stessa forma e altezza del player compatto, elemento a sinistra, testi, tocco e pressione lunga. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CardShell(
    highlighted: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    leading: @Composable () -> Unit,
    title: String,
    supporting: String,
) {
    val colors = LocalOndaColors.current
    val shape = LocalOndaTokens.current.shapes.large
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PlayerLayout.COMPACT_HEIGHT.dp)
            .then(if (highlighted) Modifier.border(2.dp, colors.accent, shape) else Modifier)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.9f))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(PlayerLayout.COMPACT_PADDING.dp),
    ) {
        leading()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            // troncamento voluto: titolo e testo di supporto stanno su una riga come nel player
            Text(title, color = colors.onWallpaper, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // troncamento voluto
            Text(supporting, color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Tessera a sinistra: stessa misura e angoli concentrici della copertina del player. */
@Composable
private fun Tile(content: @Composable () -> Unit) {
    val size = PlayerLayout.artSize(PlayerLayout.COMPACT_HEIGHT, PlayerLayout.COMPACT_PADDING).dp
    val radius = PlayerLayout.concentricRadius(PlayerLayout.CONTAINER_RADIUS, PlayerLayout.COMPACT_PADDING).dp
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size).clip(RoundedCornerShape(radius)).background(MaterialTheme.colorScheme.secondaryContainer),
    ) { content() }
}

@Composable
private fun AgendaPage(events: List<CalendarEvent>, highlighted: Boolean, callbacks: CardsCallbacks) {
    val next = events.firstOrNull()
    val locale = Locale.getDefault()
    val zone = ZoneId.systemDefault()
    val day = Instant.ofEpochMilli(next?.begin ?: System.currentTimeMillis()).atZone(zone)
    val scheme = MaterialTheme.colorScheme
    CardShell(
        highlighted = highlighted,
        onClick = { if (next != null) callbacks.onEvent(next) else callbacks.onAgendaEmpty() },
        onLongPress = { callbacks.onLongPress(CardPage.AGENDA.block) },
        leading = {
            Tile {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(day.dayOfMonth.toString(), color = scheme.onSecondaryContainer, style = MaterialTheme.typography.titleMedium)
                    Text(day.month.getDisplayName(TextStyle.SHORT, locale), color = scheme.onSecondaryContainer, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        title = next?.title ?: stringResource(R.string.cards_agenda_empty_title),
        supporting = when {
            next == null -> stringResource(R.string.cards_agenda_empty_sub)
            next.allDay -> stringResource(R.string.cards_agenda_all_day)
            else -> day.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        },
    )
}

@Composable
private fun SummaryPage(
    digest: NotificationDigest,
    appOf: (String) -> AppInfo?,
    labelOf: (String) -> String,
    themedIcons: Boolean,
    highlighted: Boolean,
    callbacks: CardsCallbacks,
) {
    val stacked = HomeCarousel.stackedIcons(digest.packages)
    val names = digest.packages.map(labelOf)
    CardShell(
        highlighted = highlighted,
        onClick = callbacks.onSummary,
        onLongPress = { callbacks.onLongPress(CardPage.NOTIFICATIONS.block) },
        leading = {
            // Icone sovrapposte delle app con notifiche (al più quattro)
            val step = 14
            Box(Modifier.size(width = (ICON + step * (stacked.size - 1).coerceAtLeast(0)).dp, height = PlayerLayout.artSize(PlayerLayout.COMPACT_HEIGHT, PlayerLayout.COMPACT_PADDING).dp)) {
                stacked.forEachIndexed { i, pkg ->
                    val app = appOf(pkg)
                    Box(Modifier.align(Alignment.CenterStart).offset(x = (i * step).dp).semantics { contentDescription = labelOf(pkg) }) {
                        if (app != null) AppIcon(app, ICON.dp, themedIcons) else TonalIcon(Icons.Filled.Notifications, size = ICON.dp)
                    }
                }
            }
        },
        title = pluralStringResource(R.plurals.cards_notifications, digest.total, digest.total),
        supporting = names.joinToString(", "),
    )
}

private const val PEEK = 32
private const val ICON = 32

/** Nome di un'app da mostrare: quello del launcher, poi quello di sistema, mai il pacchetto se c'è un nome. */
@Composable
fun rememberAppLabel(appOf: (String) -> AppInfo?): (String) -> String {
    val pm = LocalContext.current.packageManager
    return remember(appOf, pm) {
        { pkg: String ->
            appOf(pkg)?.label ?: runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrNull() ?: pkg
        }
    }
}
