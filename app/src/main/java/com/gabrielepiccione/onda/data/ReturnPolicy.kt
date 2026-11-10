// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/**
 * Regole del ritorno alla home, senza dipendenze Android (testabili su JVM): quando il lavoro in più si può
 * rimandare, quando un MAIN è davvero «Home premuto» e quando conviene l'entrata animata.
 */
object ReturnPolicy {
    /** Intent.ACTION_MAIN, scritto qui per non dipendere da Android. */
    const val ACTION_MAIN = "android.intent.action.MAIN"

    /** Gli aggiornamenti non urgenti (calendario, contatti, meteo, musica) si rifanno al più ogni minuto. */
    const val MIN_REFRESH_GAP_MS = 60_000L

    /** Tempi di ritorno conservati per l'etichetta di sviluppo. */
    const val KEEP_TIMES = 5

    fun shouldRefresh(lastMs: Long, nowMs: Long, gapMs: Long = MIN_REFRESH_GAP_MS): Boolean =
        lastMs <= 0L || nowMs < lastMs || nowMs - lastMs >= gapMs

    /**
     * MAIN senza uno stop dall'ultimo resume: il launcher era aperto e si preme Home davvero.
     * [stoppedSinceResume] è true da onStop fino al resume successivo (onNewIntent arriva tra onStart e onResume).
     */
    fun isHomePress(action: String?, stoppedSinceResume: Boolean): Boolean = action == ACTION_MAIN && !stoppedSinceResume

    /** MAIN dopo uno stop: si torna da un'app (swipe verso l'alto), il sistema riattiva il launcher solo a fine gesto. */
    fun isReturnFromApp(action: String?, stoppedSinceResume: Boolean): Boolean = action == ACTION_MAIN && stoppedSinceResume

    /** Entrata animata solo al ritorno da un'app, se l'utente la vuole e le animazioni di sistema non sono spente. */
    fun animateEntry(returnFromApp: Boolean, enabled: Boolean, animationScale: Float): Boolean =
        returnFromApp && enabled && animationScale > 0f

    /** Con il launcher fuori vista i pannelli si chiudono di colpo: un'animazione resterebbe ferma sul valore vecchio. */
    fun closeInstantly(visible: Boolean): Boolean = !visible

    /**
     * Timeout di sicurezza dell'entrata: deve restare più lungo dei tempi osservati (su realme enter-complete arriva
     * a ~860 ms e il focus a ~885 ms), altrimenti la molla finisce prima che la finestra si veda.
     */
    const val ENTRY_TIMEOUT_MS = 1_500L

    /** Chi ha avviato la molla dell'entrata. */
    enum class EntrySignal { ENTER_COMPLETE, FOCUS, TIMEOUT }

    /**
     * Una sola partenza per ritorno: il primo tra fine della transizione di ingresso e focus della finestra (o il
     * timeout) avvia la molla, gli altri si ignorano. [offer] restituisce il segnale solo la prima volta.
     */
    class EntryGate {
        private var fired = false
        fun offer(signal: EntrySignal): EntrySignal? {
            if (fired) return null
            fired = true
            return signal
        }
    }

    /** Aggiunge un tempo e tiene gli ultimi [KEEP_TIMES]. */
    fun addTime(times: List<Long>, ms: Long): List<Long> = (times + ms).takeLast(KEEP_TIMES)
}
