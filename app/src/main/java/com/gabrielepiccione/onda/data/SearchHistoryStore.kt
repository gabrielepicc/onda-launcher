// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Cronologia delle app aperte dalla ricerca: solo pacchetto, conteggio e data, mai il testo digitato.
 * Sta in `noBackupFilesDir`: resta sul telefono e non finisce nei backup (né di Onda né di Android).
 */
class SearchHistoryStore(context: Context) {
    private val file = File(context.noBackupFilesDir, "search_history.tsv")
    private val _history = MutableStateFlow<Map<String, SearchStat>>(emptyMap())
    val history: StateFlow<Map<String, SearchStat>> = _history.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        _history.value = runCatching { decode(file.readText()) }.getOrDefault(emptyMap())
    }

    suspend fun record(packageName: String, now: Long = System.currentTimeMillis()) {
        _history.value = SearchRanking.record(_history.value, packageName, now)
        save()
    }

    suspend fun clear() {
        _history.value = emptyMap()
        withContext(Dispatchers.IO) { file.delete() }
    }

    private suspend fun save() = withContext(Dispatchers.IO) {
        runCatching { file.writeText(encode(_history.value)) }
    }

    companion object {
        fun encode(h: Map<String, SearchStat>): String =
            h.entries.joinToString("\n") { "${it.key}\t${it.value.count}\t${it.value.lastAt}" }

        fun decode(text: String): Map<String, SearchStat> = text.lineSequence().mapNotNull { line ->
            val p = line.split('\t')
            val count = p.getOrNull(1)?.toIntOrNull()
            val last = p.getOrNull(2)?.toLongOrNull()
            if (p.size == 3 && p[0].isNotBlank() && count != null && last != null) p[0] to SearchStat(count, last) else null
        }.toMap()
    }
}
