// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.data.DayUsage
import com.gabrielepiccione.onda.data.NotificationCounter
import com.gabrielepiccione.onda.data.UsageStatsMath
import com.gabrielepiccione.onda.data.UsageStatsRepository
import com.gabrielepiccione.onda.ui.components.AppIcon
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Giorni mostrati nel grafico; se ne leggono il doppio per confrontare con la settimana prima. */
private const val SHOWN_DAYS = 7
private const val TOP_APPS = 10

/**
 * Benessere digitale: quanto si usa il telefono, quando e con quali app, con il confronto rispetto
 * alla settimana prima. I dati vengono da Android e restano sul telefono.
 */
@Composable
internal fun WellbeingDashboard(
    repo: UsageStatsRepository,
    hasAccess: Boolean,
    apps: List<AppInfo>,
    limits: Map<String, Int>,
    onRequestAccess: () -> Unit,
    onSetLimit: (packageName: String, minutes: Int) -> Unit,
) {
    val colors = LocalOndaColors.current
    if (!hasAccess) {
        DashCard(stringResource(R.string.wellbeing_title)) {
            Text(stringResource(R.string.wellbeing_access_body), color = colors.onWallpaper, style = MaterialTheme.typography.bodyMedium)
            FilledTonalButton(onClick = onRequestAccess, modifier = Modifier.padding(top = 12.dp)) { Text(stringResource(R.string.wellbeing_access_button)) }
        }
        return
    }
    val context = LocalContext.current
    var all by remember { mutableStateOf<List<DayUsage>?>(null) }
    LaunchedEffect(Unit) { all = repo.lastDays(SHOWN_DAYS * 2) }
    val loaded = all
    if (loaded == null) {
        DashCard(stringResource(R.string.wellbeing_title)) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        return
    }
    val previous = loaded.take(SHOWN_DAYS)
    val week = loaded.takeLast(SHOWN_DAYS)
    var selected by remember { mutableIntStateOf(week.lastIndex) }
    val day = week[selected.coerceIn(0, week.lastIndex)]
    val summary = remember(loaded) { UsageStatsMath.summary(week, previous) }
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(day.dayStart).atZone(zone).toLocalDate()
    var notificationCount by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(date) { notificationCount = NotificationCounter.forDay(context, date.toString()).values.sum() }
    var limitFor by remember { mutableStateOf<String?>(null) }
    val byPackage = remember(apps) { apps.associateBy { it.packageName } }

    // Il giorno scelto: tempo totale, confronto con la media e i tre numeri che contano
    DashCard(dayTitle(date)) {
        Text(durationText(day.screenMs), color = colors.onWallpaper, style = MaterialTheme.typography.displaySmall)
        val vsAverage = UsageStatsMath.percentChange(day.screenMs, summary.averageMs)
        if (vsAverage != null && selected == week.lastIndex) {
            Text(
                stringResource(if (vsAverage >= 0) R.string.wellbeing_above_average else R.string.wellbeing_below_average, abs(vsAverage)),
                color = colors.onWallpaperMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(day.unlocks.toString(), stringResource(R.string.wellbeing_unlocks), Modifier.weight(1f))
            StatTile(day.launches.toString(), stringResource(R.string.wellbeing_launches), Modifier.weight(1f))
            StatTile(notificationCount?.toString() ?: "–", stringResource(R.string.wellbeing_notifications), Modifier.weight(1f))
        }
    }

    DashCard(stringResource(R.string.wellbeing_week)) {
        WeekBars(week, selected, summary.averageMs) { selected = it }
    }

    DashCard(stringResource(R.string.wellbeing_hours)) {
        HourBars(day.hourlyMs)
    }

    DashCard(stringResource(R.string.wellbeing_insights)) {
        Insight(stringResource(R.string.wellbeing_daily_average), durationText(summary.averageMs))
        summary.changePercent?.let { change ->
            Insight(
                stringResource(R.string.wellbeing_vs_last_week),
                stringResource(if (change >= 0) R.string.wellbeing_more else R.string.wellbeing_less, abs(change)),
            )
        }
        Insight(stringResource(R.string.wellbeing_median_session), durationText(summary.medianSessionMs))
        Insight(stringResource(R.string.wellbeing_short_checks), stringResource(R.string.wellbeing_short_checks_value, day.shortChecks, day.sessionLengths.size))
        Insight(stringResource(R.string.wellbeing_night), durationText(day.nightMs))
        day.firstUse?.let { Insight(stringResource(R.string.wellbeing_first_use), timeText(it)) }
        Insight(stringResource(R.string.wellbeing_average_unlocks), summary.averageUnlocks.toString())
        summary.busiestDayStart?.let { Insight(stringResource(R.string.wellbeing_busiest_day), weekdayText(it)) }
        if (day.unlocks > 0) {
            Insight(stringResource(R.string.wellbeing_per_unlock), durationText(day.screenMs / day.unlocks))
        }
    }

    DashCard(stringResource(R.string.wellbeing_top_apps)) {
        if (day.apps.isEmpty()) {
            Text(stringResource(R.string.wellbeing_no_data), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
        }
        day.apps.take(TOP_APPS).forEach { usage ->
            val info = byPackage[usage.packageName]
            val limit = limits[usage.packageName] ?: 0
            AppUsageRow(
                info = info,
                label = info?.label ?: usage.packageName,
                usedMs = usage.foregroundMs,
                share = UsageStatsMath.share(usage.foregroundMs, day.apps.first().foregroundMs),
                detail = stringResource(R.string.wellbeing_app_detail, usage.launches, durationText(usage.longestSessionMs)),
                limitMinutes = limit,
                onClick = { limitFor = usage.packageName },
            )
        }
    }

    limitFor?.let { pkg ->
        LimitDialog(
            label = byPackage[pkg]?.label ?: pkg,
            current = limits[pkg] ?: 0,
            averageMs = UsageStatsMath.appTrend(week, pkg).average().toLong(),
            onSave = { minutes -> onSetLimit(pkg, minutes); limitFor = null },
            onDismiss = { limitFor = null },
        )
    }
}

@Composable
private fun DashCard(title: String, content: @Composable () -> Unit) = SettingsSection(title) { content() }

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier) {
    val colors = LocalOndaColors.current
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.panelHigh)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(value, color = colors.onWallpaper, style = MaterialTheme.typography.titleLarge)
        Text(label, color = colors.onWallpaperMuted, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun Insight(label: String, value: String) {
    val colors = LocalOndaColors.current
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, color = colors.onWallpaper, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

/**
 * Sette barre, una per giorno, con la media come riga tratteggiata. Un solo colore (l'accento): il
 * giorno scelto è pieno, gli altri più tenui. Si tocca una barra per vedere quel giorno.
 */
@Composable
private fun WeekBars(week: List<DayUsage>, selected: Int, averageMs: Long, onSelect: (Int) -> Unit) {
    val colors = LocalOndaColors.current
    val max = (week.maxOfOrNull { it.screenMs } ?: 0L).coerceAtLeast(averageMs).coerceAtLeast(1L)
    val description = stringResource(R.string.wellbeing_week_cd, durationText(averageMs))
    Text(
        durationText(week[selected].screenMs),
        color = colors.onWallpaper,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .semantics { contentDescription = description }
            .pointerInput(week.size) {
                detectTapGestures { p -> onSelect(((p.x / size.width) * week.size).toInt().coerceIn(0, week.lastIndex)) }
            },
    ) {
        val slot = size.width / week.size
        val barWidth = slot * 0.56f
        val radius = 4.dp.toPx()
        week.forEachIndexed { i, d ->
            val h = size.height * d.screenMs / max
            if (h > 0f) {
                drawRoundRect(
                    color = if (i == selected) colors.accent else colors.accent.copy(alpha = 0.35f),
                    topLeft = Offset(slot * i + (slot - barWidth) / 2f, size.height - h),
                    size = Size(barWidth, h + radius),
                    cornerRadius = CornerRadius(radius),
                )
            }
        }
        val y = size.height - size.height * averageMs / max
        drawLine(
            colors.onWallpaperMuted,
            Offset(0f, y),
            Offset(size.width, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
        )
    }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        week.forEachIndexed { i, d ->
            Text(
                weekdayShort(d.dayStart),
                color = if (i == selected) colors.onWallpaper else colors.onWallpaperMuted,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).clickable { onSelect(i) },
            )
        }
    }
    Text(
        stringResource(R.string.wellbeing_average_line, durationText(averageMs)),
        color = colors.onWallpaperMuted,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/** Le 24 ore del giorno scelto: si tocca un'ora per leggerne il valore. */
@Composable
private fun HourBars(hourly: List<Long>) {
    val colors = LocalOndaColors.current
    var hour by remember(hourly) { mutableIntStateOf(hourly.indices.maxByOrNull { hourly[it] } ?: 0) }
    val max = (hourly.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val description = stringResource(R.string.wellbeing_hours_cd)
    Text(
        stringResource(R.string.wellbeing_hour_value, hour, (hour + 1) % 24, durationText(hourly.getOrElse(hour) { 0L })),
        color = colors.onWallpaper,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(96.dp)
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                detectTapGestures { p -> hour = ((p.x / size.width) * 24).toInt().coerceIn(0, 23) }
            },
    ) {
        val slot = size.width / 24f
        val barWidth = slot * 0.62f
        val radius = 2.dp.toPx()
        hourly.forEachIndexed { i, ms ->
            val h = size.height * ms / max
            if (h > 0f) {
                drawRoundRect(
                    color = if (i == hour) colors.accent else colors.accent.copy(alpha = 0.35f),
                    topLeft = Offset(slot * i + (slot - barWidth) / 2f, size.height - h),
                    size = Size(barWidth, h + radius),
                    cornerRadius = CornerRadius(radius),
                )
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        listOf(0, 6, 12, 18).forEach { h ->
            Text("$h", color = colors.onWallpaperMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun AppUsageRow(
    info: AppInfo?,
    label: String,
    usedMs: Long,
    share: Float,
    detail: String,
    limitMinutes: Int,
    onClick: () -> Unit,
) {
    val colors = LocalOndaColors.current
    val bar by animateFloatAsState(share, label = "appShare")
    val over = UsageStatsMath.overLimit(usedMs, limitMinutes)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        if (info != null) AppIcon(info, 36.dp, false) else Spacer(Modifier.size(36.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // troncamento voluto: nome dell'app in una riga dell'elenco
                Text(label, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(durationText(usedMs), color = colors.onWallpaper, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            Box(
                Modifier
                    .padding(vertical = 6.dp)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.panelHigh),
            ) {
                // altezza fissa voluta: è la barra della quota, non contiene testo
                Box(Modifier.fillMaxWidth(bar).height(4.dp).clip(RoundedCornerShape(2.dp)).background(colors.accent))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(detail, color = colors.onWallpaperMuted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                if (limitMinutes > 0) {
                    Icon(
                        if (over) Icons.Filled.Warning else Icons.Filled.HourglassEmpty,
                        null,
                        tint = if (over) colors.onWallpaper else colors.onWallpaperMuted,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(if (over) R.string.wellbeing_limit_over else R.string.wellbeing_limit_badge, durationText(limitMinutes * 60_000L)),
                        color = if (over) colors.onWallpaper else colors.onWallpaperMuted,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

/** Limite giornaliero di un'app, a passi di 5 minuti; 0 = nessun limite. */
@Composable
private fun LimitDialog(label: String, current: Int, averageMs: Long, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    var minutes by remember { mutableIntStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.HourglassEmpty, null) },
        title = { Text(stringResource(R.string.wellbeing_limit_title, label)) },
        text = {
            Column {
                Text(
                    if (minutes == 0) stringResource(R.string.wellbeing_limit_none) else durationText(minutes * 60_000L),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Slider(
                    value = minutes.toFloat(),
                    onValueChange = { minutes = ((it / 5f).roundToInt() * 5).coerceIn(0, MAX_LIMIT) },
                    valueRange = 0f..MAX_LIMIT.toFloat(),
                )
                Text(stringResource(R.string.wellbeing_limit_average, durationText(averageMs)), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.wellbeing_limit_how), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = { TextButton(onClick = { onSave(minutes) }) { Text(stringResource(R.string.wellbeing_limit_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.breaker_cancel)) } },
    )
}

private const val MAX_LIMIT = 240

/** Una durata leggibile: "1 h 25 min", "12 min" o "meno di 1 min". */
@Composable
internal fun durationText(ms: Long): String {
    val minutes = (ms / 60_000L).toInt()
    return when {
        ms <= 0L -> stringResource(R.string.duration_zero)
        minutes < 1 -> stringResource(R.string.duration_under_minute)
        minutes < 60 -> stringResource(R.string.duration_minutes, minutes)
        minutes % 60 == 0 -> stringResource(R.string.duration_hours, minutes / 60)
        else -> stringResource(R.string.duration_hours_minutes, minutes / 60, minutes % 60)
    }
}

@Composable
private fun dayTitle(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> stringResource(R.string.today)
        today.minusDays(1) -> stringResource(R.string.yesterday)
        else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
}

private fun timeText(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

private fun weekdayText(dayStart: Long): String =
    Instant.ofEpochMilli(dayStart).atZone(ZoneId.systemDefault()).dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())

private fun weekdayShort(dayStart: Long): String =
    Instant.ofEpochMilli(dayStart).atZone(ZoneId.systemDefault()).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
