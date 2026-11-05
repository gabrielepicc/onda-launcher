// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import android.media.session.PlaybackState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaRulesTest {

    @Test
    fun playingAndBufferingAreVisible() {
        assertTrue(MediaRules.visible(PlaybackState.STATE_PLAYING, 0, 0))
        assertTrue(MediaRules.visible(PlaybackState.STATE_BUFFERING, 0, 0))
    }

    @Test
    fun pausedStaysOnlyForTheGracePeriod() {
        assertTrue(MediaRules.visible(PlaybackState.STATE_PAUSED, 1_000, 1_000 + MediaRules.PAUSE_GRACE_MS - 1))
        assertFalse(MediaRules.visible(PlaybackState.STATE_PAUSED, 1_000, 1_000 + MediaRules.PAUSE_GRACE_MS))
        assertFalse(MediaRules.visible(PlaybackState.STATE_PAUSED, 0, 5_000))
    }

    @Test
    fun stoppedOrUnknownIsHidden() {
        assertFalse(MediaRules.visible(PlaybackState.STATE_STOPPED, 0, 0))
        assertFalse(MediaRules.visible(PlaybackState.STATE_NONE, 0, 0))
        assertFalse(MediaRules.visible(PlaybackState.STATE_ERROR, 0, 0))
        assertFalse(MediaRules.visible(null, 0, 0))
    }
}
