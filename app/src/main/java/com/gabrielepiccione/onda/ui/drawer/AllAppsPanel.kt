// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.ContactHit
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

/** Lettera dell'alfabeto che porta ai contatti preferiti. */
const val CONTACTS_LETTER = '@'

/** Righe occupate dalla sezione dei contatti in cima all'elenco (intestazione + contatti). */
fun contactsRows(contacts: List<ContactHit>): Int = if (contacts.isEmpty()) 0 else contacts.size + 1

@Composable
private fun ContactListRow(contact: ContactHit, loadPhoto: suspend (ContactHit) -> android.graphics.Bitmap?, onClick: () -> Unit) {
    val colors = LocalOndaColors.current
    var photo by remember(contact.id) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(contact.id) { photo = loadPhoto(contact)?.asImageBitmap() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(36.dp).clip(CircleShape).background(colors.accentContainer),
        ) {
            val p = photo
            if (p != null) {
                Image(p, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(36.dp))
            } else {
                Text(contact.name.take(1).uppercase(), color = colors.onAccentContainer, style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(contact.name, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
    }
}

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
    onDismissNotifications: (AppInfo) -> Unit = {},
    contacts: List<ContactHit> = emptyList(),
    onContact: (ContactHit) -> Unit = {},
    loadPhoto: suspend (ContactHit) -> android.graphics.Bitmap? = { null },
    privateSpace: com.gabrielepiccione.onda.data.PrivateSpace? = null,
    privateApps: List<AppInfo> = emptyList(),
    onTogglePrivate: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalOndaColors.current
    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = modifier.fillMaxSize(),
    ) {
        // Contatti preferiti: sezione "@" in cima, raggiungibile dall'alfabeto
        if (contacts.isNotEmpty()) {
            item(key = "h_contacts") {
                Text(
                    text = stringResource(R.string.starred_contacts),
                    color = if (activeLetter == CONTACTS_LETTER) colors.accent else colors.onWallpaperMuted,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
                )
            }
            items(contacts.size, key = { "c_" + contacts[it].id }) { i ->
                ContactListRow(contacts[i], loadPhoto) { onContact(contacts[i]) }
            }
        }
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
                        swipeEnabled = true,
                        onDismissNotifications = { onDismissNotifications(app) },
                        showIcon = showIcons,
                        themedIcons = themedIcons,
                        onClick = { onClick(app) },
                        onLongClick = { onLongClick(app) },
                    )
                }
            }
        }
        // Spazio privato (Android 15+): in fondo all'elenco, con blocco e sblocco
        if (privateSpace != null) {
            item(key = "h_private") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 28.dp, bottom = 4.dp),
                ) {
                    Text(
                        stringResource(R.string.private_space),
                        color = colors.onWallpaperMuted,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    androidx.compose.material3.FilledTonalButton(onClick = onTogglePrivate, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(if (privateSpace.locked) R.string.private_unlock else R.string.private_lock))
                    }
                }
            }
            if (!privateSpace.locked) {
                privateApps.forEach { app ->
                    item(key = "p_" + app.key) {
                        AppListRow(
                            app = app,
                            hasNotification = notifications[app.packageName].orEmpty().isNotEmpty(),
                            notification = notifications[app.packageName]?.firstOrNull(),
                            notificationCount = notifications[app.packageName]?.size ?: 0,
                            onOpenPanel = { onOpenPanel(app) },
                            swipeEnabled = false,
                            onDismissNotifications = { onDismissNotifications(app) },
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
}
