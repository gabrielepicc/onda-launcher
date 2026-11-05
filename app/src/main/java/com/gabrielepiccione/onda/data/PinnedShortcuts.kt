// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/** Prefisso delle voci "scorciatoia" nell'elenco dei preferiti. */
const val SHORTCUT_PREFIX = "shortcut:"

/** Scorciatoia aggiunta alla home da un'altra app ("Aggiungi alla schermata Home"). */
@Immutable
@Serializable
data class PinnedShortcut(
    val packageName: String,
    val shortcutId: String,
    val userSerial: Long,
    val label: String,
) {
    val key: String get() = "$packageName|$userSerial|$shortcutId"
}

/** Operazioni sulle scorciatoie della home, senza dipendenze Android (testabili su JVM). */
object PinnedShortcuts {

    /** Aggiunge in fondo ai preferiti; se c'è già, aggiorna solo il nome e non crea doppioni. */
    fun add(s: Settings, shortcut: PinnedShortcut): Settings {
        val entry = SHORTCUT_PREFIX + shortcut.key
        return s.copy(
            pinnedShortcuts = s.pinnedShortcuts.filterNot { it.key == shortcut.key } + shortcut,
            favorites = if (entry in s.favorites) s.favorites else s.favorites + entry,
        )
    }

    fun remove(s: Settings, key: String): Settings = s.copy(
        pinnedShortcuts = s.pinnedShortcuts.filterNot { it.key == key },
        favorites = s.favorites - (SHORTCUT_PREFIX + key),
    )

    /** Gli id da tenere fissati per un'app e un utente: il sistema li vuole tutti insieme. */
    fun idsFor(s: Settings, packageName: String, userSerial: Long): List<String> =
        s.pinnedShortcuts.filter { it.packageName == packageName && it.userSerial == userSerial }.map { it.shortcutId }

    /** Nel backup restano fuori: una scorciatoia è valida solo sul telefono che l'ha ricevuta. */
    fun forBackup(s: Settings): Settings = s.copy(
        pinnedShortcuts = emptyList(),
        favorites = s.favorites.filterNot { it.startsWith(SHORTCUT_PREFIX) },
    )

    /** Al ripristino si tengono quelle di questo telefono, in fondo ai preferiti del backup. */
    fun forRestore(parsed: Settings, now: Settings): Settings = parsed.copy(
        pinnedShortcuts = now.pinnedShortcuts,
        favorites = parsed.favorites.filterNot { it.startsWith(SHORTCUT_PREFIX) } +
            now.favorites.filter { it.startsWith(SHORTCUT_PREFIX) },
    )
}
