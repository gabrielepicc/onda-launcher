// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gabrielepiccione.onda.MainActivity
import com.gabrielepiccione.onda.OndaApp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.UsageProfiles
import com.gabrielepiccione.onda.data.UsageReminders
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Ogni giorno controlla se c'è un profilo da suggerire e, al massimo una volta a settimana, lo ricorda con una
 * notifica (solo se "Suggerisci profili" è acceso e le notifiche sono consentite). Toccandola si apre la home,
 * dove la scheda con Crea, Modifica e Ignora aspetta. Tutto resta sul telefono.
 */
class ProfileSuggestionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val app = applicationContext as OndaApp
        val state = app.settings.current.profileSuggest
        val today = LocalDate.now().toString()
        if (!state.enabled || !UsageProfiles.shouldNotify(state, today) || !UsageReminderEngine.canNotify(app)) return Result.success()

        val settings = app.settings.current
        val now = System.currentTimeMillis()
        val launchable = app.apps.apps.value.map { it.component.packageName }.toSet()
        val fixed = settings.favorites.take(settings.fixedFavoriteSlots).map { UsageReminders.packageOf(it) }.toSet() + app.packageName
        val pool = UsageProfiles.suggest(UsageReader(app).read(now, launchable), now, ZoneId.systemDefault(), fixed, state.useWifi)
        val candidate = UsageProfiles.next(state, pool, today) ?: return Result.success()

        val manager = NotificationManagerCompat.from(app)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, app.getString(R.string.suggest_channel), NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(
            app, 0, Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        manager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(app.getString(R.string.suggest_card_title, candidate.name))
                .setContentText(app.getString(R.string.suggest_notification_text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
        app.settings.edit { it.copy(profileSuggest = UsageProfiles.notified(it.profileSuggest, today)) }
        return Result.success()
    }

    companion object {
        private const val CHANNEL_ID = "profile_suggestions"
        private const val NOTIFICATION_ID = 7301
        private const val WORK_NAME = "onda-profile-suggestions"

        /** Il controllo periodico esiste solo con "Suggerisci profili" acceso. */
        fun syncSchedule(context: Context, enabled: Boolean) {
            val manager = WorkManager.getInstance(context)
            if (enabled) {
                manager.enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<ProfileSuggestionWorker>(24, TimeUnit.HOURS).build(),
                )
            } else {
                manager.cancelUniqueWork(WORK_NAME)
            }
        }
    }
}
