// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import kotlinx.serialization.Serializable
import java.net.URLEncoder

/** Cosa fa un prefisso della ricerca. */
enum class PrefixKind { CALC, CONTACTS, SETTINGS, PLAY, SPOTIFY }

/** Prefissi della ricerca scelti dall'utente; i predefiniti sono quelli di sempre più "p" e "sp". */
@Serializable
data class SearchPrefixSet(
    val calc: String = "=",
    val contacts: String = "@",
    val settings: String = "/",
    val play: String = "p",
    val spotify: String = "sp",
) {
    fun of(kind: PrefixKind): String = when (kind) {
        PrefixKind.CALC -> calc
        PrefixKind.CONTACTS -> contacts
        PrefixKind.SETTINGS -> settings
        PrefixKind.PLAY -> play
        PrefixKind.SPOTIFY -> spotify
    }

    fun with(kind: PrefixKind, value: String): SearchPrefixSet = when (kind) {
        PrefixKind.CALC -> copy(calc = value)
        PrefixKind.CONTACTS -> copy(contacts = value)
        PrefixKind.SETTINGS -> copy(settings = value)
        PrefixKind.PLAY -> copy(play = value)
        PrefixKind.SPOTIFY -> copy(spotify = value)
    }
}

/**
 * Riconosce il prefisso all'inizio della ricerca. Senza dipendenze Android, testabile su JVM.
 * - Un prefisso fatto di simboli (=, @, /) vale subito: "=2+2".
 * - Un prefisso fatto di lettere (p, sp) vale solo se seguito da uno spazio, così "pizza" resta una ricerca
 *   normale e "p pizza" fa partire la musica. Maiuscole e minuscole non contano.
 * - Se due prefissi possono valere, vince il più lungo ("sp" prima di "p").
 */
object SearchPrefixes {
    fun parse(query: String, set: SearchPrefixSet): Pair<PrefixKind?, String> {
        val candidates = PrefixKind.entries
            .map { it to set.of(it).trim() }
            .filter { it.second.isNotEmpty() }
            .sortedByDescending { it.second.length }
        for ((kind, prefix) in candidates) {
            if (isWord(prefix)) {
                if (query.length > prefix.length && query.startsWith(prefix, ignoreCase = true) && query[prefix.length] == ' ') {
                    return kind to query.substring(prefix.length + 1)
                }
            } else if (query.startsWith(prefix)) {
                return kind to query.substring(prefix.length)
            }
        }
        return null to query
    }

    /** Valido se ogni prefisso è non vuoto, senza spazi, lungo al massimo 4 caratteri e diverso dagli altri. */
    fun valid(set: SearchPrefixSet): Boolean {
        val all = PrefixKind.entries.map { set.of(it).trim().lowercase() }
        return all.all { it.isNotEmpty() && it.length <= 4 && ' ' !in it } && all.distinct().size == all.size
    }

    private fun isWord(prefix: String) = prefix.any { it.isLetterOrDigit() }
}

/** Link di Spotify che funzionano senza internet: si aprono nell'app installata. */
object SpotifyLinks {
    const val PACKAGE = "com.spotify.music"

    /** "spotify:search:..." con il testo codificato, spazi compresi. */
    fun search(text: String): String = "spotify:search:" + URLEncoder.encode(text.trim(), "UTF-8").replace("+", "%20")
}
