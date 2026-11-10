// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.home

import com.gabrielepiccione.onda.data.ReturnPolicy
import com.gabrielepiccione.onda.LauncherViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R

/** Etichetta di sviluppo: i millisecondi da onStart al primo fotogramma, gli ultimi valori (il più recente per primo). */
@Composable
fun ReturnTimesLabel(times: List<Long>, entryOnFirstFrame: Boolean?, entryStart: LauncherViewModel.EntryStart?, modifier: Modifier = Modifier) {
    if (times.isEmpty()) return
    Text(
        stringResource(R.string.return_times_label, times.reversed().joinToString(" · ")) + when (entryOnFirstFrame) {
            true -> " · " + stringResource(R.string.return_entry_ok)
            false -> " · " + stringResource(R.string.return_entry_late)
            null -> ""
        } + (entryStart?.let { start ->
            val signal = stringResource(
                when (start.signal) {
                    ReturnPolicy.EntrySignal.ENTER_COMPLETE -> R.string.return_signal_enter
                    ReturnPolicy.EntrySignal.FOCUS -> R.string.return_signal_focus
                    ReturnPolicy.EntrySignal.TIMEOUT -> R.string.return_signal_timeout
                },
            )
            " · " + stringResource(R.string.return_spring_start, signal, start.delayMs)
        } ?: ""),
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
