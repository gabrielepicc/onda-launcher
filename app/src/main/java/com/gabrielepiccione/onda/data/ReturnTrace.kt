// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/**
 * Cosa succede dopo un ritorno da un'app, in ms dall'onNewIntent (misure di sviluppo): quando arrivano i segnali
 * del sistema, il primo fotogramma, l'avvio della molla e quanti fotogrammi di Choreographer ci sono ogni 100 ms.
 * Logica pura (testabile su JVM): chi la usa passa i tempi.
 */
enum class TraceMark { TOP_RESUMED, RESUME, FOCUS, ENTER_COMPLETE, FIRST_FRAME, SPRING_START }

/** Fotografia di un ritorno: un tempo per segnale (null = non arrivato) e i fotogrammi per ogni 100 ms. */
data class TraceSnapshot(
    val marks: Map<TraceMark, Long>,
    val framesPer100ms: List<Int>,
)

class ReturnTrace(private val startMs: Long) {
    private val marks = LinkedHashMap<TraceMark, Long>()
    private val frames = IntArray(BUCKETS)

    /** Registra il primo arrivo di [mark]; i segnali tardivi si registrano ancora, fino a [MAX_MARK_MS]. */
    fun mark(mark: TraceMark, nowMs: Long): Boolean {
        val at = nowMs - startMs
        if (at < 0 || at > MAX_MARK_MS || mark in marks) return false
        marks[mark] = at
        return true
    }

    /** Un fotogramma a [nowMs]: true se ha cambiato i conteggi (cioè cade nei primi [BUCKETS] × 100 ms). */
    fun frame(nowMs: Long): Boolean {
        val at = nowMs - startMs
        if (at < 0) return false
        val bucket = (at / BUCKET_MS).toInt()
        if (bucket >= BUCKETS) return false
        frames[bucket] += 1
        return true
    }

    fun snapshot(): TraceSnapshot = TraceSnapshot(marks.toMap(), frames.toList())

    companion object {
        const val BUCKET_MS = 100L
        const val BUCKETS = 10
        const val MAX_MARK_MS = 5_000L

        /** Conserva gli ultimi [KEEP] ritorni, il più recente per ultimo. */
        const val KEEP = 3
        fun keepLast(list: List<TraceSnapshot>, added: TraceSnapshot): List<TraceSnapshot> = (list + added).takeLast(KEEP)
    }
}

/** Ritardo e durata dell'entrata (impostazioni di sviluppo): con ritardo 0 la molla parte con i segnali del sistema. */
object EntryTiming {
    const val DELAY_MIN = 0
    const val DELAY_MAX = 900
    /** 0 = la molla parte con i segnali del sistema (comportamento normale); >0 = ritardo fisso, solo per prova. */
    const val DELAY_DEFAULT = 0
    const val DURATION_MIN = 150
    const val DURATION_MAX = 1200
    const val DURATION_DEFAULT = 300
    const val STEP = 50

    enum class Phase { WAITING, RUNNING, DONE }

    fun delay(ms: Int): Int = snap(ms, DELAY_MIN, DELAY_MAX)
    fun duration(ms: Int): Int = snap(ms, DURATION_MIN, DURATION_MAX)

    private fun snap(ms: Int, min: Int, max: Int): Int = (Math.round(ms.toFloat() / STEP) * STEP).coerceIn(min, max)

    /** Fase dell'entrata [elapsedMs] dopo l'onNewIntent: attesa nei valori di partenza, in corso, finita. */
    fun phase(elapsedMs: Long, delayMs: Int, durationMs: Int): Phase = when {
        elapsedMs < delay(delayMs) -> Phase.WAITING
        elapsedMs < delay(delayMs) + duration(durationMs) -> Phase.RUNNING
        else -> Phase.DONE
    }

    /** Avanzamento lineare 0..1 (0 in attesa, 1 a fine entrata): la curva vera la mette l'animazione. */
    fun progress(elapsedMs: Long, delayMs: Int, durationMs: Int): Float {
        val d = delay(delayMs)
        val total = duration(durationMs)
        return ((elapsedMs - d).toFloat() / total).coerceIn(0f, 1f)
    }
}
