// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gabrielepiccione.onda.OndaApp
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Crea e ripristina i backup completi. Le funzioni fanno I/O: da chiamare fuori dal thread principale. */
object Backups {

    private fun resources(context: Context): Map<String, File> {
        val result = LinkedHashMap<String, File>()
        BackupArchive.RESOURCE_DIRS.forEach { dir ->
            File(context.filesDir, dir).listFiles().orEmpty().filter { it.isFile }.forEach { result["$dir/${it.name}"] = it }
        }
        return result
    }

    fun write(context: Context, settings: SettingsRepository, output: OutputStream) {
        val json = BackupArchive.toPortable(settings.exportJson(), context.filesDir.absolutePath)
        BackupArchive.write(output, json, resources(context))
    }

    /** Accetta il file unico e i vecchi backup in JSON. */
    fun restore(context: Context, settings: SettingsRepository, input: InputStream): Boolean {
        val buffered = BufferedInputStream(input)
        buffered.mark(2)
        val head = ByteArray(2)
        val read = buffered.read(head)
        buffered.reset()
        if (read < 2 || !BackupArchive.isArchive(head)) {
            return settings.importJson(buffered.readBytes().decodeToString())
        }
        val root = context.filesDir
        val json = BackupArchive.read(buffered) { path ->
            File(root, path).also { it.parentFile?.mkdirs() }.outputStream()
        } ?: return false
        return settings.importJson(BackupArchive.toLocal(json, root.absolutePath))
    }

    /** Vero se non c'era nulla da fare o se il backup è riuscito. */
    fun runAutoIfDue(context: Context, settings: SettingsRepository): Boolean {
        val today = LocalDate.now().toString()
        val current = settings.current
        if (!AutoBackup.isDue(current, today)) return true
        val done = AutoBackup.write(context, Uri.parse(current.autoBackupUri), today) { write(context, settings, it) }
        if (done) settings.edit { it.copy(autoBackupLastDay = today) }
        return done
    }

    /** Il controllo periodico esiste solo se il backup automatico è attivo. */
    fun syncSchedule(context: Context, enabled: Boolean) {
        val manager = WorkManager.getInstance(context)
        if (enabled) {
            manager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<AutoBackupWorker>(24, TimeUnit.HOURS).build(),
            )
        } else {
            manager.cancelUniqueWork(WORK_NAME)
        }
    }

    private const val WORK_NAME = "onda-auto-backup"
}

/** Backup automatico ogni 24 ore, anche se il launcher non viene riaperto. */
class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as OndaApp
        return if (Backups.runAutoIfDue(app, app.settings)) Result.success() else Result.retry()
    }
}
