// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.home

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

/** Usage Breaker: una pausa prima di aprire un'app che si vuole usare meno. */
@Composable
fun UsageBreakerDialog(
    app: AppInfo,
    seconds: Int,
    opensToday: Int,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    var left by remember(app.key) { mutableIntStateOf(seconds) }
    LaunchedEffect(app.key) {
        while (left > 0) {
            delay(1000)
            left -= 1
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.breaker_title)) },
        text = {
            Column {
                Text(stringResource(R.string.breaker_body, app.label), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.breaker_opens, opensToday), style = MaterialTheme.typography.bodyMedium)
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
