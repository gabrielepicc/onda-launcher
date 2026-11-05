// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.data.UsageStatsMath
import com.gabrielepiccione.onda.ui.settings.durationText
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.AppInfo
import kotlinx.coroutines.delay

/**
 * Usage Breaker: una pausa prima di aprire un'app che si vuole usare meno, o che ha superato il limite di
 * oggi. Mostra il conto alla rovescia, quante volte la si è aperta oggi e per quanto tempo.
 */
@Composable
fun UsageBreakerDialog(
    app: AppInfo,
    seconds: Int,
    opensToday: Int,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    usedTodayMs: Long? = null,
    limitMinutes: Int = 0,
) {
    var left by remember(app.key) { mutableIntStateOf(seconds) }
    LaunchedEffect(app.key) {
        while (left > 0) {
            delay(1000)
            left -= 1
        }
    }
    val progress by animateFloatAsState(if (seconds <= 0) 1f else 1f - left.toFloat() / seconds, tween(900), label = "breakerProgress")
    val overLimit = usedTodayMs != null && UsageStatsMath.overLimit(usedTodayMs, limitMinutes)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(64.dp), strokeWidth = 4.dp)
                if (left > 0) Text("$left", style = MaterialTheme.typography.titleLarge) else Icon(Icons.Filled.HourglassEmpty, null)
            }
        },
        title = { Text(stringResource(if (overLimit) R.string.limit_reached_title else R.string.breaker_title), textAlign = TextAlign.Center) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.breaker_body, app.label), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.breaker_opens, opensToday), style = MaterialTheme.typography.bodyMedium)
                if (usedTodayMs != null) {
                    Text(stringResource(R.string.breaker_used_today, durationText(usedTodayMs)), style = MaterialTheme.typography.bodyMedium)
                }
                if (limitMinutes > 0) {
                    Text(
                        stringResource(if (overLimit) R.string.wellbeing_limit_over else R.string.wellbeing_limit_badge, durationText(limitMinutes * 60_000L)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onOpen, enabled = left == 0) {
                Text(if (left > 0) stringResource(R.string.breaker_wait, left) else stringResource(R.string.breaker_open))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.breaker_cancel)) }
        },
    )
}
