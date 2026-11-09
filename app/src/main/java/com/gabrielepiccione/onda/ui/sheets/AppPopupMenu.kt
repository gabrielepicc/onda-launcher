// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.sheets

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import com.gabrielepiccione.onda.ui.theme.LocalOndaTokens

/**
 * Il ⋮ dell'intestazione del popup: un menu M3 ancorato al pulsante e allineato a destra.
 * Il menu si apre sotto il pulsante (o sopra se manca spazio) e la sua larghezza segue la voce più lunga.
 */
@Composable
internal fun AppPopupMenuButton(
    appLabel: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onInfo: () -> Unit,
    onEditPopup: () -> Unit,
    onMoreOptions: () -> Unit,
    onUninstall: () -> Unit,
    onStopSuggesting: (() -> Unit)?,
) {
    var open by remember { mutableStateOf(false) }
    var confirmUninstall by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options), tint = LocalOndaColors.current.onWallpaper)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            // Appena sotto il pulsante, così l'intestazione resta visibile
            offset = DpOffset(0.dp, 4.dp),
            shape = LocalOndaTokens.current.shapes.medium,
            containerColor = scheme.surfaceContainerHigh,
        ) {
            Entry(if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder, stringResource(if (isFavorite) R.string.favorite_remove else R.string.favorite_add)) {
                open = false; onToggleFavorite()
            }
            Entry(Icons.Filled.Info, stringResource(R.string.app_info)) { open = false; onInfo() }
            Entry(Icons.Filled.Edit, stringResource(R.string.popup_edit)) { open = false; onEditPopup() }
            onStopSuggesting?.let { stop -> Entry(Icons.Filled.VisibilityOff, stringResource(R.string.suggested_stop)) { open = false; stop() } }
            Entry(Icons.Filled.MoreHoriz, stringResource(R.string.more_options_ellipsis)) { open = false; onMoreOptions() }
            HorizontalDivider()
            Entry(Icons.Filled.Delete, stringResource(R.string.uninstall), scheme.error) { open = false; confirmUninstall = true }
        }
    }
    if (confirmUninstall) {
        AlertDialog(
            onDismissRequest = { confirmUninstall = false },
            text = { Text(stringResource(R.string.opt_uninstall_confirm, appLabel)) },
            confirmButton = {
                TextButton(onClick = { confirmUninstall = false; onUninstall() }) { Text(stringResource(R.string.uninstall), color = scheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmUninstall = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun Entry(icon: ImageVector, text: String, tint: Color = Color.Unspecified, onClick: () -> Unit) {
    val color = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint
    // Voci da 48 dp (minimo di DropdownMenuItem), testo intero su una o più righe
    DropdownMenuItem(
        text = { Text(text, color = color) },
        leadingIcon = { Icon(icon, null, tint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else tint) },
        onClick = onClick,
    )
}
