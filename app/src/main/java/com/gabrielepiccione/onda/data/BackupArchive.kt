// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup in un unico file ZIP: le impostazioni più le risorse dell'utente (immagini delle icone e
 * font importati). Senza dipendenze Android, testabile su JVM.
 */
object BackupArchive {
    const val SETTINGS_ENTRY = "settings.json"
    private const val FILES_PREFIX = "files/"

    /** Sostituisce la cartella privata dell'app nei percorsi, che cambia da un telefono all'altro. */
    private const val ROOT_TOKEN = "onda-files:"

    /** Le uniche cartelle che un backup può scrivere. */
    val RESOURCE_DIRS = setOf("icons", "fonts", MusicWallpaper.DIR)

    private const val MAX_ENTRY_BYTES = 64L * 1024 * 1024

    fun toPortable(json: String, filesRoot: String): String = json.replace(filesRoot, ROOT_TOKEN)

    fun toLocal(json: String, filesRoot: String): String = json.replace(ROOT_TOKEN, filesRoot)

    /** I file ZIP iniziano con "PK"; i vecchi backup sono JSON. */
    fun isArchive(firstBytes: ByteArray): Boolean =
        firstBytes.size >= 2 && firstBytes[0] == 'P'.code.toByte() && firstBytes[1] == 'K'.code.toByte()

    /**
     * Percorso relativo in cui estrarre una voce, oppure null se non è una risorsa ammessa.
     * Un file di backup può arrivare da fuori: non deve poter scrivere altrove.
     */
    fun safeResourcePath(entryName: String): String? {
        if (!entryName.startsWith(FILES_PREFIX)) return null
        val parts = entryName.removePrefix(FILES_PREFIX).split('/')
        if (parts.size != 2) return null
        val (dir, name) = parts
        val valid = dir in RESOURCE_DIRS && name.isNotEmpty() && name != "." && name != ".." && '\\' !in name
        return if (valid) "$dir/$name" else null
    }

    /** [files]: percorso relativo ("icons/a.png") -> file da includere. */
    fun write(output: OutputStream, settingsJson: String, files: Map<String, File>) {
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(SETTINGS_ENTRY))
            zip.write(settingsJson.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            files.forEach { (path, file) ->
                zip.putNextEntry(ZipEntry(FILES_PREFIX + path))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /**
     * Legge l'archivio passando le risorse ammesse a [openTarget] e restituisce il JSON delle
     * impostazioni; null se manca. Le voci non ammesse vengono ignorate.
     * @throws IOException se una voce è troppo grande o il file è illeggibile.
     */
    fun read(input: InputStream, openTarget: (String) -> OutputStream): String? {
        var json: String? = null
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                if (entry.name == SETTINGS_ENTRY) {
                    val bytes = java.io.ByteArrayOutputStream()
                    copyLimited(zip, bytes)
                    json = bytes.toString("UTF-8")
                } else {
                    val path = safeResourcePath(entry.name) ?: continue
                    openTarget(path).use { copyLimited(zip, it) }
                }
            }
        }
        return json
    }

    private fun copyLimited(from: InputStream, to: OutputStream) {
        val buffer = ByteArray(8192)
        var total = 0L
        while (true) {
            val read = from.read(buffer)
            if (read < 0) return
            total += read
            if (total > MAX_ENTRY_BYTES) throw IOException("Voce del backup troppo grande")
            to.write(buffer, 0, read)
        }
    }
}
