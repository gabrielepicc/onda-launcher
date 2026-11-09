// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/** Cosa mostra il popup di un'app, senza dipendenze Android (testabile su JVM). */
object PopupLayout {
    /**
     * Le scorciatoie si vedono solo senza notifiche: con le notifiche il popup mostra solo quelle
     * (salvo che l'utente abbia scelto di non piegarle).
     */
    fun showShortcuts(hasNotifications: Boolean, collapseWithNotifications: Boolean): Boolean =
        !hasNotifications || !collapseWithNotifications
}
