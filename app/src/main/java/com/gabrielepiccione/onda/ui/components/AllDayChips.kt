// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.CalendarEvent
import com.gabrielepiccione.onda.data.DayLabel
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors

/** Il testo di un evento di tutto il giorno: il titolo, con «giorno N di M» se dura più giorni. */
@Composable
fun allDayText(label: DayLabel): String =
    if (label.multiDay) stringResource(R.string.all_day_day_n, label.event.title, label.dayIndex, label.dayCount) else label.event.title

/**
 * Gli eventi di tutto il giorno come etichette colorate (colore del calendario). [max] limita quante se ne vedono,
 * le altre diventano «+N»; il tocco su un'etichetta apre l'evento, quello su «+N» chiama [onMore].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AllDayChips(
    labels: List<DayLabel>,
    onEvent: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier,
    max: Int = Int.MAX_VALUE,
    onMore: () -> Unit = {},
) {
    if (labels.isEmpty()) return
    val shown = labels.take(max)
    val more = (labels.size - shown.size).coerceAtLeast(0)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp), modifier = modifier) {
        shown.forEach { l -> Chip(allDayText(l), l.event.color, { onEvent(l.event) }) }
        if (more > 0) Chip(stringResource(R.string.all_day_more, more), 0, onMore)
    }
}

@Composable
private fun Chip(text: String, calendarColor: Int, onClick: () -> Unit) {
    val colors = LocalOndaColors.current
    val dot = if (calendarColor != 0) Color(calendarColor) else colors.accent
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(dot.copy(alpha = 0.28f))
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp),
    ) {
        Spacer(Modifier.size(8.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(8.dp))
        // troncamento voluto: il titolo dell'evento sta su una riga, nei chip del calendario
        Text(text, color = colors.onWallpaper, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
