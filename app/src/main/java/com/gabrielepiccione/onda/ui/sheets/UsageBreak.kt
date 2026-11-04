// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.sheets

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import com.gabrielepiccione.onda.ui.theme.blockTouches
import kotlinx.coroutines.delay

/**
 * Pausa consapevole prima di aprire un'app scelta dall'utente: un cerchio che "respira",
 * quante volte l'app è stata aperta oggi (ed eventualmente da quanti minuti è in uso),
 * e il pulsante per aprirla che si attiva solo al termine del conto alla rovescia.
 */
@Composable
fun UsageBreakScreen(
    app: AppInfo,
    seconds: Int,
    opensToday: Int,
    dailyLimit: Int,
    minutesToday: Int?,
    onOpen: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LocalOndaColors.current
    val overLimit = dailyLimit in 1..opensToday
    // Oltre il limite giornaliero l'attesa raddoppia: si può comunque aprire, ma con più calma
    val wait = (if (overLimit) seconds * 2 else seconds).coerceAtLeast(1)
    var remaining by remember(app.key) { mutableIntStateOf(wait) }
    LaunchedEffect(app.key) {
        while (remaining > 0) {
            delay(1000)
            remaining--
        }
    }
    val breath = rememberInfiniteTransition(label = "breath")
    val scale by breath.animateFloat(
        initialValue = 0.62f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathScale",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .blockTouches()
            .systemBarsPadding()
            .padding(24.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                Box(
                    Modifier
                        .size(220.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(CircleShape)
                        .background(colors.accentContainer)
                )
                Image(app.icon, contentDescription = app.label, modifier = Modifier.size(64.dp))
            }
            Text(
                stringResource(if (scale > 0.81f) R.string.break_breathe_out else R.string.break_breathe_in),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onWallpaper,
            )
            Text(
                pluralStringResource(R.plurals.break_opens_today, opensToday, opensToday),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onWallpaperMuted,
                textAlign = TextAlign.Center,
            )
            if (minutesToday != null) {
                Text(
                    pluralStringResource(R.plurals.break_minutes_today, minutesToday, minutesToday),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onWallpaperMuted,
                )
            }
            if (overLimit) {
                Text(
                    stringResource(R.string.break_limit_reached, dailyLimit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.accent,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.height(48.dp)) { Text(stringResource(R.string.break_not_now)) }
                Button(onClick = onOpen, enabled = remaining == 0, modifier = Modifier.height(48.dp)) {
                    Text(
                        if (remaining > 0) stringResource(R.string.break_open_in, remaining)
                        else stringResource(R.string.break_open, app.label)
                    )
                }
            }
        }
    }
}
