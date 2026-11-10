// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.home

import com.gabrielepiccione.onda.data.TraceMark
import com.gabrielepiccione.onda.data.TraceSnapshot
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

/**
 * Etichetta di sviluppo: onStart → primo fotogramma, poi per gli ultimi ritorni i ms dall'onNewIntent di ogni segnale
 * (— = non arrivato) e i fotogrammi di Choreographer ogni 100 ms nel primo secondo. Il ritorno più recente è in cima.
 */
@Composable
fun ReturnTimesLabel(times: List<Long>, entryOnFirstFrame: Boolean?, traces: List<TraceSnapshot>, modifier: Modifier = Modifier) {
    if (times.isEmpty() && traces.isEmpty()) return
    val none = stringResource(R.string.return_trace_none)
    fun ms(snapshot: TraceSnapshot, mark: TraceMark): String = snapshot.marks[mark]?.toString() ?: none
    val header = stringResource(R.string.return_times_label, times.reversed().joinToString(" · ")) + when (entryOnFirstFrame) {
        true -> " · " + stringResource(R.string.return_entry_ok)
        false -> " · " + stringResource(R.string.return_entry_late)
        null -> ""
    }
    val lines = traces.reversed().map { t ->
        stringResource(
            R.string.return_trace_line,
            ms(t, TraceMark.TOP_RESUMED), ms(t, TraceMark.RESUME), ms(t, TraceMark.FOCUS), ms(t, TraceMark.ENTER_COMPLETE),
            ms(t, TraceMark.FIRST_FRAME), ms(t, TraceMark.SPRING_START), t.framesPer100ms.joinToString(" "),
        )
    }
    Text(
        (listOf(header) + lines).joinToString("\n"),
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
