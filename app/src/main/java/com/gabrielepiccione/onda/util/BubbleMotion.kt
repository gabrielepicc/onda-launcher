// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Fisica della bolla del popup, senza dipendenze Android (testabile su JVM). Una bolla vera non si
 * rimpicciolisce: si fora in un punto, la pellicola si ritira dal foro e il bordo si rompe in goccioline.
 * Le misure sono in pixel, i tempi vanno da 0 a 1.
 */
object BubbleMotion {
    /** Dove si fora la bolla: in alto a sinistra, in radianti. */
    const val PUNCTURE_ANGLE = -2.35f

    /** Quanto la bolla si tende prima di forarsi, rispetto al raggio di riposo. */
    private const val END_RADIUS_RATIO = 0.92f

    /** Parte del tempo di scoppio in cui la pellicola sparisce: le gocce volano ancora dopo. */
    private const val FILM_END = 0.45f

    /** Una goccia: dove nasce sul bordo, quanto corre, quanto è grande e quando parte. */
    data class Droplet(val angle: Float, val speed: Float, val size: Float, val delay: Float)

    /** Stato di una goccia in un istante: distanza dal centro in pixel, raggio e opacità. */
    data class DropletState(val x: Float, val y: Float, val radius: Float, val alpha: Float)

    /** Distanza dal punto (x, y) all'angolo più lontano: il cerchio di questo raggio copre tutto il popup. */
    fun coveringRadius(width: Float, height: Float, x: Float, y: Float): Float =
        hypot(max(x, width - x), max(y, height - y))

    /** Raggio della bolla quando si è raccolta: tonda dentro il popup, per quanto stretto sia. */
    fun roundRadius(width: Float, height: Float): Float = min(width, height) / 2f * END_RADIUS_RATIO

    /** Raggio che cresce aprendo: da [start] (l'icona) al raggio che copre tutto. La molla può superare 1. */
    fun openRadius(progress: Float, start: Float, covering: Float): Float =
        max(0f, start + (covering - start) * progress)

    /** Raggio mentre si raccoglie in un cerchio: 0 = copre tutto, 1 = bolla tonda. */
    fun gatherRadius(gather: Float, covering: Float, round: Float): Float =
        covering + (round - covering) * gather.coerceIn(0f, 1f)

    /** Raggio del foro nella pellicola: corre veloce fino a superare tutta la bolla. */
    fun holeRadius(burst: Float, bubbleRadius: Float): Float {
        val t = (burst / FILM_END).coerceIn(0f, 1f)
        // Decelera un poco: il foro è rapidissimo all'inizio, come la pellicola che si ritira
        return bubbleRadius * 2.1f * (1f - (1f - t) * (1f - t))
    }

    /** Vero quando della pellicola non resta nulla: si disegnano solo le gocce. */
    fun filmGone(burst: Float): Boolean = burst >= FILM_END

    /** Le gocce nascono più tardi quanto più sono lontane dal foro, così l'onda si vede girare. */
    fun droplets(seed: Int, count: Int): List<Droplet> {
        val random = Random(seed)
        return List(count) { i ->
            val angle = (2f * PI.toFloat() * i / count) + (random.nextFloat() - 0.5f) * 0.35f
            Droplet(
                angle = angle,
                speed = 0.35f + random.nextFloat() * 0.75f,
                size = 0.5f + random.nextFloat() * 0.8f,
                delay = 0.4f * angleFromPuncture(angle) / PI.toFloat(),
            )
        }
    }

    /** Distanza angolare dal foro, da 0 a pi greco. */
    fun angleFromPuncture(angle: Float): Float {
        val twoPi = 2f * PI.toFloat()
        val diff = abs(((angle - PUNCTURE_ANGLE) % twoPi + twoPi) % twoPi)
        return min(diff, twoPi - diff)
    }

    /** Posizione di una goccia rispetto al centro della bolla, con un poco di gravità (y cresce in basso). */
    fun dropletAt(droplet: Droplet, burst: Float, bubbleRadius: Float, baseRadius: Float): DropletState {
        val t = ((burst - droplet.delay) / (1f - droplet.delay).coerceAtLeast(0.01f)).coerceIn(0f, 1f)
        if (burst < droplet.delay) return DropletState(0f, 0f, 0f, 0f)
        val eased = 1f - (1f - t) * (1f - t) * (1f - t)
        val distance = bubbleRadius * (1f + droplet.speed * eased)
        return DropletState(
            x = cos(droplet.angle) * distance,
            y = sin(droplet.angle) * distance + bubbleRadius * 0.35f * t * t,
            radius = baseRadius * droplet.size * (1f - 0.65f * t),
            alpha = 1f - t * t,
        )
    }
}
