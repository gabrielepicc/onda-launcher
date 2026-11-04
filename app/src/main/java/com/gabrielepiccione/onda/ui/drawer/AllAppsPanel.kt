// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.drawer

import com.gabrielepiccione.onda.ui.theme.*
import com.gabrielepiccione.onda.ui.components.*
import com.gabrielepiccione.onda.ui.home.*
import com.gabrielepiccione.onda.ui.search.*
import com.gabrielepiccione.onda.ui.settings.*
import com.gabrielepiccione.onda.ui.sheets.*

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.data.NotificationItem

data class AppSection(val letter: Char, val apps: List<AppInfo>, val firstIndex: Int)

/** Raggruppa le app (già ordinate) per lettera e calcola l'indice d'inizio di ogni sezione. */
/**
 * Raggruppa le app per lettera; dentro ogni lettera le più usate vengono prima,
 * a parità di utilizzo resta l'ordine alfabetico.
 */
fun buildSections(apps: List<AppInfo>, launchCounts: Map<String, Int> = emptyMap()): List<AppSection> {
    val grouped = LinkedHashMap<Char, MutableList<AppInfo>>()
    apps.filter { it.section == '#' }.takeIf { it.isNotEmpty() }?.let { grouped['#'] = it.toMutableList() }
    apps.filter { it.section != '#' }.forEach { grouped.getOrPut(it.section) { mutableListOf() }.add(it) }
    var index = 0
    return grouped.map { (letter, list) ->
        // sortedByDescending è stabile: a parità di conteggio resta l'ordine alfabetico
        val ordered = list.sortedByDescending { launchCounts[it.key] ?: 0 }
        AppSection(letter, ordered, index).also { index += 1 + ordered.size }
    }
}

@Composable
fun AllAppsPanel(
    sections: List<AppSection>,
    listState: LazyListState,
    activeLetter: Char?,
    notifications: Map<String, List<NotificationItem>>,
    showIcons: Boolean,
    themedIcons: Boolean,
    onClick: (AppInfo) -> Unit,
    onLongClick: (AppInfo) -> Unit,
    contentPadding: PaddingValues,
    onOpenPanel: (AppInfo) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalOndaColors.current
    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = modifier.fillMaxSize(),
    ) {
        sections.forEach { section ->
            item(key = "h_${section.letter}") {
                val color by animateColorAsState(
                    if (section.letter == activeLetter) colors.accent else colors.onWallpaperMuted,
                    label = "header",
                )
                Text(
                    text = section.letter.toString(),
                    color = color,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
                )
            }
            section.apps.forEach { app ->
                item(key = app.key) {
                    AppListRow(
                        app = app,
                        hasNotification = notifications[app.packageName].orEmpty().isNotEmpty(),
                        notification = notifications[app.packageName]?.firstOrNull(),
                        notificationCount = notifications[app.packageName]?.size ?: 0,
                        onOpenPanel = { onOpenPanel(app) },
                        showIcon = showIcons,
                        themedIcons = themedIcons,
                        onClick = { onClick(app) },
                        onLongClick = { onLongClick(app) },
                    )
                }
            }
        }
    }
}
