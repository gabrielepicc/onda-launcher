// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Tema dell'utente: tutto ciò che riguarda l'aspetto (colori, testo, icone, orologio),
 * senza dati personali (preferiti, cartelle, app nascoste restano fuori).
 */
@Immutable
@Serializable
data class LookTheme(
    val id: String,
    val name: String,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val accent: Long = 0xFF7FE3D1,
    val font: FontChoice = FontChoice.SYSTEM,
    val iconPack: String = "",
    val iconShape: String = "SYSTEM",
    val iconStyle: String = "ORIGINAL",
    val themedIcons: Boolean = false,
    val iconSize: Float = 1f,
    val showIcons: Boolean = true,
    val clockStyle: ClockStyle = ClockStyle.LARGE_THIN,
    val clockScale: Float = 1f,
    val clockAlign: HAlign = HAlign.START,
    val textColor: Long = 0,
    val themeAccent: Long = 0,
    val listAlign: HAlign = HAlign.START,
    val listFontWeight: Int = 400,
    val listLetterSpacing: Float = 0f,
    val textShadow: Boolean = false,
    val wallpaperDim: Float = 0.2f,
    val hideFavoriteNames: Boolean = false,
    val mediaCompact: Boolean = true,
    val customFontList: String = "",
    val customFontClock: String = "",
) {
    /** Applica il tema alle impostazioni, lasciando intatti i dati personali. */
    fun applyTo(s: Settings): Settings = s.copy(
        themeMode = themeMode, dynamicColor = dynamicColor, accent = accent, themeAccent = themeAccent, font = font,
        iconPack = iconPack, iconShape = iconShape, iconStyle = iconStyle, themedIcons = themedIcons,
        iconSize = iconSize, showIcons = showIcons, clockStyle = clockStyle, clockScale = clockScale,
        clockAlign = clockAlign, textColor = textColor, listAlign = listAlign, listFontWeight = listFontWeight,
        listLetterSpacing = listLetterSpacing, textShadow = textShadow, wallpaperDim = wallpaperDim,
        hideFavoriteNames = hideFavoriteNames, mediaCompact = mediaCompact,
        customFontList = customFontList, customFontClock = customFontClock,
        stylePreset = "theme:$id",
    )

    companion object {
        /** Cattura l'aspetto corrente come nuovo tema. */
        fun capture(s: Settings, id: String, name: String) = LookTheme(
            id = id, name = name, themeMode = s.themeMode, dynamicColor = s.dynamicColor, accent = s.accent, themeAccent = s.themeAccent,
            font = s.font, iconPack = s.iconPack, iconShape = s.iconShape, iconStyle = s.iconStyle,
            themedIcons = s.themedIcons, iconSize = s.iconSize, showIcons = s.showIcons, clockStyle = s.clockStyle,
            clockScale = s.clockScale, clockAlign = s.clockAlign, textColor = s.textColor, listAlign = s.listAlign,
            listFontWeight = s.listFontWeight, listLetterSpacing = s.listLetterSpacing, textShadow = s.textShadow,
            wallpaperDim = s.wallpaperDim, hideFavoriteNames = s.hideFavoriteNames, mediaCompact = s.mediaCompact,
            customFontList = s.customFontList, customFontClock = s.customFontClock,
        )
    }
}

/**
 * File `.onda-theme`: il tema più gli eventuali font incorporati in Base64,
 * così chi lo importa ottiene lo stesso aspetto anche senza avere i file.
 */
@Serializable
data class ThemeFile(
    val format: String = "onda-theme",
    val version: Int = 1,
    val theme: LookTheme,
    /** nome file -> contenuto Base64 */
    val fonts: Map<String, String> = emptyMap(),
) {
    companion object {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true; prettyPrint = true }
        fun decode(text: String): ThemeFile? = runCatching { json.decodeFromString(serializer(), text) }
            .getOrNull()?.takeIf { it.format == "onda-theme" }
        fun encode(file: ThemeFile): String = json.encodeToString(serializer(), file)
    }
}
