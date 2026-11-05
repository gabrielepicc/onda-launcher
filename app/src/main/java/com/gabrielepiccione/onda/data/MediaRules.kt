// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import android.media.session.PlaybackState

/** Quando mostrare il player nella home: solo se qualcosa sta suonando (o è appena stato messo in pausa). */
object MediaRules {
    /** Dopo una pausa il player resta per questo tempo, così si può riprendere senza cercarlo. */
    const val PAUSE_GRACE_MS = 60_000L

    private val active = setOf(
        PlaybackState.STATE_PLAYING,
        PlaybackState.STATE_BUFFERING,
        PlaybackState.STATE_CONNECTING,
        PlaybackState.STATE_FAST_FORWARDING,
        PlaybackState.STATE_REWINDING,
        PlaybackState.STATE_SKIPPING_TO_NEXT,
        PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
        PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM,
    )

    fun isActive(state: Int?): Boolean = state in active

    /**
     * [pausedAt] = momento (ms, orologio monotono) in cui è iniziata la pausa, 0 se non è in pausa.
     * Fermo, errore o nessuno stato: nascosto subito.
     */
    fun visible(state: Int?, pausedAt: Long, now: Long): Boolean = when {
        isActive(state) -> true
        state == PlaybackState.STATE_PAUSED -> pausedAt > 0 && now - pausedAt < PAUSE_GRACE_MS
        else -> false
    }
}
