// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/** Personalizzazione del popup di un'app: app correlate, widget e scorciatoie nascoste. */
@Immutable
@Serializable
data class AppPopup(
    val relatedApps: List<String> = emptyList(),
    val widgets: List<Int> = emptyList(),
    val hiddenShortcuts: Set<String> = emptySet(),
    /** Azioni personalizzate in fondo al popup, nel formato di [Actions.encode]. */
    val actions: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = relatedApps.isEmpty() && widgets.isEmpty() && hiddenShortcuts.isEmpty() && actions.isEmpty()
}

/** Operazioni sui popup delle app, senza dipendenze Android (testabili su JVM). */
object AppPopups {
    const val MAX_WIDGETS = 4
    const val MAX_RELATED = 8
    const val MAX_ACTIONS = 6

    fun of(s: Settings, appKey: String): AppPopup = s.appPopups[appKey] ?: AppPopup()

    /** Widget del popup: quello storico dell'app ([Settings.appWidgets]) per primo, poi gli altri. */
    fun widgetsOf(s: Settings, appKey: String): List<Int> =
        (listOfNotNull(s.appWidgets[appKey]) + of(s, appKey).widgets).distinct()

    fun canAddWidget(s: Settings, appKey: String): Boolean = widgetsOf(s, appKey).size < MAX_WIDGETS

    fun addWidget(s: Settings, appKey: String, widgetId: Int): Settings {
        if (!canAddWidget(s, appKey)) return s
        return update(s, appKey) { it.copy(widgets = it.widgets + widgetId) }
    }

    fun removeWidget(s: Settings, appKey: String, widgetId: Int): Settings {
        val cleaned = if (s.appWidgets[appKey] == widgetId) s.copy(appWidgets = s.appWidgets - appKey) else s
        return update(cleaned, appKey) { it.copy(widgets = it.widgets - widgetId) }
    }

    fun addRelated(s: Settings, appKey: String, related: String): Settings {
        val current = of(s, appKey).relatedApps
        if (related == appKey || related in current || current.size >= MAX_RELATED) return s
        return update(s, appKey) { it.copy(relatedApps = it.relatedApps + related) }
    }

    fun removeRelated(s: Settings, appKey: String, related: String): Settings =
        update(s, appKey) { it.copy(relatedApps = it.relatedApps - related) }

    fun addAction(s: Settings, appKey: String, encoded: String): Settings {
        val current = of(s, appKey).actions
        if (encoded.isBlank() || encoded in current || current.size >= MAX_ACTIONS) return s
        return update(s, appKey) { it.copy(actions = it.actions + encoded) }
    }

    fun removeAction(s: Settings, appKey: String, encoded: String): Settings =
        update(s, appKey) { it.copy(actions = it.actions - encoded) }

    fun toggleShortcut(s: Settings, appKey: String, shortcutId: String): Settings = update(s, appKey) {
        val hidden = it.hiddenShortcuts
        it.copy(hiddenShortcuts = if (shortcutId in hidden) hidden - shortcutId else hidden + shortcutId)
    }

    /** Nel backup i widget restano fuori: gli id valgono solo su questo telefono. */
    fun forBackup(popups: Map<String, AppPopup>): Map<String, AppPopup> =
        popups.mapValues { (_, p) -> p.copy(widgets = emptyList()) }.filterValues { !it.isEmpty }

    /** Al ripristino: app correlate e scorciatoie dal backup, widget da questo telefono. */
    fun forRestore(parsed: Map<String, AppPopup>, now: Map<String, AppPopup>): Map<String, AppPopup> =
        (parsed.keys + now.keys).associateWith { key ->
            (parsed[key] ?: AppPopup()).copy(widgets = now[key]?.widgets.orEmpty())
        }.filterValues { !it.isEmpty }

    private fun update(s: Settings, appKey: String, change: (AppPopup) -> AppPopup): Settings {
        val next = change(of(s, appKey))
        return s.copy(appPopups = if (next.isEmpty) s.appPopups - appKey else s.appPopups + (appKey to next))
    }
}
