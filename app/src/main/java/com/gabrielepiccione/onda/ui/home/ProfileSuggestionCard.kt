// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.ui.theme.LocalOndaTokens
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.ProfileSuggestion
import com.gabrielepiccione.onda.ui.components.OndaChip
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import com.gabrielepiccione.onda.ui.theme.Motion

/**
 * Scheda "Ho notato che…": propone un profilo ricavato dall'uso. Mai un profilo si attiva da solo:
 * "Crea" lo aggiunge, "Modifica" lo apre per cambiarlo, "Ignora" lo mette a tacere per 30 giorni.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileSuggestionCard(
    suggestion: ProfileSuggestion?,
    appLabels: (List<String>) -> String,
    onCreate: () -> Unit,
    onEdit: () -> Unit,
    onIgnore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalOndaColors.current
    AnimatedVisibility(
        visible = suggestion != null,
        enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
        exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit()),
        modifier = modifier,
    ) {
        val s = suggestion ?: return@AnimatedVisibility
        Column(
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(LocalOndaTokens.current.shapes.large)
                .background(colors.accentContainer.copy(alpha = 0.55f))
                .padding(16.dp),
        ) {
            Text(stringResource(R.string.suggest_card_label), color = colors.onAccentContainer, style = MaterialTheme.typography.labelMedium)
            Text(
                stringResource(R.string.suggest_card_title, s.name),
                color = colors.onAccentContainer,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp).semantics { heading() },
            )
            Text(
                stringResource(R.string.suggest_card_body, appLabels(s.apps)),
                color = colors.onAccentContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OndaChip(stringResource(R.string.suggest_create), onCreate, primary = true, icon = Icons.Filled.AutoAwesome)
                OndaChip(stringResource(R.string.suggest_edit), onEdit)
                OndaChip(stringResource(R.string.suggest_ignore), onIgnore)
            }
        }
    }
}
