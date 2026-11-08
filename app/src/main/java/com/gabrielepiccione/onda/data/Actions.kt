// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import androidx.annotation.StringRes
import com.gabrielepiccione.onda.R

/** Che cosa serve all'utente per far funzionare un'azione. */
enum class ActionNeed { NONE, ACCESSIBILITY, POLICY_ACCESS }

/** Che cosa va scelto insieme all'azione. */
enum class ActionArg { NONE, APP, SHORTCUT, LAUNCHER }

/** Gruppi del catalogo, nell'ordine in cui compaiono. */
enum class ActionGroup(@StringRes val labelRes: Int) {
    LAUNCHER(R.string.action_group_launcher),
    SYSTEM(R.string.action_group_system),
    APPS(R.string.action_group_apps),
    SHORTCUTS(R.string.action_group_shortcuts),
}

/** Un'azione scelta dall'utente: la voce del catalogo e, se serve, l'app o la scorciatoia. */
data class ActionChoice(val action: GestureAction, val arg: String = "")

/**
 * Catalogo UNICO delle azioni personalizzabili (gesti, swipe, popup, cartelle, ricerca):
 * ogni ambito lo usa così com'è, senza elenchi propri. Logica pura, senza Android.
 */
object Actions {
    /** Voci del catalogo per gruppo, nell'ordine di visualizzazione ([GestureAction.NONE] resta fuori: la gestisce chi chiama). */
    val catalog: List<Pair<ActionGroup, List<GestureAction>>> = listOf(
        ActionGroup.LAUNCHER to listOf(GestureAction.SEARCH, GestureAction.ALL_APPS, GestureAction.SETTINGS, GestureAction.LAUNCHER_SET),
        ActionGroup.SYSTEM to listOf(
            GestureAction.NOTIFICATIONS,
            GestureAction.QUICK_SETTINGS,
            GestureAction.FLASHLIGHT,
            GestureAction.SILENT,
            GestureAction.SILENT_ON,
            GestureAction.SOUND_ON,
            GestureAction.LOCK_SCREEN,
            GestureAction.SCREENSHOT,
            GestureAction.RECENTS,
        ),
        ActionGroup.APPS to listOf(GestureAction.OPEN_APP, GestureAction.BLOCK_APP, GestureAction.UNBLOCK_APP),
        ActionGroup.SHORTCUTS to listOf(GestureAction.OPEN_SHORTCUT),
    )

    fun need(action: GestureAction): ActionNeed = when (action) {
        GestureAction.LOCK_SCREEN, GestureAction.SCREENSHOT, GestureAction.RECENTS -> ActionNeed.ACCESSIBILITY
        GestureAction.SILENT, GestureAction.SILENT_ON, GestureAction.SOUND_ON -> ActionNeed.POLICY_ACCESS
        else -> ActionNeed.NONE
    }

    /** Azioni che si possono eseguire senza schermate: le automazioni girano anche a launcher chiuso. */
    fun isAutomatable(action: GestureAction): Boolean =
        action != GestureAction.SEARCH && action != GestureAction.ALL_APPS && action != GestureAction.SETTINGS && action != GestureAction.NONE

    /** Azioni che hanno senso solo dentro un'automazione (bloccare e sbloccare un'app). */
    fun isAutomationOnly(action: GestureAction): Boolean =
        action == GestureAction.BLOCK_APP || action == GestureAction.UNBLOCK_APP || action == GestureAction.LAUNCHER_SET

    fun argOf(action: GestureAction): ActionArg = when (action) {
        GestureAction.OPEN_APP, GestureAction.BLOCK_APP, GestureAction.UNBLOCK_APP -> ActionArg.APP
        GestureAction.OPEN_SHORTCUT -> ActionArg.SHORTCUT
        GestureAction.LAUNCHER_SET -> ActionArg.LAUNCHER
        else -> ActionArg.NONE
    }

    /** Scelta pronta all'uso: ha l'app o la scorciatoia quando servono. */
    fun isComplete(c: ActionChoice): Boolean =
        c.action != GestureAction.NONE && (argOf(c.action) == ActionArg.NONE || c.arg.isNotBlank())

    /** Testo salvato nelle impostazioni: "" = nessuna, "NOME" oppure "NOME:argomento". */
    fun encode(c: ActionChoice): String = when {
        c.action == GestureAction.NONE -> ""
        c.arg.isBlank() -> c.action.name
        else -> c.action.name + ":" + c.arg
    }

    /** Legge il testo salvato; un testo senza nome di azione è una chiave d'app delle versioni precedenti. */
    fun decode(text: String?): ActionChoice {
        if (text.isNullOrBlank()) return ActionChoice(GestureAction.NONE)
        val head = text.substringBefore(':')
        val action = GestureAction.entries.firstOrNull { it.name == head }
        return when {
            action == null -> ActionChoice(GestureAction.OPEN_APP, text)
            text.contains(':') -> ActionChoice(action, text.substringAfter(':'))
            else -> ActionChoice(action)
        }
    }

    /** Argomento di una scorciatoia: pacchetto e id. */
    fun shortcutArg(packageName: String, shortcutId: String): String = "$packageName/$shortcutId"

    fun parseShortcut(arg: String): Pair<String, String>? {
        val i = arg.indexOf('/')
        if (i <= 0 || i == arg.lastIndex) return null
        return arg.substring(0, i) to arg.substring(i + 1)
    }

    /** Azioni per la ricerca: le voci senza argomento il cui nome contiene [query] (senza maiuscole né accenti). */
    fun matching(query: String, labelOf: (GestureAction) -> String): List<GestureAction> {
        val q = fold(query)
        if (q.length < 2) return emptyList()
        return catalog.flatMap { it.second }
            .filter { argOf(it) == ActionArg.NONE }
            .filter { fold(labelOf(it)).contains(q) }
    }

    private fun fold(text: String): String =
        java.text.Normalizer.normalize(text.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .trim()
}
