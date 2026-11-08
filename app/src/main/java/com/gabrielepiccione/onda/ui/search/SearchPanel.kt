// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.search

import com.gabrielepiccione.onda.ui.components.TonalIcon
import com.gabrielepiccione.onda.ui.theme.LocalOndaTokens
import com.gabrielepiccione.onda.data.SettingsSearch
import com.gabrielepiccione.onda.ui.settings.SETTING_ENTRIES
import com.gabrielepiccione.onda.ui.settings.SettingEntry
import com.gabrielepiccione.onda.data.MediaShortcuts
import com.gabrielepiccione.onda.data.SpotifyItem
import com.gabrielepiccione.onda.data.SpotifyType
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import com.gabrielepiccione.onda.ui.theme.*
import com.gabrielepiccione.onda.ui.components.*
import com.gabrielepiccione.onda.ui.home.*
import com.gabrielepiccione.onda.ui.drawer.*
import com.gabrielepiccione.onda.ui.settings.*
import com.gabrielepiccione.onda.ui.sheets.*

import androidx.compose.ui.res.stringResource
import com.gabrielepiccione.onda.R
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.SearchResults
import com.gabrielepiccione.onda.data.Actions
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.data.GestureAction
import com.gabrielepiccione.onda.ui.components.actionIcon
import com.gabrielepiccione.onda.data.ContactHit

@Composable
internal fun SearchPanel(
    results: SearchResults,
    suggestions: List<AppInfo>,
    showIcons: Boolean,
    themedIcons: Boolean,
    contactsPermission: Boolean,
    onQueryChange: (String) -> Unit,
    onApp: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onContact: (ContactHit) -> Unit,
    onWebSearch: (String) -> Unit,
    onSettingsShortcut: (com.gabrielepiccione.onda.util.SettingsShortcut) -> Unit = {},
    /** Una voce delle impostazioni di Onda: si apre la sua pagina e la voce lampeggia. */
    onOndaSetting: (SettingEntry) -> Unit = {},
    /** Nome dell'app musicale per "riproduci", null se non ce n'è una. */
    musicAppLabel: String? = null,
    spotifyInstalled: Boolean = false,
    onPlay: (String) -> Unit = {},
    onSpotifySearch: (String) -> Unit = {},
    /** Un link di Spotify incollato: aggiungilo ai preferiti con questo nome. */
    onAddMedia: (SpotifyItem, String) -> Unit = { _, _ -> },
    onRequestContacts: () -> Unit,
    onClose: () -> Unit,
    /** Un'azione del catalogo unico (torcia, silenzioso, schermata…) scelta dai risultati. */
    onAction: (GestureAction) -> Unit = {},
) {
    val colors = LocalOndaColors.current
    // Le azioni del catalogo si trovano anche dalla ricerca, per nome
    val actionLabels = Actions.catalog.flatMap { it.second }.associateWith { stringResource(it.labelRes) }
    val actionHits = if (results.mode == com.gabrielepiccione.onda.SearchMode.ALL) {
        Actions.matching(results.query) { actionLabels.getValue(it) }
    } else {
        emptyList()
    }
    // Con il prefisso delle impostazioni si cercano anche le voci di Onda, per nome e per sinonimi
    val ondaHits = if (results.mode == com.gabrielepiccione.onda.SearchMode.SETTINGS && results.term.isNotBlank()) {
        SETTING_ENTRIES.filter { e ->
            SettingsSearch.matches(results.term, stringResource(e.titleRes), stringResource(e.page.titleRes), stringResource(e.keywordsRes))
        }.take(MAX_ONDA_HITS)
    } else {
        emptyList()
    }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }

    Column(
        Modifier
            .fillMaxSize()
            // Nessun pannello opaco: resta visibile lo sfondo, appena velato per leggere i risultati
            .background((if (colors.dark) Color.Black else Color.White).copy(alpha = SEARCH_TINT))
            .blockTouches()
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 16.dp, bottom = 8.dp)
                .fillMaxWidth()
                .clip(LocalOndaTokens.current.shapes.large)
                // Barra di ricerca di Material 3: superficie tonale alta
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .height(56.dp)
                .padding(start = 18.dp, end = 6.dp)
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = colors.onWallpaperMuted)
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (results.query.isEmpty()) {
                    Text(stringResource(R.string.search_hint), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyLarge)
                }
                BasicTextField(
                    value = results.query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onWallpaper),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        // Invio: apre il primo risultato della modalità corrente
                        val first = results.apps.firstOrNull()
                        when {
                            results.mode == com.gabrielepiccione.onda.SearchMode.PLAY -> if (results.term.isNotBlank() && musicAppLabel != null) onPlay(results.term)
                            results.mode == com.gabrielepiccione.onda.SearchMode.SPOTIFY -> if (results.term.isNotBlank() && spotifyInstalled) onSpotifySearch(results.term)
                            results.mode == com.gabrielepiccione.onda.SearchMode.SETTINGS && ondaHits.isNotEmpty() -> onOndaSetting(ondaHits.first())
                            results.mode == com.gabrielepiccione.onda.SearchMode.SETTINGS -> results.settingsHits.firstOrNull()?.let(onSettingsShortcut)
                            results.mode == com.gabrielepiccione.onda.SearchMode.CONTACTS -> results.contacts.firstOrNull()?.let(onContact)
                            first != null -> onApp(first)
                            results.mode == com.gabrielepiccione.onda.SearchMode.ALL && results.query.isNotBlank() -> onWebSearch(results.query)
                        }
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus),
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClose)
            ) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close), tint = colors.onWallpaperMuted)
            }
        }

        LazyColumn(Modifier.fillMaxSize()) {
            if (results.query.isBlank() && suggestions.isNotEmpty()) {
                item(key = "sugg_label") { SectionLabel(stringResource(R.string.frequent_apps)) }
                items(suggestions, key = { "s_" + it.key }) { app ->
                    AppListRow(
                        app = app,
                        hasNotification = false,
                        showIcon = showIcons,
                        themedIcons = themedIcons,
                        onClick = { onApp(app) },
                        onLongClick = { onAppLongClick(app) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            if (actionHits.isNotEmpty()) {
                item(key = "actions_label") { SectionLabel(stringResource(R.string.search_actions_title)) }
                items(actionHits, key = { "act_" + it.name }) { action ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .clickable { onAction(action) }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                            .animateItem(),
                    ) {
                        TonalIcon(actionIcon(action))
                        Spacer(Modifier.width(16.dp))
                        Text(actionLabels.getValue(action), color = colors.onWallpaper, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            results.calc?.let { value ->
                item(key = "calc") {
                    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(colors.accentContainer)
                            .clickable { clipboard.setText(androidx.compose.ui.text.AnnotatedString(value)) }
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Text("= $value", color = colors.onAccentContainer, style = MaterialTheme.typography.headlineLarge)
                        Text(stringResource(R.string.tap_to_copy), color = colors.onAccentContainer, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            items(results.apps, key = { it.key }) { app ->
                AppListRow(
                    app = app,
                    hasNotification = false,
                    showIcon = showIcons,
                    themedIcons = themedIcons,
                    highlighted = app == results.apps.firstOrNull(),
                    onClick = { onApp(app) },
                    onLongClick = { onAppLongClick(app) },
                )
            }
            if (ondaHits.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.app_name)) }
                items(ondaHits.size) { i ->
                    val e = ondaHits[i]
                    SimpleRow(
                        icon = { Icon(Icons.Filled.Settings, null, tint = colors.accent) },
                        label = stringResource(e.titleRes) + " · " + stringResource(e.page.titleRes),
                    ) { onOndaSetting(e) }
                }
            }
            if (results.mode == com.gabrielepiccione.onda.SearchMode.SETTINGS) {
                item { SectionLabel(stringResource(R.string.settings)) }
                items(results.settingsHits.size) { i ->
                    val sc = results.settingsHits[i]
                    SimpleRow(icon = { Icon(Icons.Filled.Settings, null, tint = colors.accent) }, label = stringResource(sc.labelRes)) { onSettingsShortcut(sc) }
                }
            }
            if (results.query.isBlank()) {
                item(key = "prefix_hint") {
                    Text(
                        stringResource(R.string.search_prefix_hint),
                        color = colors.onWallpaperMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 12.dp, top = 16.dp, end = 12.dp),
                    )
                }
            }
            // Link di Spotify incollato: si può tenere come voce dei preferiti
            val link = MediaShortcuts.parse(results.query)
            if (link != null) {
                item { SectionLabel(stringResource(R.string.music)) }
                item {
                    val name = stringResource(R.string.media_default_name, stringResource(spotifyTypeLabel(link.type)))
                    SimpleRow(
                        icon = { Icon(Icons.Filled.Add, null, tint = colors.accent) },
                        label = stringResource(R.string.media_add, name),
                    ) { onAddMedia(link, name) }
                }
            } else if (MediaShortcuts.isShortLink(results.query)) {
                item {
                    SimpleRow(icon = { Icon(Icons.Filled.MusicNote, null, tint = colors.onWallpaperMuted) }, label = stringResource(R.string.media_short_link), muted = true) {}
                }
            }
            // Musica: "p testo" fa partire il brano, "sp testo" apre la ricerca di Spotify
            if (results.mode == com.gabrielepiccione.onda.SearchMode.PLAY) {
                item { SectionLabel(stringResource(R.string.music)) }
                item {
                    if (musicAppLabel == null) {
                        SimpleRow(icon = { Icon(Icons.Filled.MusicNote, null, tint = colors.onWallpaperMuted) }, label = stringResource(R.string.music_no_app), muted = true) {}
                    } else {
                        SimpleRow(
                            icon = { Icon(Icons.Filled.PlayArrow, null, tint = colors.accent) },
                            label = stringResource(R.string.music_play_on, results.term.ifBlank { "…" }, musicAppLabel),
                        ) { if (results.term.isNotBlank()) onPlay(results.term) }
                    }
                }
            }
            if (results.mode == com.gabrielepiccione.onda.SearchMode.SPOTIFY) {
                item { SectionLabel(stringResource(R.string.music)) }
                item {
                    SimpleRow(
                        icon = { Icon(Icons.Filled.MusicNote, null, tint = if (spotifyInstalled) colors.accent else colors.onWallpaperMuted) },
                        label = if (spotifyInstalled) stringResource(R.string.music_spotify_search, results.term.ifBlank { "…" }) else stringResource(R.string.music_no_spotify),
                        muted = !spotifyInstalled,
                    ) { if (spotifyInstalled && results.term.isNotBlank()) onSpotifySearch(results.term) }
                }
            }
            if (results.query.isNotBlank() && results.mode != com.gabrielepiccione.onda.SearchMode.SETTINGS &&
                results.mode != com.gabrielepiccione.onda.SearchMode.PLAY && results.mode != com.gabrielepiccione.onda.SearchMode.SPOTIFY
            ) {
                if (results.contacts.isNotEmpty()) {
                    item { SectionLabel(stringResource(R.string.contacts)) }
                    items(results.contacts, key = { "c_${it.id}" }) { c ->
                        SimpleRow(icon = { Icon(Icons.Filled.Person, null, tint = colors.accent) }, label = c.name) { onContact(c) }
                    }
                } else if (!contactsPermission) {
                    item {
                        SimpleRow(icon = { Icon(Icons.Filled.Person, null, tint = colors.onWallpaperMuted) }, label = stringResource(R.string.search_contacts_too), muted = true, onClick = onRequestContacts)
                    }
                }
                if (results.mode == com.gabrielepiccione.onda.SearchMode.ALL) {
                    item { SectionLabel(stringResource(R.string.web)) }
                    item {
                        SimpleRow(icon = { Icon(Icons.Filled.Search, null, tint = colors.accent) }, label = stringResource(R.string.web_search_for, results.query)) {
                            onWebSearch(results.query)
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
        modifier = Modifier.padding(start = 12.dp, top = 18.dp, bottom = 4.dp),
    )
}

@Composable
private fun SimpleRow(icon: @Composable () -> Unit, label: String, muted: Boolean = false, onClick: () -> Unit) {
    val colors = LocalOndaColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(LocalOndaTokens.current.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(14.dp))
        Text(label, color = if (muted) colors.onWallpaperMuted else colors.onWallpaper, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Velatura sopra lo sfondo durante la ricerca (si somma a quella generale della home). */
private const val SEARCH_TINT = 0.28f

private fun spotifyTypeLabel(type: SpotifyType): Int = when (type) {
    SpotifyType.PLAYLIST -> R.string.media_playlist
    SpotifyType.ALBUM -> R.string.media_album
    SpotifyType.ARTIST -> R.string.media_artist
    SpotifyType.TRACK -> R.string.media_track
    SpotifyType.EPISODE -> R.string.media_episode
    SpotifyType.SHOW -> R.string.media_show
}

private const val MAX_ONDA_HITS = 6
