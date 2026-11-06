// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/** Regole di Plug & Play con le cuffie, senza dipendenze Android (testabili su JVM). */
object PlugPlay {
    /** Il dispositivo scelto è tra quelli collegati? Vuoto = qualsiasi cuffia. */
    fun matches(device: String, connected: Set<String>): Boolean =
        device.isBlank() || connected.any { it.trim().equals(device.trim(), ignoreCase = true) }

    /**
     * "Riprendi la musica": solo appena si collegano le cuffie (non mentre restano collegate), con
     * l'opzione attiva, il dispositivo giusto, nessuna chiamata in corso e nessun'altra app che sta già suonando.
     */
    fun shouldResume(
        justConnected: Boolean,
        enabled: Boolean,
        device: String,
        connected: Set<String>,
        inCall: Boolean,
        somethingPlaying: Boolean,
    ): Boolean = justConnected && enabled && matches(device, connected) && !inCall && !somethingPlaying
}
