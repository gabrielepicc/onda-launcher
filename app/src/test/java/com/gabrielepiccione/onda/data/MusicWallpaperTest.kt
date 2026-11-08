// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicWallpaperTest {
    @Test
    fun withoutMusicOnlyTheBaseImageShows() {
        val p = MusicWallpaper.plan(hasBaseImage = true, playing = false, mode = MusicBackdropMode.MOTION, intensity = 0.7f)
        assertEquals(MusicWallpaper.Background.IMAGE, p.background)
        assertEquals(0f, p.musicAlpha, 0f)
        assertFalse(p.needsBaseImage)
    }

    @Test
    fun musicGoesOverTheBaseImage() {
        val p = MusicWallpaper.plan(true, playing = true, mode = MusicBackdropMode.MOTION, intensity = 0.7f)
        assertEquals(MusicWallpaper.Background.IMAGE, p.background)
        assertEquals(0.7f, p.musicAlpha, 0f)
    }

    @Test
    fun withoutABaseImageTheThemeGradientIsUsedAndTheUserIsInvited() {
        val p = MusicWallpaper.plan(false, playing = false, mode = MusicBackdropMode.MOTION, intensity = 0.7f)
        assertEquals(MusicWallpaper.Background.THEME_GRADIENT, p.background)
        assertTrue(p.needsBaseImage)
    }

    @Test
    fun backdropOffShowsNoMusicColors() {
        assertEquals(0f, MusicWallpaper.plan(true, playing = true, mode = MusicBackdropMode.OFF, intensity = 1f).musicAlpha, 0f)
    }

    @Test
    fun centerCropKeepsTheMiddle() {
        // Immagine orizzontale 2000×1000 su schermo verticale 1000×2000: si tiene la striscia centrale
        assertArrayEquals(intArrayOf(750, 0, 1250, 1000), MusicWallpaper.centerCrop(2000, 1000, 1000, 2000))
        // Immagine verticale più alta dello schermo: si tagliano sopra e sotto
        assertArrayEquals(intArrayOf(0, 500, 1000, 2500), MusicWallpaper.centerCrop(1000, 3000, 1000, 2000))
        // Stesse proporzioni: tutta l'immagine
        assertArrayEquals(intArrayOf(0, 0, 500, 1000), MusicWallpaper.centerCrop(500, 1000, 1000, 2000))
    }

    @Test
    fun sampleSizeNeverGoesBelowTheScreen() {
        assertEquals(1, MusicWallpaper.sampleSize(1080, 2400, 1080, 2400))
        assertEquals(2, MusicWallpaper.sampleSize(4320, 4800, 1080, 2400))
        assertEquals(4, MusicWallpaper.sampleSize(8000, 12000, 1080, 2400))
    }

    @Test
    fun lockScreenSupportDependsOnTheAndroidVersion() {
        assertEquals(MusicWallpaper.LockSupport.CHOOSE_IN_PREVIEW, MusicWallpaper.lockSupport(34))
        assertEquals(MusicWallpaper.LockSupport.WITH_HOME_UNLESS_SEPARATE_LOCK, MusicWallpaper.lockSupport(33))
    }

    @Test
    fun theFallbackIsNeverAFlatGrey() {
        val (a, b) = MusicWallpaper.fallbackColors(0xFF6750A4.toInt(), 0xFF006A6A.toInt(), dark = true)
        assertNotEquals(a, b)
        val (_, s, _) = Hsl.of(a)
        assertTrue(s > 0.1)
    }
}
