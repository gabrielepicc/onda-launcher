// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.gabrielepiccione.onda.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.NotificationDigest
import com.gabrielepiccione.onda.data.NotificationItem
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors

/** Blocco della home: quante notifiche ci sono e di quali app. */
@Composable
fun NotificationSummaryCard(
    digest: NotificationDigest,
    appLabel: (String) -> String,
    highlighted: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalOndaColors.current
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .fillMaxWidth()
            .then(if (highlighted) Modifier.border(2.dp, colors.accent, shape) else Modifier)
            .clip(shape)
            .background(colors.panel)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text(
            stringResource(R.string.summary_count, digest.total, digest.packages.size),
            color = colors.onWallpaper,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            digest.packages.joinToString(", ", transform = appLabel),
            color = colors.onWallpaperMuted,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Tutte le notifiche raggruppate per app, con l'eliminazione in un colpo solo. */
@Composable
fun NotificationSummarySheet(
    notifications: Map<String, List<NotificationItem>>,
    appLabel: (String) -> String,
    onNotification: (NotificationItem) -> Unit,
    onDismissAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalOndaColors.current
    val digest = NotificationDigest.of(notifications)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.panel,
        contentColor = colors.onWallpaper,
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 12.dp)) {
            Text(stringResource(R.string.summary_title), style = MaterialTheme.typography.headlineSmall)
            if (digest.total == 0) {
                Text(
                    stringResource(R.string.summary_empty),
                    color = colors.onWallpaperMuted,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                digest.packages.forEach { pkg ->
                    item(key = "app_$pkg") {
                        Text(
                            appLabel(pkg),
                            color = colors.accent,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
                        )
                    }
                    items(notifications[pkg].orEmpty(), key = { it.key }) { n ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onNotification(n) }
                                .padding(vertical = 8.dp),
                        ) {
                            if (n.title.isNotBlank()) Text(n.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (n.text.isNotBlank()) {
                                Text(n.text, color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            if (digest.total > 0) {
                TextButton(onClick = onDismissAll, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.summary_clear))
                }
            }
        }
    }
}
