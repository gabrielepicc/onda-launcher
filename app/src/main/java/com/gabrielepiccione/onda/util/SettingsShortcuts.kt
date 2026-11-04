// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.annotation.StringRes
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.normalizedForSearch

/** Voce delle impostazioni di sistema raggiungibile dalla ricerca con il prefisso "/". */
data class SettingsShortcut(@StringRes val labelRes: Int, val action: String, val keywords: List<String>)

object SettingsShortcuts {

    /** Azione speciale: apre le impostazioni di Onda invece di quelle di sistema. */
    const val ONDA = "onda:settings"

    val all = listOf(
        SettingsShortcut(R.string.set_onda, ONDA, listOf("onda", "launcher", "home")),
        SettingsShortcut(R.string.set_wifi, Settings.ACTION_WIFI_SETTINGS, listOf("wifi", "wi-fi", "rete", "network")),
        SettingsShortcut(R.string.set_bluetooth, Settings.ACTION_BLUETOOTH_SETTINGS, listOf("bluetooth", "bt")),
        SettingsShortcut(R.string.set_display, Settings.ACTION_DISPLAY_SETTINGS, listOf("display", "schermo", "luminosita", "brightness")),
        SettingsShortcut(R.string.set_sound, Settings.ACTION_SOUND_SETTINGS, listOf("suono", "sound", "volume", "suoneria")),
        SettingsShortcut(R.string.set_battery, Intent.ACTION_POWER_USAGE_SUMMARY, listOf("batteria", "battery")),
        SettingsShortcut(R.string.set_apps, Settings.ACTION_APPLICATION_SETTINGS, listOf("app", "applicazioni", "apps")),
        SettingsShortcut(R.string.set_notifications, "android.settings.ALL_APPS_NOTIFICATION_SETTINGS", listOf("notifiche", "notifications")),
        SettingsShortcut(R.string.set_location, Settings.ACTION_LOCATION_SOURCE_SETTINGS, listOf("posizione", "location", "gps")),
        SettingsShortcut(R.string.set_security, Settings.ACTION_SECURITY_SETTINGS, listOf("sicurezza", "security", "blocco", "lock")),
        SettingsShortcut(R.string.set_storage, Settings.ACTION_INTERNAL_STORAGE_SETTINGS, listOf("memoria", "storage", "spazio")),
        SettingsShortcut(R.string.set_accessibility, Settings.ACTION_ACCESSIBILITY_SETTINGS, listOf("accessibilita", "accessibility")),
        SettingsShortcut(R.string.set_date, Settings.ACTION_DATE_SETTINGS, listOf("data", "ora", "date", "time")),
        SettingsShortcut(R.string.set_language, Settings.ACTION_LOCALE_SETTINGS, listOf("lingua", "language")),
        SettingsShortcut(R.string.set_mobile_data, Settings.ACTION_DATA_ROAMING_SETTINGS, listOf("dati", "mobile", "sim", "roaming")),
        SettingsShortcut(R.string.set_airplane, Settings.ACTION_AIRPLANE_MODE_SETTINGS, listOf("aereo", "airplane", "flight")),
        SettingsShortcut(R.string.set_nfc, Settings.ACTION_NFC_SETTINGS, listOf("nfc", "pagamenti")),
        SettingsShortcut(R.string.set_hotspot, "android.settings.TETHER_SETTINGS", listOf("hotspot", "tethering")),
    )

    fun search(context: Context, query: String): List<SettingsShortcut> {
        val q = query.normalizedForSearch().trim()
        if (q.isEmpty()) return all.take(8)
        return all.filter { s ->
            context.getString(s.labelRes).normalizedForSearch().contains(q) || s.keywords.any { it.startsWith(q) }
        }
    }

    fun open(context: Context, shortcut: SettingsShortcut) {
        runCatching { context.startActivity(Intent(shortcut.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    }
}
