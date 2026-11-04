// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.gabrielepiccione.onda.OndaApp
import com.gabrielepiccione.onda.data.NotificationStore
import com.gabrielepiccione.onda.util.DigestSchedule
import java.time.LocalTime

class OndaNotificationListener : NotificationListenerService() {

    /** Notifiche rinviate al riepilogo: chiave -> momento in cui ricompaiono (ms). */
    private val deferred = HashMap<String, Long>()

    override fun onListenerConnected() {
        NotificationStore.listener = this
        NotificationStore.setConnected(true)
        refresh()
    }

    override fun onListenerDisconnected() {
        if (NotificationStore.listener === this) NotificationStore.listener = null
        NotificationStore.setConnected(false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn != null && deferToDigest(sbn)) return
        refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    /**
     * Riepilogo notifiche: per le app scelte dall'utente la notifica viene posticipata (snooze di
     * sistema) fino al prossimo orario del riepilogo, quando ricompare normalmente.
     */
    private fun deferToDigest(sbn: StatusBarNotification): Boolean {
        val settings = (application as? OndaApp)?.settings?.current ?: return false
        if (sbn.packageName !in settings.digestApps || !sbn.isClearable) return false
        if ((sbn.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return false
        val now = System.currentTimeMillis()
        // Ricompare ora dal riepilogo: non va rinviata di nuovo
        val releaseAt = deferred[sbn.key]
        if (releaseAt != null && now >= releaseAt - RELEASE_TOLERANCE_MS) {
            deferred.remove(sbn.key)
            return false
        }
        val nowMinute = LocalTime.now().let { it.hour * 60 + it.minute }
        if ((DigestSchedule.minutesSinceLast(nowMinute, settings.digestTimes) ?: return false) < RELEASE_WINDOW_MIN) return false
        val minutes = DigestSchedule.minutesUntilNext(nowMinute, settings.digestTimes) ?: return false
        val delay = minutes * 60_000L - LocalTime.now().second * 1000L
        val ok = runCatching { snoozeNotification(sbn.key, delay) }.isSuccess
        if (ok) deferred[sbn.key] = now + delay
        return ok
    }

    private fun refresh() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        NotificationStore.publish(this, active, packageName)
    }

    private companion object {
        const val RELEASE_TOLERANCE_MS = 90_000L
        const val RELEASE_WINDOW_MIN = 2
    }
}
