// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Contrasto secondo WCAG 2.x, su colori ARGB (l'opacità è ignorata). */
object Wcag {
    const val AA_TEXT = 4.5

    private fun channel(v: Int): Double {
        val c = v / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    fun luminance(argb: Int): Double =
        0.2126 * channel((argb shr 16) and 0xFF) + 0.7152 * channel((argb shr 8) and 0xFF) + 0.0722 * channel(argb and 0xFF)

    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** Bianco o quasi nero, il più leggibile sopra [background]. */
    fun bestOn(background: Int): Int = if (contrast(WHITE, background) >= contrast(NEAR_BLACK, background)) WHITE else NEAR_BLACK

    /**
     * Schiarisce o scurisce [color] (tenendo tinta e saturazione) finché ha almeno [min] di contrasto
     * con ognuno di [backgrounds]. Se non basta, ripiega su bianco o nero.
     */
    fun ensure(color: Int, backgrounds: List<Int>, min: Double = AA_TEXT): Int {
        fun ok(c: Int) = backgrounds.all { contrast(c, it) >= min }
        if (ok(color)) return color
        val (h, s, l) = Hsl.of(color)
        val towardLight = backgrounds.map { luminance(it) }.average() < 0.3
        var step = 1
        while (step <= 40) {
            val nl = if (towardLight) l + (1.0 - l) * step / 40.0 else l * (1.0 - step / 40.0)
            val candidate = Hsl.argb(h, s, nl)
            if (ok(candidate)) return candidate
            step++
        }
        return if (towardLight) WHITE else NEAR_BLACK
    }

    const val WHITE = 0xFFFFFFFF.toInt()
    const val NEAR_BLACK = 0xFF0B0D0E.toInt()
}

/** Conversioni tinta-saturazione-luminosità (HSL) su colori ARGB. */
object Hsl {
    /** Tinta in gradi (0–360), saturazione e luminosità (0–1). */
    fun of(argb: Int): Triple<Double, Double, Double> {
        val r = ((argb shr 16) and 0xFF) / 255.0
        val g = ((argb shr 8) and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0
        val hi = max(r, max(g, b))
        val lo = min(r, min(g, b))
        val l = (hi + lo) / 2.0
        val d = hi - lo
        if (d < 1e-9) return Triple(0.0, 0.0, l)
        val s = d / (1.0 - abs(2.0 * l - 1.0))
        val h = when (hi) {
            r -> 60.0 * (((g - b) / d) % 6.0)
            g -> 60.0 * ((b - r) / d + 2.0)
            else -> 60.0 * ((r - g) / d + 4.0)
        }
        return Triple((h + 360.0) % 360.0, s.coerceIn(0.0, 1.0), l)
    }

    fun argb(h: Double, s: Double, l: Double): Int {
        val ss = s.coerceIn(0.0, 1.0)
        val ll = l.coerceIn(0.0, 1.0)
        val c = (1.0 - abs(2.0 * ll - 1.0)) * ss
        val hh = ((h % 360.0) + 360.0) % 360.0 / 60.0
        val x = c * (1.0 - abs(hh % 2.0 - 1.0))
        val (r1, g1, b1) = when {
            hh < 1 -> Triple(c, x, 0.0)
            hh < 2 -> Triple(x, c, 0.0)
            hh < 3 -> Triple(0.0, c, x)
            hh < 4 -> Triple(0.0, x, c)
            hh < 5 -> Triple(x, 0.0, c)
            else -> Triple(c, 0.0, x)
        }
        val m = ll - c / 2.0
        fun byte(v: Double) = ((v + m) * 255.0 + 0.5).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (byte(r1) shl 16) or (byte(g1) shl 8) or byte(b1)
    }
}

/** I colori del tema, pronti per costruire lo schema dell'interfaccia. */
data class PaletteRoles(
    val primary: Int,
    val onPrimary: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int,
    val accent: Int,
    val onAccent: Int,
    val accentContainer: Int,
    val onAccentContainer: Int,
    val surface: Int,
    val surfaceContainerLow: Int,
    val surfaceContainer: Int,
    val surfaceContainerHigh: Int,
    val surfaceContainerHighest: Int,
    val onSurface: Int,
    val onSurfaceVariant: Int,
    val outline: Int,
)

/**
 * Il tema ruota su DUE colori: il primario (superfici, contenitori, componenti) e l'accento (evidenziazioni,
 * interruttori, lettere). Da ognuno si ricavano tinte con contrasti forti, e ogni coppia testo-sfondo
 * arriva ad almeno AA (4,5:1), in chiaro e in scuro. Logica pura, testata su JVM.
 */
object ThemePalette {
    /** Distanza di tinta usata per l'accento quando non è scelto né dato dal sistema. */
    private const val AUTO_ACCENT_HUE_SHIFT = 40.0

    /** Contrasto minimo tra un contenitore colorato e la superficie. */
    const val CONTAINER_CONTRAST = 1.25

    /**
     * Colori di partenza: il primario è quello di sistema (colori dinamici) oppure [staticAccent];
     * l'accento è quello scelto, altrimenti quello di sistema, altrimenti il primario con la tinta ruotata.
     */
    fun resolveSeeds(
        staticAccent: Long,
        dynamic: Boolean,
        chosenAccent: Long,
        systemPrimary: Int?,
        systemAccent: Int?,
    ): Pair<Int, Int> {
        val primary = if (dynamic && systemPrimary != null) systemPrimary else staticAccent.toInt()
        val accent = when {
            chosenAccent != 0L -> chosenAccent.toInt()
            dynamic && systemAccent != null -> systemAccent
            else -> rotateHue(primary, AUTO_ACCENT_HUE_SHIFT)
        }
        return primary to accent
    }

    fun rotateHue(argb: Int, degrees: Double): Int {
        val (h, s, l) = Hsl.of(argb)
        return Hsl.argb(h + degrees, s, l)
    }

    /** Grigi e quasi grigi restano neutri: non vanno colorati forzando la saturazione. */
    private fun sat(s: Double, low: Double, high: Double) = if (s < 0.08) 0.0 else s.coerceIn(low, high)

    fun roles(primarySeed: Int, accentSeed: Int, dark: Boolean): PaletteRoles {
        val (hp, sp0, _) = Hsl.of(primarySeed)
        val (ha, sa0, _) = Hsl.of(accentSeed)
        val sp = sat(sp0, 0.55, 0.9)
        val sa = sat(sa0, 0.6, 0.95)
        val tint = if (sp0 < 0.08) 0.0 else 0.3
        val surface: Int
        val low: Int
        val container: Int
        val high: Int
        val highest: Int
        val onSurface0: Int
        val onVariant0: Int
        val primary0: Int
        val primaryContainer0: Int
        val onPrimaryContainer0: Int
        val accent0: Int
        val accentContainer0: Int
        val onAccentContainer0: Int
        if (dark) {
            surface = Hsl.argb(hp, tint, 0.07)
            low = Hsl.argb(hp, tint, 0.095)
            container = Hsl.argb(hp, tint, 0.12)
            high = Hsl.argb(hp, tint, 0.17)
            highest = Hsl.argb(hp, tint, 0.22)
            onSurface0 = Hsl.argb(hp, tint / 2, 0.93)
            onVariant0 = Hsl.argb(hp, tint / 2, 0.76)
            primary0 = Hsl.argb(hp, sp, 0.72)
            primaryContainer0 = Hsl.argb(hp, sat(sp0, 0.45, 0.85), 0.22)
            onPrimaryContainer0 = Hsl.argb(hp, sat(sp0, 0.5, 0.8), 0.92)
            accent0 = Hsl.argb(ha, sa, 0.7)
            accentContainer0 = Hsl.argb(ha, sat(sa0, 0.5, 0.9), 0.24)
            onAccentContainer0 = Hsl.argb(ha, sat(sa0, 0.5, 0.8), 0.92)
        } else {
            surface = Hsl.argb(hp, tint, 0.97)
            low = Hsl.argb(hp, tint + 0.03, 0.945)
            container = Hsl.argb(hp, tint + 0.05, 0.92)
            high = Hsl.argb(hp, tint + 0.08, 0.87)
            highest = Hsl.argb(hp, tint + 0.1, 0.82)
            onSurface0 = Hsl.argb(hp, tint, 0.09)
            onVariant0 = Hsl.argb(hp, tint / 2, 0.28)
            primary0 = Hsl.argb(hp, sp, 0.32)
            primaryContainer0 = Hsl.argb(hp, sat(sp0, 0.6, 1.0), 0.8)
            onPrimaryContainer0 = Hsl.argb(hp, sat(sp0, 0.6, 0.95), 0.14)
            accent0 = Hsl.argb(ha, sa, 0.36)
            accentContainer0 = Hsl.argb(ha, sat(sa0, 0.6, 1.0), 0.8)
            onAccentContainer0 = Hsl.argb(ha, sat(sa0, 0.6, 0.95), 0.14)
        }
        // I contenitori si staccano dalla superficie (almeno 1,25:1) e il testo ci arriva ad AA
        val primaryContainer = Wcag.ensure(primaryContainer0, listOf(surface), CONTAINER_CONTRAST)
        val accentContainer = Wcag.ensure(accentContainer0, listOf(surface), CONTAINER_CONTRAST)
        val panels = listOf(surface, low, container, high, highest)
        // Ogni testo arriva ad AA su tutti gli sfondi su cui può comparire
        val primary = Wcag.ensure(primary0, panels)
        val accent = Wcag.ensure(accent0, panels)
        return PaletteRoles(
            primary = primary,
            onPrimary = Wcag.bestOn(primary),
            primaryContainer = primaryContainer,
            onPrimaryContainer = Wcag.ensure(onPrimaryContainer0, listOf(primaryContainer)),
            accent = accent,
            onAccent = Wcag.bestOn(accent),
            accentContainer = accentContainer,
            onAccentContainer = Wcag.ensure(onAccentContainer0, listOf(accentContainer)),
            surface = surface,
            surfaceContainerLow = low,
            surfaceContainer = container,
            surfaceContainerHigh = high,
            surfaceContainerHighest = highest,
            onSurface = Wcag.ensure(onSurface0, panels),
            onSurfaceVariant = Wcag.ensure(onVariant0, panels),
            outline = Wcag.ensure(Hsl.argb(hp, tint, if (dark) 0.5 else 0.45), listOf(surface), 3.0),
        )
    }
}
