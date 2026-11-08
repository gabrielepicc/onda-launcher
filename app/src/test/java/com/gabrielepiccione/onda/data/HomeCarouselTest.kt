// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeCarouselTest {
    private val blocks = listOf(HomeBlocks.CLOCK, HomeBlocks.NOTIFICATIONS, HomeBlocks.MEDIA, HomeBlocks.AGENDA, HomeBlocks.APPS)

    @Test
    fun pagesFollowTheBlockOrderAndOnlyThoseWithContent() {
        assertEquals(
            listOf(CardPage.NOTIFICATIONS, CardPage.MEDIA, CardPage.AGENDA),
            HomeCarousel.pages(blocks, hasMedia = true, hasAgenda = true, hasNotifications = true),
        )
        // Nessun brano e nessuna notifica: resta solo l'agenda
        assertEquals(listOf(CardPage.AGENDA), HomeCarousel.pages(blocks, hasMedia = false, hasAgenda = true, hasNotifications = false))
        assertTrue(HomeCarousel.pages(listOf(HomeBlocks.CLOCK), true, true, true).isEmpty())
    }

    @Test
    fun theCarouselTakesThePlaceOfTheFirstCardBlock() {
        assertEquals(HomeBlocks.NOTIFICATIONS, HomeCarousel.anchorBlock(blocks))
        assertNull(HomeCarousel.anchorBlock(listOf(HomeBlocks.CLOCK, HomeBlocks.APPS)))
    }

    @Test
    fun whenMusicStartsItGoesToThePlayerUnlessTheUserIsDragging() {
        val pages = listOf(CardPage.AGENDA, CardPage.MEDIA)
        assertEquals(1, HomeCarousel.musicTarget(pages, wasPlaying = false, playing = true, userDragging = false))
        assertNull(HomeCarousel.musicTarget(pages, wasPlaying = false, playing = true, userDragging = true))
        // Già in riproduzione o in pausa: nessun salto
        assertNull(HomeCarousel.musicTarget(pages, wasPlaying = true, playing = true, userDragging = false))
        assertNull(HomeCarousel.musicTarget(pages, wasPlaying = true, playing = false, userDragging = false))
        assertNull(HomeCarousel.musicTarget(listOf(CardPage.AGENDA), false, true, false))
    }

    @Test
    fun dotsOnlyWithMoreThanOnePage() {
        assertFalse(HomeCarousel.showDots(1))
        assertTrue(HomeCarousel.showDots(2))
    }

    @Test
    fun atMostFourStackedIcons() {
        assertEquals(listOf("a", "b", "c", "d"), HomeCarousel.stackedIcons(listOf("a", "b", "a", "c", "d", "e")))
    }
}
