// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.ui.theme.LocalOndaTokens
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.Tip
import com.gabrielepiccione.onda.ui.components.OndaChip
import com.gabrielepiccione.onda.ui.settings.SettingEntry
import com.gabrielepiccione.onda.ui.settings.SettingsTargets
import com.gabrielepiccione.onda.ui.theme.Motion
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors

/** Titolo, testo e voce delle impostazioni a cui porta "Mostrami", per ogni suggerimento. */
private class TipContent(val title: Int, val body: Int, val target: SettingEntry?)

private fun contentOf(tip: Tip) = when (tip) {
    Tip.FAVORITE -> TipContent(R.string.tip_favorite_title, R.string.tip_favorite_body, SettingsTargets.favorites)
    Tip.SWIPE_NOTIFICATIONS -> TipContent(R.string.tip_swipe_title, R.string.tip_swipe_body, SettingsTargets.gestures)
    Tip.ALPHABET -> TipContent(R.string.tip_alphabet_title, R.string.tip_alphabet_body, SettingsTargets.alphabet)
    Tip.PLAYER -> TipContent(R.string.tip_player_title, R.string.tip_player_body, SettingsTargets.player)
    Tip.HEADPHONES -> TipContent(R.string.tip_headphones_title, R.string.tip_headphones_body, SettingsTargets.plugPlay)
    Tip.GRAYSCALE -> TipContent(R.string.tip_grayscale_title, R.string.tip_grayscale_body, SettingsTargets.grayscale)
    Tip.PROFILES -> TipContent(R.string.tip_profiles_title, R.string.tip_profiles_body, SettingsTargets.profiles)
}

/**
 * Scheda "Lo sapevi?" in home: piccola, una al giorno, con "Mostrami" (apre l'impostazione e la fa lampeggiare),
 * "Non ora" e "Non mostrare più". Compare e sparisce con un'animazione leggera, senza muovere il resto di scatto.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TipCard(
    tip: Tip?,
    onShowMe: (SettingEntry) -> Unit,
    onNotNow: () -> Unit,
    onNever: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalOndaColors.current
    AnimatedVisibility(
        visible = tip != null,
        enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
        exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit()),
        modifier = modifier,
    ) {
        // Mentre sparisce si ricorda il contenuto dell'ultimo suggerimento
        val content = tip?.let(::contentOf) ?: return@AnimatedVisibility
        Column(
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(LocalOndaTokens.current.shapes.large)
                .background(colors.accentContainer.copy(alpha = 0.55f))
                .padding(16.dp),
        ) {
            Text(
                stringResource(R.string.tip_label),
                color = colors.onAccentContainer,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                stringResource(content.title),
                color = colors.onAccentContainer,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp).semantics { heading() },
            )
            Text(
                stringResource(content.body),
                color = colors.onAccentContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                content.target?.let { target ->
                    OndaChip(stringResource(R.string.tip_show_me), { onShowMe(target) }, primary = true, icon = Icons.Filled.AutoAwesome)
                }
                OndaChip(stringResource(R.string.tip_not_now), onNotNow)
                OndaChip(stringResource(R.string.tip_never), onNever)
            }
        }
    }
}
