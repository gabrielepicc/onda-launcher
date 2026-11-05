// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.gabrielepiccione.onda.data.NotificationCounter
import com.gabrielepiccione.onda.data.NotificationStore

class OndaNotificationListener : NotificationListenerService() {

    private var reminders: UsageReminderEngine? = null

    override fun onListenerConnected() {
        NotificationStore.listener = this
        NotificationStore.setConnected(true)
        refresh()
        reminders?.stop()
        reminders = UsageReminderEngine(this).also { it.start() }
    }

    override fun onListenerDisconnected() {
        if (NotificationStore.listener === this) NotificationStore.listener = null
        NotificationStore.setConnected(false)
        reminders?.stop()
        reminders = null
    }

    override fun onDestroy() {
        reminders?.stop()
        reminders = null
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let { runCatching { NotificationCounter.record(this, it) } }
        refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        NotificationStore.publish(this, active, packageName)
    }
}
