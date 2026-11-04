// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.StylePreset
import com.gabrielepiccione.onda.data.StylePresets
import com.gabrielepiccione.onda.data.ThemeMode
import com.gabrielepiccione.onda.ui.components.ClockFace
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import com.gabrielepiccione.onda.ui.theme.blockTouches
import com.gabrielepiccione.onda.ui.theme.fontFamilyOf
import com.gabrielepiccione.onda.ui.theme.typographyWith
import com.gabrielepiccione.onda.util.WallpaperGen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/** Galleria dei preset di stile, con anteprima reale di sfondo, carattere e orologio. */
@Composable
fun StyleGallery(
    currentPresetId: String,
    onApply: (preset: StylePreset, withWallpaper: Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val colors = LocalOndaColors.current
    var confirm by remember { mutableStateOf<StylePreset?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .blockTouches()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClose),
                    ) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Filled.ArrowBack, stringResource(R.string.back), tint = colors.onWallpaper)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.styles_title), style = MaterialTheme.typography.headlineMedium, color = colors.onWallpaper)
                }
                Text(
                    stringResource(R.string.styles_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onWallpaperMuted,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                )
            }
        }
        items(StylePresets.all, key = { it.id }) { preset ->
            PresetCard(preset, selected = preset.id == currentPresetId) { confirm = preset }
        }
    }

    confirm?.let { preset ->
        var withWallpaper by remember(preset.id) { mutableStateOf(true) }
        val name = stringResource(preset.nameRes)
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(stringResource(R.string.styles_apply_title, name)) },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { withWallpaper = !withWallpaper },
                ) {
                    Checkbox(checked = withWallpaper, onCheckedChange = { withWallpaper = it })
                    Text(stringResource(R.string.styles_with_wallpaper), style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = {
                TextButton(onClick = { onApply(preset, withWallpaper); confirm = null }) { Text(stringResource(R.string.styles_apply)) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun PresetCard(preset: StylePreset, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalOndaColors.current
    var bitmap by remember(preset.id) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(preset.id) {
        bitmap = withContext(Dispatchers.Default) { WallpaperGen.render(preset.wallpaper, 270, 480).asImageBitmap() }
    }
    val dark = preset.themeMode == ThemeMode.DARK
    val fg = if (dark) Color(0xFFF2F4F5) else Color(0xFF15191C)
    val previewColors = colors.copy(
        dark = dark,
        accent = Color(preset.accent),
        onWallpaper = fg,
        onWallpaperMuted = fg.copy(alpha = 0.65f),
        panel = if (dark) Color(0x33FFFFFF) else Color(0x22000000),
    )
    val typography = remember(preset.font) { typographyWith(fontFamilyOf(preset.font)) }
    val now = remember { LocalDateTime.now() }
    val name = stringResource(preset.nameRes)

    Column(
        Modifier
            .clip(RoundedCornerShape(28.dp))
            .then(if (selected) Modifier.border(3.dp, colors.accent, RoundedCornerShape(28.dp)) else Modifier)
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(preset.wallpaper.colors.first()))
        ) {
            bitmap?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            CompositionLocalProvider(LocalOndaColors provides previewColors) {
                MaterialTheme(typography = typography) {
                    Column(Modifier.padding(12.dp)) {
                        Box(Modifier.height(96.dp)) { ClockFace(now, preset.clockStyle, 0.42f) }
                        Spacer(Modifier.height(10.dp))
                        listOf(R.string.preview_app_1, R.string.preview_app_2, R.string.preview_app_3).forEach { res ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                                Box(
                                    Modifier
                                        .size(16.dp)
                                        .clip(if (preset.themedIcons) CircleShape else RoundedCornerShape(5.dp))
                                        .background(if (preset.themedIcons) previewColors.accent.copy(alpha = 0.6f) else fg.copy(alpha = 0.35f))
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(res),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = fg,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
        Text(
            name,
            style = MaterialTheme.typography.titleSmall,
            color = colors.onWallpaper,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}
