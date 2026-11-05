// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Meteo letto dall'app Breezy Weather installata, attraverso il suo provider locale.
 * Onda non si collega a internet: è Breezy Weather a scaricare le previsioni.
 */
class WeatherRepository(private val context: Context) {

    fun isInstalled(): Boolean = runCatching {
        context.packageManager.getPackageInfo(PACKAGE, 0)
        true
    }.getOrDefault(false)

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED

    /** Previsioni della prima località di Breezy Weather; null se non sono disponibili. */
    suspend fun load(): WeatherSnapshot? = withContext(Dispatchers.IO) {
        if (!isInstalled() || !hasPermission()) return@withContext null
        runCatching { read() }.getOrNull()
    }

    private fun read(): WeatherSnapshot? {
        if (!isCompatible()) return null
        val resolver = context.contentResolver
        val locations = base.buildUpon().appendPath("locations").appendQueryParameter("limit", "1").build()
        val (id, place) = resolver.query(locations, null, null, null, null)?.use { c ->
            if (!c.moveToFirst()) return null
            val id = c.text("id") ?: return null
            id to (c.text("custom_name") ?: c.text("city") ?: c.text("district").orEmpty())
        } ?: return null
        val weather = base.buildUpon().appendPath("weather")
            .appendQueryParameter("withMinutely", "false")
            .appendQueryParameter("withAlerts", "false")
            .appendQueryParameter("withNormals", "false")
            .build()
        return resolver.query(weather, null, "id=$id", null, null)?.use { c ->
            if (!c.moveToFirst()) return null
            val index = c.getColumnIndex("weather")
            if (index < 0 || c.isNull(index)) return null
            WeatherParser.parse(WeatherParser.gunzip(c.getBlob(index)), place, System.currentTimeMillis())
        }
    }

    /** La versione maggiore cambia solo se il formato si rompe: con una diversa non si legge nulla. */
    private fun isCompatible(): Boolean {
        val version = base.buildUpon().appendPath("version").build()
        return context.contentResolver.query(version, null, null, null, null)?.use { c ->
            c.moveToFirst() && c.getColumnIndex("major").let { it >= 0 && c.getInt(it) == SUPPORTED_MAJOR }
        } ?: false
    }

    private fun Cursor.text(column: String): String? =
        getColumnIndex(column).takeIf { it >= 0 }?.let { getString(it) }?.takeIf { it.isNotBlank() }

    /** Apre Breezy Weather; false se non è installata. */
    fun open(): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE) ?: return false
        return runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
    }

    companion object {
        const val PACKAGE = "org.breezyweather"
        const val PERMISSION = "org.breezyweather.READ_PROVIDER"
        private const val SUPPORTED_MAJOR = 0
        private val base: Uri = Uri.parse("content://$PACKAGE.provider.weather")
    }
}
