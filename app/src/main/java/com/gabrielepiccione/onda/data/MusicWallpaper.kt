// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/**
 * Regole dello sfondo musicale di sistema, senza dipendenze Android (testabili su JVM).
 * Un live wallpaper sostituisce lo sfondo: senza un'immagine di base, a musica spenta resterebbe un fondo
 * piatto. Per questo l'utente sceglie l'immagine di base; se manca si usa un gradiente dai colori del tema.
 */
object MusicWallpaper {
    /** Cartella (dentro lo spazio privato dell'app) dell'immagine di base: entra nel backup. */
    const val DIR = "wallpaper"

    enum class Background { IMAGE, THEME_GRADIENT }

    /** Cosa disegnare: lo sfondo e, sopra, i colori della musica con questa opacità (0 = invisibili). */
    data class Plan(val background: Background, val musicAlpha: Float) {
        val needsBaseImage: Boolean get() = background == Background.THEME_GRADIENT
    }

    fun plan(hasBaseImage: Boolean, playing: Boolean, mode: MusicBackdropMode, intensity: Float): Plan = Plan(
        background = if (hasBaseImage) Background.IMAGE else Background.THEME_GRADIENT,
        musicAlpha = if (playing && mode != MusicBackdropMode.OFF) intensity.coerceIn(0.1f, 1f) else 0f,
    )

    /** Ritaglio centrato come lo sfondo normale: la parte dell'immagine [srcW]×[srcH] che riempie [dstW]×[dstH]. */
    fun centerCrop(srcW: Int, srcH: Int, dstW: Int, dstH: Int): IntArray {
        if (srcW <= 0 || srcH <= 0 || dstW <= 0 || dstH <= 0) return intArrayOf(0, 0, srcW.coerceAtLeast(0), srcH.coerceAtLeast(0))
        val srcRatio = srcW.toDouble() / srcH
        val dstRatio = dstW.toDouble() / dstH
        return if (srcRatio > dstRatio) {
            // Immagine più larga: si tagliano i lati
            val w = (srcH * dstRatio).toInt().coerceIn(1, srcW)
            val left = (srcW - w) / 2
            intArrayOf(left, 0, left + w, srcH)
        } else {
            val h = (srcW / dstRatio).toInt().coerceIn(1, srcH)
            val top = (srcH - h) / 2
            intArrayOf(0, top, srcW, top + h)
        }
    }

    /** Riduzione in potenze di 2 per caricare l'immagine senza superare (di molto) la misura dello schermo. */
    fun sampleSize(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Int {
        var sample = 1
        while (srcW / (sample * 2) >= dstW && srcH / (sample * 2) >= dstH) sample *= 2
        return sample
    }

    /** Cosa si può fare per la schermata di blocco su questa versione di Android. */
    enum class LockSupport {
        /** Android 14+: nell'anteprima di sistema si sceglie «Home e schermata di blocco». */
        CHOOSE_IN_PREVIEW,

        /** Prima di Android 14: il live wallpaper vale anche sul blocco, se non c'è un'immagine di blocco separata. */
        WITH_HOME_UNLESS_SEPARATE_LOCK,
    }

    fun lockSupport(sdk: Int): LockSupport = if (sdk >= 34) LockSupport.CHOOSE_IN_PREVIEW else LockSupport.WITH_HOME_UNLESS_SEPARATE_LOCK

    /** Gradiente di ripiego dai colori del tema (mai un grigio fisso): contenitore del primario → dell'accento. */
    fun fallbackColors(primarySeed: Int, accentSeed: Int, dark: Boolean): Pair<Int, Int> {
        val roles = ThemePalette.roles(primarySeed, accentSeed, dark)
        return roles.primaryContainer to roles.accentContainer
    }
}
