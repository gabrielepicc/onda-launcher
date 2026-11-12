// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.settings

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalClipboardManager
import com.gabrielepiccione.onda.data.ReadPath
import com.gabrielepiccione.onda.data.CalendarReading
import com.gabrielepiccione.onda.data.AllDayMode
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.CalendarDiagnostics
import com.gabrielepiccione.onda.data.CalendarInfo
import com.gabrielepiccione.onda.data.CalendarLogic
import com.gabrielepiccione.onda.data.CalendarProblem
import com.gabrielepiccione.onda.data.Settings
import com.gabrielepiccione.onda.ui.components.ActionRow
import com.gabrielepiccione.onda.ui.components.ChoiceRow
import com.gabrielepiccione.onda.ui.components.GroupCard
import com.gabrielepiccione.onda.ui.components.SwitchRow
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors

/**
 * Agenda e calendari: quanto avanti cercare l'evento sotto l'orologio, quali calendari mostrare e la diagnostica
 * (permesso, calendari trovati, eventi dei prossimi 14 giorni per calendario, motivo se non ce n'è). Nessun titolo
 * di evento: solo nomi dei calendari e conteggi.
 */
@Composable
internal fun CalendarSettings(
    settings: Settings,
    permission: Boolean,
    onEdit: ((Settings) -> Settings) -> Unit,
    loadDiagnostics: suspend () -> CalendarDiagnostics,
    onOpenSystemSettings: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    val colors = LocalOndaColors.current
    var info by remember { mutableStateOf<CalendarDiagnostics?>(null) }
    LaunchedEffect(permission) { info = loadDiagnostics() }
    val days = CalendarLogic.nextEventDays(settings.nextEventDays)

    GroupCard(stringResource(R.string.cal_group)) {
        ChoiceRow(
            stringResource(R.string.cal_next_days),
            CalendarLogic.NEXT_EVENT_CHOICES.map { it to pluralStringResource(R.plurals.cal_days_choice, it, it) },
            days,
        ) { v -> onEdit { it.copy(nextEventDays = v) } }

        // Eventi di tutto il giorno: compleanni, festività, ferie
        ChoiceRow(
            stringResource(R.string.cal_all_day_title),
            AllDayMode.entries.map { it to stringResource(allDayLabel(it)) },
            settings.allDayMode,
            descriptions = AllDayMode.entries.associateWith { stringResource(allDayDescription(it)) },
        ) { v -> onEdit { it.copy(allDayMode = v) } }

        val data = info
        Text(stringResource(R.string.cal_shown_title), color = colors.accent, style = MaterialTheme.typography.titleSmall)
        if (data == null || data.calendars.isEmpty()) {
            Text(stringResource(R.string.cal_none_found), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
        } else {
            data.calendars.forEach { c -> CalendarRow(c, c.id !in settings.calendarsExcluded) { on ->
                onEdit { it.copy(calendarsExcluded = if (on) it.calendarsExcluded - c.id else it.calendarsExcluded + c.id) }
            } }
        }

        // Diagnostica
        Text(stringResource(R.string.cal_diag_title), color = colors.accent, style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.cal_diag_permission, stringResource(if (permission) R.string.yes else R.string.no)),
            color = colors.onWallpaper, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.heightIn(min = 32.dp),
        )
        if (data != null && permission) {
            Text(
                pluralStringResource(R.plurals.cal_diag_found, data.calendars.size, data.calendars.size),
                color = colors.onWallpaper, style = MaterialTheme.typography.bodyMedium,
            )
            // Da dove sono arrivati gli eventi: righe per sorgente, percorso e ogni errore della lettura (mai inghiottiti)
            data.read?.let { read ->
                Text(stringResource(R.string.cal_diag_path, stringResource(pathLabel(read.path))), color = colors.onWallpaper, style = MaterialTheme.typography.bodyMedium)
                read.sources.forEach { src ->
                    Text(
                        stringResource(R.string.cal_diag_source, src.source.name.lowercase().replaceFirstChar { it.uppercase() }, src.rows),
                        color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall,
                    )
                    src.error?.let { e ->
                        Text(stringResource(R.string.cal_diag_error, src.source.name.lowercase().replaceFirstChar { it.uppercase() }, e), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            val problem = CalendarLogic.diagnose(permission, data.calendars, settings.calendarsExcluded)
            Text(
                problem?.let { stringResource(problemText(it)) } ?: stringResource(R.string.cal_diag_ok),
                color = if (problem == null) colors.onWallpaperMuted else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        data?.let { d ->
            val clipboard = LocalClipboardManager.current
            ActionRow(Icons.Filled.Settings, stringResource(R.string.cal_diag_copy), stringResource(R.string.cal_diag_copy_sub)) {
                clipboard.setText(AnnotatedString(CalendarReading.copyText(permission, d.calendars, settings.calendarsExcluded, d.read, settings.allDayMode)))
            }
        }
        if (!permission) ActionRow(Icons.Filled.Settings, stringResource(R.string.cal_grant), stringResource(R.string.cal_grant_sub), onClick = onRequestPermission)
        ActionRow(Icons.Filled.Settings, stringResource(R.string.cal_open_system), stringResource(R.string.cal_open_system_sub), onClick = onOpenSystemSettings)
    }
}

private fun allDayLabel(m: AllDayMode): Int = when (m) {
    AllDayMode.ALWAYS -> R.string.cal_all_day_always
    AllDayMode.ONLY_IF_NONE -> R.string.cal_all_day_if_none
    AllDayMode.HIDE -> R.string.cal_all_day_hide
}

private fun allDayDescription(m: AllDayMode): Int = when (m) {
    AllDayMode.ALWAYS -> R.string.cal_all_day_always_sub
    AllDayMode.ONLY_IF_NONE -> R.string.cal_all_day_if_none_sub
    AllDayMode.HIDE -> R.string.cal_all_day_hide_sub
}

private fun pathLabel(p: ReadPath): Int = when (p) {
    ReadPath.INSTANCES -> R.string.cal_path_instances
    ReadPath.EVENTS_FALLBACK -> R.string.cal_path_fallback
    ReadPath.EVENTS_ONLY -> R.string.cal_path_events
    ReadPath.NONE -> R.string.cal_path_none
}

private fun problemText(p: CalendarProblem): Int = when (p) {
    CalendarProblem.NO_PERMISSION -> R.string.cal_problem_permission
    CalendarProblem.NO_CALENDARS -> R.string.cal_problem_no_calendars
    CalendarProblem.NONE_SELECTED -> R.string.cal_problem_none_selected
    CalendarProblem.NONE_VISIBLE -> R.string.cal_problem_none_visible
    CalendarProblem.NOT_SYNCED -> R.string.cal_problem_not_synced
    CalendarProblem.NO_EVENTS -> R.string.cal_problem_no_events
}

@Composable
private fun CalendarRow(c: CalendarInfo, shown: Boolean, onChange: (Boolean) -> Unit) {
    val flags = listOf(
        stringResource(if (c.visible) R.string.cal_visible else R.string.cal_not_visible),
        stringResource(if (c.synced) R.string.cal_synced else R.string.cal_not_synced),
    ).joinToString(" · ")
    val supporting = c.account.takeIf { it.isNotBlank() }?.let { "$it · " }.orEmpty() +
        pluralStringResource(R.plurals.cal_events_14, c.events14, c.events14) +
        (if (c.allDay14 > 0) " (" + stringResource(R.string.cal_all_day_count, c.allDay14) + ")" else "") + " · " + flags
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        // Il colore del calendario, come lo mostra il telefono
        if (c.color != 0) Box(Modifier.size(12.dp).clip(CircleShape).background(Color(c.color)))
        SwitchRow(c.name.ifBlank { c.account }, supporting, shown, modifier = Modifier.weight(1f).padding(start = if (c.color != 0) 8.dp else 0.dp), onChange = onChange)
    }
}
