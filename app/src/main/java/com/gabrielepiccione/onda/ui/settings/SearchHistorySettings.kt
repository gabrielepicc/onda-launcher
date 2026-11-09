// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.SearchRanking
import com.gabrielepiccione.onda.data.Settings
import com.gabrielepiccione.onda.ui.components.DestructiveRow
import com.gabrielepiccione.onda.ui.components.SliderRow
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors

/** Impostazioni → Ricerca: quante app suggerire a ricerca vuota e cancellazione della cronologia. */
@Composable
internal fun SearchHistorySettings(settings: Settings, onEdit: ((Settings) -> Settings) -> Unit, onClear: () -> Unit) {
    SettingsSection(stringResource(R.string.search_suggestions_title)) {
        val n = settings.searchSuggestions
        SliderRow(
            stringResource(R.string.search_suggestions),
            if (n == 0) stringResource(R.string.search_suggestions_none) else n.toString(),
            n.toFloat(),
            SearchRanking.DEFAULT_SHOWN.toFloat(),
            0f..SearchRanking.MAX_SHOWN.toFloat(),
            step = 1f,
        ) { v -> onEdit { it.copy(searchSuggestions = v.toInt()) } }
        Text(stringResource(R.string.search_suggestions_sub), color = LocalOndaColors.current.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
        DestructiveRow(Icons.Filled.DeleteSweep, stringResource(R.string.search_history_clear), stringResource(R.string.search_history_clear_confirm), onClear)
    }
}
