// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(ExperimentalMaterial3Api::class)

package com.gabrielepiccione.onda.ui.sheets

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.data.AppPopup
import com.gabrielepiccione.onda.data.AppPopups
import com.gabrielepiccione.onda.ui.components.AppIcon
import com.gabrielepiccione.onda.ui.components.ConfirmDeleteWidgetDialog
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors

/** Azioni del foglio che personalizza il popup di un'app. */
interface AppPopupEditActions {
    fun addRelated(key: String)
    fun removeRelated(key: String)
    fun addWidget()
    fun removeWidget(id: Int)
    fun toggleShortcut(id: String)
}

/** Sceglie cosa mostrare nel popup dell'app: altre app, widget e quali scorciatoie. */
@Composable
fun AppPopupEditSheet(
    app: AppInfo,
    popup: AppPopup,
    widgetIds: List<Int>,
    canAddWidget: Boolean,
    widgetLabel: (Int) -> String?,
    allApps: List<AppInfo>,
    appOf: (String) -> AppInfo?,
    shortcuts: List<ShortcutInfo>,
    themedIcons: Boolean,
    actions: AppPopupEditActions,
    onDismiss: () -> Unit,
) {
    val colors = LocalOndaColors.current
    var picking by remember { mutableStateOf(false) }
    var widgetToRemove by remember { mutableStateOf<Int?>(null) }
    val related = popup.relatedApps.mapNotNull(appOf)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.panel,
        contentColor = colors.onWallpaper,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(stringResource(R.string.popup_edit_title, app.label), style = MaterialTheme.typography.headlineSmall)

            EditTitle(R.string.popup_related)
            related.forEach { other ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    AppIcon(other, 32.dp, themedIcons)
                    Spacer(Modifier.width(12.dp))
                    Text(other.label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    TextButton(onClick = { actions.removeRelated(other.key) }) { Text(stringResource(R.string.remove)) }
                }
            }
            if (popup.relatedApps.size < AppPopups.MAX_RELATED) {
                TextButton(onClick = { picking = true }) { Text(stringResource(R.string.popup_related_add)) }
            }

            EditTitle(R.string.section_widgets)
            widgetIds.forEach { id ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        widgetLabel(id) ?: stringResource(R.string.widget_number, id),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { widgetToRemove = id }) { Text(stringResource(R.string.remove)) }
                }
            }
            if (canAddWidget) {
                TextButton(onClick = actions::addWidget) { Text(stringResource(R.string.add_widget)) }
            }

            if (shortcuts.isNotEmpty()) {
                EditTitle(R.string.popup_shortcuts)
                shortcuts.forEach { sc ->
                    val visible = sc.id !in popup.hiddenShortcuts
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { actions.toggleShortcut(sc.id) },
                    ) {
                        Checkbox(checked = visible, onCheckedChange = { actions.toggleShortcut(sc.id) })
                        Text((sc.shortLabel ?: sc.longLabel ?: "").toString(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }

    widgetToRemove?.let { id ->
        ConfirmDeleteWidgetDialog(
            onConfirm = {
                actions.removeWidget(id)
                widgetToRemove = null
            },
            onDismiss = { widgetToRemove = null },
        )
    }

    if (picking) {
        val choices = remember(allApps, popup.relatedApps) { allApps.filter { it.key != app.key && it.key !in popup.relatedApps } }
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text(stringResource(R.string.popup_related_add)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(choices, key = { it.key }) { other ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    actions.addRelated(other.key)
                                    picking = false
                                }
                                .padding(vertical = 8.dp),
                        ) {
                            AppIcon(other, 32.dp, themedIcons)
                            Spacer(Modifier.width(12.dp))
                            Text(other.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.close)) } },
        )
    }
}

@Composable
private fun EditTitle(res: Int) {
    Text(
        stringResource(res),
        color = LocalOndaColors.current.accent,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}
