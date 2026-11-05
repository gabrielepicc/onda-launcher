// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConversationSummaryTest {

    private fun chat(key: String, time: Long, count: Int, title: String = "Chat $key", sender: String = "Ugo") =
        NotificationItem(
            key, "app", title, "ultimo", time, null, autoCancel = false, clearable = true,
            messages = List(count) { MessageLine(sender, "msg $it") },
            messageCount = count,
        )

    @Test
    fun noNotificationsMeansNoSummary() {
        assertNull(ConversationSummary.of(emptyList()))
    }

    @Test
    fun aSingleMessageKeepsTheNormalRow() {
        assertNull(ConversationSummary.of(listOf(chat("a", 10, 1))))
    }

    @Test
    fun severalMessagesInOneChat() {
        val s = ConversationSummary.of(listOf(chat("a", 10, 3, title = "Famiglia")))!!
        assertEquals(3, s.messages)
        assertEquals(1, s.chats)
        assertEquals("Famiglia", s.chatName)
    }

    @Test
    fun countsMessagesAndChatsAndTakesTheLatest() {
        val s = ConversationSummary.of(listOf(chat("a", 10, 3), chat("b", 30, 2)))!!
        assertEquals(5, s.messages)
        assertEquals(2, s.chats)
        assertEquals("b", s.latest.key)
        assertEquals("Ugo: msg 1", s.lastLine)
    }

    @Test
    fun notificationsThatAreNotConversationsAreNotSummarized() {
        val plain = NotificationItem("m", "app", "Mail", "Oggetto", 5, null, autoCancel = true, clearable = true)
        assertNull(ConversationSummary.of(listOf(chat("a", 10, 3), plain)))
    }

    @Test
    fun lastLineWithoutSenderIsJustTheText() {
        val s = ConversationSummary.of(listOf(chat("a", 10, 2, sender = "")))!!
        assertEquals("msg 1", s.lastLine)
    }
}
