// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.BatteryManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gabrielepiccione.onda.OndaApp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.Actions
import com.gabrielepiccione.onda.data.Automation
import com.gabrielepiccione.onda.data.Automations
import com.gabrielepiccione.onda.data.EnvSnapshot
import com.gabrielepiccione.onda.data.GestureAction
import com.gabrielepiccione.onda.data.HomeLights
import com.gabrielepiccione.onda.data.HomePresence
import com.gabrielepiccione.onda.data.ProfileOverride
import com.gabrielepiccione.onda.data.Settings
import com.gabrielepiccione.onda.util.ActionRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Esegue le automazioni «se… allora»: ogni pochi secondi, e solo se ce n'è una accesa, fotografa lo stato del
 * telefono (Wi-Fi di casa, cuffie, carica, dispositivi, ora, sole) e fa partire quelle il cui trigger è scattato
 * dall'ultima fotografia. Tutto sul telefono, senza internet. Gira finché il processo di Onda è vivo (lo tengono
 * vivo l'accesso alle notifiche e l'accessibilità, se attivi).
 */
class AutomationEngine(private val app: OndaApp) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null

    fun start() {
        app.settings.settings
            .map { s -> s.automations.any { it.enabled && it.isUsable } }
            .distinctUntilChanged()
            .onEach { on ->
                loop?.cancel()
                loop = if (on) scope.launch { watch() } else null
            }
            .launchIn(scope)
    }

    private suspend fun watch() {
        var previous: EnvSnapshot? = null
        while (scope.isActive) {
            val settings = app.settings.current
            val now = snapshot(settings)
            Automations.fired(settings.automations, previous, now).forEach { run(it) }
            previous = now
            delay(POLL_MS)
        }
    }

    private fun snapshot(s: Settings): EnvSnapshot {
        val zone = ZoneId.systemDefault()
        val time = LocalDateTime.now()
        val sun = if (s.homeLatitude == 0.0 && s.homeLongitude == 0.0) null else HomeLights.sunTimes(LocalDate.now(), s.homeLatitude, s.homeLongitude)
        fun minuteOf(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone).let { it.hour * 60 + it.minute }
        val battery = app.getSystemService(BatteryManager::class.java)
        return EnvSnapshot(
            atHome = s.homeWifi.isNotBlank() && HomePresence.currentSsid(app) == s.homeWifi,
            headphones = app.audio.headphonesConnected.value,
            charging = battery?.isCharging == true,
            devices = app.audio.connectedNames.value,
            minuteOfDay = time.hour * 60 + time.minute,
            dayOfWeek = time.dayOfWeek.value,
            sunriseMinute = sun?.let { minuteOf(it.sunrise) },
            sunsetMinute = sun?.let { minuteOf(it.sunset) },
        )
    }

    private fun run(a: Automation) {
        a.actions.forEach { encoded ->
            val choice = Actions.decode(encoded)
            when (choice.action) {
                GestureAction.OPEN_APP -> app.apps.launchByKey(choice.arg)
                GestureAction.OPEN_SHORTCUT -> Actions.parseShortcut(choice.arg)?.let { (pkg, id) -> app.apps.startShortcutById(pkg, id) }
                else -> if (Actions.isAutomatable(choice.action)) ActionRunner.run(app, choice.action)
            }
        }
        if (a.profile.isNotBlank()) {
            app.settings.edit { it.copy(profileOverride = ProfileOverride(a.profile, System.currentTimeMillis() + Automations.ONE_HOUR_MS)) }
        }
        if (a.reminder.isNotBlank()) remind(a)
    }

    @SuppressLint("MissingPermission")
    private fun remind(a: Automation) {
        if (!UsageReminderEngine.canNotify(app)) return
        val manager = NotificationManagerCompat.from(app)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, app.getString(R.string.auto_channel), NotificationManager.IMPORTANCE_DEFAULT),
        )
        manager.notify(
            a.id.hashCode(),
            NotificationCompat.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(a.name)
                .setContentText(a.reminder)
                .setAutoCancel(true)
                .build(),
        )
    }

    private companion object {
        const val POLL_MS = 15_000L
        const val CHANNEL_ID = "automations"
    }
}
