// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gabrielepiccione.onda.OndaApp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.UsageEvent
import com.gabrielepiccione.onda.data.UsageReminders
import com.gabrielepiccione.onda.data.UsageStatsMath
import com.gabrielepiccione.onda.data.UsageStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Promemoria durante l'uso delle app scelte: non blocca mai nulla, manda solo una notifica.
 * Gira nel servizio delle notifiche di Onda, mentre il launcher è in secondo piano; non fa
 * nulla se non ci sono app scelte o se lo schermo è spento. I dati letti restano sul telefono.
 */
class UsageReminderEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    /** Ultima soglia notificata per pacchetto. */
    private val notified = HashMap<String, Int>()

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                runCatching { tick() }
                runCatching { checkLimits() }
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    fun stop() = scope.cancel()

    private fun tick() {
        val settings = (context.applicationContext as OndaApp).settings.current
        if (settings.reminderApps.isEmpty() || !hasUsageAccess(context) || !canNotify(context)) return
        if (!context.getSystemService(PowerManager::class.java).isInteractive) return

        val now = System.currentTimeMillis()
        val events = readEvents(now - WINDOW_MS, now)
        settings.reminderApps.map(UsageReminders::packageOf).distinct().forEach { pkg ->
            val minutes = (UsageReminders.continuousMs(events, pkg, now) / 60_000L).toInt()
            if (minutes == 0) return@forEach
            // Un nuovo periodo d'uso fa ripartire le soglie
            val last = notified[pkg]?.takeIf { it <= minutes } ?: 0
            val threshold = UsageReminders.thresholdToNotify(minutes, settings.reminderStartMinutes, last)
            notified[pkg] = threshold ?: last
            if (threshold != null) notify(pkg, threshold)
        }
    }

    /** Giorno e pacchetti già avvisati per il limite: una volta al giorno per app. */
    private var limitDay = ""
    private val limitNotified = HashSet<String>()
    private val usage by lazy { UsageStatsRepository(context) }

    private suspend fun checkLimits() {
        val settings = (context.applicationContext as OndaApp).settings.current
        val limits = settings.appLimits.filterValues { it > 0 }
        if (limits.isEmpty() || !hasUsageAccess(context) || !canNotify(context)) return
        if (!context.getSystemService(PowerManager::class.java).isInteractive) return
        val today = java.time.LocalDate.now().toString()
        if (today != limitDay) {
            limitDay = today
            limitNotified.clear()
        }
        val now = System.currentTimeMillis()
        val events = readEvents(now - WINDOW_MS, now)
        val day = usage.lastDays(1).firstOrNull() ?: return
        limits.forEach { (pkg, minutes) ->
            if (pkg in limitNotified) return@forEach
            // Solo mentre l'app è in uso: chi l'ha già chiusa non ha bisogno di un avviso
            if (UsageReminders.continuousMs(events, pkg, now) == 0L) return@forEach
            val used = day.apps.firstOrNull { it.packageName == pkg }?.foregroundMs ?: 0L
            if (UsageStatsMath.overLimit(used, minutes)) {
                limitNotified += pkg
                notifyText(pkg, context.getString(R.string.limit_reached_text, minutes))
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun readEvents(from: Long, to: Long): List<UsageEvent> {
        val stats = context.getSystemService(UsageStatsManager::class.java).queryEvents(from, to) ?: return emptyList()
        val out = ArrayList<UsageEvent>()
        val event = UsageEvents.Event()
        while (stats.hasNextEvent()) {
            stats.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> out += UsageEvent(event.timeStamp, event.packageName, true)
                UsageEvents.Event.MOVE_TO_BACKGROUND -> out += UsageEvent(event.timeStamp, event.packageName, false)
            }
        }
        return out
    }

    // Il permesso è controllato da canNotify, nel giro di controllo
    @SuppressLint("MissingPermission")
    private fun notify(pkg: String, minutes: Int) = notifyText(pkg, context.getString(R.string.reminder_text, minutes))

    @SuppressLint("MissingPermission")
    private fun notifyText(pkg: String, text: String) {
        if (!canNotify(context)) return
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val pm = context.packageManager
        val label = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(label)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        manager.notify(pkg.hashCode(), notification)
    }

    companion object {
        private const val CHANNEL_ID = "usage_reminders"
        private const val CHECK_INTERVAL_MS = 60_000L

        /** L'ultima soglia è 4 ore: si guarda un po' più indietro per contare le interruzioni brevi. */
        private const val WINDOW_MS = 6 * 60 * 60_000L

        /** L'accesso ai dati di utilizzo si concede a mano nelle impostazioni di sistema. */
        fun hasUsageAccess(context: Context): Boolean {
            val ops = context.getSystemService(AppOpsManager::class.java)
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            } else {
                @Suppress("DEPRECATION")
                ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }

        fun canNotify(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
}
