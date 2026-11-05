// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Dove si trova il telefono, per le luci di casa e i preferiti contestuali: il nome del Wi-Fi a cui è
 * collegato e la posizione approssimativa (per alba e tramonto). Android chiede il permesso di
 * posizione anche solo per leggere il nome del Wi-Fi, e la posizione del telefono deve essere accesa.
 * Niente lascia il telefono.
 */
object HomePresence {
    /** Il pacchetto dell'app Google Home, per filtrare i suoi widget nel selettore. */
    const val GOOGLE_HOME_PACKAGE = "com.google.android.apps.chromecast.app"

    val PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    /** Ultimo nome del Wi-Fi visto dalla richiesta di rete (Android 12+), che è la via affidabile. */
    @Volatile
    private var watchedSsid: String? = null
    private var watching = false

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** La posizione del telefono è accesa? Senza, Android non dice il nome del Wi-Fi. */
    fun locationEnabled(context: Context): Boolean =
        context.getSystemService(LocationManager::class.java)?.let { LocationManagerCompat.isLocationEnabled(it) } == true

    /**
     * Nome del Wi-Fi collegato, già senza virgolette; null se non c'è Wi-Fi, se manca il permesso o se la
     * posizione è spenta. Da Android 12 la lettura diretta spesso restituisce "<unknown ssid>": si segue
     * quindi la rete Wi-Fi con una richiesta che include le informazioni di posizione.
     */
    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    fun currentSsid(context: Context): String? {
        if (!hasPermission(context)) return null
        watch(context)
        watchedSsid?.let { return it }
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java) ?: return null
        return HomeLights.normalizeSsid(runCatching { wifi.connectionInfo?.ssid }.getOrNull())
    }

    private fun watch(context: Context) {
        if (watching || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val connectivity = context.applicationContext.getSystemService(ConnectivityManager::class.java) ?: return
        watching = true
        val request = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
        val callback = object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val info = caps.transportInfo as? WifiInfo ?: return
                watchedSsid = HomeLights.normalizeSsid(info.ssid)
            }

            override fun onLost(network: Network) {
                watchedSsid = null
            }
        }
        runCatching { connectivity.registerNetworkCallback(request, callback) }.onFailure { watching = false }
    }

    /** Ultima posizione nota, senza accendere il GPS: per alba e tramonto basta quella approssimativa. */
    @SuppressLint("MissingPermission")
    fun lastLocation(context: Context): Pair<Double, Double>? {
        if (!hasPermission(context)) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        return manager.allProviders
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.let { it.latitude to it.longitude }
    }

    /**
     * Posizione approssimativa adesso: l'ultima nota o, se il telefono non ne ha una, una lettura nuova
     * dalla rete (al massimo [timeoutMs]). Serve una volta, quando si imposta la casa.
     */
    @SuppressLint("MissingPermission")
    suspend fun locate(context: Context, timeoutMs: Long = 15_000L): Pair<Double, Double>? {
        lastLocation(context)?.let { return it }
        if (!hasPermission(context)) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val provider = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) } ?: return null
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val cancel = CancellationSignal()
                cont.invokeOnCancellation { cancel.cancel() }
                LocationManagerCompat.getCurrentLocation(manager, provider, cancel, ContextCompat.getMainExecutor(context)) { location: Location? ->
                    if (cont.isActive) cont.resume(location?.let { it.latitude to it.longitude })
                }
            }
        }
    }
}
