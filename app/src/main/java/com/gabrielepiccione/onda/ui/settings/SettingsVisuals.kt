// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.settings

import com.gabrielepiccione.onda.ui.components.ChoiceRow
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.gabrielepiccione.onda.data.SearchButton
import com.gabrielepiccione.onda.ui.home.searchButtonColors
import com.gabrielepiccione.onda.ui.home.searchButtonIcon
import com.gabrielepiccione.onda.ui.theme.AccentChoices
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.core.icons.IconRenderer
import com.gabrielepiccione.onda.core.icons.IconShape
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.data.CalendarEvent
import com.gabrielepiccione.onda.data.ClockSpacing
import com.gabrielepiccione.onda.data.ClockStyle
import com.gabrielepiccione.onda.data.FontChoice
import com.gabrielepiccione.onda.data.HAlign
import com.gabrielepiccione.onda.data.HomeBlocks
import com.gabrielepiccione.onda.data.Settings
import com.gabrielepiccione.onda.data.ThemeMode
import com.gabrielepiccione.onda.ui.components.AppIcon
import com.gabrielepiccione.onda.ui.components.ClockFace
import com.gabrielepiccione.onda.ui.components.ClockHeader
import com.gabrielepiccione.onda.ui.components.FavoriteRow
import com.gabrielepiccione.onda.ui.home.MAX_HOME_TOP
import com.gabrielepiccione.onda.ui.theme.LocalIconStyle
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import com.gabrielepiccione.onda.ui.theme.blockTouches
import com.gabrielepiccione.onda.ui.theme.fontFamilyOf
import java.time.LocalDateTime

/** Una scelta che mostra il proprio risultato: sopra il disegno, sotto il nome. */
@Composable
internal fun ChoiceTile(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val colors = LocalOndaColors.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.accent else colors.panelBorder, shape)
            .background(if (selected) colors.accentContainer.copy(alpha = 0.35f) else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
    ) {
        content()
        Text(
            label,
            color = if (selected) colors.accent else colors.onWallpaper,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
        )
    }
}

/** Colori che si possono dare al testo. */
internal val TextColorChoices = listOf(0xFFFFFFFF, 0xFFF2EDE4, 0xFF1A1C1E, 0xFF7FE3D1, 0xFFC3A6FF, 0xFFFFD27F, 0xFFFFAB91)

/**
 * Campioni di colore: bersaglio da 48dp con il cerchio da 36dp al centro, e vanno a capo invece di
 * uscire dal riquadro sugli schermi stretti.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColorSwatches(choices: List<Long>, selected: Long, onPick: (Long) -> Unit) {
    val colors = LocalOndaColors.current
    FlowRow(Modifier.padding(bottom = 4.dp)) {
        choices.forEachIndexed { index, argb ->
            val isSelected = selected == argb
            val description = stringResource(R.string.color_option, index + 1, choices.size)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .semantics { contentDescription = description }
                    .selectable(selected = isSelected, role = Role.RadioButton) { onPick(argb) },
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(argb))
                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) colors.accent else colors.panelBorder, CircleShape),
                ) {
                    if (isSelected) Icon(Icons.Filled.Check, null, tint = if (Color(argb).luminance() > 0.5f) Color.Black else Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/** Griglia di scelte a tutta larghezza: nessuna opzione resta nascosta da uno scorrimento laterale. */
@Composable
internal fun <T> TileGrid(items: List<T>, columns: Int, tile: @Composable (T, Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
        items.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { tile(it, Modifier.weight(1f)) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Griglia di scelte con il solo nome (animazioni, motori di ricerca…). */
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun <T> TextChoices(items: List<Pair<T, String>>, selected: T, onPick: (T) -> Unit) =
    // Stesso controllo dappertutto: pulsanti segmentati fino a quattro opzioni, oltre un elenco a tutta riga
    ChoiceRow(null, items, selected, onSelect = onPick)

/** Voce con pulsante radio: si tocca tutta la riga, che per TalkBack è un'unica scelta. */
@Composable
internal fun RadioItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalOndaColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(16.dp))
        Text(label, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge)
    }
}


/** Stili dell'orologio. [compact] li mette in una riga scorrevole, per i fogli che lasciano vedere la home. */
@Composable
internal fun ClockStyleTiles(selected: ClockStyle, compact: Boolean = false, onPick: (ClockStyle) -> Unit) {
    val now = remember { LocalDateTime.now() }
    val tile: @Composable (ClockStyle, Modifier) -> Unit = { style, modifier ->
        ChoiceTile(selected == style, stringResource(style.labelRes), { onPick(style) }, modifier) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(76.dp).clipToBounds()) {
                ClockFace(now, style, TILE_CLOCK_SCALE, TextAlign.Center)
            }
        }
    }
    if (compact) {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ClockStyle.entries.forEach { tile(it, Modifier.width(128.dp)) }
        }
    } else {
        TileGrid(ClockStyle.entries, 2, tile)
    }
}

/** Player compatto o esteso, mostrato con il disegno della scheda. */
@Composable
internal fun MediaStyleTiles(compactSelected: Boolean, onPick: (Boolean) -> Unit) {
    val colors = LocalOndaColors.current
    TileGrid(listOf(true to R.string.media_compact, false to R.string.media_full), 2) { (compact, label), modifier ->
        ChoiceTile(compactSelected == compact, stringResource(label), { onPick(compact) }, modifier) {
            Canvas(Modifier.size(64.dp, 40.dp)) {
                val card = if (compact) 0.45f else 1f
                val top = size.height * (1f - card) / 2f
                drawRoundRect(colors.accent.copy(alpha = 0.25f), Offset(0f, top), Size(size.width, size.height * card), CornerRadius(8.dp.toPx()))
                val art = size.height * card * 0.7f
                drawRoundRect(colors.accent, Offset(5.dp.toPx(), top + (size.height * card - art) / 2f), Size(art, art), CornerRadius(4.dp.toPx()))
                val lineX = 5.dp.toPx() + art + 5.dp.toPx()
                drawRoundRect(colors.accent, Offset(lineX, top + size.height * card * 0.3f), Size(size.width - lineX - 6.dp.toPx(), 3.dp.toPx()), CornerRadius(1.5f.dp.toPx()))
                drawRoundRect(colors.accent.copy(alpha = 0.6f), Offset(lineX, top + size.height * card * 0.55f), Size((size.width - lineX) * 0.5f, 3.dp.toPx()), CornerRadius(1.5f.dp.toPx()))
            }
        }
    }
}

@Composable
internal fun FontTiles(selected: FontChoice, onPick: (FontChoice) -> Unit) {
    val colors = LocalOndaColors.current
    TileGrid(FontChoice.entries, 3) { font, modifier ->
        ChoiceTile(selected == font, stringResource(font.labelRes), { onPick(font) }, modifier) {
            Text("Aa", color = colors.onWallpaper, fontFamily = fontFamilyOf(font), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
internal fun ThemeTiles(selected: ThemeMode, onPick: (ThemeMode) -> Unit) {
    val choices = listOf(ThemeMode.SYSTEM to R.string.theme_system, ThemeMode.LIGHT to R.string.theme_light, ThemeMode.DARK to R.string.theme_dark)
    TileGrid(choices, 3) { (mode, label), modifier ->
        ChoiceTile(selected == mode, stringResource(label), { onPick(mode) }, modifier) {
            Row(Modifier.size(56.dp, 34.dp).clip(RoundedCornerShape(8.dp))) {
                if (mode != ThemeMode.DARK) Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFFF2F2F2)))
                if (mode != ThemeMode.LIGHT) Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF1C1C1E)))
            }
        }
    }
}

/** Allineamento mostrato con tre righe di testo stilizzate. */
@Composable
internal fun AlignTiles(selected: HAlign, onPick: (HAlign) -> Unit) {
    val colors = LocalOndaColors.current
    TileGrid(HAlign.entries, 3) { align, modifier ->
        ChoiceTile(selected == align, stringResource(align.labelRes), { onPick(align) }, modifier) {
            Canvas(Modifier.size(44.dp, 26.dp)) {
                listOf(1f, 0.55f, 0.8f).forEachIndexed { i, fraction ->
                    val width = size.width * fraction
                    val x = when (align) {
                        HAlign.START -> 0f
                        HAlign.CENTER -> (size.width - width) / 2f
                        HAlign.END -> size.width - width
                    }
                    drawRoundRect(colors.accent, Offset(x, i * size.height / 2.6f), Size(width, 4.dp.toPx()), CornerRadius(2.dp.toPx()))
                }
            }
        }
    }
}

/** Stile delle icone mostrato su un'app vera, se ce n'è una. */
@Composable
internal fun IconStyleTiles(selected: String, sample: AppInfo?, onPick: (String) -> Unit) {
    val colors = LocalOndaColors.current
    val styles = listOf(
        "ORIGINAL" to R.string.style_original, "MATERIAL" to R.string.style_material, "OUTLINE" to R.string.style_outline,
        "STICKER" to R.string.style_sticker, "WATERCOLOR" to R.string.style_watercolor, "MARBLE" to R.string.style_marble,
        "DOTS" to R.string.style_dots,
    )
    TileGrid(styles, 4) { (id, label), modifier ->
        ChoiceTile(selected == id, stringResource(label), { onPick(id) }, modifier) {
            when {
                // "Puntini" nasce quando si caricano le icone: qui se ne disegna un assaggio
                id == "DOTS" -> Canvas(Modifier.size(40.dp)) {
                    val tint = sample?.let { Color(it.color) } ?: colors.accent
                    for (x in 0..4) for (y in 0..4) {
                        drawCircle(tint, size.width / 13f, Offset(size.width * (x + 0.5f) / 5f, size.height * (y + 0.5f) / 5f))
                    }
                }
                sample != null -> CompositionLocalProvider(LocalIconStyle provides id) { AppIcon(sample, 40.dp, false) }
            }
        }
    }
}

@Composable
internal fun IconShapeTiles(selected: String, onPick: (String) -> Unit) {
    val colors = LocalOndaColors.current
    val shapes = listOf(
        IconShape.SYSTEM to R.string.shape_system, IconShape.CIRCLE to R.string.shape_circle, IconShape.SQUIRCLE to R.string.shape_squircle,
        IconShape.ROUNDED_SQUARE to R.string.shape_rounded, IconShape.TEARDROP to R.string.shape_teardrop,
        IconShape.CYLINDER to R.string.shape_cylinder, IconShape.PETAL to R.string.shape_petal,
    )
    TileGrid(shapes, 4) { (shape, label), modifier ->
        ChoiceTile(selected == shape.name, stringResource(label), { onPick(shape.name) }, modifier) {
            Canvas(Modifier.size(36.dp)) {
                drawPath(IconRenderer.shapePath(shape, size.minDimension).asComposePath(), colors.accent)
            }
        }
    }
}

/** Cornice dell'anteprima, sopra lo sfondo vero: reagisce a ogni modifica e non è toccabile. */
@Composable
private fun LivePreview(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp)
            .clipToBounds()
            .clearAndSetSemantics { }
            .padding(horizontal = 28.dp, vertical = 8.dp),
    ) {
        Column { content() }
        Box(Modifier.matchParentSize().blockTouches())
    }
}

/** Righe vere dei preferiti, con le stesse impostazioni della home. */
@Composable
private fun PreviewRows(settings: Settings, allApps: List<AppInfo>, count: Int) {
    val apps = remember(settings.favorites, allApps, count) {
        allApps.filter { it.key in settings.favorites }.take(count).ifEmpty { allApps.take(count) }
    }
    apps.forEach { app ->
        FavoriteRow(
            app = app,
            swipeTarget = null,
            notifications = emptyList(),
            showIcon = settings.showIcons,
            themedIcons = settings.themedIcons,
            hideName = settings.hideFavoriteNames,
            iconScale = settings.iconSize,
            onClick = {},
            onLongClick = {},
            onSwipe = {},
            onNotificationClick = {},
            onNotificationDismiss = {},
        )
    }
}

/** Orologio con data e, a titolo d'esempio, evento e temperatura se sono attivi. */
@Composable
private fun PreviewClock(settings: Settings) {
    val colors = LocalOndaColors.current
    if (!settings.showClock) {
        Text(stringResource(R.string.preview_clock_hidden), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
        return
    }
    val eventTitle = stringResource(R.string.preview_event)
    val event = remember(eventTitle) {
        val begin = System.currentTimeMillis() + 3_600_000L
        CalendarEvent(title = eventTitle, begin = begin, allDay = false, eventId = 0L)
    }
    ClockHeader(
        event = event.takeIf { settings.showCalendar },
        onTime = {},
        onDate = {},
        onEvent = {},
        style = settings.clockStyle,
        scale = settings.clockScale,
        align = settings.clockAlign,
        showBattery = settings.showBattery,
        temperature = PREVIEW_TEMPERATURE.takeIf { settings.showWeather },
        innerSpacing = settings.clockInnerSpacing,
    )
}

@Composable
internal fun StylePreview(settings: Settings, allApps: List<AppInfo>) = LivePreview {
    PreviewClock(settings)
    PreviewRows(settings, allApps, 2)
}

@Composable
internal fun ClockPreview(settings: Settings) = LivePreview { PreviewClock(settings) }

@Composable
internal fun RowsPreview(settings: Settings, allApps: List<AppInfo>) = LivePreview { PreviewRows(settings, allApps, 3) }

/** Telefono in miniatura con accanto la spiegazione di ciò che si vede. */
@Composable
internal fun PhonePreview(
    settings: Settings,
    legend: List<String>,
    emphasizeAlphabet: Boolean = false,
    onPosition: ((Float) -> Unit)? = null,
) {
    val colors = LocalOndaColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        PhoneMock(settings, emphasizeAlphabet, onPosition)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            legend.forEach { Text(it, color = colors.onWallpaper, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

/** Schema della home in proporzione: blocchi nell'ordine scelto, alfabeto e pulsante di ricerca. */
@Composable
private fun PhoneMock(settings: Settings, emphasizeAlphabet: Boolean, onPosition: ((Float) -> Unit)?) {
    val colors = LocalOndaColors.current
    val shape = RoundedCornerShape(16.dp)
    val currentOnPosition by rememberUpdatedState(onPosition)
    val phoneHeight = PHONE_HEIGHT
    val ratio = phoneHeight.value / LocalConfiguration.current.screenHeightDp.coerceAtLeast(1)
    val manual = settings.homeTopFraction >= 0f
    Box(
        Modifier
            .size(PHONE_WIDTH, phoneHeight)
            .clip(shape)
            .background(colors.panel)
            .border(1.dp, colors.onWallpaperMuted, shape)
            .clearAndSetSemantics { }
            .pointerInput(onPosition != null) {
                if (onPosition == null) return@pointerInput
                awaitEachGesture {
                    fun place(y: Float) = currentOnPosition?.invoke((y / size.height).coerceIn(0f, MAX_HOME_TOP))
                    val down = awaitFirstDown()
                    down.consume()
                    place(down.position.y)
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        change.consume()
                        place(change.position.y)
                    }
                }
            },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy((settings.homeBlockSpacing / 3f).dp),
            modifier = Modifier
                .align(if (manual) Alignment.TopStart else Alignment.BottomStart)
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = if (manual) phoneHeight * settings.homeTopFraction else 0.dp,
                    bottom = if (manual) 0.dp else 10.dp,
                ),
        ) {
            HomeBlocks.resolve(settings).forEach { MockBlock(it, settings) }
        }
        // Lettere: stesso passo e stessa dimensione del telefono vero, in proporzione
        val letters = 27
        val letterTint = if (emphasizeAlphabet) colors.accent else colors.onWallpaperMuted
        val lettersAlpha = if (settings.hideAlphabet) 0.25f else 1f
        val bottomInset = 10.dp
        Canvas(
            Modifier
                .align(if (settings.leftHanded) Alignment.CenterStart else Alignment.CenterEnd)
                .fillMaxHeight()
                .width(12.dp),
        ) {
            val step = (settings.alphabetStepDp.dp.toPx() * ratio).coerceAtMost((size.height - bottomInset.toPx()) / letters)
            val radius = 1.dp.toPx() * settings.alphabetScale
            for (i in 0 until letters) {
                val free = (size.height - bottomInset.toPx() - step * letters).coerceAtLeast(0f)
                val y = size.height - bottomInset.toPx() - step * (letters - i - 0.5f) - free * (1f - settings.alphabetRest.coerceIn(0f, 1f))
                drawCircle(letterTint.copy(alpha = lettersAlpha), radius, Offset(size.width / 2f, y))
            }
        }
        if (emphasizeAlphabet) {
            // Il lato opposto scorre come l'alfabeto, ma non si vede
            Box(
                Modifier
                    .align(if (settings.leftHanded) Alignment.CenterEnd else Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(5.dp)
                    .background(colors.accent.copy(alpha = 0.25f)),
            )
        }
        if (settings.showSearchButton) {
            val (container, content) = searchButtonColors(settings)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(if (settings.leftHanded) Alignment.BottomStart else Alignment.BottomEnd)
                    .padding(start = if (settings.leftHanded) 14.dp else 0.dp, end = if (settings.leftHanded) 0.dp else 14.dp, bottom = 14.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(container),
            ) {
                Icon(searchButtonIcon(settings.searchButtonIcon), null, tint = content, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun MockBlock(block: String, settings: Settings) {
    val colors = LocalOndaColors.current
    val muted = colors.onWallpaperMuted.copy(alpha = 0.55f)
    @Composable
    fun Bar(height: Int, fraction: Float = 1f, color: Color = muted) {
        Box(Modifier.fillMaxWidth(fraction).height(height.dp).clip(RoundedCornerShape(4.dp)).background(color))
    }
    when {
        block == HomeBlocks.CLOCK && settings.showClock -> Column(
            verticalArrangement = Arrangement.spacedBy(ClockSpacing.previewDp(settings.clockInnerSpacing).dp),
            horizontalAlignment = when (settings.clockAlign) {
                HAlign.START -> Alignment.Start
                HAlign.CENTER -> Alignment.CenterHorizontally
                HAlign.END -> Alignment.End
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Bar(16, 0.75f, colors.accent)
            Bar(4, 0.55f)
        }
        block == HomeBlocks.MEDIA && settings.showMedia -> Bar(10, color = muted.copy(alpha = 0.3f))
        block == HomeBlocks.AGENDA || block == HomeBlocks.NOTIFICATIONS -> Bar(14)
        block.startsWith(HomeBlocks.WIDGET_PREFIX) || block.startsWith(HomeBlocks.STACK_PREFIX) -> Bar(26)
        block == HomeBlocks.APPS -> repeat(5) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                if (settings.showIcons) Box(Modifier.size((8 * settings.iconSize).dp).clip(CircleShape).background(colors.onWallpaper))
                if (!settings.hideFavoriteNames) Bar(4, if (it % 2 == 0) 0.8f else 0.55f)
            }
        }
    }
}

private const val TILE_CLOCK_SCALE = 0.42f
private const val PREVIEW_TEMPERATURE = 21
private val PHONE_HEIGHT = 216.dp
private val PHONE_WIDTH = 106.dp

/** Colore del pulsante: automatico (quello del tema) o uno a scelta. */
@Composable
internal fun SearchButtonColorChoices(selected: Long, onPick: (Long) -> Unit) {
    val colors = LocalOndaColors.current
    val choices = listOf(0L) + AccentChoices + listOf(0xFF1C1C1EL, 0xFFFFFFFFL)
    TileGrid(choices, 5) { argb, modifier ->
        val isSelected = selected == argb
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .heightIn(min = 48.dp)
                .clip(CircleShape)
                .border(if (isSelected) 3.dp else 1.dp, if (isSelected) colors.accent else colors.panelBorder, CircleShape)
                .background(if (argb == 0L) colors.accentContainer.copy(alpha = 0.6f) else Color(argb), CircleShape)
                .selectable(selected = isSelected, role = Role.RadioButton) { onPick(argb) },
        ) {
            if (argb == 0L) Text(stringResource(R.string.text_color_auto), color = colors.onAccentContainer, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Icona del pulsante tra quelle disponibili. */
@Composable
internal fun SearchButtonIconChoices(selected: String, onPick: (String) -> Unit) {
    val colors = LocalOndaColors.current
    TileGrid(SearchButton.ICONS, 6) { name, modifier ->
        val isSelected = SearchButton.iconOrDefault(selected) == name
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(if (isSelected) 2.dp else 1.dp, if (isSelected) colors.accent else colors.panelBorder, RoundedCornerShape(16.dp))
                .background(if (isSelected) colors.accentContainer.copy(alpha = 0.35f) else Color.Transparent)
                .semantics { contentDescription = name }
                .selectable(selected = isSelected, role = Role.RadioButton) { onPick(name) },
        ) {
            Icon(searchButtonIcon(name), contentDescription = null, tint = if (isSelected) colors.accent else colors.onWallpaper)
        }
    }
}
