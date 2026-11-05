// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/** Scelte del pulsante fluttuante, senza dipendenze Android (testabili su JVM). */
object SearchButton {
    const val DEFAULT_ICON = "search"

    /** Icone disponibili, nell'ordine in cui compaiono nel selettore. */
    val ICONS = listOf(
        "search", "apps", "notifications", "lock", "settings", "star",
        "home", "bolt", "phone", "mail", "music", "camera",
    )

    /** Il nome salvato, o l'icona predefinita se non esiste più. */
    fun iconOrDefault(name: String): String = if (name in ICONS) name else DEFAULT_ICON

    /** Colore scelto dall'utente: 0 = quello del tema. */
    fun isAutoColor(argb: Long): Boolean = argb == 0L
}
