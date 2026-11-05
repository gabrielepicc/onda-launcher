// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Come si comporta lo sfondo con la musica. */
@Serializable
enum class MusicBackdropMode { OFF, COLORS, MOTION, BEAT }

/** Parametri dell'animazione, tutti tra 0 e 1 tranne la velocità (giri al secondo, circa). */
data class MoodParams(val speed: Float, val turbulence: Float, val brightness: Float, val pulse: Float)

/**
 * "Carattere" di un brano stimato dai colori della copertina, senza permessi: colori accesi e
 * contrastati danno un movimento vivace, colori scuri o tenui un movimento lento e ampio. Contiene anche
 * le regole che tengono lo strato sempre sotto il testo per leggibilità. Testabile su JVM.
 */
object MusicMood {
    /** Fuori dal tema scuro il testo è scuro: lo strato deve restare chiaro, e viceversa. */
    const val MAX_LUMINANCE_DARK = 0.12
    const val MIN_LUMINANCE_LIGHT = 0.32

    /** Riprende da una copertina tipica: movimento medio. */
    val NEUTRAL = MoodParams(speed = 0.35f, turbulence = 0.4f, brightness = 0.5f, pulse = 0.04f)

    fun from(colors: List<Int>): MoodParams {
        if (colors.isEmpty()) return NEUTRAL
        val hsl = colors.map { hsl(it) }
        val saturation = hsl.map { it[1] }.average().toFloat()
        val lightness = hsl.map { it[2] }.average().toFloat()
        val contrast = (hsl.maxOf { it[2] } - hsl.minOf { it[2] }).toFloat()
        // Energia: colori pieni e contrastati alzano, colori spenti o molto scuri abbassano
        val energy = (0.55f * saturation + 0.45f * contrast).coerceIn(0f, 1f) * (0.6f + 0.4f * lightness.coerceIn(0f, 1f))
        return MoodParams(
            speed = 0.15f + 0.85f * energy,
            turbulence = (0.2f + 0.8f * contrast).coerceIn(0f, 1f),
            brightness = (0.35f + 0.5f * lightness).coerceIn(0.35f, 0.85f),
            pulse = 0.02f + 0.1f * energy,
        )
    }

    /** Fermata la musica: velocità che scende a zero in [SLOWDOWN] e opacità che svanisce nel tempo. */
    const val SLOWDOWN_MS = 2_000L
    const val FADE_MS = 1_500L

    fun speedAfterStop(params: MoodParams, sinceStopMs: Long): Float =
        params.speed * (1f - (sinceStopMs.toFloat() / SLOWDOWN_MS).coerceIn(0f, 1f))

    fun alphaAfterStop(sinceStopMs: Long): Float = 1f - ((sinceStopMs - SLOWDOWN_MS + FADE_MS).toFloat() / FADE_MS).coerceIn(0f, 1f)

    /**
     * Colore dello strato reso leggibile: con il tema scuro (testo chiaro) lo si scurisce finché la
     * luminanza resta sotto [MAX_LUMINANCE_DARK]; con il tema chiaro lo si schiarisce fino a [MIN_LUMINANCE_LIGHT].
     * Così il testo mantiene almeno 4,5:1 qualunque sia la copertina.
     */
    fun readable(argb: Int, dark: Boolean): Int {
        val r = (argb shr 16 and 0xFF) / 255.0
        val g = (argb shr 8 and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0
        return if (dark) {
            var k = 1.0
            while (luminance(r * k, g * k, b * k) > MAX_LUMINANCE_DARK && k > 0.02) k *= 0.9
            rgb(r * k, g * k, b * k)
        } else {
            var t = 0.0
            while (luminance(mix(r, t), mix(g, t), mix(b, t)) < MIN_LUMINANCE_LIGHT && t < 1.0) t = min(1.0, t + 0.05)
            rgb(mix(r, t), mix(g, t), mix(b, t))
        }
    }

    /** Luminanza relativa (WCAG) di un colore sRGB con componenti 0..1. */
    fun luminance(r: Double, g: Double, b: Double): Double {
        fun lin(c: Double) = if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        return 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b)
    }

    fun luminance(argb: Int): Double = luminance((argb shr 16 and 0xFF) / 255.0, (argb shr 8 and 0xFF) / 255.0, (argb and 0xFF) / 255.0)

    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** Tonalità, saturazione e luminosità (HSL), tutte 0..1. */
    fun hsl(argb: Int): DoubleArray {
        val r = (argb shr 16 and 0xFF) / 255.0
        val g = (argb shr 8 and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val l = (maxC + minC) / 2
        val d = maxC - minC
        if (d == 0.0) return doubleArrayOf(0.0, 0.0, l)
        val s = d / (1 - kotlin.math.abs(2 * l - 1))
        val h = when (maxC) {
            r -> ((g - b) / d).mod(6.0)
            g -> (b - r) / d + 2
            else -> (r - g) / d + 4
        } / 6
        return doubleArrayOf(h, s.coerceIn(0.0, 1.0), l)
    }

    private fun mix(c: Double, t: Double) = c + (1 - c) * t

    private fun rgb(r: Double, g: Double, b: Double): Int {
        fun c(v: Double) = (v.coerceIn(0.0, 1.0) * 255 + 0.5).toInt()
        return (0xFF shl 24) or (c(r) shl 16) or (c(g) shl 8) or c(b)
    }
}
