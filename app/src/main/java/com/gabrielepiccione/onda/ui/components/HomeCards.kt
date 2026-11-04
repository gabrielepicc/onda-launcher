// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.gabrielepiccione.onda.ui.components

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
) {
    val colors = LocalOndaColors.current
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
        Text(
            now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale)).replaceFirstChar { it.titlecase(locale) },
            color = colors.onWallpaperMuted,
            style = MaterialTheme.typography.titleMedium,
            textAlign = textAlign,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onDate).padding(vertical = 2.dp),
        )
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

/** Solo il quadrante, riusato anche nelle anteprime dei preset. */
@Composable
fun ClockFace(now: LocalDateTime, style: ClockStyle, scale: Float, textAlign: TextAlign = TextAlign.Start) {
    val colors = LocalOndaColors.current
    val locale = Locale.getDefault()
    val hh = now.format(DateTimeFormatter.ofPattern("HH", locale))
    val mm = now.format(DateTimeFormatter.ofPattern("mm", locale))
    val base = MaterialTheme.typography.displayLarge.copy(color = colors.onWallpaper)
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

/** Mini-player per la sessione multimediale attiva. */
@Composable
fun MediaCard(
    media: MediaInfo,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalOndaColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.panel.copy(alpha = 0.9f))
            .clickable(onClick = onOpen)
            .padding(10.dp)
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.accentContainer),
            contentAlignment = Alignment.Center,
        ) {
            val art = media.art
            if (art != null) {
                Image(art, null, contentScale = ContentScale.Crop, modifier = Modifier.size(52.dp))
            } else {
                Icon(Icons.Filled.PlayArrow, null, tint = colors.onAccentContainer)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(media.title, color = colors.onWallpaper, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (media.artist.isNotBlank()) {
                Text(media.artist, color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        MediaButton(onPrev) { Icon(Icons.Filled.KeyboardArrowLeft, stringResource(R.string.media_previous), tint = colors.onWallpaper) }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(if (media.playing) 14.dp else 22.dp))
                .background(colors.accent)
                .clickable(onClick = onPlayPause)
        ) {
            AnimatedContent(targetState = media.playing, label = "playpause") { playing ->
                if (playing) PauseGlyph(colors.onAccent) else Icon(Icons.Filled.PlayArrow, stringResource(R.string.media_play), tint = colors.onAccent)
            }
        }
        MediaButton(onNext) { Icon(Icons.Filled.KeyboardArrowRight, stringResource(R.string.media_next), tint = colors.onWallpaper) }
    }
}

@Composable
private fun MediaButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick)
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
