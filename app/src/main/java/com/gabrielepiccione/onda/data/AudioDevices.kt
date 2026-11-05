// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Rileva cuffie o casse collegate (cavo, USB, Bluetooth) per la funzione Plug & Play. Nessun permesso richiesto. */
class AudioDevices(context: Context) {

    private val audio = context.getSystemService(AudioManager::class.java)
    private val _connected = MutableStateFlow(false)
    val headphonesConnected: StateFlow<Boolean> = _connected.asStateFlow()

    private val outputTypes = buildSet {
        add(AudioDeviceInfo.TYPE_WIRED_HEADSET)
        add(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
        add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
        add(AudioDeviceInfo.TYPE_USB_HEADSET)
        add(AudioDeviceInfo.TYPE_LINE_ANALOG)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(AudioDeviceInfo.TYPE_BLE_HEADSET)
            add(AudioDeviceInfo.TYPE_BLE_SPEAKER)
        }
    }

    private fun refresh() {
        _connected.value = runCatching {
            audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in outputTypes }
        }.getOrDefault(false)
    }

    init {
        refresh()
        runCatching {
            audio.registerAudioDeviceCallback(object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = refresh()
                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = refresh()
            }, Handler(Looper.getMainLooper()))
        }
    }
}
