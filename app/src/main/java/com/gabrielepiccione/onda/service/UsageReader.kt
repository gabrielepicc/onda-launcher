// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.gabrielepiccione.onda.data.LaunchLog
import com.gabrielepiccione.onda.data.UsageEventCtx
import com.gabrielepiccione.onda.data.UsageProfiles
import com.gabrielepiccione.onda.data.UsageReminders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Legge l'uso del telefono per proporre profili: le aperture dalle statistiche di Android (permesso
 * "Accesso ai dati di utilizzo", chiesto solo da "Suggerisci profili") unite a quelle che Onda stessa
 * registra aprendo le app, che portano anche il contesto (cuffie, carica, Wi-Fi). Tutto resta sul telefono.
 * Android conserva gli eventi dettagliati solo per una decina di giorni: il resto della storia viene dal
 * registro di Onda, che parte da quando la funzione è attiva.
 */
class UsageReader(private val context: Context) {

    fun hasAccess(): Boolean = UsageReminderEngine.hasUsageAccess(context)

    /**
     * Le aperture degli ultimi [UsageProfiles.WINDOW_DAYS] giorni, solo di app tra [launchable].
     * Senza il permesso restano quelle del registro di Onda, che già bastano a capire se c'è abbastanza storia.
     */
    suspend fun read(now: Long, launchable: Set<String>): List<UsageEventCtx> = withContext(Dispatchers.IO) {
        val from = now - UsageProfiles.WINDOW_DAYS * DAY_MS
        val own = LaunchLog.read(context)
            .filter { it.time in from..now }
            .map { UsageEventCtx(UsageReminders.packageOf(it.appKey), it.time, it.headphones, it.charging, car = false, wifiHash = it.wifiHash) }
        val system = if (hasAccess()) readSystem(from, now) else emptyList()
        // Un'apertura vista da entrambe è la stessa: si tiene quella del registro, che porta il contesto
        val extra = system.filter { s -> own.none { it.packageName == s.packageName && abs(it.time - s.time) < MERGE_MS } }
        (own + extra).filter { it.packageName in launchable }
    }

    private fun readSystem(from: Long, to: Long): List<UsageEventCtx> {
        val events = runCatching { context.getSystemService(UsageStatsManager::class.java).queryEvents(from, to) }.getOrNull()
            ?: return emptyList()
        val out = ArrayList<UsageEventCtx>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) out += UsageEventCtx(event.packageName, event.timeStamp)
        }
        return out
    }

    private companion object {
        const val DAY_MS = 24 * 3_600_000L
        const val MERGE_MS = 3 * 60_000L
    }
}
