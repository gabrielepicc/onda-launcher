// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/** Le voci del foglio «Altre opzioni» di un'app, senza dipendenze Android (testabile su JVM). */
enum class AppOption {
    FAVORITE, MOVE_UP, MOVE_DOWN, FOLDER, RENAME, ICON, EDIT_POPUP,
    SWIPE, NOTIFICATIONS_HOME, REMINDER,
    PROTECT, BREAKER, VISIBLE,
    ADD_WIDGET, PIN_SHORTCUT,
    INFO, UNINSTALL,
}

/** Gruppi del foglio, nell'ordine standard (Identità → Frequenti → Comportamento → Avanzate → Pericolo). */
enum class AppOptionGroup(val options: List<AppOption>) {
    ORGANIZE(listOf(AppOption.FAVORITE, AppOption.MOVE_UP, AppOption.MOVE_DOWN, AppOption.FOLDER, AppOption.RENAME, AppOption.ICON, AppOption.EDIT_POPUP)),
    BEHAVIOR(listOf(AppOption.SWIPE, AppOption.NOTIFICATIONS_HOME, AppOption.REMINDER)),
    PRIVACY(listOf(AppOption.PROTECT, AppOption.BREAKER, AppOption.VISIBLE)),
    ADD(listOf(AppOption.ADD_WIDGET, AppOption.PIN_SHORTCUT)),
    SYSTEM(listOf(AppOption.INFO, AppOption.UNINSTALL)),
}

object AppOptions {
    /**
     * Le voci che compaiono: Su/Giù solo per un preferito che può muoversi, «Scorciatoia sulla home» solo se l'app
     * ha scorciatoie, «Disinstalla» solo se il sistema lo permette.
     */
    fun visible(isFavorite: Boolean, favoriteIndex: Int, favoriteCount: Int, hasShortcuts: Boolean, canUninstall: Boolean = true): Set<AppOption> =
        AppOption.entries.filter {
            when (it) {
                AppOption.MOVE_UP -> isFavorite && favoriteIndex > 0
                AppOption.MOVE_DOWN -> isFavorite && favoriteIndex in 0 until favoriteCount - 1
                AppOption.PIN_SHORTCUT -> hasShortcuts
                AppOption.UNINSTALL -> canUninstall
                else -> true
            }
        }.toSet()

    /** I gruppi con almeno una voce visibile, nell'ordine standard, con le loro voci. */
    fun groups(visible: Set<AppOption>): List<Pair<AppOptionGroup, List<AppOption>>> =
        AppOptionGroup.entries.map { g -> g to g.options.filter { it in visible } }.filter { it.second.isNotEmpty() }
}
