// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat

/**
 * Dove si trova il telefono, per le luci di casa: il nome del Wi-Fi a cui è collegato e, una volta sola
 * quando si imposta la casa, la posizione approssimativa per calcolare alba e tramonto. Android chiede
 * il permesso di posizione anche solo per leggere il nome del Wi-Fi. Niente lascia il telefono.
 */
object HomePresence {
    /** Il pacchetto dell'app Google Home, per filtrare i suoi widget nel selettore. */
    const val GOOGLE_HOME_PACKAGE = "com.google.android.apps.chromecast.app"

    val PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Nome del Wi-Fi collegato, già senza virgolette; null se non c'è, se manca il permesso o se la posizione è spenta. */
    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    fun currentSsid(context: Context): String? {
        if (!hasPermission(context)) return null
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java) ?: return null
        return HomeLights.normalizeSsid(runCatching { wifi.connectionInfo?.ssid }.getOrNull())
    }

    /** Ultima posizione nota, senza accendere il GPS: per alba e tramonto basta quella approssimativa. */
    @SuppressLint("MissingPermission")
    fun lastLocation(context: Context): Pair<Double, Double>? {
        if (!hasPermission(context)) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        return listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.let { it.latitude to it.longitude }
    }
}
