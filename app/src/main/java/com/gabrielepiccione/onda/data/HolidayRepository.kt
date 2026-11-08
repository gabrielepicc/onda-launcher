// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Giorni festivi dal calendario «Festività in Italia» di Google, se è sul telefono (account Google con
 * il calendario delle festività attivo) e il permesso del calendario c'è. Solo lettura, solo sul telefono.
 */
class HolidayRepository(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /**
     * Le date festive attorno a [today] (ieri, oggi, domani), o null se manca il permesso o il calendario delle
     * festività: allora vale l'elenco interno ([Holidays.builtInItalian]). Il risultato vale per tutta la giornata.
     */
    suspend fun around(today: LocalDate): Set<LocalDate>? {
        cache?.let { (day, set) -> if (day == today) return set }
        val read = read(today.minusDays(1), today.plusDays(1))
        cache = today to read
        return read
    }

    private suspend fun read(from: LocalDate, to: LocalDate): Set<LocalDate>? = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext null
        val ids = calendarIds()
        if (ids.isEmpty()) return@withContext null
        val zone = ZoneId.systemDefault()
        val begin = from.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = to.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, begin)
            ContentUris.appendId(it, end)
        }.build()
        val projection = arrayOf(CalendarContract.Instances.BEGIN, CalendarContract.Instances.END, CalendarContract.Instances.ALL_DAY)
        val out = HashSet<LocalDate>()
        runCatching {
            context.contentResolver.query(uri, projection, "${CalendarContract.Instances.CALENDAR_ID} IN (${ids.joinToString(",")})", null, null)?.use { c ->
                while (c.moveToNext()) {
                    val start = c.getLong(0)
                    val finish = c.getLong(1)
                    if (c.getInt(2) == 1) {
                        // Gli eventi "tutto il giorno" sono in UTC: la fine non è inclusa
                        var d = Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC).toLocalDate()
                        val last = Instant.ofEpochMilli(finish).atZone(ZoneOffset.UTC).toLocalDate()
                        while (d < last) {
                            out += d
                            d = d.plusDays(1)
                        }
                    } else {
                        out += Instant.ofEpochMilli(start).atZone(zone).toLocalDate()
                    }
                }
            }
        }
        out
    }

    private fun calendarIds(): List<Long> = runCatching {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.OWNER_ACCOUNT,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        )
        buildList {
            context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    if (Holidays.isItalianHolidayCalendar(c.getString(1), c.getString(2))) add(c.getLong(0))
                }
            }
        }
    }.getOrDefault(emptyList())

    private companion object {
        @Volatile
        var cache: Pair<LocalDate, Set<LocalDate>?>? = null
    }
}
