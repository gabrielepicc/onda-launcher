// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors

/**
 * Chip e pulsante a pillola di Onda: 16dp di margine ai lati e 8dp sopra e sotto, alto almeno 40dp con
 * area di tocco da 48dp. Il testo resta su una riga e non viene mai tagliato: il chip si allarga
 * (in un FlowRow va a capo intero).
 */
@Composable
fun OndaChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    icon: ImageVector? = null,
) {
    val colors = LocalOndaColors.current
    val content = if (primary) colors.onAccentContainer else colors.onWallpaper
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(50))
            .background(if (primary) colors.accentContainer else colors.panelHigh)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (icon != null) {
            Icon(icon, null, tint = content, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = content, style = MaterialTheme.typography.labelLarge, softWrap = false)
    }
}
