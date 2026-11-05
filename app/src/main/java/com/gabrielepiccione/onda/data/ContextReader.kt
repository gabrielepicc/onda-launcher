// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.time.LocalDateTime

/**
 * Legge il contesto per i preferiti contestuali: Wi-Fi, dispositivi Bluetooth collegati, posizione
 * approssimativa, eventi in corso, carica, cuffie e Non disturbare. Ogni lettura funziona solo se il
 * permesso relativo è stato dato; altrimenti quella condizione risulta semplicemente falsa.
 * Nulla lascia il telefono.
 */
class ContextReader(context: Context, private val audio: AudioDevices, private val calendar: CalendarRepository) {
    private val app = context.applicationContext
    private val bluetooth = BluetoothWatcher(app)

    fun start() = bluetooth.start()

    suspend fun snapshot(needsLocation: Boolean, needsCalendar: Boolean): ContextSnapshot = ContextSnapshot(
        time = LocalDateTime.now(),
        wifi = HomePresence.currentSsid(app),
        bluetooth = bluetooth.connected,
        location = if (needsLocation) HomePresence.lastLocation(app) else null,
        calendarNow = if (needsCalendar) calendar.currentEventTexts() else emptyList(),
        charging = app.getSystemService(BatteryManager::class.java)?.isCharging == true,
        headphones = audio.headphonesConnected.value,
        focus = app.getSystemService(NotificationManager::class.java)
            ?.currentInterruptionFilter?.let { it != NotificationManager.INTERRUPTION_FILTER_ALL && it != NotificationManager.INTERRUPTION_FILTER_UNKNOWN } == true,
    )

    /** Dispositivi Bluetooth già associati, per sceglierne uno nella condizione. */
    @SuppressLint("MissingPermission")
    fun bondedDevices(): List<String> {
        if (!BluetoothWatcher.hasPermission(app)) return emptyList()
        val adapter = app.getSystemService(BluetoothManager::class.java)?.adapter ?: return emptyList()
        return runCatching { adapter.bondedDevices.mapNotNull { it.name } }.getOrDefault(emptyList()).sorted()
    }
}

/**
 * Tiene il nome dei dispositivi Bluetooth collegati (auto, cuffie, orologio). Parte dai profili audio
 * già collegati e poi segue gli eventi di collegamento. Da Android 12 serve il permesso "Dispositivi nelle
 * vicinanze", chiesto solo quando si aggiunge una condizione Bluetooth.
 */
class BluetoothWatcher(private val context: Context) {
    @Volatile
    var connected: Set<String> = emptySet()
        private set
    private var started = false

    @SuppressLint("MissingPermission")
    fun start() {
        if (started || !hasPermission(context)) return
        started = true
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return
        listOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET).forEach { profile ->
            runCatching {
                adapter.getProfileProxy(
                    context,
                    object : BluetoothProfile.ServiceListener {
                        override fun onServiceConnected(p: Int, proxy: BluetoothProfile) {
                            val names = runCatching { proxy.connectedDevices.mapNotNull { it.name } }.getOrDefault(emptyList())
                            connected = connected + names
                            adapter.closeProfileProxy(p, proxy)
                        }

                        override fun onServiceDisconnected(p: Int) = Unit
                    },
                    profile,
                )
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(
            context,
            object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                        if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) connected = emptySet()
                        return
                    }
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val name = runCatching { device?.name }.getOrNull() ?: return
                    connected = if (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED) connected + name else connected - name
                }
            },
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    companion object {
        val PERMISSION: String? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT else null

        fun hasPermission(context: Context): Boolean =
            PERMISSION == null || ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Registro delle aperture fatte dal launcher (app e ora), per proporre regole basate sulle abitudini.
 * Resta sul telefono e si tengono solo le ultime [KEEP_DAYS] giornate.
 */
object LaunchLog {
    private const val PREFS = "launch_log"
    private const val KEY = "log"
    private const val KEEP_DAYS = 35L
    private const val MAX_ENTRIES = 3000

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun record(context: Context, appKey: String, time: Long = System.currentTimeMillis()) {
        val cutoff = time - KEEP_DAYS * 24 * 3_600_000L
        val kept = read(context).filter { it.time >= cutoff }.takeLast(MAX_ENTRIES - 1) + LaunchRecord(appKey, time)
        prefs(context).edit().putString(KEY, kept.joinToString("\n") { "${it.time}\t${it.appKey}" }).apply()
    }

    fun read(context: Context): List<LaunchRecord> =
        prefs(context).getString(KEY, "").orEmpty().lineSequence().mapNotNull { line ->
            val tab = line.indexOf('\t')
            if (tab <= 0) null else line.substring(0, tab).toLongOrNull()?.let { LaunchRecord(line.substring(tab + 1), it) }
        }.toList()
}
