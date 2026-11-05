// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors

/*
 * Componenti delle impostazioni, uguali in tutte le pagine e fedeli alle regole di Material 3:
 * - titolo di sezione: titleSmall, colore primario, 16dp dal bordo, 24dp sopra e 8dp sotto;
 * - contenitore: surfaceContainer, angoli da 28dp (Extra Large);
 * - voce di elenco: titolo bodyLarge (onSurface) e testo di supporto bodyMedium (onSurfaceVariant),
 *   alta almeno 56dp con una riga e 72dp con due, icona iniziale da 24dp, controllo in fondo;
 * - spiegazioni: bodyMedium onSurfaceVariant; etichette dei controlli (cursori, riquadri): bodyLarge.
 */

/** Sezione: titolo e contenitore con le sue voci. */
@Composable
internal fun SettingsSection(title: String?, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalOndaColors.current
    if (title != null) {
        Text(
            title,
            color = colors.accent,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
        )
    } else {
        Spacer(Modifier.size(16.dp))
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.panel)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        content = content,
    )
}

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
            .heightIn(min = if (supporting == null) 56.dp else 72.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
    ) {
        if (icon != null) {
            Icon(icon, null, tint = colors.onWallpaperMuted, modifier = Modifier.size(24.dp))
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

/** Voce con interruttore: si tocca tutta la riga, e per TalkBack è un interruttore. */
@Composable
internal fun SettingsSwitch(headline: String, supporting: String?, checked: Boolean, icon: ImageVector? = null, onChange: (Boolean) -> Unit) {
    SettingsItem(
        headline = headline,
        supporting = supporting,
        icon = icon,
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        trailing = { Switch(checked = checked, onCheckedChange = null) },
    )
}

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
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
    )
}
