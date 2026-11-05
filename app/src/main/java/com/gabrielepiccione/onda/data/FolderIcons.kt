// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/** Icone e misure delle cartelle, senza dipendenze Android (testabili su JVM). */
object FolderIcons {
    const val AUTO = ""
    private const val APP = "app:"
    private const val SYMBOL = "sym:"

    /** Chiave usata per scegliere un'immagine per la cartella con lo stesso selettore delle icone delle app. */
    const val IMAGE_KEY_PREFIX = "folder-icon:"

    /** Simboli disponibili, nell'ordine in cui compaiono nel selettore. */
    val SYMBOLS = listOf(
        "folder", "star", "favorite", "work", "home", "school", "games", "music", "photo", "movie",
        "chat", "mail", "shopping", "wallet", "travel", "car", "food", "fitness", "health", "pets",
        "book", "news", "code", "tools", "lightbulb", "cloud", "sport", "family",
        "map", "bank", "card", "receipt", "phone", "calendar", "settings", "security", "coffee", "beach",
        "bike", "train", "palette", "brush", "science", "wifi", "tv", "headphones", "flower", "baby",
        "group", "gift", "bolt", "moon", "sun", "language", "bookmark", "puzzle",
    )

    /** Altezze ammesse per i widget dentro una cartella (dp). */
    val HEIGHT_RANGE = 72..480

    sealed interface Kind {
        data object Auto : Kind
        data class App(val key: String) : Kind
        data class Symbol(val name: String) : Kind
        data class Image(val path: String) : Kind
    }

    fun app(key: String) = APP + key
    fun symbol(name: String) = SYMBOL + name
    fun image(path: String) = ICON_FILE_PREFIX + path

    fun parse(spec: String): Kind = when {
        spec.startsWith(APP) -> Kind.App(spec.removePrefix(APP))
        spec.startsWith(SYMBOL) -> spec.removePrefix(SYMBOL).takeIf { it in SYMBOLS }?.let { Kind.Symbol(it) } ?: Kind.Auto
        spec.startsWith(ICON_FILE_PREFIX) -> Kind.Image(spec.removePrefix(ICON_FILE_PREFIX))
        else -> Kind.Auto
    }

    fun setIcon(s: Settings, folderId: String, spec: String): Settings {
        val f = s.folders[folderId] ?: return s
        return s.copy(folders = s.folders + (folderId to f.copy(icon = spec)))
    }

    fun setWidgetHeight(s: Settings, folderId: String, dp: Int?): Settings {
        val f = s.folders[folderId] ?: return s
        val clamped = dp?.coerceIn(HEIGHT_RANGE.first, HEIGHT_RANGE.last)
        return s.copy(folders = s.folders + (folderId to f.copy(widgetHeightDp = clamped)))
    }

    /** Id della cartella da una chiave del selettore d'immagini, se è di una cartella. */
    fun folderOfImageKey(key: String): String? = key.takeIf { it.startsWith(IMAGE_KEY_PREFIX) }?.removePrefix(IMAGE_KEY_PREFIX)
}
