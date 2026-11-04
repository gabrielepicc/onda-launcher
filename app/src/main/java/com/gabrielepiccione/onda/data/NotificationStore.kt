// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import android.app.ActivityOptions
import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.app.RemoteInput
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.os.Build
import android.provider.Settings
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.gabrielepiccione.onda.service.OndaNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Notifiche attive raggruppate per pacchetto, alimentate dal NotificationListenerService. */
object NotificationStore {

    private val _byPackage = MutableStateFlow<Map<String, List<NotificationItem>>>(emptyMap())
    val byPackage: StateFlow<Map<String, List<NotificationItem>>> = _byPackage.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    internal var listener: OndaNotificationListener? = null

    internal fun setConnected(value: Boolean) {
        _connected.value = value
        if (!value) _byPackage.value = emptyMap()
    }

    internal fun publish(context: Context, active: Array<StatusBarNotification>, ownPackage: String) {
        _byPackage.value = active
            .asSequence()
            .filter { it.packageName != ownPackage }
            .filter { sbn ->
                val n = sbn.notification
                // Saltiamo i riassunti di gruppo e le notifiche "di servizio" senza contenuto
                (n.flags and Notification.FLAG_GROUP_SUMMARY) == 0 || active.count { it.groupKey == sbn.groupKey } == 1
            }
            .mapNotNull { it.toItem(context) }
            .groupBy { it.packageName }
            .mapValues { (_, list) -> list.sortedByDescending { it.postTime } }
    }

    private fun StatusBarNotification.toItem(context: Context): NotificationItem? {
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
            ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: ""
        if (title.isBlank() && text.isBlank()) return null
        val ongoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        // Le notifiche persistenti senza testo (es. servizi in primo piano) le ignoriamo
        if (ongoing && text.isBlank()) return null

        val all = notification.actions.orEmpty()
        val reply = all.firstOrNull { a -> a.remoteInputs?.any { it.allowFreeFormInput } == true }
        val others = all.filter { it !== reply && it.remoteInputs.isNullOrEmpty() && it.actionIntent != null }.take(3)
        val style = runCatching { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification) }.getOrNull()
        val messages = runCatching {
            style
                ?.messages
                ?.takeLast(8)
                ?.mapNotNull { m ->
                    val body = m.text?.toString()?.trim().orEmpty()
                    if (body.isEmpty()) null else MessageLine(m.person?.name?.toString().orEmpty(), body)
                }
        }.getOrNull().orEmpty()

        return NotificationItem(
            key = key,
            packageName = packageName,
            title = title.trim(),
            text = text.trim().replace('\n', ' '),
            postTime = postTime,
            contentIntent = notification.contentIntent,
            autoCancel = (notification.flags and Notification.FLAG_AUTO_CANCEL) != 0,
            clearable = isClearable,
            messages = messages,
            replyAction = reply,
            actions = others,
            largeIcon = largeIconOf(context),
            conversationTitle = style?.conversationTitle?.toString()?.takeIf { it.isNotBlank() && it != title },
        )
    }

    /** Icona grande (foto del contatto), ridotta a 128px. */
    private fun StatusBarNotification.largeIconOf(context: Context): androidx.compose.ui.graphics.ImageBitmap? = runCatching {
        val d = notification.getLargeIcon()?.loadDrawable(context) ?: return@runCatching null
        val size = 128
        d.toBitmap(size, size).asImageBitmap()
    }.getOrNull()

    fun isPermissionGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun permissionIntent(context: Context): android.content.Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                ComponentName(context, OndaNotificationListener::class.java).flattenToString()
            )
        } else {
            android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }

    /** Apre la notifica come se fosse stata toccata nella tendina. */
    fun open(context: Context, item: NotificationItem) {
        val pi = item.contentIntent
        if (pi != null) {
            runCatching { pi.send(context, 0, null, null, null, null, sendOptions()) }
        }
        if (item.autoCancel) dismiss(item)
    }

    private fun sendOptions(): android.os.Bundle? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                .toBundle()
        } else null

    /**
     * Risposta diretta: compila i campi RemoteInput dell'azione di risposta e la invia all'app
     * (lo stesso meccanismo usato dalla tendina di sistema). Restituisce false se non riesce.
     */
    fun reply(context: Context, item: NotificationItem, text: String): Boolean {
        val action = item.replyAction ?: return false
        val inputs = action.remoteInputs ?: return false
        if (text.isBlank()) return false
        val intent = Intent().addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        val results = Bundle().apply {
            inputs.filter { it.allowFreeFormInput }.forEach { putCharSequence(it.resultKey, text.trim()) }
        }
        RemoteInput.addResultsToIntent(inputs, intent, results)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            RemoteInput.setResultsSource(intent, RemoteInput.SOURCE_FREE_FORM_INPUT)
        }
        return runCatching { action.actionIntent.send(context, 0, intent, null, null, null, sendOptions()) }.isSuccess
    }

    /** Esegue un'azione della notifica (es. "Segna come letto"). */
    fun runAction(context: Context, item: NotificationItem, action: Notification.Action) {
        runCatching { action.actionIntent.send(context, 0, null, null, null, null, sendOptions()) }
        if (item.autoCancel) dismiss(item)
    }

    /** Posticipa la notifica (ricompare dopo [minutes]). */
    fun snooze(item: NotificationItem, minutes: Long = 60) {
        if (item.clearable) runCatching { listener?.snoozeNotification(item.key, minutes * 60_000L) }
    }

    fun dismiss(item: NotificationItem) {
        if (item.clearable) runCatching { listener?.cancelNotification(item.key) }
    }

    fun dismissAll(packageName: String) {
        _byPackage.value[packageName].orEmpty().forEach(::dismiss)
    }
}
