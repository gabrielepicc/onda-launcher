// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.service.notification.StatusBarNotification
import android.app.Notification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/**
 * Legge i dati di utilizzo di Android (permesso "Accesso ai dati di utilizzo", già usato dai promemoria)
 * e li riassume con [UsageStatsMath]. Tutto resta sul telefono: niente viene salvato fuori dall'app.
 * Android conserva gli eventi dettagliati per circa una settimana, quindi i confronti più lunghi
 * possono avere giorni vuoti.
 */
class UsageStatsRepository(context: Context) {
    private val app = context.applicationContext
    private val manager = app.getSystemService(UsageStatsManager::class.java)

    /** App che non contano: il launcher stesso e quelli di sistema, come fa il benessere digitale. */
    private val ignored: Set<String> by lazy {
        val homes = runCatching {
            app.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
                .map { it.activityInfo.packageName }
        }.getOrDefault(emptyList())
        (homes + app.packageName + IGNORED_SYSTEM).toSet()
    }

    /** Gli ultimi [count] giorni, dal più vecchio a oggi. */
    suspend fun lastDays(count: Int): List<DayUsage> = withContext(Dispatchers.Default) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val now = System.currentTimeMillis()
        val firstDay = today.minusDays((count - 1).toLong())
        val from = firstDay.atStartOfDay(zone).toInstant().toEpochMilli()
        // Si legge da un po' prima per sapere quale app era aperta a mezzanotte
        val records = read(from - LOOK_BEHIND_MS, now)
        (0 until count).map { i ->
            val date = firstDay.plusDays(i.toLong())
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayRecords = records.filter { it.time >= start - LOOK_BEHIND_MS && it.time < start + UsageStatsMath.DAY_MS }
            UsageStatsMath.day(dayRecords, start, now, ignored)
        }
    }

    /** Quanto è stata usata oggi un'app, per i limiti giornalieri. */
    suspend fun todayMs(packageName: String): Long = withContext(Dispatchers.Default) {
        lastDays(1).firstOrNull()?.apps?.firstOrNull { it.packageName == packageName }?.foregroundMs ?: 0L
    }

    @Suppress("DEPRECATION")
    private fun read(from: Long, to: Long): List<UsageRecord> {
        val events = runCatching { manager.queryEvents(from, to) }.getOrNull() ?: return emptyList()
        val out = ArrayList<UsageRecord>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val kind = when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> UsageKind.FOREGROUND
                UsageEvents.Event.MOVE_TO_BACKGROUND -> UsageKind.BACKGROUND
                // Costanti di Android 9: i valori sono fissi, sulle versioni prima semplicemente non arrivano
                EVENT_SCREEN_INTERACTIVE -> UsageKind.SCREEN_ON
                EVENT_SCREEN_NON_INTERACTIVE -> UsageKind.SCREEN_OFF
                EVENT_KEYGUARD_HIDDEN -> UsageKind.UNLOCK
                else -> null
            } ?: continue
            out += UsageRecord(event.timeStamp, event.packageName.orEmpty(), kind)
        }
        return out
    }

    private companion object {
        const val LOOK_BEHIND_MS = 3 * UsageStatsMath.HOUR_MS
        const val EVENT_SCREEN_INTERACTIVE = 15
        const val EVENT_SCREEN_NON_INTERACTIVE = 16
        const val EVENT_KEYGUARD_HIDDEN = 18
        val IGNORED_SYSTEM = listOf("com.android.systemui", "android", "com.android.permissioncontroller")
    }
}

/**
 * Notifiche ricevute per giorno e per app. Android non le conta, quindi lo fa il servizio delle
 * notifiche di Onda: solo un numero per app, mai il contenuto, per gli ultimi [KEEP_DAYS] giorni.
 */
object NotificationCounter {
    private const val PREFS = "notification_counts"
    private const val KEEP_DAYS = 35L

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Conta una notifica appena arrivata, se è una vera novità (non un aggiornamento silenzioso o un riepilogo). */
    fun record(context: Context, sbn: StatusBarNotification) {
        val n = sbn.notification ?: return
        if (sbn.packageName == context.packageName || sbn.isOngoing) return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0 || n.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0) return
        val today = LocalDate.now().toString()
        val p = prefs(context)
        val key = today + "|" + sbn.packageName
        val editor = p.edit().putInt(key, p.getInt(key, 0) + 1)
        // Una volta al giorno si buttano i conteggi troppo vecchi
        if (p.getString("pruned", "") != today) {
            val oldest = LocalDate.now().minusDays(KEEP_DAYS).toString()
            p.all.keys.filter { it.contains('|') && it.substringBefore('|') < oldest }.forEach(editor::remove)
            editor.putString("pruned", today)
        }
        editor.apply()
    }

    /** Notifiche ricevute in un giorno ("AAAA-MM-GG"), per app. */
    fun forDay(context: Context, day: String): Map<String, Int> =
        prefs(context).all.mapNotNull { (k, v) ->
            if (k.startsWith("$day|") && v is Int) k.substringAfter('|') to v else null
        }.toMap()
}
