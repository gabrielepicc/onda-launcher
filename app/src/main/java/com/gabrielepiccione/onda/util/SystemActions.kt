// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

import android.annotation.SuppressLint
import android.app.Activity
import android.app.SearchManager
import android.app.role.RoleManager
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.service.MusicWallpaperService

object SystemActions {

    /** Apre la tendina delle notifiche (API non pubblica, usata da molti launcher). */
    @SuppressLint("WrongConstant", "PrivateApi")
    fun expandNotifications(context: Context) {
        runCatching {
            val service = context.getSystemService("statusbar")
            val clazz = Class.forName("android.app.StatusBarManager")
            clazz.getMethod("expandNotificationsPanel").invoke(service)
        }
    }

    /** Apre il pannello rapido delle impostazioni (stessa API non pubblica della tendina). */
    @SuppressLint("WrongConstant", "PrivateApi")
    fun expandQuickSettings(context: Context) {
        runCatching {
            val service = context.getSystemService("statusbar")
            val clazz = Class.forName("android.app.StatusBarManager")
            clazz.getMethod("expandSettingsPanel").invoke(service)
        }
    }

    /** Schermata dove si concede l'accesso a "Non disturbare", necessario per il modo silenzioso. */
    fun openPolicyAccessSettings(context: Context) =
        start(context, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))

    fun uninstall(context: Context, app: AppInfo) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
            .putExtra(Intent.EXTRA_USER, app.user)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** Indirizzi dei motori di ricerca selezionabili (la query va in coda, già codificata). */
    val SEARCH_ENGINES = linkedMapOf(
        "DUCKDUCKGO" to "https://duckduckgo.com/?q=",
        "STARTPAGE" to "https://www.startpage.com/do/search?q=",
        "GOOGLE" to "https://www.google.com/search?q=",
        "ECOSIA" to "https://www.ecosia.org/search?q=",
        "BRAVE" to "https://search.brave.com/search?q=",
    )

    fun webSearch(context: Context, query: String, engine: String = "SYSTEM") {
        SEARCH_ENGINES[engine]?.let { base ->
            val ok = runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(base + Uri.encode(query))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isSuccess
            if (ok) return
        }
        val search = Intent(Intent.ACTION_WEB_SEARCH)
            .putExtra(SearchManager.QUERY, query)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val ok = runCatching { context.startActivity(search) }.isSuccess
        if (!ok) {
            val url = "https://duckduckgo.com/?q=" + Uri.encode(query)
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    fun openAlarms(context: Context) = start(context, Intent(AlarmClock.ACTION_SHOW_ALARMS))

    fun openCalendar(context: Context) {
        val uri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
            .appendPath(System.currentTimeMillis().toString()).build()
        start(context, Intent(Intent.ACTION_VIEW, uri))
    }

    fun openWallpaperPicker(context: Context) =
        start(context, Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), context.getString(com.gabrielepiccione.onda.R.string.wallpaper_chooser)))

    /** Propone lo sfondo musicale di Onda come sfondo animato di sistema (anche per la schermata di blocco). */
    fun openMusicWallpaper(context: Context) {
        val component = ComponentName(context, MusicWallpaperService::class.java)
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
        start(context, intent)
    }

    fun isDefaultLauncher(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(intent, 0)
        return resolved?.activityInfo?.packageName == context.packageName
    }

    @Suppress("DEPRECATION")
    fun requestDefaultLauncher(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = activity.getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                runCatching { activity.startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), 7001) }
                    .onSuccess { return }
            }
        }
        start(activity, Intent(Settings.ACTION_HOME_SETTINGS))
    }

    private fun start(context: Context, intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
