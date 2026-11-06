// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import kotlin.math.hypot
import kotlin.math.max

/**
 * Pulsazione sui bassi per lo sfondo "a ritmo di musica". Riceve lo spettro dell'audio in riproduzione
 * dall'analizzatore di Android (solo ampiezze, istante per istante: nessun audio viene registrato o salvato)
 * e restituisce un valore tra 0 e 1 che sale in fretta sui colpi e scende morbido. Testabile su JVM.
 */
class BeatTracker {
    private var level = 0f
    private var peak = MIN_PEAK

    /** Nuova lettura dell'energia dei bassi; restituisce la pulsazione da 0 a 1. */
    fun update(energy: Float): Float {
        // Il massimo recente si adatta al volume: a basso volume la pulsazione resta visibile
        peak = max(MIN_PEAK, max(energy, peak * PEAK_DECAY))
        val target = (energy / peak).coerceIn(0f, 1f)
        level = if (target > level) level + (target - level) * ATTACK else level + (target - level) * RELEASE
        return level
    }

    fun reset() {
        level = 0f
        peak = MIN_PEAK
    }

    companion object {
        const val BASS_LOW_HZ = 20
        const val BASS_HIGH_HZ = 150
        private const val ATTACK = 0.6f
        private const val RELEASE = 0.12f
        private const val PEAK_DECAY = 0.995f
        private const val MIN_PEAK = 4f

        /**
         * Energia media tra [BASS_LOW_HZ] e [BASS_HIGH_HZ] nello spettro nel formato di Android
         * (Visualizer.getFft: [0] parte reale della componente continua, [1] di Nyquist, poi coppie reale/immaginaria).
         * [samplingRateMilliHz] come lo dà Visualizer.getSamplingRate().
         */
        fun bassEnergy(fft: ByteArray, samplingRateMilliHz: Int): Float {
            val n = fft.size
            if (n < 4 || samplingRateMilliHz <= 0) return 0f
            val binHz = samplingRateMilliHz / 1000f / n
            var sum = 0f
            var count = 0
            var k = 1
            while (2 * k + 1 < n) {
                val hz = k * binHz
                if (hz > BASS_HIGH_HZ) break
                if (hz >= BASS_LOW_HZ) {
                    sum += hypot(fft[2 * k].toFloat(), fft[2 * k + 1].toFloat())
                    count++
                }
                k++
            }
            return if (count == 0) 0f else sum / count
        }
    }
}
