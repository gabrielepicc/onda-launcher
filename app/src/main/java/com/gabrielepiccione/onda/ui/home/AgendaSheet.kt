// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(ExperimentalMaterial3Api::class)

package com.gabrielepiccione.onda.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.CalendarEvent
import com.gabrielepiccione.onda.data.WeatherDay
import com.gabrielepiccione.onda.data.WeatherSnapshot
import com.gabrielepiccione.onda.ui.components.AgendaCard
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Foglio che si apre toccando la data: meteo (se attivo) e prossimi eventi. */
@Composable
fun AgendaSheet(
    weather: WeatherSnapshot?,
    weatherHint: String?,
    events: List<CalendarEvent>,
    onEvent: (CalendarEvent) -> Unit,
    onEmptyAgenda: () -> Unit,
    onOpenWeather: () -> Unit,
    onOpenCalendar: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalOndaColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = colors.onWallpaper,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (weather != null) WeatherSection(weather, onOpenWeather)
            if (weather == null && weatherHint != null) {
                Text(weatherHint, color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Text(stringResource(R.string.agenda_title), color = colors.accent, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
            AgendaCard(events = events, onEvent = onEvent, highlighted = false, onLongPress = {}, onEmptyClick = onEmptyAgenda)
            TextButton(onClick = onOpenCalendar, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.agenda_open_calendar))
            }
        }
    }
}

@Composable
private fun WeatherSection(weather: WeatherSnapshot, onOpen: () -> Unit) {
    val colors = LocalOndaColors.current
    val locale = Locale.getDefault()
    val zone = ZoneId.systemDefault()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .padding(vertical = 4.dp),
    ) {
        if (weather.place.isNotBlank()) {
            Text(weather.place, color = colors.onWallpaperMuted, style = MaterialTheme.typography.titleSmall)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            weather.temperature?.let { Text(degrees(it), style = MaterialTheme.typography.displaySmall) }
            weather.text?.let { Text(it, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp)) }
        }
        weather.feelsLike?.let {
            Text(stringResource(R.string.weather_feels, degrees(it)), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
    if (weather.hours.isNotEmpty()) {
        val format = DateTimeFormatter.ofPattern("HH:mm", locale)
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
        ) {
            weather.hours.forEach { h ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(h.temperature?.let { degrees(it) }.orEmpty(), style = MaterialTheme.typography.titleMedium)
                    Text(format.format(Instant.ofEpochMilli(h.time).atZone(zone)), color = colors.onWallpaperMuted, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
    weather.days.forEach { DayRow(it, zone, locale) }
    weather.refreshedAt?.let {
        Text(
            stringResource(R.string.weather_updated, DateTimeFormatter.ofPattern("HH:mm", locale).format(Instant.ofEpochMilli(it).atZone(zone))),
            color = colors.onWallpaperMuted,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun DayRow(day: WeatherDay, zone: ZoneId, locale: Locale) {
    val colors = LocalOndaColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Column(Modifier.weight(1f)) {
            Text(
                DateTimeFormatter.ofPattern("EEEE", locale).format(Instant.ofEpochMilli(day.date).atZone(zone)).replaceFirstChar { it.titlecase(locale) },
                style = MaterialTheme.typography.bodyLarge,
            )
            day.text?.let {
                Text(it, color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(listOfNotNull(day.high, day.low).map { degrees(it) }.joinToString(" / "), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun degrees(value: Int): String = stringResource(R.string.weather_degrees, value)
