// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Icona in un cerchio tonale (secondaryContainer / onSecondaryContainer), come nelle liste di Material 3.
 * Componente comune: elenchi delle impostazioni, pannello della home, selettore delle azioni, automazioni.
 */
@Composable
fun TonalIcon(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 40.dp, contentDescription: String? = null) {
    val scheme = MaterialTheme.colorScheme
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(size).clip(CircleShape).background(scheme.secondaryContainer)) {
        Icon(icon, contentDescription, tint = scheme.onSecondaryContainer, modifier = Modifier.size(size / 2))
    }
}
