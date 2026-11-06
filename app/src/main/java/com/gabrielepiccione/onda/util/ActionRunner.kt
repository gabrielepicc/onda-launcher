// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

import android.app.NotificationManager
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import com.gabrielepiccione.onda.data.GestureAction
import com.gabrielepiccione.onda.service.OndaAccessibilityService

/**
 * Esegue le azioni di sistema del catalogo ([com.gabrielepiccione.onda.data.Actions]).
 * Le altre (ricerca, elenco app, impostazioni, app e scorciatoie) sono schermate o avvii di Onda e li gestisce la home.
 */
object ActionRunner {
    enum class Outcome { DONE, NEEDS_ACCESSIBILITY, NEEDS_POLICY_ACCESS, NOT_SYSTEM }

    private var torchOn = false

    fun run(context: Context, action: GestureAction): Outcome = when (action) {
        GestureAction.NOTIFICATIONS -> done { SystemActions.expandNotifications(context) }
        GestureAction.QUICK_SETTINGS -> done { SystemActions.expandQuickSettings(context) }
        GestureAction.FLASHLIGHT -> done { toggleTorch(context) }
        GestureAction.SILENT -> toggleSilent(context)
        GestureAction.LOCK_SCREEN -> accessibility(OndaAccessibilityService.lockScreen())
        GestureAction.SCREENSHOT -> accessibility(OndaAccessibilityService.screenshot())
        GestureAction.RECENTS -> accessibility(OndaAccessibilityService.recents())
        else -> Outcome.NOT_SYSTEM
    }

    private inline fun done(block: () -> Unit): Outcome {
        block()
        return Outcome.DONE
    }

    private fun accessibility(ok: Boolean) = if (ok) Outcome.DONE else Outcome.NEEDS_ACCESSIBILITY

    private fun toggleTorch(context: Context) {
        runCatching {
            val cameras = context.getSystemService(CameraManager::class.java)
            val id = cameras.cameraIdList.firstOrNull {
                cameras.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            torchOn = !torchOn
            cameras.setTorchMode(id, torchOn)
        }
    }

    /** Suoneria normale ↔ silenzioso; per impostare il silenzioso Android chiede l'accesso a "Non disturbare". */
    private fun toggleSilent(context: Context): Outcome {
        val notifications = context.getSystemService(NotificationManager::class.java)
        if (!notifications.isNotificationPolicyAccessGranted) return Outcome.NEEDS_POLICY_ACCESS
        val audio = context.getSystemService(AudioManager::class.java)
        audio.ringerMode =
            if (audio.ringerMode == AudioManager.RINGER_MODE_NORMAL) AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL
        return Outcome.DONE
    }
}
