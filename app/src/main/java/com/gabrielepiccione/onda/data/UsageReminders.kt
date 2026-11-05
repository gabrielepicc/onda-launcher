// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/** Un'app che passa in primo piano ([foreground] vero) o lo lascia. */
data class UsageEvent(val time: Long, val packageName: String, val foreground: Boolean)

/** Regole dei promemoria di Usage Breaker, senza dipendenze Android (testabili su JVM). */
object UsageReminders {
    /** Dopo quanti minuti di uso continuo arriva un promemoria. */
    val THRESHOLDS = listOf(5, 10, 20, 30, 45, 60, 90, 120, 180, 240)

    /** Da quale soglia si può iniziare, nelle impostazioni. */
    val START_CHOICES = listOf(5, 10, 20, 30)

    /** Un'interruzione più breve (un messaggio, una ricerca) non azzera il tempo. */
    const val TOLERANCE_MS = 5 * 60_000L

    /** La chiave di un'app è "pacchetto/attività#utente". */
    fun packageOf(appKey: String): String = appKey.substringBefore('/')

    fun toggle(s: Settings, appKey: String): Settings =
        s.copy(reminderApps = if (appKey in s.reminderApps) s.reminderApps - appKey else s.reminderApps + appKey)

    /**
     * Da quanto si usa [packageName] senza interruzioni lunghe; 0 se ora non è in primo piano.
     * Conta solo il tempo effettivo nell'app, non quello delle interruzioni.
     */
    fun continuousMs(events: List<UsageEvent>, packageName: String, now: Long, toleranceMs: Long = TOLERANCE_MS): Long {
        val closed = ArrayList<LongRange>()
        var openSince: Long? = null
        for (e in events.sortedBy { it.time }) {
            val since = openSince
            if (e.packageName == packageName && e.foreground) {
                if (since == null) openSince = e.time
            } else if (since != null && (e.packageName == packageName || e.foreground)) {
                // Lascia il primo piano, oppure un'altra app ci è passata sopra
                closed += since..e.time
                openSince = null
            }
        }
        val current = openSince ?: return 0L
        var total = now - current
        var start = current
        for (session in closed.asReversed()) {
            if (start - session.last > toleranceMs) break
            total += session.last - session.first
            start = session.first
        }
        return total.coerceAtLeast(0L)
    }

    /** La soglia da notificare ora, oppure null: ogni soglia una sola volta per ciascun periodo d'uso. */
    fun thresholdToNotify(minutes: Int, startFrom: Int, lastNotified: Int): Int? =
        THRESHOLDS.lastOrNull { it <= minutes }?.takeIf { it >= startFrom && it > lastNotified }
}
