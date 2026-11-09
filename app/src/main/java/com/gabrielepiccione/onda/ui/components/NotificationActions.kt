// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.components

import android.app.Notification
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.NotificationItem

/**
 * Le azioni di una notifica (Segna come letto, Silenzia, Rispondi…) su UNA riga, mai a capo né tagliate:
 * se non ci stanno tutte la riga scorre di lato. Sono pulsanti tonali di Material 3 a pillola, quindi si
 * distinguono dalla scheda; «Rispondi» usa il contenitore primario per risaltare.
 */
@Composable
fun NotificationActionRow(
    item: NotificationItem,
    onAction: (Notification.Action) -> Unit,
    onReply: () -> Unit,
    modifier: Modifier = Modifier,
    /** Nasconde «Rispondi» (per esempio mentre il campo di risposta è già aperto). */
    showReply: Boolean = true,
) {
    if (item.actions.isEmpty() && !(item.canReply && showReply)) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.horizontalScroll(rememberScrollState()),
    ) {
        item.actions.forEach { a -> TonalActionButton(a.title?.toString().orEmpty(), primary = false) { onAction(a) } }
        if (item.canReply && showReply) TonalActionButton(stringResource(R.string.notif_reply), primary = true, onClick = onReply)
    }
}

/** Pulsante tonale a pillola: contenitore secondario (o primario), testo su una riga, area di tocco da 48 dp. */
@Composable
private fun TonalActionButton(label: String, primary: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(50))
            .background(if (primary) scheme.primaryContainer else scheme.secondaryContainer)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = if (primary) scheme.onPrimaryContainer else scheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelLarge,
            softWrap = false,
        )
    }
}
