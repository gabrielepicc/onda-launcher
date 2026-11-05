// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaShortcutsTest {
    private val id = "37i9dQZF1DXcBWIGoYBM5M"

    @Test
    fun webLinksOfEveryKind() {
        assertEquals(SpotifyItem(SpotifyType.PLAYLIST, id), MediaShortcuts.parse("https://open.spotify.com/playlist/$id?si=abc123"))
        assertEquals(SpotifyType.ALBUM, MediaShortcuts.parse("https://open.spotify.com/intl-it/album/$id")?.type)
        assertEquals(SpotifyType.ARTIST, MediaShortcuts.parse("open.spotify.com/artist/$id")?.type)
        assertEquals(SpotifyType.TRACK, MediaShortcuts.parse("  https://open.spotify.com/track/$id  ")?.type)
        assertEquals(SpotifyType.EPISODE, MediaShortcuts.parse("https://open.spotify.com/episode/$id")?.type)
        assertEquals(SpotifyType.SHOW, MediaShortcuts.parse("https://open.spotify.com/show/$id#x")?.type)
    }

    @Test
    fun spotifyUrisAndRoundTrip() {
        val item = MediaShortcuts.parse("spotify:playlist:$id")
        assertEquals(SpotifyItem(SpotifyType.PLAYLIST, id), item)
        assertEquals("spotify:playlist:$id", item?.uri)
        assertEquals(item, MediaShortcuts.parse(item!!.uri))
    }

    @Test
    fun invalidLinksAreRejected() {
        assertNull(MediaShortcuts.parse("https://open.spotify.com/user/someone"))
        assertNull(MediaShortcuts.parse("https://open.spotify.com/playlist/../../x"))
        assertNull(MediaShortcuts.parse("https://example.com/playlist/$id"))
        assertNull(MediaShortcuts.parse("spotify:search:queen"))
        assertNull(MediaShortcuts.parse("ciao"))
    }

    @Test
    fun shortLinksNeedInternet() {
        assertTrue(MediaShortcuts.isShortLink("https://spotify.link/AbCdEf"))
        assertFalse(MediaShortcuts.isShortLink("https://open.spotify.com/track/$id"))
    }

    @Test
    fun addRenameRemove() {
        val shortcut = MediaShortcut("m1", "spotify:playlist:$id", "Spotify · Playlist", "playlist")
        val added = MediaShortcuts.add(Settings(favorites = listOf("app")), shortcut)
        assertEquals(listOf("app", MEDIA_PREFIX + "m1"), added.favorites)
        assertEquals(added, MediaShortcuts.add(added, shortcut))
        assertEquals("Corsa", MediaShortcuts.rename(added, "m1", " Corsa ").mediaShortcuts.single().name)
        assertEquals("Spotify · Playlist", MediaShortcuts.rename(added, "m1", "  ").mediaShortcuts.single().name)
        val removed = MediaShortcuts.remove(added, "m1")
        assertEquals(listOf("app"), removed.favorites)
        assertTrue(removed.mediaShortcuts.isEmpty())
    }
}
