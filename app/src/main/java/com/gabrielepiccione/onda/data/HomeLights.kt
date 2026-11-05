// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** Alba e tramonto di un giorno, in millisecondi UTC; null se il sole quel giorno non sorge o non tramonta. */
data class SunTimes(val sunrise: Long, val sunset: Long)

/**
 * Comandi delle luci di casa: il widget di Google Home accanto all'orologio, solo quando il telefono è
 * sul Wi-Fi di casa e fuori è buio. Calcoli senza dipendenze Android (testabili su JVM).
 */
object HomeLights {
    /** Il buio "di casa" inizia un po' prima del tramonto: con il sole basso dentro casa è già scuro. */
    const val DUSK_MARGIN_MS = 20 * 60_000L

    /** Android mostra il nome del Wi-Fi tra virgolette, o "<unknown ssid>" se non può leggerlo. */
    fun normalizeSsid(raw: String?): String? {
        val s = raw?.trim()?.removeSurrounding("\"")?.trim() ?: return null
        return s.takeIf { it.isNotEmpty() && it != "<unknown ssid>" }
    }

    fun isHome(currentSsid: String?, homeSsid: String): Boolean {
        val now = normalizeSsid(currentSsid) ?: return false
        val home = normalizeSsid(homeSsid) ?: return false
        return now == home
    }

    /** Buio: prima dell'alba o dal tramonto (meno il margine). Senza orari di sole, non si sa: no. */
    fun isDark(now: Long, sun: SunTimes?): Boolean {
        sun ?: return false
        return now < sun.sunrise || now >= sun.sunset - DUSK_MARGIN_MS
    }

    fun shouldShow(enabled: Boolean, widgetId: Int, currentSsid: String?, homeSsid: String, now: Long, sun: SunTimes?): Boolean =
        enabled && widgetId > 0 && isHome(currentSsid, homeSsid) && isDark(now, sun)

    /**
     * Alba e tramonto con l'algoritmo semplificato della NOAA (errore di pochi minuti), per [date] alla
     * latitudine e longitudine date. Null vicino ai poli, nei giorni senza alba o senza tramonto.
     */
    fun sunTimes(date: LocalDate, latitude: Double, longitude: Double): SunTimes? {
        val dayOfYear = date.dayOfYear
        val gamma = 2 * PI / 365.0 * (dayOfYear - 1)
        val eqTime = 229.18 * (
            0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
                0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma)
            )
        val decl = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
            0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) -
            0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)
        val lat = latitude * PI / 180.0
        val zenith = 90.833 * PI / 180.0
        val cosHa = cos(zenith) / (cos(lat) * cos(decl)) - tan(lat) * tan(decl)
        if (cosHa < -1.0 || cosHa > 1.0) return null
        val ha = acos(cosHa) * 180.0 / PI
        val midnightUtc = date.toEpochDay() * 86_400_000L
        val sunriseMin = 720 - 4 * (longitude + ha) - eqTime
        val sunsetMin = 720 - 4 * (longitude - ha) - eqTime
        return SunTimes(midnightUtc + (sunriseMin * 60_000).toLong(), midnightUtc + (sunsetMin * 60_000).toLong())
    }
}
