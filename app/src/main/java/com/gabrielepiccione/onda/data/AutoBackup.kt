// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.time.LocalDate

/**
 * Backup automatico giornaliero nella cartella scelta dall'utente (Storage Access Framework):
 * nessun permesso di archiviazione, nessuna rete. Tiene gli ultimi [KEEP] file.
 */
object AutoBackup {

    private const val PREFIX = "onda-auto-"
    private const val KEEP = 7
    private const val DAY_MS = 24 * 60 * 60 * 1000L

    fun isDue(settings: Settings, now: Long = System.currentTimeMillis()): Boolean =
        settings.autoBackupUri.isNotBlank() && now - settings.lastAutoBackup >= DAY_MS

    /** Scrive il backup di oggi e rimuove i più vecchi. Restituisce true se riuscito. */
    fun run(context: Context, treeUri: String, json: String): Boolean = runCatching {
        val tree = Uri.parse(treeUri)
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val name = "$PREFIX${LocalDate.now()}.json"
        // Stesso giorno: sostituisce il file esistente
        children(context, tree).filter { it.second == name }.forEach { DocumentsContract.deleteDocument(context.contentResolver, it.first) }
        val doc = DocumentsContract.createDocument(context.contentResolver, parent, "application/json", name) ?: return false
        context.contentResolver.openOutputStream(doc)?.use { it.write(json.toByteArray()) } ?: return false
        children(context, tree)
            .filter { it.second.startsWith(PREFIX) }
            .sortedByDescending { it.second }
            .drop(KEEP)
            .forEach { DocumentsContract.deleteDocument(context.contentResolver, it.first) }
        true
    }.getOrDefault(false)

    private fun children(context: Context, tree: Uri): List<Pair<Uri, String>> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val out = mutableListOf<Pair<Uri, String>>()
        context.contentResolver.query(
            childrenUri,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0) ?: continue
                out += DocumentsContract.buildDocumentUriUsingTree(tree, id) to (c.getString(1) ?: "")
            }
        }
        return out
    }
}
