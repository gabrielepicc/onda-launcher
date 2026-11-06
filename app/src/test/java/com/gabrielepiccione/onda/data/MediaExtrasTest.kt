// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaExtrasTest {
    private val queue = listOf(QueueEntry(10, "Uno", "A"), QueueEntry(11, "Due", "B"), QueueEntry(12, "Tre", ""))

    @Test
    fun nextIsTheEntryAfterTheActiveOne() {
        assertEquals("Due", MediaExtras.nextInQueue(queue, 10)?.title)
        assertEquals("Tre", MediaExtras.nextInQueue(queue, 11)?.title)
    }

    @Test
    fun noNextWhenLastMissingOrEmpty() {
        assertNull(MediaExtras.nextInQueue(queue, 12))
        assertNull(MediaExtras.nextInQueue(queue, 99))
        assertNull(MediaExtras.nextInQueue(emptyList(), 10))
    }

    @Test
    fun nextLineAddsTheArtistOnlyWhenThere() {
        assertEquals("Due — B", MediaExtras.nextLine(queue[1]))
        assertEquals("Tre", MediaExtras.nextLine(queue[2]))
    }

    @Test
    fun actionsAreLimitedDedupedAndNeedAnIcon() {
        val actions = listOf(
            MediaAction("like", "Mi piace", 1),
            MediaAction("like", "Mi piace", 1),
            MediaAction("noicon", "Senza icona", 0),
            MediaAction("shuffle", "Casuale", 2),
            MediaAction("repeat", "Ripeti", 3),
            MediaAction("radio", "Radio", 4),
        )
        assertEquals(listOf("like"), MediaExtras.visibleActions(actions, compact = true).map { it.id })
        // Senza "mi piace" la versione compatta salta comunque la ripetizione casuale
        assertEquals(listOf("repeat"), MediaExtras.visibleActions(actions.filter { it.id != "like" }, compact = true).map { it.id })
        assertEquals(listOf("like", "shuffle", "repeat"), MediaExtras.visibleActions(actions, compact = false).map { it.id })
    }
}
