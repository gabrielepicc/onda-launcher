// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/**
 * Decide quando mostrare la pausa di Usage Breaker per un'app aperta fuori dal launcher (notifiche
 * di sistema, app recenti, link da altre app). Riceve solo il nome del pacchetto in primo piano.
 * Senza dipendenze Android, testabile su JVM.
 *
 * Regole:
 * - la pausa vale per l'ingresso nell'app, non per ogni sua schermata;
 * - tendina, tastiera e altri elementi di sistema sopra l'app non contano come uscita;
 * - dopo "Apri comunque" l'app resta libera finché non si passa a un'altra app o alla home.
 */
class BreakerGate(private val overlays: Set<String>) {
    private var current: String? = null
    private val passes = HashSet<String>()

    /** L'utente ha confermato la pausa (o l'app si apre da Onda dopo la pausa): niente seconda pausa. */
    @Synchronized
    fun allow(packageName: String) {
        passes += packageName
    }

    /**
     * Un'app è passata in primo piano. Restituisce il pacchetto per cui mostrare la pausa, o null.
     * [watched] dice se l'app ha Usage Breaker o un limite giornaliero.
     */
    @Synchronized
    fun onForeground(packageName: String, watched: (String) -> Boolean): String? {
        if (packageName in overlays || packageName == current) return null
        val previous = current
        current = packageName
        // Uscire da un'app chiude il suo permesso: la prossima volta torna la pausa
        if (previous != null) passes.remove(previous)
        if (packageName in passes) return null
        return packageName.takeIf(watched)
    }
}
