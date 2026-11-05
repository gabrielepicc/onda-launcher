// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.drawer

import com.gabrielepiccione.onda.data.ConversationSummary
import com.gabrielepiccione.onda.ui.theme.*
import com.gabrielepiccione.onda.ui.components.*
import com.gabrielepiccione.onda.ui.home.*
import com.gabrielepiccione.onda.ui.search.*
import com.gabrielepiccione.onda.ui.settings.*
import com.gabrielepiccione.onda.ui.sheets.*

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import com.gabrielepiccione.onda.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.data.ContactHit
import com.gabrielepiccione.onda.data.Folder
import com.gabrielepiccione.onda.data.NotificationItem
import com.gabrielepiccione.onda.data.PlainSectionIndex
import com.gabrielepiccione.onda.data.SectionIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppSection(
    val letter: String,
    val apps: List<AppInfo>,
    val firstIndex: Int,
    val contacts: List<ContactHit> = emptyList(),
)

/** Segno dell'alfabeto che porta in fondo all'elenco (cartelle e voci di Onda). */
const val END_OF_LIST_MARK = "•"

/** Segno in cima all'alfabeto che riporta ai preferiti. */
const val FAVORITES_MARK = "★"

/**
 * Posizione nell'elenco della prima voce dopo le lettere: tutte le sezioni (intestazione + righe)
 * più il contenitore dello Spazio privato, se visibile ([privateRows] = intestazione + app).
 */
fun endOfListIndex(sections: List<AppSection>, privateRows: Int = 0): Int {
    val last = sections.lastOrNull() ?: return privateRows
    return last.firstIndex + 1 + last.apps.size + last.contacts.size + privateRows
}

/**
 * Raggruppa app e contatti per lettera ('#' per primo) e calcola l'indice d'inizio di ogni sezione.
 * Dentro ogni lettera le app più usate vengono prima, a parità di utilizzo resta l'ordine
 * alfabetico; i contatti seguono le app.
 */
fun buildSections(
    apps: List<AppInfo>,
    launchCounts: Map<String, Int> = emptyMap(),
    contacts: List<ContactHit> = emptyList(),
    alphabet: SectionIndex = PlainSectionIndex,
): List<AppSection> {
    val appsByLetter = apps.groupBy { alphabet.sectionOf(it.label) }
    val contactsByLetter = contacts.groupBy { alphabet.sectionOf(it.name) }
    var index = 0
    return (appsByLetter.keys + contactsByLetter.keys)
        .sortedWith(compareBy<String> { alphabet.rank(it) }.thenBy { it })
        .map { letter ->
            // sortedByDescending è stabile: a parità di conteggio resta l'ordine alfabetico
            val ordered = appsByLetter[letter].orEmpty().sortedByDescending { launchCounts[it.key] ?: 0 }
            val people = contactsByLetter[letter].orEmpty()
            AppSection(letter, ordered, index, people).also { index += 1 + ordered.size + people.size }
        }
}

@Composable
fun AllAppsPanel(
    sections: List<AppSection>,
    listState: LazyListState,
    activeLetter: String?,
    notifications: Map<String, List<NotificationItem>>,
    showIcons: Boolean,
    themedIcons: Boolean,
    onClick: (AppInfo) -> Unit,
    onLongClick: (AppInfo) -> Unit,
    contentPadding: PaddingValues,
    onOpenPanel: (AppInfo) -> Unit = {},
    onDismissNotifications: (AppInfo) -> Unit = {},
    onContact: (ContactHit) -> Unit = {},
    /** Modifier della riga di un'app, ad esempio per conoscerne la posizione. */
    rowModifier: (AppInfo) -> Modifier = { Modifier },
    /** Stato dello Spazio privato e sue app; null = nessun contenitore da mostrare. */
    privateLocked: Boolean? = null,
    privateApps: List<AppInfo> = emptyList(),
    onTogglePrivateLock: () -> Unit = {},
    /** Cartelle mostrate in fondo all'elenco, dopo le lettere; ciascuna disegnata da [folderRow]. */
    folders: List<Folder> = emptyList(),
    folderRow: @Composable (Folder) -> Unit = {},
    onRecentlyInstalled: () -> Unit = {},
    onLauncherSettings: () -> Unit = {},
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
                    text = section.letter,
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
                        summary = ConversationSummary.of(notifications[app.packageName].orEmpty()),
                        onOpenPanel = { onOpenPanel(app) },
                        swipeEnabled = true,
                        onDismissNotifications = { onDismissNotifications(app) },
                        showIcon = showIcons,
                        themedIcons = themedIcons,
                        onClick = { onClick(app) },
                        onLongClick = { onLongClick(app) },
                        modifier = rowModifier(app),
                    )
                }
            }
            section.contacts.forEach { contact ->
                item(key = "c_${contact.id}") { ContactListRow(contact, onClick = { onContact(contact) }) }
            }
        }
        // Contenitore separato dello Spazio privato, dopo tutte le lettere
        if (privateLocked != null) {
            item(key = "private_header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 24.dp),
                ) {
                    Text(
                        stringResource(R.string.private_space),
                        color = colors.onWallpaperMuted,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onTogglePrivateLock) {
                        Icon(if (privateLocked) Icons.Filled.LockOpen else Icons.Filled.Lock, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(if (privateLocked) R.string.private_unlock else R.string.private_lock))
                    }
                }
            }
            privateApps.forEach { app ->
                item(key = "p_" + app.key) {
                    AppListRow(
                        app = app,
                        hasNotification = notifications[app.packageName].orEmpty().isNotEmpty(),
                        showIcon = showIcons,
                        themedIcons = themedIcons,
                        onClick = { onClick(app) },
                        onLongClick = { onLongClick(app) },
                        modifier = rowModifier(app),
                    )
                }
            }
        }
        // Cartelle, poi le voci del launcher: sempre in fondo, raggiungibili con il segno finale dell'alfabeto
        if (folders.isNotEmpty()) {
            item(key = "folders_header") { EndSectionHeader(stringResource(R.string.list_folders)) }
            folders.forEach { folder ->
                item(key = "f_" + folder.id) { folderRow(folder) }
            }
        }
        item(key = "onda_header") { EndSectionHeader(stringResource(R.string.app_name)) }
        item(key = "onda_recent") {
            LauncherEntryRow(Icons.Filled.History, stringResource(R.string.recently_installed), onRecentlyInstalled)
        }
        item(key = "onda_settings") {
            LauncherEntryRow(Icons.Filled.Settings, stringResource(R.string.onda_settings), onLauncherSettings)
        }
    }
}

@Composable
private fun EndSectionHeader(text: String) {
    Text(
        text,
        color = LocalOndaColors.current.onWallpaperMuted,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp),
    )
}

/** Voce del launcher in fondo all'elenco: icona su fondo tonale, stessa misura delle righe delle app. */
@Composable
private fun LauncherEntryRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = LocalOndaColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(colors.accentContainer),
        ) {
            Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(label, color = colors.onWallpaper, style = MaterialTheme.typography.titleMedium)
    }
}

/** Riga di un contatto nell'elenco: foto se disponibile, altrimenti icona generica. */
@Composable
private fun ContactListRow(contact: ContactHit, onClick: () -> Unit) {
    val colors = LocalOndaColors.current
    val context = LocalContext.current
    var photo by remember(contact.photoUri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(contact.photoUri) {
        val uri = contact.photoUri ?: return@LaunchedEffect
        photo = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            }.getOrNull()
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
            val image = photo
            if (image != null) {
                Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(36.dp))
            } else {
                Icon(Icons.Filled.Person, contentDescription = null, tint = colors.accent)
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(contact.name, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge)
    }
}
