// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * L'orologio dell'interfaccia: un solo tempo corrente (ms, ora reale di sistema) letto da orologio, tempi relativi,
 * bianco e nero a orario, luci di casa e agenda. Si aggiorna con i broadcast del sistema (tick al minuto, cambio di ora,
 * di fuso e di data) mentre l'app è visibile, subito in onStart/onResume/focus, e da un ciclo di sicurezza che ricalcola
 * l'attesa dall'orologio di sistema a ogni giro.
 */
class UiClock {
    private val _now = mutableLongStateOf(System.currentTimeMillis())

    /** Il tempo corrente: chi lo legge in composizione si ridisegna solo quando cambia. */
    val now: State<Long> get() = _now

    private var receiver: BroadcastReceiver? = null

    /** Riallinea subito all'ora di sistema. */
    fun refresh() {
        _now.longValue = System.currentTimeMillis()
    }

    /** Ascolta i cambi di ora (solo mentre l'app è visibile): da chiamare in onStart. */
    fun start(context: Context) {
        if (receiver != null) return
        val r = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Dopo un cambio di fuso il fuso predefinito di Java va riletto
                if (intent?.action == Intent.ACTION_TIMEZONE_CHANGED) java.util.TimeZone.setDefault(null)
                refresh()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        // Gli azioni di orologio sono broadcast di sistema: il ricevitore non è esportato
        androidx.core.content.ContextCompat.registerReceiver(context, r, filter, androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        receiver = r
        refresh()
    }

    /** Smette di ascoltare: da chiamare in onStop. */
    fun stop(context: Context) {
        receiver?.let { runCatching { context.unregisterReceiver(it) } }
        receiver = null
    }
}

/** Il tempo corrente dell'interfaccia; un valore di riserva se nessuno lo fornisce (anteprime, test). */
val LocalNow = staticCompositionLocalOf<State<Long>> { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }

/** L'orologio dell'interfaccia, per chi deve chiedere un aggiornamento (secondi accesi). */
val LocalUiClock = staticCompositionLocalOf<UiClock?> { null }
