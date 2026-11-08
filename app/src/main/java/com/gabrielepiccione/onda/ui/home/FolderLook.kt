// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(ExperimentalMaterial3Api::class)

package com.gabrielepiccione.onda.ui.home

import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import com.gabrielepiccione.onda.core.icons.IconPackCache
import com.gabrielepiccione.onda.core.icons.IconPacks
import com.gabrielepiccione.onda.ui.sheets.PackIconBrowser
import com.gabrielepiccione.onda.ui.sheets.suggestPackIcons
import java.io.File
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.data.Folder
import com.gabrielepiccione.onda.data.FolderIcons
import com.gabrielepiccione.onda.ui.components.AppIcon
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Disegno di ogni simbolo disponibile per le cartelle (nomi in [FolderIcons.SYMBOLS]). */
fun folderSymbol(name: String): ImageVector = when (name) {
    "star" -> Icons.Filled.Star
    "favorite" -> Icons.Filled.Favorite
    "work" -> Icons.Filled.Work
    "home" -> Icons.Filled.Home
    "school" -> Icons.Filled.School
    "games" -> Icons.Filled.SportsEsports
    "music" -> Icons.Filled.MusicNote
    "photo" -> Icons.Filled.PhotoCamera
    "movie" -> Icons.Filled.Movie
    "chat" -> Icons.AutoMirrored.Filled.Chat
    "mail" -> Icons.Filled.Mail
    "shopping" -> Icons.Filled.ShoppingCart
    "wallet" -> Icons.Filled.AccountBalanceWallet
    "travel" -> Icons.Filled.Flight
    "car" -> Icons.Filled.DirectionsCar
    "food" -> Icons.Filled.Restaurant
    "fitness" -> Icons.Filled.FitnessCenter
    "health" -> Icons.Filled.LocalHospital
    "pets" -> Icons.Filled.Pets
    "book" -> Icons.AutoMirrored.Filled.MenuBook
    "news" -> Icons.Filled.Newspaper
    "code" -> Icons.Filled.Code
    "tools" -> Icons.Filled.Build
    "lightbulb" -> Icons.Filled.Lightbulb
    "cloud" -> Icons.Filled.Cloud
    "sport" -> Icons.Filled.SportsSoccer
    "family" -> Icons.Filled.FamilyRestroom
    "map" -> Icons.Filled.Map
    "bank" -> Icons.Filled.AccountBalance
    "card" -> Icons.Filled.CreditCard
    "receipt" -> Icons.Filled.Receipt
    "phone" -> Icons.Filled.Phone
    "calendar" -> Icons.Filled.CalendarMonth
    "settings" -> Icons.Filled.Settings
    "security" -> Icons.Filled.Security
    "coffee" -> Icons.Filled.LocalCafe
    "beach" -> Icons.Filled.BeachAccess
    "bike" -> Icons.Filled.DirectionsBike
    "train" -> Icons.Filled.Train
    "palette" -> Icons.Filled.Palette
    "brush" -> Icons.Filled.Brush
    "science" -> Icons.Filled.Science
    "wifi" -> Icons.Filled.Wifi
    "tv" -> Icons.Filled.Tv
    "headphones" -> Icons.Filled.Headphones
    "flower" -> Icons.Filled.LocalFlorist
    "baby" -> Icons.Filled.ChildCare
    "group" -> Icons.Filled.Groups
    "gift" -> Icons.Filled.CardGiftcard
    "bolt" -> Icons.Filled.Bolt
    "moon" -> Icons.Filled.DarkMode
    "sun" -> Icons.Filled.WbSunny
    "language" -> Icons.Filled.Language
    "bookmark" -> Icons.Filled.Bookmark
    "puzzle" -> Icons.Filled.Extension
    else -> Icons.Filled.Folder
}

/** Icona della cartella secondo la scelta dell'utente; senza scelta, le prime app sovrapposte. */
@Composable
fun FolderIcon(folder: Folder, apps: List<AppInfo>, size: Dp, themedIcons: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalOndaColors.current
    when (val kind = FolderIcons.parse(folder.icon)) {
        is FolderIcons.Kind.Symbol -> Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(colors.accentContainer),
        ) {
            Icon(folderSymbol(kind.name), contentDescription = null, tint = colors.onAccentContainer, modifier = Modifier.size(size * 0.55f))
        }
        is FolderIcons.Kind.Image -> {
            var bitmap by remember(kind.path) { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(kind.path) {
                bitmap = withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeFile(kind.path)?.asImageBitmap() }.getOrNull() }
            }
            Box(modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(colors.accentContainer)) {
                bitmap?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size)) }
            }
        }
        is FolderIcons.Kind.App -> {
            val app = apps.firstOrNull { it.key == kind.key }
            if (app != null) AppIcon(app, size, themedIcons, modifier) else AppsCollage(apps, size, themedIcons, modifier)
        }
        FolderIcons.Kind.Auto -> AppsCollage(apps, size, themedIcons, modifier)
    }
}

@Composable
private fun AppsCollage(apps: List<AppInfo>, size: Dp, themedIcons: Boolean, modifier: Modifier) {
    val colors = LocalOndaColors.current
    Box(modifier.size(size)) {
        val shown = apps.take(3)
        if (shown.isEmpty()) {
            Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(colors.accentContainer))
        }
        val step = size / 5.5f
        shown.forEachIndexed { i, a ->
            AppIcon(a, size * 0.64f, themedIcons, Modifier.offset(x = step * i, y = step * i))
        }
    }
}

/** Copia un'icona di un pack tra le immagini di Onda: resta nel backup anche se il pack viene tolto. */
suspend fun savePackIconForFolder(context: Context, packs: IconPackCache, folderId: String, pkg: String, name: String): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val dpi = context.resources.displayMetrics.densityDpi
            val bitmap = packs.get(pkg)?.drawable(name, dpi)?.toBitmap(FOLDER_ICON_PX, FOLDER_ICON_PX) ?: return@runCatching null
            val dir = File(context.filesDir, "icons").apply { mkdirs() }
            val safeId = folderId.filter { it.isLetterOrDigit() }
            val file = File(dir, "folder_${safeId}_${System.currentTimeMillis()}.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            FolderIcons.image(file.absolutePath)
        }.getOrNull()
    }

private const val FOLDER_ICON_PX = 256

/** Scelta dell'icona della cartella: automatica, un'immagine, una delle sue app, un simbolo o un icon pack. */
@Composable
fun FolderIconSheet(
    folder: Folder,
    apps: List<AppInfo>,
    themedIcons: Boolean,
    packs: IconPackCache,
    onPick: (String) -> Unit,
    onPickPackIcon: (pkg: String, name: String) -> Unit,
    onPickImage: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalOndaColors.current
    val context = LocalContext.current
    var showPacks by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var installed by remember { mutableStateOf<List<IconPacks.Pack>>(emptyList()) }
    var suggestions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    LaunchedEffect(showPacks) {
        if (showPacks && installed.isEmpty()) {
            val list = withContext(Dispatchers.IO) { IconPacks.installed(context) }
            installed = list
            suggestions = suggestPackIcons(packs, list, folder.name, "")
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.panel,
        contentColor = colors.onWallpaper,
    ) {
        Column(Modifier.navigationBarsPadding().imePadding().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FolderIcon(folder, apps, 44.dp, themedIcons)
                Text(
                    stringResource(R.string.folder_icon),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
            }
            if (!searching) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    FilterChip(
                        selected = folder.icon == FolderIcons.AUTO,
                        onClick = { onPick(FolderIcons.AUTO) },
                        label = { Text(stringResource(R.string.folder_icon_auto)) },
                    )
                    FilledTonalButton(onClick = { onPickImage(); onDismiss() }) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.icon_from_image), modifier = Modifier.padding(start = 8.dp))
                    }
                }
                if (apps.isNotEmpty()) {
                    SectionLabel(stringResource(R.string.folder_icon_from_app))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        apps.forEach { app ->
                            val spec = FolderIcons.app(app.key)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .then(if (folder.icon == spec) Modifier.border(2.dp, colors.accent, RoundedCornerShape(16.dp)) else Modifier)
                                    .clickable { onPick(spec) }
                                    .semantics { contentDescription = app.label },
                            ) { AppIcon(app, 40.dp, themedIcons) }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                    FilterChip(selected = !showPacks, onClick = { showPacks = false }, label = { Text(stringResource(R.string.folder_icon_symbols)) })
                    FilterChip(selected = showPacks, onClick = { showPacks = true }, label = { Text(stringResource(R.string.icon_pack)) })
                }
            }
            if (showPacks) {
                if (installed.isEmpty()) {
                    Text(stringResource(R.string.no_icon_packs), color = colors.onWallpaperMuted, modifier = Modifier.padding(vertical = 24.dp))
                } else {
                    PackIconBrowser(
                        packs = packs,
                        installed = installed,
                        suggestions = suggestions,
                        onPick = onPickPackIcon,
                        onSearching = { searching = it },
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(56.dp),
                    modifier = Modifier.heightIn(max = 360.dp).padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(FolderIcons.SYMBOLS) { name ->
                        val spec = FolderIcons.symbol(name)
                        val selected = folder.icon == spec
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) colors.accentContainer else colors.panelHigh)
                                .clickable { onPick(spec) }
                                .semantics { contentDescription = name },
                        ) {
                            Icon(
                                folderSymbol(name),
                                contentDescription = null,
                                tint = if (selected) colors.onAccentContainer else colors.onWallpaper,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}
