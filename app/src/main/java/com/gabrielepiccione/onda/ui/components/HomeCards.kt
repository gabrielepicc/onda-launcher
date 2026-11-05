// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.gabrielepiccione.onda.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.gabrielepiccione.onda.ui.theme.*
import com.gabrielepiccione.onda.ui.home.*
import com.gabrielepiccione.onda.ui.drawer.*
import com.gabrielepiccione.onda.ui.search.*
import com.gabrielepiccione.onda.ui.settings.*
import com.gabrielepiccione.onda.ui.sheets.*

import androidx.compose.ui.res.stringResource
import com.gabrielepiccione.onda.R
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import com.gabrielepiccione.onda.data.ClockStyle
import com.gabrielepiccione.onda.data.HAlign
import com.gabrielepiccione.onda.util.TimeInWords
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabrielepiccione.onda.data.CalendarEvent
import com.gabrielepiccione.onda.data.MediaInfo
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Orologio della home in uno degli stili [ClockStyle], con data e prossimo evento.
 * @param scale moltiplicatore della dimensione (0.6–1.6)
 */
@Composable
fun ClockHeader(
    event: CalendarEvent?,
    onTime: () -> Unit,
    onDate: () -> Unit,
    onEvent: (CalendarEvent) -> Unit,
    style: ClockStyle = ClockStyle.LARGE_THIN,
    scale: Float = 1f,
    align: HAlign = HAlign.START,
    highlighted: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showBattery: Boolean = false,
    /** Temperatura attuale accanto alla data; null = meteo spento o non disponibile. */
    temperature: Int? = null,
) {
    val colors = LocalOndaColors.current
    val shadow = LocalTextShadow.current
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(60_000L - (System.currentTimeMillis() % 60_000L) + 30)
        }
    }
    val locale = Locale.getDefault()
    val horizontal = when (align) {
        HAlign.START -> Alignment.Start
        HAlign.CENTER -> Alignment.CenterHorizontally
        HAlign.END -> Alignment.End
    }
    val textAlign = when (align) {
        HAlign.START -> TextAlign.Start
        HAlign.CENTER -> TextAlign.Center
        HAlign.END -> TextAlign.End
    }

    Column(
        horizontalAlignment = horizontal,
        modifier = modifier
            .fillMaxWidth()
            .then(if (highlighted) Modifier.border(2.dp, colors.accent, RoundedCornerShape(24.dp)) else Modifier)
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(onClick = onTime, onLongClick = onLongPress)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        ClockFace(now, style, scale, textAlign)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale)).replaceFirstChar { it.titlecase(locale) },
                color = colors.onWallpaperMuted,
                style = MaterialTheme.typography.titleMedium.copy(shadow = shadow),
                textAlign = textAlign,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onDate).padding(vertical = 2.dp),
            )
            if (showBattery) {
                val battery = rememberBatteryState()
                if (battery.level >= 0) {
                    Text(
                        "  ·  " + (if (battery.charging) "⚡" else "") + "${battery.level}%",
                        color = if (!battery.charging && battery.level <= 15) MaterialTheme.colorScheme.error else colors.onWallpaperMuted,
                        style = MaterialTheme.typography.titleMedium.copy(shadow = shadow),
                    )
                }
            }
            if (temperature != null) {
                Text(
                    "  ·  " + stringResource(R.string.weather_degrees, temperature),
                    color = colors.onWallpaperMuted,
                    style = MaterialTheme.typography.titleMedium.copy(shadow = shadow),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onDate).padding(vertical = 2.dp),
                )
            }
        }
        if (event != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.accentContainer)
                    .clickable { onEvent(event) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Filled.DateRange, null, tint = colors.onAccentContainer, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "${eventWhen(event, locale, stringResource(R.string.today), stringResource(R.string.tomorrow_at, "%s"))} · ${event.title}",
                    color = colors.onAccentContainer,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Livello e stato di carica della batteria, aggiornati dal broadcast di sistema. */
data class BatteryState(val level: Int, val charging: Boolean)

@Composable
fun rememberBatteryState(): BatteryState {
    val context = androidx.compose.ui.platform.LocalContext.current
    var state by remember { mutableStateOf(BatteryState(-1, false)) }
    androidx.compose.runtime.DisposableEffect(Unit) {
        fun read(i: android.content.Intent?) {
            if (i == null) return
            val level = i.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
            val scale = i.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            val status = i.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
            val charging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL
            state = BatteryState(if (level < 0) -1 else level * 100 / scale, charging)
        }
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: android.content.Context?, i: android.content.Intent?) = read(i)
        }
        // ACTION_BATTERY_CHANGED è "sticky": la registrazione restituisce subito l'ultimo valore
        read(androidx.core.content.ContextCompat.registerReceiver(
            context, receiver, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED,
        ))
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return state
}

/** Solo il quadrante, riusato anche nelle anteprime dei preset. */
@Composable
fun ClockFace(now: LocalDateTime, style: ClockStyle, scale: Float, textAlign: TextAlign = TextAlign.Start) {
    val colors = LocalOndaColors.current
    val locale = Locale.getDefault()
    val hh = now.format(DateTimeFormatter.ofPattern("HH", locale))
    val mm = now.format(DateTimeFormatter.ofPattern("mm", locale))
    val clockFont = LocalClockFont.current
    val base = MaterialTheme.typography.displayLarge.copy(color = colors.onWallpaper, shadow = LocalTextShadow.current)
        .let { if (clockFont != null) it.copy(fontFamily = clockFont) else it }
    when (style) {
        ClockStyle.LARGE_THIN -> AnimatedDigits("$hh:$mm", base.copy(fontSize = 76.sp * scale, fontWeight = FontWeight.Light))
        ClockStyle.STACKED -> Column {
            AnimatedDigits(hh, base.copy(fontSize = 92.sp * scale, fontWeight = FontWeight.Medium, lineHeight = 86.sp * scale))
            AnimatedDigits(mm, base.copy(fontSize = 92.sp * scale, fontWeight = FontWeight.Light, lineHeight = 86.sp * scale, color = colors.accent))
        }
        ClockStyle.BOLD -> AnimatedDigits("$hh:$mm", base.copy(fontSize = 72.sp * scale, fontWeight = FontWeight.Black, color = colors.accent))
        ClockStyle.MONO -> AnimatedDigits(
            "$hh:$mm",
            base.copy(fontSize = 60.sp * scale, fontFamily = FontFamily.Monospace, letterSpacing = 4.sp, fontWeight = FontWeight.Normal),
        )
        ClockStyle.OUTLINE -> AnimatedDigits(
            "$hh:$mm",
            base.copy(fontSize = 84.sp * scale, fontWeight = FontWeight.Bold, drawStyle = Stroke(width = 3f * scale)),
        )
        ClockStyle.GRADIENT -> AnimatedDigits(
            "$hh:$mm",
            base.copy(
                fontSize = 80.sp * scale,
                fontWeight = FontWeight.Bold,
                brush = Brush.linearGradient(listOf(colors.accent, MaterialTheme.colorScheme.tertiary, colors.onWallpaper)),
            ),
        )
        ClockStyle.WORDS -> Text(
            TimeInWords.format(now.hour, now.minute, locale.language),
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp * scale, lineHeight = 40.sp * scale, fontWeight = FontWeight.Medium),
            color = colors.onWallpaper,
            textAlign = textAlign,
        )
        ClockStyle.ANALOG -> AnalogClock(now, 132.dp * scale)
    }
}

/** Cifre che scorrono singolarmente al cambio (solo quelle che cambiano). */
@Composable
private fun AnimatedDigits(text: String, style: androidx.compose.ui.text.TextStyle) {
    Row {
        text.forEachIndexed { i, ch ->
            AnimatedContent(
                targetState = ch,
                transitionSpec = {
                    (slideInVertically(Motion.offsetEnter) { it / 2 } + fadeIn(Motion.enter()))
                        .togetherWith(slideOutVertically(Motion.offsetExit) { -it / 2 } + fadeOut(Motion.exit()))
                        .using(SizeTransform(clip = false))
                },
                label = "digit$i",
            ) { c -> Text(c.toString(), style = style) }
        }
    }
}

/** Orologio analogico minimale: tacche, lancette arrotondate nel colore d'accento. */
@Composable
private fun AnalogClock(now: LocalDateTime, size: androidx.compose.ui.unit.Dp) {
    val colors = LocalOndaColors.current
    val minuteAngle = now.minute * 6f
    val hourAngle = (now.hour % 12) * 30f + now.minute * 0.5f
    val animatedMinute by androidx.compose.animation.core.animateFloatAsState(minuteAngle, Motion.bouncy(), label = "min")
    androidx.compose.foundation.Canvas(Modifier.size(size).padding(4.dp)) {
        val r = this.size.minDimension / 2f
        val c = center
        drawCircle(colors.panel.copy(alpha = 0.55f), radius = r)
        for (i in 0 until 12) {
            val a = Math.toRadians(i * 30.0 - 90)
            val outer = r * 0.9f
            val inner = if (i % 3 == 0) r * 0.74f else r * 0.82f
            drawLine(
                colors.onWallpaperMuted,
                start = androidx.compose.ui.geometry.Offset(c.x + inner * kotlin.math.cos(a).toFloat(), c.y + inner * kotlin.math.sin(a).toFloat()),
                end = androidx.compose.ui.geometry.Offset(c.x + outer * kotlin.math.cos(a).toFloat(), c.y + outer * kotlin.math.sin(a).toFloat()),
                strokeWidth = if (i % 3 == 0) r * 0.045f else r * 0.025f,
                cap = StrokeCap.Round,
            )
        }
        fun hand(angleDeg: Float, length: Float, width: Float, color: androidx.compose.ui.graphics.Color) {
            val a = Math.toRadians(angleDeg - 90.0)
            drawLine(
                color,
                start = c,
                end = androidx.compose.ui.geometry.Offset(c.x + length * kotlin.math.cos(a).toFloat(), c.y + length * kotlin.math.sin(a).toFloat()),
                strokeWidth = width,
                cap = StrokeCap.Round,
            )
        }
        hand(hourAngle, r * 0.5f, r * 0.09f, colors.onWallpaper)
        hand(animatedMinute, r * 0.74f, r * 0.06f, colors.accent)
        drawCircle(colors.accent, radius = r * 0.07f)
    }
}

private fun eventWhen(e: CalendarEvent, locale: Locale, today: String, tomorrowFormat: String): String {
    if (e.allDay) return today
    val start = Instant.ofEpochMilli(e.begin).atZone(ZoneId.systemDefault())
    val hour = start.format(DateTimeFormatter.ofPattern("HH:mm", locale))
    return if (start.toLocalDate() == LocalDate.now()) hour else tomorrowFormat.format(hour)
}

/**
 * Player della musica in riproduzione.
 * Compatto: una riga, copertina e comandi di dimensione fissa, il testo ha la priorità.
 * Esteso: titolo in evidenza, barra di avanzamento sottile e comandi discreti.
 * [scale] ingrandisce tutto insieme: copertina, testo, pulsanti e spazi.
 */
@Composable
fun MediaCard(
    media: MediaInfo,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    onSeek: (Long) -> Unit = {},
    compact: Boolean = true,
    scale: Float = 1f,
    highlighted: Boolean = false,
    onLongPress: (() -> Unit)? = null,
) {
    // Una densità diversa scala in un colpo solo tutte le misure del player
    val base = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(base.density * scale.coerceIn(0.75f, 1.25f), base.fontScale)) {
        if (compact) {
            CompactMediaCard(media, onOpen, onPlayPause, onPrev, onNext, modifier, highlighted, onLongPress)
        } else {
            FullMediaCard(media, onOpen, onPlayPause, onPrev, onNext, modifier, onSeek, highlighted, onLongPress)
        }
    }
}

@Composable
private fun FullMediaCard(
    media: MediaInfo,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier,
    onSeek: (Long) -> Unit,
    highlighted: Boolean,
    onLongPress: (() -> Unit)?,
) {
    val colors = LocalOndaColors.current
    var now by remember { androidx.compose.runtime.mutableLongStateOf(android.os.SystemClock.elapsedRealtime()) }
    androidx.compose.runtime.LaunchedEffect(media.playing, media.positionTime) {
        now = android.os.SystemClock.elapsedRealtime()
        while (media.playing) {
            delay(500)
            now = android.os.SystemClock.elapsedRealtime()
        }
    }
    var dragging by remember { mutableStateOf<Float?>(null) }

    Column(
        modifier
            .fillMaxWidth()
            .then(if (highlighted) Modifier.border(2.dp, colors.accent, RoundedCornerShape(28.dp)) else Modifier)
            .clip(RoundedCornerShape(28.dp))
            .background(colors.panel.copy(alpha = 0.92f))
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MediaArt(media, 56.dp, 16.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    media.title,
                    color = colors.onWallpaper,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 2000),
                )
                if (media.artist.isNotBlank()) {
                    Text(
                        media.artist,
                        color = colors.onWallpaperMuted,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 3000),
                    )
                }
            }
        }

        if (media.duration > 0) {
            val fraction = dragging ?: (media.positionAt(now).toFloat() / media.duration).coerceIn(0f, 1f)
            ThinSeekBar(
                fraction = fraction,
                onDrag = { dragging = it },
                onDragEnd = {
                    dragging?.let { onSeek((it * media.duration).toLong()) }
                    dragging = null
                },
                modifier = Modifier.padding(top = 14.dp),
            )
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(formatMs((fraction * media.duration).toLong()), color = colors.onWallpaperMuted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.weight(1f))
                Text(formatMs(media.duration), color = colors.onWallpaperMuted, style = MaterialTheme.typography.labelSmall)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        ) {
            MediaButton(onPrev) { Icon(Icons.Filled.KeyboardArrowLeft, stringResource(R.string.media_previous), tint = colors.onWallpaper) }
            Spacer(Modifier.width(20.dp))
            PlayButton(media.playing, onPlayPause, width = 64.dp, height = 44.dp)
            Spacer(Modifier.width(20.dp))
            MediaButton(onNext) { Icon(Icons.Filled.KeyboardArrowRight, stringResource(R.string.media_next), tint = colors.onWallpaper) }
        }
    }
}

/** Versione su una riga: copertina piccola, testo con la priorità, comandi compatti. */
@Composable
private fun CompactMediaCard(
    media: MediaInfo,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier,
    highlighted: Boolean,
    onLongPress: (() -> Unit)?,
) {
    val colors = LocalOndaColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(if (highlighted) Modifier.border(2.dp, colors.accent, RoundedCornerShape(24.dp)) else Modifier)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.panel.copy(alpha = 0.9f))
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress)
            .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
    ) {
        MediaArt(media, 40.dp, 12.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                media.title,
                color = colors.onWallpaper,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 2000),
            )
            if (media.artist.isNotBlank()) {
                Text(
                    media.artist,
                    color = colors.onWallpaperMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 3000),
                )
            }
        }
        SmallMediaButton(onPrev) { Icon(Icons.Filled.KeyboardArrowLeft, stringResource(R.string.media_previous), tint = colors.onWallpaper, modifier = Modifier.size(20.dp)) }
        PlayButton(media.playing, onPlayPause, width = 40.dp, height = 40.dp)
        SmallMediaButton(onNext) { Icon(Icons.Filled.KeyboardArrowRight, stringResource(R.string.media_next), tint = colors.onWallpaper, modifier = Modifier.size(20.dp)) }
    }
}

@Composable
private fun MediaArt(media: MediaInfo, size: androidx.compose.ui.unit.Dp, corner: androidx.compose.ui.unit.Dp) {
    val colors = LocalOndaColors.current
    Box(
        Modifier.size(size).clip(RoundedCornerShape(corner)).background(colors.accentContainer),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = media.art
        if (bmp != null) Image(bmp, null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        else Icon(Icons.Filled.PlayArrow, null, tint = colors.onAccentContainer)
    }
}

/** Play/pausa: la forma passa da pillola a quadrato arrotondato quando suona. */
@Composable
private fun PlayButton(playing: Boolean, onClick: () -> Unit, width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) {
    val colors = LocalOndaColors.current
    val cd = stringResource(if (playing) R.string.media_pause else R.string.media_play)
    val corner by androidx.compose.animation.core.animateDpAsState(if (playing) 12.dp else height / 2, Motion.bouncy(), label = "playShape")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(corner))
            .background(colors.accent)
            .semantics { contentDescription = cd }
            .clickable(onClick = onClick)
    ) {
        AnimatedContent(targetState = playing, label = "playpause") { p ->
            if (p) PauseGlyph(colors.onAccent) else Icon(Icons.Filled.PlayArrow, null, tint = colors.onAccent, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SmallMediaButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(width = 36.dp, height = 48.dp).clip(CircleShape).clickable(onClick = onClick)
    ) { content() }
}

/** Barra di avanzamento sottile (4dp) con pallino che compare durante il trascinamento. */
@Composable
private fun ThinSeekBar(
    fraction: Float,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalOndaColors.current
    var active by remember { mutableStateOf(false) }
    val thumb by androidx.compose.animation.core.animateFloatAsState(if (active) 1f else 0f, Motion.bouncy(), label = "thumb")
    val cd = stringResource(R.string.media_seek)
    androidx.compose.foundation.Canvas(
        modifier
            .fillMaxWidth()
            .height(20.dp)
            .semantics { contentDescription = cd }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { o -> active = true; onDrag((o.x / size.width).coerceIn(0f, 1f)) },
                    onDragEnd = { active = false; onDragEnd() },
                    onDragCancel = { active = false; onDragEnd() },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        onDrag((change.position.x / size.width).coerceIn(0f, 1f))
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { o -> onDrag((o.x / size.width).coerceIn(0f, 1f)); onDragEnd() }
            }
    ) {
        val y = size.height / 2
        val h = 4.dp.toPx() + 2.dp.toPx() * thumb
        val x = size.width * fraction
        drawRoundRect(colors.onWallpaperMuted.copy(alpha = 0.25f), topLeft = androidx.compose.ui.geometry.Offset(0f, y - h / 2), size = androidx.compose.ui.geometry.Size(size.width, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2))
        drawRoundRect(colors.accent, topLeft = androidx.compose.ui.geometry.Offset(0f, y - h / 2), size = androidx.compose.ui.geometry.Size(x, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2))
        drawCircle(colors.accent, radius = 4.dp.toPx() + 4.dp.toPx() * thumb, center = androidx.compose.ui.geometry.Offset(x, y))
    }
}

private fun formatMs(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val sec = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

@Composable
private fun MediaButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick)
    ) { content() }
}

/** Icona "pausa" disegnata con due barre (non presente nel set di icone core). */
@Composable
private fun PauseGlyph(color: androidx.compose.ui.graphics.Color) {
    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(width = 4.dp, height = 14.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Box(Modifier.size(width = 4.dp, height = 14.dp).clip(RoundedCornerShape(2.dp)).background(color))
    }
}

/** Agenda: eventi dei prossimi giorni raggruppati per giorno, con il colore del calendario. */
@Composable
fun AgendaCard(
    events: List<CalendarEvent>,
    onEvent: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onEmptyClick: () -> Unit = {},
) {
    val colors = LocalOndaColors.current
    val locale = Locale.getDefault()
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    Column(
        modifier
            .fillMaxWidth()
            .then(if (highlighted) Modifier.border(2.dp, colors.accent, RoundedCornerShape(28.dp)) else Modifier)
            .clip(RoundedCornerShape(28.dp))
            .background(colors.panel.copy(alpha = 0.85f))
            .combinedClickable(onClick = onEmptyClick, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (events.isEmpty()) {
            Text(stringResource(R.string.agenda_empty), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
            return@Column
        }
        events.groupBy { Instant.ofEpochMilli(it.begin).atZone(zone).toLocalDate() }.forEach { (day, list) ->
            val dayLabel = when (day) {
                today -> stringResource(R.string.today)
                today.plusDays(1) -> stringResource(R.string.agenda_tomorrow)
                else -> day.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale)).replaceFirstChar { it.titlecase(locale) }
            }
            Text(dayLabel, color = colors.accent, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
            list.forEach { e ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onEvent(e) }
                        .padding(vertical = 6.dp)
                ) {
                    Box(
                        Modifier
                            .size(width = 4.dp, height = 28.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (e.color != 0) androidx.compose.ui.graphics.Color(e.color) else colors.accent)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.title, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (e.allDay) stringResource(R.string.agenda_all_day)
                            else Instant.ofEpochMilli(e.begin).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm", locale)) +
                                " – " + Instant.ofEpochMilli(e.end).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm", locale)),
                            color = colors.onWallpaperMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
