// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(ExperimentalMaterial3Api::class)

package com.gabrielepiccione.onda.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.DesignRules
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import com.gabrielepiccione.onda.ui.theme.LocalOndaTokens

/*
 * Componenti comuni dei menu e delle impostazioni (regole in docs/design-material-you.md).
 * Ordine standard dei gruppi: Identità → Azioni frequenti → Aspetto/Dimensioni → Comportamento → Avanzate → Pericolo.
 */

/**
 * Foglio dal basso: maniglia, intestazione fissa con icona e nome, corpo che scorre, alto al massimo il 90%
 * dello schermo, su superficie tonale bassa.
 */
@Composable
fun SheetScaffold(
    title: String,
    onDismiss: () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp * DesignRules.SHEET_MAX_FRACTION).dp
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = LocalOndaColors.current.onWallpaper,
    ) {
        Column(Modifier.heightIn(max = maxHeight).navigationBarsPadding()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                if (icon != null) {
                    icon()
                    Spacer(Modifier.width(16.dp))
                }
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            }
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

/** Scheda tonale di un gruppo, con il titolo in labelLarge nel colore primario. */
@Composable
fun GroupCard(
    title: String?,
    modifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                title,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = titleModifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            )
        } else {
            Spacer(Modifier.size(16.dp))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(LocalOndaTokens.current.shapes.large)
                .background(container)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            content = content,
        )
    }
}

/** Voce con interruttore: si tocca tutta la riga, che per TalkBack è un interruttore. Nome sempre positivo. */
@Composable
fun SwitchRow(
    title: String,
    supporting: String?,
    checked: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onChange: (Boolean) -> Unit,
) {
    RowBase(
        title = title,
        supporting = supporting,
        icon = icon,
        modifier = modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        trailing = { Switch(checked = checked, onCheckedChange = null) },
    )
}

/**
 * Cursore con etichetta a sinistra, valore a destra e «Ripristina» quando è cambiato: è [ValueSlider], con le tacche
 * calcolate da [step] quando i valori sono discreti.
 */
@Composable
fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    default: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float = 0f,
    onChange: (Float) -> Unit,
) = ValueSlider(label, valueText, value, default, range, DesignRules.sliderSteps(range.start, range.endInclusive, step), onChange = onChange)

/** Scelta fra opzioni: pulsanti segmentati fino a quattro, altrimenti un elenco a tutta riga (mai lunghi elenchi di radio). */
@Composable
fun <T> ChoiceRow(label: String?, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val colors = LocalOndaColors.current
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        if (label != null) {
            Text(label, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
        }
        when (DesignRules.choiceStyle(options.size)) {
            DesignRules.ChoiceStyle.SEGMENTED -> SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, (value, text) ->
                    SegmentedButton(
                        selected = value == selected,
                        onClick = { onSelect(value) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(text, softWrap = false) }
                }
            }
            DesignRules.ChoiceStyle.LIST -> options.forEach { (value, text) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(LocalOndaTokens.current.shapes.small)
                        .selectable(selected = value == selected, role = Role.RadioButton) { onSelect(value) }
                        .padding(vertical = 4.dp),
                ) {
                    RadioButton(selected = value == selected, onClick = null)
                    Spacer(Modifier.width(16.dp))
                    Text(text, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

/** Voce che porta altrove: icona in cerchio tonale, titolo su una riga, testo di supporto, freccia. */
@Composable
fun ActionRow(icon: ImageVector, title: String, supporting: String? = null, modifier: Modifier = Modifier, onClick: () -> Unit) {
    RowBase(
        title = title,
        supporting = supporting,
        icon = icon,
        singleLineTitle = true,
        modifier = modifier.clickable(onClick = onClick),
        trailing = { Icon(Icons.Filled.KeyboardArrowRight, null, tint = LocalOndaColors.current.onWallpaperMuted) },
    )
}

/** Azione pericolosa (Elimina, Disinstalla): nel colore d'errore, chiede sempre conferma. */
@Composable
fun DestructiveRow(icon: ImageVector, title: String, confirmText: String, onConfirm: () -> Unit) {
    var asking by remember { mutableStateOf(false) }
    val error = MaterialTheme.colorScheme.error
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(LocalOndaTokens.current.shapes.medium)
            .clickable { asking = true }
            .padding(vertical = 8.dp),
    ) {
        Icon(icon, null, tint = error, modifier = Modifier.padding(horizontal = 8.dp).size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, color = error, style = MaterialTheme.typography.bodyLarge)
    }
    if (asking) {
        AlertDialog(
            onDismissRequest = { asking = false },
            title = { Text(title) },
            text = { Text(confirmText) },
            confirmButton = {
                TextButton(onClick = { asking = false; onConfirm() }) { Text(title, color = error) }
            },
            dismissButton = { TextButton(onClick = { asking = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** Riga di base: icona tonale facoltativa, titolo, supporto, controllo in fondo; alta 56 dp con una riga, 72 con due. */
@Composable
private fun RowBase(
    title: String,
    supporting: String?,
    icon: ImageVector?,
    modifier: Modifier,
    singleLineTitle: Boolean = false,
    trailing: (@Composable () -> Unit)?,
) {
    val colors = LocalOndaColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (supporting == null) 56.dp else 72.dp)
            .padding(vertical = 8.dp),
    ) {
        if (icon != null) {
            TonalIcon(icon)
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            if (singleLineTitle) {
                // troncamento voluto: il titolo di una voce che porta altrove sta su una riga, il supporto sotto spiega
                Text(title, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Text(title, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge)
            }
            if (supporting != null) Text(supporting, color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
        }
        if (trailing != null) {
            Spacer(Modifier.width(16.dp))
            trailing()
        }
    }
}
