// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.gabrielepiccione.onda.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.WidgetCatalog
import com.gabrielepiccione.onda.data.WidgetEntry
import com.gabrielepiccione.onda.data.WidgetManager
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import com.gabrielepiccione.onda.ui.theme.LocalOndaTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Selettore dei widget: ricerca anche per nome dell'app, «Usati di recente» e «Suggeriti» in alto, poi un gruppo
 * per app con intestazione fissa e una fila orizzontale di anteprime (nome e misura, es. «4×2»), più la barra
 * dell'alfabeto a destra. Le anteprime si caricano solo quando compaiono, con un segnaposto e una cache.
 * Tocco = aggiungi; pressione lunga = dettagli.
 */
@Composable
fun WidgetPickerSheet(
    widgets: WidgetManager,
    packageFilter: String?,
    onPick: (WidgetManager.Provider) -> Unit,
    onDismiss: () -> Unit,
    recentKeys: List<String> = emptyList(),
    favoritePackages: List<String> = emptyList(),
) {
    val colors = LocalOndaColors.current
    val density = LocalDensity.current
    var providers by remember(packageFilter) { mutableStateOf(emptyList<WidgetManager.Provider>()) }
    LaunchedEffect(packageFilter) { providers = withContext(Dispatchers.IO) { widgets.providers(packageFilter) } }
    val byKey = remember(providers) { providers.associateBy { widgetKey(it) } }
    val entries = remember(providers) {
        providers.map { p ->
            with(density) {
                WidgetEntry(widgetKey(p), p.label, p.appLabel, p.packageName, p.info.minWidth.toDp().value.toInt(), p.info.minHeight.toDp().value.toInt())
            }
        }
    }
    var query by remember(packageFilter) { mutableStateOf("") }
    val filtered = remember(entries, query) { WidgetCatalog.filter(entries, query) }
    val groups = remember(filtered) { WidgetCatalog.groups(filtered) }
    val showTop = packageFilter == null && query.isBlank()
    val recent = if (showTop) WidgetCatalog.recent(entries, recentKeys) else emptyList()
    val suggested = if (showTop) WidgetCatalog.suggested(entries, favoritePackages, recent.map { it.key }.toSet()) else emptyList()
    val previews = remember { mutableStateMapOf<String, ImageBitmap?>() }
    var details by remember { mutableStateOf<WidgetEntry?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    fun pick(e: WidgetEntry) { byKey[e.key]?.let(onPick) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = colors.onWallpaper,
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Text(stringResource(R.string.add_widget), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            if (packageFilter == null && providers.isNotEmpty()) {
                OndaTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    pill = true,
                    placeholder = { Text(stringResource(R.string.widget_search)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                )
            }
            when {
                providers.isEmpty() -> Text(
                    stringResource(if (packageFilter != null) R.string.app_no_widgets else R.string.loading),
                    color = colors.onWallpaperMuted,
                    modifier = Modifier.padding(24.dp),
                )
                groups.isEmpty() -> Text(stringResource(R.string.widget_no_results_query, query), color = colors.onWallpaperMuted, modifier = Modifier.padding(24.dp))
            }
            // Righe prima dei gruppi (per saltare alla lettera giusta): due per ogni sezione in alto
            val topItems = (if (recent.isNotEmpty()) 2 else 0) + (if (suggested.isNotEmpty()) 2 else 0)
            Box(Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
                LazyColumn(state = listState, contentPadding = PaddingValues(end = 40.dp, bottom = 16.dp)) {
                    if (recent.isNotEmpty()) {
                        item(key = "recent_label") { PickerLabel(stringResource(R.string.widget_recent)) }
                        item(key = "recent_row") { TileRow(recent, widgets, byKey, previews, ::pick) { details = it } }
                    }
                    if (suggested.isNotEmpty()) {
                        item(key = "suggested_label") { PickerLabel(stringResource(R.string.widget_suggested)) }
                        item(key = "suggested_row") { TileRow(suggested, widgets, byKey, previews, ::pick) { details = it } }
                    }
                    groups.forEach { g ->
                        stickyHeader(key = "h_" + g.packageName) { GroupHeader(g.packageName, g.appLabel, g.widgets.size) }
                        item(key = "r_" + g.packageName) { TileRow(g.widgets, widgets, byKey, previews, ::pick) { details = it } }
                    }
                }
                // Barra dell'alfabeto per saltare tra le app
                if (packageFilter == null && groups.size > 1) {
                    AlphabetWaveScroller(
                        letters = WidgetCatalog.letters(groups),
                        leftSide = false,
                        modifier = Modifier.align(Alignment.CenterEnd),
                        onLetter = { letter ->
                            val i = WidgetCatalog.indexOfLetter(groups, letter)
                            if (i >= 0) scope.launch { listState.scrollToItem(topItems + i * 2) }
                        },
                    )
                }
            }
        }
    }

    details?.let { e ->
        AlertDialog(
            onDismissRequest = { details = null },
            title = { Text(e.label) },
            text = { Text(stringResource(R.string.widget_details, e.appLabel, WidgetCatalog.sizeText(e), e.minWidthDp, e.minHeightDp)) },
            confirmButton = { TextButton(onClick = { details = null; pick(e) }) { Text(stringResource(R.string.add_widget)) } },
            dismissButton = { TextButton(onClick = { details = null }) { Text(stringResource(R.string.close)) } },
        )
    }
}

/** Chiave stabile di un provider (componente e profilo), usata anche per «Usati di recente». */
fun widgetKey(p: WidgetManager.Provider): String = p.info.provider.flattenToString() + "#" + p.info.profile.hashCode()

@Composable
private fun PickerLabel(text: String) {
    Text(text, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 4.dp))
}

/** Intestazione fissa del gruppo: icona e nome dell'app, numero di widget. */
@Composable
private fun GroupHeader(packageName: String, appLabel: String, count: Int) {
    val context = LocalContext.current
    var icon by remember(packageName) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(packageName) {
        icon = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap() }.getOrNull()
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 24.dp, vertical = 6.dp),
    ) {
        val image = icon
        if (image != null) Image(image, null, Modifier.size(28.dp)) else TonalIcon(Icons.Filled.Widgets, size = 28.dp)
        Spacer(Modifier.width(12.dp))
        Text(appLabel, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(count.toString(), color = LocalOndaColors.current.onWallpaperMuted, style = MaterialTheme.typography.labelLarge)
    }
}

/** Fila orizzontale di anteprime. */
@Composable
private fun TileRow(
    list: List<WidgetEntry>,
    widgets: WidgetManager,
    byKey: Map<String, WidgetManager.Provider>,
    previews: MutableMap<String, ImageBitmap?>,
    onPick: (WidgetEntry) -> Unit,
    onDetails: (WidgetEntry) -> Unit,
) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(list, key = { it.key }) { e -> WidgetTile(e, widgets, byKey[e.key], previews, onPick, onDetails) }
    }
}

/** Una tessera: anteprima caricata solo quando compare (con segnaposto e cache), nome e misura. */
@Composable
private fun WidgetTile(
    e: WidgetEntry,
    widgets: WidgetManager,
    provider: WidgetManager.Provider?,
    previews: MutableMap<String, ImageBitmap?>,
    onPick: (WidgetEntry) -> Unit,
    onDetails: (WidgetEntry) -> Unit,
) {
    val colors = LocalOndaColors.current
    LaunchedEffect(e.key) {
        if (provider != null && e.key !in previews) {
            previews[e.key] = withContext(Dispatchers.IO) {
                runCatching {
                    val d = widgets.previewOf(provider) ?: return@runCatching null
                    val w = d.intrinsicWidth.takeIf { it > 0 } ?: 300
                    val h = d.intrinsicHeight.takeIf { it > 0 } ?: 150
                    val scale = (PREVIEW_PX / w).coerceAtMost(1f)
                    d.toBitmap((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1)).asImageBitmap()
                }.getOrNull()
            }
        }
    }
    val size = WidgetCatalog.sizeText(e)
    Column(
        Modifier
            .width(TILE_WIDTH.dp)
            .clip(LocalOndaTokens.current.shapes.medium)
            .combinedClickable(onClick = { onPick(e) }, onLongClick = { onDetails(e) })
            .semantics { contentDescription = e.label + ", " + size }
            .padding(6.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .clip(LocalOndaTokens.current.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            val image = previews[e.key]
            if (image != null) {
                Image(image, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp).padding(6.dp))
            } else {
                TonalIcon(Icons.Filled.Widgets)
            }
        }
        Text(e.label, color = colors.onWallpaper, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
        Text(size, color = colors.onWallpaperMuted, style = MaterialTheme.typography.labelMedium)
    }
}

private const val TILE_WIDTH = 160
private const val PREVIEW_PX = 480f
