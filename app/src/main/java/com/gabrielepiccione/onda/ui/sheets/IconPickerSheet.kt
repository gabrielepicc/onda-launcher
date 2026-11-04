// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(ExperimentalMaterial3Api::class)

package com.gabrielepiccione.onda.ui.sheets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import com.gabrielepiccione.onda.ui.components.AppIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.core.icons.IconMatcher
import com.gabrielepiccione.onda.core.icons.IconPackCache
import com.gabrielepiccione.onda.core.icons.IconPacks
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scelta manuale dell'icona di un'app: suggerimenti (ricerca "fuzzy" sul nome in tutti
 * i pack installati), poi l'intero catalogo del pack selezionato, filtrabile.
 */
@Composable
fun IconPickerSheet(
    app: AppInfo,
    packs: IconPackCache,
    onPick: (packPackage: String, drawable: String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    /** Forma scelta solo per questa app; null = quella generale. */
    currentShape: String? = null,
    onShape: (String?) -> Unit = {},
    onPickImage: () -> Unit = {},
    themedIcons: Boolean = false,
) {
    val colors = LocalOndaColors.current
    val context = LocalContext.current
    var installed by remember { mutableStateOf<List<IconPacks.Pack>>(emptyList()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var catalog by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        val list = withContext(Dispatchers.IO) { IconPacks.installed(context) }
        installed = list
        selected = list.firstOrNull()?.packageName
        // Suggerimenti da tutti i pack, i migliori per primi
        suggestions = withContext(Dispatchers.Default) {
            list.flatMap { p ->
                val loaded = packs.get(p.packageName) ?: return@flatMap emptyList()
                IconMatcher.suggest(app.originalLabel, app.packageName, loaded.allDrawables, limit = 6).map { p.packageName to it }
            }.take(18)
        }
    }
    LaunchedEffect(selected) {
        val pkg = selected ?: return@LaunchedEffect
        catalog = withContext(Dispatchers.IO) { packs.get(pkg)?.allDrawables.orEmpty() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.panel,
        contentColor = colors.onWallpaper,
    ) {
        Column(Modifier.navigationBarsPadding().imePadding().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // L'icona si aggiorna dal vivo mentre si sceglie la forma
                AppIcon(app, 44.dp, themedIcons)
                Text(
                    stringResource(R.string.icon_change),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
                TextButton(onClick = { onReset(); onDismiss() }) { Text(stringResource(R.string.icon_reset)) }
            }

            FilledTonalButton(onClick = { onPickImage(); onDismiss() }, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.icon_from_image))
            }

            Label(stringResource(R.string.icon_shape))
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = currentShape == null, onClick = { onShape(null) }, label = { Text(stringResource(R.string.icon_shape_default)) })
                listOf(
                    "CIRCLE" to R.string.shape_circle, "SQUIRCLE" to R.string.shape_squircle, "ROUNDED_SQUARE" to R.string.shape_rounded,
                    "TEARDROP" to R.string.shape_teardrop, "CYLINDER" to R.string.shape_cylinder, "PETAL" to R.string.shape_petal,
                ).forEach { (id, res) ->
                    FilterChip(selected = currentShape == id, onClick = { onShape(id) }, label = { Text(stringResource(res)) })
                }
            }

            if (installed.isEmpty()) {
                Text(stringResource(R.string.no_icon_packs), color = colors.onWallpaperMuted, modifier = Modifier.padding(vertical = 24.dp))
                return@Column
            }

            val filtered = remember(catalog, query) {
                if (query.isBlank()) catalog else catalog.filter { it.contains(query.trim().replace(' ', '_'), ignoreCase = true) }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(64.dp),
                modifier = Modifier.heightIn(max = 560.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (suggestions.isNotEmpty() && query.isBlank()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { Label(stringResource(R.string.icon_suggested)) }
                    items(suggestions, key = { "s_${it.first}_${it.second}" }) { (pkg, name) ->
                        IconCell(packs, pkg, name) { onPick(pkg, name); onDismiss() }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            installed.forEach { p ->
                                FilterChip(selected = selected == p.packageName, onClick = { selected = p.packageName }, label = { Text(p.label) })
                            }
                        }
                        Box(
                            Modifier
                                .padding(vertical = 8.dp)
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(colors.panelHigh)
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (query.isEmpty()) Text(stringResource(R.string.icon_search), color = colors.onWallpaperMuted)
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onWallpaper),
                                cursorBrush = SolidColor(colors.accent),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                val pkg = selected
                if (pkg != null) {
                    items(filtered, key = { "c_${pkg}_$it" }) { name ->
                        IconCell(packs, pkg, name) { onPick(pkg, name); onDismiss() }
                    }
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = LocalOndaColors.current.accent, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
}

/** Cella della griglia: l'icona viene caricata in background solo quando è visibile. */
@Composable
private fun IconCell(packs: IconPackCache, pkg: String, name: String, onClick: () -> Unit) {
    val context = LocalContext.current
    var bmp by remember(pkg, name) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(pkg, name) {
        bmp = withContext(Dispatchers.IO) {
            val dpi = context.resources.displayMetrics.densityDpi
            runCatching { packs.get(pkg)?.drawable(name, dpi)?.toBitmap(144, 144)?.asImageBitmap() }.getOrNull()
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .semantics { contentDescription = name.replace('_', ' ') }
            .clickable(onClick = onClick)
            .padding(6.dp),
    ) {
        bmp?.let { Image(it, null, Modifier.size(52.dp)) }
    }
}
