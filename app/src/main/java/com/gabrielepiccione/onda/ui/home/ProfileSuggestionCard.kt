// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.data.DayKind
import com.gabrielepiccione.onda.data.DaySlot
import com.gabrielepiccione.onda.data.ProfileSuggestion
import com.gabrielepiccione.onda.data.SuggestedSignal
import com.gabrielepiccione.onda.ui.components.AppIcon
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import com.gabrielepiccione.onda.ui.theme.LocalOndaTokens
import com.gabrielepiccione.onda.ui.theme.Motion

/**
 * Scheda "Ho notato che…": propone un profilo ricavato dall'uso. Mai un profilo si attiva da solo:
 * "Crea" lo aggiunge, "Modifica" lo apre per cambiarlo, "Ignora" lo mette a tacere per 30 giorni.
 * La stessa scheda vale in home (una alla volta) e nelle impostazioni (l'elenco completo).
 * [apps] sono le app del suggerimento già risolte (per icone e nomi); con [suggestion] nullo la scheda esce animata.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileSuggestionCard(
    suggestion: ProfileSuggestion?,
    apps: List<AppInfo>,
    themedIcons: Boolean,
    onCreate: () -> Unit,
    onEdit: () -> Unit,
    onIgnore: () -> Unit,
    modifier: Modifier = Modifier,
    /** Margine laterale: 16 dp in home, 0 dentro una sezione delle impostazioni. */
    horizontalInset: Dp = 16.dp,
) {
    val colors = LocalOndaColors.current
    val scheme = MaterialTheme.colorScheme
    AnimatedVisibility(
        visible = suggestion != null,
        enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
        exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit()),
        modifier = modifier,
    ) {
        val s = suggestion ?: return@AnimatedVisibility
        Column(
            Modifier
                .padding(horizontal = horizontalInset, vertical = 8.dp)
                .clip(LocalOndaTokens.current.shapes.large)
                .background(scheme.surfaceContainer)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(stringResource(R.string.suggest_card_label), color = scheme.primary, style = MaterialTheme.typography.labelLarge)
            Text(
                s.name,
                color = colors.onWallpaper,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(suggestionCondition(s.days, s.slot, s.signal), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
            // Le app, in fila con le loro icone
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                apps.forEach { app ->
                    AppIcon(app, 36.dp, themedIcons, Modifier.size(36.dp).semantics { contentDescription = app.label })
                }
            }
            Text(apps.joinToString(", ") { it.label }, color = colors.onWallpaper, style = MaterialTheme.typography.bodyMedium)
            if (s.togetherDays > 0 && s.historyDays > 0) {
                Text(
                    stringResource(R.string.suggest_why, s.togetherDays, s.historyDays),
                    color = colors.onWallpaperMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = onCreate, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.suggest_create), softWrap = false) }
                FilledTonalButton(onClick = onEdit, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.suggest_edit), softWrap = false) }
                TextButton(onClick = onIgnore, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.suggest_ignore), softWrap = false) }
            }
        }
    }
}

/** Fascia o condizione leggibile: «Sera · feriali», «Con le cuffie». */
@Composable
internal fun suggestionCondition(days: DayKind?, slot: DaySlot?, signal: SuggestedSignal?): String {
    if (signal != null) {
        return stringResource(
            when (signal) {
                SuggestedSignal.HEADPHONES -> R.string.suggest_signal_headphones
                SuggestedSignal.CHARGING -> R.string.suggest_signal_charging
                SuggestedSignal.CAR -> R.string.suggest_signal_car
                SuggestedSignal.WIFI -> R.string.suggest_signal_wifi
            },
        )
    }
    val slotText = when (slot) {
        DaySlot.MORNING -> stringResource(R.string.suggest_slot_morning)
        DaySlot.AFTERNOON -> stringResource(R.string.suggest_slot_afternoon)
        DaySlot.EVENING -> stringResource(R.string.suggest_slot_evening)
        DaySlot.NIGHT -> stringResource(R.string.suggest_slot_night)
        null -> ""
    }
    val daysText = when (days) {
        DayKind.WEEKDAYS -> stringResource(R.string.suggest_days_weekdays)
        DayKind.WEEKEND -> stringResource(R.string.suggest_days_weekend)
        null -> ""
    }
    return listOf(slotText, daysText).filter { it.isNotBlank() }.joinToString(" · ")
}
