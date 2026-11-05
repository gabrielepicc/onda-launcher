// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/**
 * Riepilogo delle notifiche di un'app di messaggi: "5 messaggi da 2 chat" con l'ultimo messaggio sotto.
 * Esiste solo per le conversazioni e solo se i messaggi sono più d'uno; negli altri casi la riga
 * mostra la notifica più recente, come prima.
 */
data class ConversationSummary(
    val messages: Int,
    val chats: Int,
    /** Notifica più recente, da cui si prendono ultimo messaggio, orario e immagine. */
    val latest: NotificationItem,
) {
    /** Nome della chat quando ce n'è una sola. */
    val chatName: String get() = latest.replyTo.ifBlank { latest.title }

    /** Ultimo messaggio con il mittente davanti, per capire da chi arriva. */
    val lastLine: String
        get() {
            val last = latest.messages.lastOrNull()
            val text = last?.text ?: latest.text
            val sender = last?.sender.orEmpty()
            return if (sender.isNotBlank()) "$sender: $text" else text
        }

    companion object {
        fun of(items: List<NotificationItem>): ConversationSummary? {
            if (items.isEmpty() || items.any { it.messages.isEmpty() }) return null
            val messages = items.sumOf { it.messageCount.coerceAtLeast(1) }
            if (messages < 2) return null
            val latest = items.maxByOrNull { it.postTime } ?: return null
            return ConversationSummary(messages, items.size, latest)
        }
    }
}
