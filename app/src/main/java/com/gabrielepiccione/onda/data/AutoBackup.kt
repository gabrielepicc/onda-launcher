// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.OutputStream

/**
 * Backup automatico giornaliero nella cartella scelta dall'utente: un file al giorno,
 * conservando solo gli ultimi [KEEP]. Onda non si collega a internet: il file resta dove
 * l'utente ha deciso di salvarlo.
 */
object AutoBackup {
    private const val PREFIX = "onda-auto-"
    private const val SUFFIX = ".zip"

    /** Prima della 0.10 il backup automatico era un JSON: anche quelli contano tra le copie da tenere. */
    private const val OLD_SUFFIX = ".json"
    private const val KEEP = 7

    fun fileName(day: String) = "$PREFIX$day$SUFFIX"

    /** Vero se la cartella è impostata e oggi non è stato ancora fatto un backup. */
    fun isDue(s: Settings, today: String): Boolean = s.autoBackupUri.isNotBlank() && s.autoBackupLastDay != today

    /** File da eliminare: solo quelli di Onda, oltre i più recenti [keep] (il nome contiene la data). */
    fun toDelete(names: List<String>, keep: Int = KEEP): List<String> =
        names.filter { it.startsWith(PREFIX) && (it.endsWith(SUFFIX) || it.endsWith(OLD_SUFFIX)) }.sortedDescending().drop(keep)

    /** Scrive il backup di [today] e pulisce i vecchi. Da chiamare fuori dal thread principale. */
    fun write(context: Context, treeUri: Uri, today: String, content: (OutputStream) -> Unit): Boolean = runCatching {
        val resolver = context.contentResolver
        val treeId = DocumentsContract.getTreeDocumentId(treeUri)
        val folder = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeId)
        val file = DocumentsContract.createDocument(resolver, folder, "application/zip", fileName(today))
            ?: error("Impossibile creare il file")
        resolver.openOutputStream(file)?.use(content) ?: error("Impossibile scrivere il file")

        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeId)
        val found = HashMap<String, String>()
        resolver.query(
            children,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(1) ?: continue
                found[name] = c.getString(0) ?: continue
            }
        }
        toDelete(found.keys.toList()).forEach { name ->
            found[name]?.let { id -> DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(treeUri, id)) }
        }
    }.isSuccess
}
