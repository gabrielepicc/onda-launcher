// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import kotlin.math.roundToInt

data class WeatherHour(val time: Long, val temperature: Int?)

data class WeatherDay(val date: Long, val high: Int?, val low: Int?, val text: String?)

/** Meteo pronto per la UI. Le temperature sono nell'unità scelta dall'utente in Breezy Weather. */
data class WeatherSnapshot(
    val place: String,
    val temperature: Int?,
    val feelsLike: Int?,
    val text: String?,
    val refreshedAt: Long?,
    val hours: List<WeatherHour>,
    val days: List<WeatherDay>,
)

// Solo i campi che servono a Onda: tutto il resto del formato di Breezy Weather viene ignorato.
@Serializable
internal data class BreezyValue(val value: Double? = null)

@Serializable
internal data class BreezyTemperatures(
    val temperature: BreezyValue? = null,
    val sourceFeelsLike: BreezyValue? = null,
    val computedApparent: BreezyValue? = null,
)

@Serializable
internal data class BreezyNow(val weatherText: String? = null, val temperature: BreezyTemperatures? = null)

@Serializable
internal data class BreezyHour(val date: Long, val temperature: BreezyTemperatures? = null)

@Serializable
internal data class BreezyHalf(val weatherText: String? = null, val temperature: BreezyTemperatures? = null)

@Serializable
internal data class BreezyDay(val date: Long, val day: BreezyHalf? = null, val night: BreezyHalf? = null)

@Serializable
internal data class BreezyPayload(
    val refreshTime: Long? = null,
    val current: BreezyNow? = null,
    val daily: List<BreezyDay>? = null,
    val hourly: List<BreezyHour>? = null,
)

/** Lettura dei dati pubblicati da Breezy Weather, senza dipendenze Android (testabile su JVM). */
object WeatherParser {
    private const val HOUR_MS = 3_600_000L
    private const val DAY_MS = 24 * HOUR_MS
    private const val MAX_HOURS = 24
    private const val MAX_DAYS = 7

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    /** Il campo "weather" del provider è un JSON compresso con GZIP. */
    fun gunzip(bytes: ByteArray): String =
        GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader(Charsets.UTF_8).use { it.readText() }

    /** Null se il testo non è leggibile o non contiene nessun dato meteo. */
    fun parse(text: String, place: String, now: Long): WeatherSnapshot? {
        val payload = runCatching { json.decodeFromString(BreezyPayload.serializer(), text) }.getOrNull() ?: return null
        val current = payload.current
        val hours = payload.hourly.orEmpty()
            .filter { it.date >= now - HOUR_MS }
            .sortedBy { it.date }
            .take(MAX_HOURS)
            .map { WeatherHour(it.date, it.temperature?.temperature.rounded()) }
        val days = payload.daily.orEmpty()
            // Comprende anche oggi, che è iniziato prima di adesso
            .filter { it.date >= now - DAY_MS }
            .sortedBy { it.date }
            .take(MAX_DAYS)
            .map {
                WeatherDay(
                    date = it.date,
                    high = it.day?.temperature?.temperature.rounded(),
                    low = it.night?.temperature?.temperature.rounded(),
                    text = it.day?.weatherText ?: it.night?.weatherText,
                )
            }
        val temperature = current?.temperature?.temperature.rounded()
        if (temperature == null && hours.isEmpty() && days.isEmpty()) return null
        return WeatherSnapshot(
            place = place,
            temperature = temperature,
            feelsLike = (current?.temperature?.sourceFeelsLike ?: current?.temperature?.computedApparent).rounded(),
            text = current?.weatherText,
            refreshedAt = payload.refreshTime,
            hours = hours,
            days = days,
        )
    }

    private fun BreezyValue?.rounded(): Int? = this?.value?.takeIf { it.isFinite() }?.roundToInt()
}
