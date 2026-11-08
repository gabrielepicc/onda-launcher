// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.ui.components.SwitchRow
import com.gabrielepiccione.onda.ui.components.GroupCard
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import com.gabrielepiccione.onda.ui.components.TonalIcon

/*
 * Componenti delle impostazioni, uguali in tutte le pagine e fedeli alle regole di Material 3:
 * - titolo di sezione: labelLarge, colore primario, 16dp dal bordo, 24dp sopra e 8dp sotto;
 * - contenitore: surfaceContainerLow, angoli da 28dp (forma large dei token);
 * - voce di elenco: titolo bodyLarge (onSurface) e testo di supporto bodyMedium (onSurfaceVariant),
 *   alta almeno 56dp con una riga e 72dp con due, icona in un cerchio tonale da 40dp, controllo in fondo;
 * - spiegazioni: bodyMedium onSurfaceVariant; etichette dei controlli (cursori, riquadri): bodyLarge.
 */

/** Titolo della voce da far lampeggiare (la cercata o quella mostrata da un suggerimento), o null. */
internal val LocalSettingsHighlight = staticCompositionLocalOf<String?> { null }

/**
 * Se [text] è la voce cercata: la porta in vista e la fa lampeggiare tre volte con il colore d'accento.
 * Altrimenti non cambia nulla.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.flashIfTarget(text: String): Modifier {
    if (LocalSettingsHighlight.current != text) return this
    val requester = remember { BringIntoViewRequester() }
    val alpha = remember { Animatable(0f) }
    val accent = LocalOndaColors.current.accent
    LaunchedEffect(Unit) {
        requester.bringIntoView()
        repeat(FLASHES) {
            alpha.animateTo(FLASH_ALPHA, tween(300))
            alpha.animateTo(0f, tween(300))
        }
    }
    return this
        .bringIntoViewRequester(requester)
        .drawBehind { drawRoundRect(accent.copy(alpha = alpha.value), cornerRadius = CornerRadius(16.dp.toPx())) }
}

private const val FLASHES = 3
private const val FLASH_ALPHA = 0.3f

/** Sezione: è la [GroupCard] comune, con il titolo che lampeggia quando è la voce cercata. */
@Composable
internal fun SettingsSection(title: String?, content: @Composable ColumnScope.() -> Unit) =
    GroupCard(title = title, titleModifier = if (title != null) Modifier.flashIfTarget(title) else Modifier, content = content)

/** Voce di elenco: titolo, supporto facoltativo, icona iniziale e controllo finale. */
@Composable
internal fun SettingsItem(
    headline: String,
    supporting: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = LocalOndaColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .flashIfTarget(headline)
            .heightIn(min = if (supporting == null) 56.dp else 72.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
    ) {
        if (icon != null) {
            TonalIcon(icon)
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(headline, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge)
            if (supporting != null) Text(supporting, color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
        }
        if (trailing != null) {
            Spacer(Modifier.width(16.dp))
            Box(contentAlignment = Alignment.Center) { trailing() }
        }
    }
}

/** Voce con interruttore: è lo [SwitchRow] comune, che lampeggia quando è la voce cercata. */
@Composable
internal fun SettingsSwitch(headline: String, supporting: String?, checked: Boolean, icon: ImageVector? = null, onChange: (Boolean) -> Unit) =
    SwitchRow(headline, supporting, checked, Modifier.flashIfTarget(headline), icon, onChange)

/** Spiegazione dentro una sezione. */
@Composable
internal fun SettingsSupporting(text: String) {
    Text(
        text,
        color = LocalOndaColors.current.onWallpaperMuted,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

/** Etichetta di un controllo che non è una voce di elenco (cursore, riquadri di scelta). */
@Composable
internal fun SettingsLabel(text: String) {
    Text(
        text,
        color = LocalOndaColors.current.onWallpaper,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.flashIfTarget(text).padding(top = 12.dp, bottom = 8.dp),
    )
}
