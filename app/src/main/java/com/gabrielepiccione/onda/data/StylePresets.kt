// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import android.app.WallpaperManager
import android.content.Context
import androidx.annotation.StringRes
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.util.WallpaperGen
import com.gabrielepiccione.onda.util.WallpaperKind
import com.gabrielepiccione.onda.util.WallpaperSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Preset di stile: sfondo generato + carattere + orologio + icone + colori.
 * Con "colori dinamici" attivi, Material You ricava la palette dallo sfondo appena applicato.
 */
data class StylePreset(
    val id: String,
    @StringRes val nameRes: Int,
    val wallpaper: WallpaperSpec,
    val font: FontChoice,
    val clockStyle: ClockStyle,
    val clockAlign: HAlign = HAlign.START,
    val themeMode: ThemeMode,
    val accent: Long,
    val themedIcons: Boolean = false,
    val hideFavoriteNames: Boolean = false,
    val iconSize: Float = 1f,
    val wallpaperDim: Float = 0.15f,
) {
    /** Applica le impostazioni (non lo sfondo). */
    fun applyTo(s: Settings): Settings = s.copy(
        font = font,
        clockStyle = clockStyle,
        clockAlign = clockAlign,
        themeMode = themeMode,
        accent = accent,
        themedIcons = themedIcons,
        hideFavoriteNames = hideFavoriteNames,
        iconSize = iconSize,
        wallpaperDim = wallpaperDim,
        showClock = true,
        stylePreset = id,
    )
}

object StylePresets {

    val all: List<StylePreset> = listOf(
        StylePreset(
            "alba", R.string.preset_alba,
            WallpaperSpec(WallpaperKind.MESH, listOf(0xFFFFE3D3, 0xFFFFB4A2, 0xFFE5989B, 0xFFFFD6A5), seed = 3),
            FontChoice.SERIF, ClockStyle.LARGE_THIN, HAlign.START, ThemeMode.LIGHT, 0xFFE5989B,
        ),
        StylePreset(
            "fondale", R.string.preset_fondale,
            WallpaperSpec(WallpaperKind.WAVES, listOf(0xFF06222E, 0xFF0B3C4D, 0xFF0F5A6B, 0xFF14828A), seed = 11),
            FontChoice.SANS, ClockStyle.STACKED, HAlign.START, ThemeMode.DARK, 0xFF7FE3D1, themedIcons = true,
        ),
        StylePreset(
            "notte", R.string.preset_notte,
            WallpaperSpec(WallpaperKind.SOLID, listOf(0xFF000000)),
            FontChoice.MONO, ClockStyle.OUTLINE, HAlign.CENTER, ThemeMode.DARK, 0xFFB0B0B0, themedIcons = true, wallpaperDim = 0f,
        ),
        StylePreset(
            "carta", R.string.preset_carta,
            WallpaperSpec(WallpaperKind.GRADIENT, listOf(0xFFF7F3EA, 0xFFEDE6D6), grain = true),
            FontChoice.SERIF, ClockStyle.WORDS, HAlign.START, ThemeMode.LIGHT, 0xFF8D6E63, wallpaperDim = 0f,
        ),
        StylePreset(
            "lavanda", R.string.preset_lavanda,
            WallpaperSpec(WallpaperKind.MESH, listOf(0xFFEDE7F6, 0xFFC5B3F0, 0xFFB3D1F0, 0xFFF0C5E0), seed = 21),
            FontChoice.SANS, ClockStyle.GRADIENT, HAlign.CENTER, ThemeMode.LIGHT, 0xFFC3A6FF,
        ),
        StylePreset(
            "dune", R.string.preset_dune,
            WallpaperSpec(WallpaperKind.DUNES, listOf(0xFFFFE0B2, 0xFFFFCC80, 0xFFE0A96D, 0xFFC68642, 0xFF9C6644), seed = 5),
            FontChoice.SERIF, ClockStyle.BOLD, HAlign.START, ThemeMode.LIGHT, 0xFFC68642,
        ),
        StylePreset(
            "bosco", R.string.preset_bosco,
            WallpaperSpec(WallpaperKind.BLOBS, listOf(0xFF0F1F17, 0xFF1F4D36, 0xFF2E7D57, 0xFF6BA368), seed = 9),
            FontChoice.SANS, ClockStyle.ANALOG, HAlign.START, ThemeMode.DARK, 0xFFA5D6A7, themedIcons = true,
        ),
        StylePreset(
            "retro", R.string.preset_retro,
            WallpaperSpec(WallpaperKind.SOLID, listOf(0xFFE9E4D4), grain = true),
            FontChoice.MONO, ClockStyle.MONO, HAlign.START, ThemeMode.LIGHT, 0xFF5D5A50, wallpaperDim = 0f,
        ),
        StylePreset(
            "corallo", R.string.preset_corallo,
            WallpaperSpec(WallpaperKind.WAVES, listOf(0xFFFFF1EC, 0xFFFFC2B0, 0xFFFF9C8A, 0xFFF06E5A), seed = 17),
            FontChoice.SANS, ClockStyle.LARGE_THIN, HAlign.START, ThemeMode.LIGHT, 0xFFFF7A66,
        ),
        StylePreset(
            "ghiacciaio", R.string.preset_ghiacciaio,
            WallpaperSpec(WallpaperKind.MESH, listOf(0xFF0B1A2A, 0xFF16365C, 0xFF3A6EA5, 0xFF9CC4E4), seed = 13),
            FontChoice.SYSTEM, ClockStyle.LARGE_THIN, HAlign.CENTER, ThemeMode.DARK, 0xFF8AB4F8, hideFavoriteNames = false,
        ),
        StylePreset(
            "vulcano", R.string.preset_vulcano,
            WallpaperSpec(WallpaperKind.DUNES, listOf(0xFF1A0A0A, 0xFF3D0F0F, 0xFF7A1F12, 0xFFB8401E, 0xFFE07B39), seed = 2),
            FontChoice.SANS, ClockStyle.BOLD, HAlign.START, ThemeMode.DARK, 0xFFFF8A50,
        ),
        StylePreset(
            "essenziale", R.string.preset_essenziale,
            WallpaperSpec(WallpaperKind.GRADIENT, listOf(0xFFF2F4F5, 0xFFE3E7EA)),
            FontChoice.SYSTEM, ClockStyle.STACKED, HAlign.START, ThemeMode.LIGHT, 0xFF5F7A8A, themedIcons = true,
            hideFavoriteNames = false, wallpaperDim = 0f,
        ),
    )

    fun byId(id: String): StylePreset? = all.firstOrNull { it.id == id }

    /** Genera lo sfondo alla risoluzione dello schermo e lo imposta (schermata home). */
    suspend fun applyWallpaper(context: Context, preset: StylePreset): Boolean = withContext(Dispatchers.Default) {
        runCatching {
            val wm = WallpaperManager.getInstance(context)
            val dm = context.resources.displayMetrics
            val w = wm.desiredMinimumWidth.takeIf { it > 0 } ?: dm.widthPixels
            val h = wm.desiredMinimumHeight.takeIf { it > 0 } ?: dm.heightPixels
            val bmp = WallpaperGen.render(preset.wallpaper, minOf(w, dm.widthPixels * 2), h.coerceAtLeast(dm.heightPixels))
            wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_SYSTEM)
            bmp.recycle()
        }.isSuccess
    }
}
