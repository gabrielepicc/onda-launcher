// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import kotlin.math.abs
import kotlin.math.roundToInt

/** Slider con il valore attuale ben visibile e il ritorno al predefinito quando è stato cambiato. */
@Composable
fun ValueSlider(
    label: String,
    valueText: String,
    value: Float,
    default: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    accentLabel: Boolean = false,
    onChange: (Float) -> Unit,
) {
    val colors = LocalOndaColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Text(
            label,
            color = if (accentLabel) colors.accent else colors.onWallpaper,
            style = if (accentLabel) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(valueText, color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
        if (abs(value - default) > 0.001f) {
            TextButton(onClick = { onChange(default) }) { Text(stringResource(R.string.slider_reset)) }
        }
    }
    Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
}

/** Valore in percentuale (1.0 = 100%). */
@Composable
fun percentText(value: Float): String = stringResource(R.string.value_percent, (value * 100).roundToInt())
