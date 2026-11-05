// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import kotlinx.serialization.Serializable

/** Prefisso delle voci multimediali nell'elenco dei preferiti. */
const val MEDIA_PREFIX = "media:"

/** Cosa apre un link di Spotify. */
enum class SpotifyType { PLAYLIST, ALBUM, ARTIST, TRACK, EPISODE, SHOW }

/** Un contenuto di Spotify riconosciuto da un link: [uri] è la forma "spotify:tipo:id" che apre l'app. */
data class SpotifyItem(val type: SpotifyType, val id: String) {
    val uri: String get() = "spotify:${type.name.lowercase()}:$id"
}

/** Voce del launcher che apre (o fa partire) un contenuto multimediale. */
@Serializable
data class MediaShortcut(
    val id: String,
    val uri: String,
    val name: String,
    val type: String = "",
    val packageName: String = SpotifyLinks.PACKAGE,
)

/** Riconoscimento dei link e gestione delle voci, senza dipendenze Android (testabile su JVM). */
object MediaShortcuts {
    private val ID = Regex("^[A-Za-z0-9]{10,40}$")
    private val WEB = Regex("^(?:https?://)?open\\.spotify\\.com/(?:intl-[a-zA-Z-]+/)?(?:embed/)?([a-z]+)/([^/?#\\s]+)")
    private val URI = Regex("^spotify:([a-z]+):([^:\\s]+)$")

    /** Link corti di Spotify: si risolvono solo con internet, quindi non si possono usare. */
    fun isShortLink(text: String): Boolean = text.trim().let { it.contains("spotify.link/") || it.contains("spoti.fi/") }

    /** Il contenuto di un link di Spotify (web o "spotify:"), o null se non è un link valido. */
    fun parse(text: String): SpotifyItem? {
        val t = text.trim()
        val match = WEB.find(t) ?: URI.find(t) ?: return null
        val type = SpotifyType.entries.firstOrNull { it.name.equals(match.groupValues[1], ignoreCase = true) } ?: return null
        val id = match.groupValues[2]
        return if (ID.matches(id)) SpotifyItem(type, id) else null
    }

    fun add(s: Settings, shortcut: MediaShortcut): Settings = s.copy(
        mediaShortcuts = s.mediaShortcuts.filterNot { it.id == shortcut.id } + shortcut,
        favorites = if ((MEDIA_PREFIX + shortcut.id) in s.favorites) s.favorites else s.favorites + (MEDIA_PREFIX + shortcut.id),
    )

    fun remove(s: Settings, id: String): Settings = s.copy(
        mediaShortcuts = s.mediaShortcuts.filterNot { it.id == id },
        favorites = s.favorites - (MEDIA_PREFIX + id),
    )

    fun rename(s: Settings, id: String, name: String): Settings =
        s.copy(mediaShortcuts = s.mediaShortcuts.map { if (it.id == id) it.copy(name = name.trim().ifBlank { it.name }) else it })
}
