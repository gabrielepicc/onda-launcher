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

    /** Il launcher era già visibile (tra onStart e onStop) e riceve MAIN: è il tasto Home premuto davvero. */
    fun isHomePress(action: String?, visible: Boolean): Boolean = action == ACTION_MAIN && visible

    /** MAIN a launcher fermo: si torna da un'app (swipe verso l'alto), il sistema lo riattiva solo a fine gesto. */
    fun isReturnFromApp(action: String?, visible: Boolean): Boolean = action == ACTION_MAIN && !visible

    /** Entrata animata solo al ritorno da un'app, se l'utente la vuole e le animazioni di sistema non sono spente. */
    fun animateEntry(returnFromApp: Boolean, enabled: Boolean, animationScale: Float): Boolean =
        returnFromApp && enabled && animationScale > 0f

    /** Con il launcher fuori vista i pannelli si chiudono di colpo: un'animazione resterebbe ferma sul valore vecchio. */
    fun closeInstantly(visible: Boolean): Boolean = !visible

    /** Aggiunge un tempo e tiene gli ultimi [KEEP_TIMES]. */
    fun addTime(times: List<Long>, ms: Long): List<Long> = (times + ms).takeLast(KEEP_TIMES)
}
