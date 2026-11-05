// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

/**
 * Momenti dell'interfaccia che danno un feedback aptico. Ognuno ha un "peso" diverso, così
 * il dito distingue lo scorrere delle lettere da una soglia superata o da una pressione lunga.
 */
enum class HapticEvent {
    /** Cambio di lettera sull'alfabeto: leggerissimo, ripetuto spesso. */
    LETTER,

    /** Prima o ultima voce raggiunta (inizio/fine alfabeto, fondo elenco). */
    EDGE,

    /** Uno swipe supera la soglia: lasciando, succede qualcosa. */
    THRESHOLD_ON,

    /** Lo swipe torna sotto la soglia: lasciando, non succede nulla. */
    THRESHOLD_OFF,

    /** Pressione lunga riconosciuta. */
    LONG_PRESS,

    /** Un popup o un pannello compare. */
    OPEN,

    /** Azione completata (notifica eliminata, schermo bloccato…). */
    CONFIRM,
}

/** Un colpo della composizione: primitiva di [android.os.VibrationEffect.Composition], intensità 0–1, ritardo in ms. */
data class HapticStep(val primitive: Int, val scale: Float, val delayMs: Int = 0)

/**
 * Sceglie come far vibrare ogni evento, senza dipendere da Android (testabile su JVM).
 *
 * Con le "vibrazioni leggere" (primitive supportate dal motore aptico, Android 11+) si usa una
 * composizione dosata per evento; altrimenti si ripiega sulle costanti di sistema di
 * [android.view.HapticFeedbackConstants], che il produttore tara sul proprio motore.
 */
object HapticPlan {
    // Valori di VibrationEffect.Composition (stabili da API 30/31)
    const val PRIMITIVE_CLICK = 1
    const val PRIMITIVE_THUD = 2
    const val PRIMITIVE_SPIN = 3
    const val PRIMITIVE_QUICK_RISE = 4
    const val PRIMITIVE_SLOW_RISE = 5
    const val PRIMITIVE_QUICK_FALL = 6
    const val PRIMITIVE_TICK = 7
    const val PRIMITIVE_LOW_TICK = 8

    // Valori di HapticFeedbackConstants
    const val FB_LONG_PRESS = 0
    const val FB_VIRTUAL_KEY = 1
    const val FB_KEYBOARD_TAP = 3
    const val FB_CLOCK_TICK = 4
    const val FB_CONTEXT_CLICK = 6
    const val FB_CONFIRM = 16 // API 30
    const val FB_GESTURE_THRESHOLD_ACTIVATE = 23 // API 34
    const val FB_GESTURE_THRESHOLD_DEACTIVATE = 24 // API 34
    const val FB_SEGMENT_TICK = 26 // API 34
    const val FB_SEGMENT_FREQUENT_TICK = 27 // API 34

    /** Intervallo minimo tra due colpi uguali e ravvicinati (scorrimento veloce dell'alfabeto). */
    const val MIN_GAP_MS = 28L

    /** Composizione per [event] con le primitive [supported]; null se ne manca una (si usa il ripiego). */
    fun composition(event: HapticEvent, supported: Set<Int>): List<HapticStep>? {
        // LOW_TICK è il colpo più delicato; se manca, un TICK attenuato fa quasi lo stesso
        val soft = if (PRIMITIVE_LOW_TICK in supported) HapticStep(PRIMITIVE_LOW_TICK, 0.7f) else HapticStep(PRIMITIVE_TICK, 0.3f)
        val steps = when (event) {
            HapticEvent.LETTER -> listOf(soft)
            HapticEvent.EDGE -> listOf(HapticStep(PRIMITIVE_TICK, 0.9f))
            HapticEvent.THRESHOLD_ON -> listOf(HapticStep(PRIMITIVE_CLICK, 0.7f))
            HapticEvent.THRESHOLD_OFF -> listOf(HapticStep(PRIMITIVE_TICK, 0.4f))
            HapticEvent.LONG_PRESS -> listOf(HapticStep(PRIMITIVE_CLICK, 1f))
            HapticEvent.OPEN -> listOf(HapticStep(PRIMITIVE_QUICK_RISE, 0.35f), HapticStep(PRIMITIVE_TICK, 0.5f, 10))
            HapticEvent.CONFIRM -> listOf(HapticStep(PRIMITIVE_CLICK, 0.6f), HapticStep(PRIMITIVE_TICK, 0.8f, 70))
        }
        return steps.takeIf { s -> s.all { it.primitive in supported } }
    }

    /** Costante di sistema per [event] su [api], quando la composizione non è disponibile. */
    fun fallback(event: HapticEvent, api: Int): Int = when (event) {
        HapticEvent.LETTER -> if (api >= 34) FB_SEGMENT_FREQUENT_TICK else FB_CLOCK_TICK
        HapticEvent.EDGE -> if (api >= 34) FB_SEGMENT_TICK else FB_CLOCK_TICK
        HapticEvent.THRESHOLD_ON -> if (api >= 34) FB_GESTURE_THRESHOLD_ACTIVATE else FB_CONTEXT_CLICK
        HapticEvent.THRESHOLD_OFF -> if (api >= 34) FB_GESTURE_THRESHOLD_DEACTIVATE else FB_CLOCK_TICK
        HapticEvent.LONG_PRESS -> FB_LONG_PRESS
        HapticEvent.OPEN -> FB_KEYBOARD_TAP
        HapticEvent.CONFIRM -> if (api >= 30) FB_CONFIRM else FB_VIRTUAL_KEY
    }

    /** Vero se il colpo va suonato: gli eventi frequenti rispettano [MIN_GAP_MS]. */
    fun shouldPlay(event: HapticEvent, nowMs: Long, lastLetterMs: Long): Boolean =
        event != HapticEvent.LETTER || nowMs - lastLetterMs >= MIN_GAP_MS
}
