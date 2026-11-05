// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/** Un elemento della coda di riproduzione, come lo espone la sessione dell'app. */
data class QueueEntry(val id: Long, val title: String, val subtitle: String)

/**
 * Un'azione in più che l'app mette nella sessione (es. il cuore "Mi piace"): [id] è il nome che si
 * rimanda all'app, [iconRes] l'icona dentro le risorse dell'app stessa.
 */
data class MediaAction(val id: String, val name: String, val iconRes: Int)

/** Regole per azioni dell'app e prossimo brano, senza dipendenze Android (testabili su JVM). */
object MediaExtras {
    /** Nel player compatto c'è posto per una sola azione, in quello esteso per tre. */
    const val COMPACT_ACTIONS = 1
    const val FULL_ACTIONS = 3

    /** Il brano dopo quello attivo; null se la coda manca, se l'attivo non c'è o se è l'ultimo. */
    fun nextInQueue(queue: List<QueueEntry>, activeId: Long): QueueEntry? {
        val at = queue.indexOfFirst { it.id == activeId }
        if (at < 0) return null
        return queue.getOrNull(at + 1)?.takeIf { it.title.isNotBlank() }
    }

    /** Le azioni da mostrare, nell'ordine dato dall'app, senza doppioni e senza quelle senza icona. */
    fun visibleActions(actions: List<MediaAction>, compact: Boolean): List<MediaAction> =
        actions.filter { it.iconRes != 0 && it.id.isNotBlank() }
            .distinctBy { it.id }
            .take(if (compact) COMPACT_ACTIONS else FULL_ACTIONS)

    /** Testo della riga "Dopo": titolo, e artista se c'è. */
    fun nextLine(next: QueueEntry): String =
        if (next.subtitle.isBlank()) next.title else "${next.title} — ${next.subtitle}"
}
