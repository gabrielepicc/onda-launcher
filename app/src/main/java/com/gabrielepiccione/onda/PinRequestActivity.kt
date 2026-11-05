// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.UserManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.gabrielepiccione.onda.data.PinnedShortcut
import com.gabrielepiccione.onda.data.PinnedShortcuts
import com.gabrielepiccione.onda.data.WidgetManager
import com.gabrielepiccione.onda.ui.theme.OndaTheme

/**
 * Riceve dalle altre app "Aggiungi alla schermata Home" (Chrome, Maps, WhatsApp…): chiede conferma
 * e aggiunge alla home la scorciatoia o il widget. Android la invoca solo se Onda è il launcher predefinito.
 */
class PinRequestActivity : ComponentActivity() {

    private val ondaApp: OndaApp get() = application as OndaApp
    private val widgets: WidgetManager get() = ondaApp.widgets
    private lateinit var request: LauncherApps.PinItemRequest
    private var pendingWidgetId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingWidgetId = savedInstanceState?.getInt(KEY_PENDING, -1) ?: -1
        val launcherApps = getSystemService(LauncherApps::class.java)
        // Null se l'intent non arriva dal sistema: niente da confermare
        val pin = launcherApps.getPinItemRequest(intent)
        if (pin == null || !pin.isValid) {
            finish()
            return
        }
        request = pin
        // Dopo una rotazione o il ritorno dal consenso il dialogo non va ripresentato
        if (pendingWidgetId != -1) return

        val shortcut = pin.shortcutInfo
        val widget = pin.getAppWidgetProviderInfo(this)
        val label: String
        val preview: ImageBitmap?
        val onAdd: () -> Unit
        when {
            shortcut != null -> {
                label = (shortcut.shortLabel ?: shortcut.longLabel ?: "").toString()
                preview = runCatching { launcherApps.getShortcutIconDrawable(shortcut, densityDpi())?.toPreview() }.getOrNull()
                onAdd = { addShortcut(shortcut, label) }
            }
            widget != null -> {
                label = runCatching { widget.loadLabel(packageManager) }.getOrNull().orEmpty()
                preview = runCatching { (widget.loadPreviewImage(this, densityDpi()) ?: widget.loadIcon(this, densityDpi()))?.toPreview() }.getOrNull()
                onAdd = { addWidget(widget, label) }
            }
            else -> {
                finish()
                return
            }
        }

        setContent {
            OndaTheme(ondaApp.settings.current) {
                AlertDialog(
                    onDismissRequest = { finish() },
                    title = { Text(stringResource(R.string.pin_title)) },
                    text = {
                        Column {
                            preview?.let {
                                Image(it, contentDescription = null, modifier = Modifier.heightIn(max = 160.dp).padding(bottom = 12.dp))
                            }
                            Text(label)
                        }
                    },
                    confirmButton = { TextButton(onClick = onAdd) { Text(stringResource(R.string.pin_add)) } },
                    dismissButton = { TextButton(onClick = { finish() }) { Text(stringResource(R.string.cancel)) } },
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PENDING, pendingWidgetId)
    }

    private fun densityDpi() = resources.displayMetrics.densityDpi

    private fun Drawable.toPreview(): ImageBitmap {
        val w = intrinsicWidth.takeIf { it > 0 } ?: PREVIEW_PX
        val h = intrinsicHeight.takeIf { it > 0 } ?: PREVIEW_PX
        val scale = (PREVIEW_PX.toFloat() / maxOf(w, h)).coerceAtMost(1f)
        return toBitmap((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1)).asImageBitmap()
    }

    private fun addShortcut(shortcut: ShortcutInfo, label: String) {
        if (!request.accept()) {
            finishWith(R.string.pin_failed)
            return
        }
        val serial = getSystemService(UserManager::class.java).getSerialNumberForUser(shortcut.userHandle)
        ondaApp.settings.edit { PinnedShortcuts.add(it, PinnedShortcut(shortcut.`package`, shortcut.id, serial, label)) }
        finishWith(R.string.pin_added)
    }

    @Suppress("DEPRECATION")
    private fun addWidget(info: AppWidgetProviderInfo, label: String) {
        val id = widgets.allocateId()
        pendingWidgetId = id
        val provider = WidgetManager.Provider(info, label, label)
        if (widgets.bindIfAllowed(id, provider)) {
            configureOrFinish(id)
        } else {
            runCatching { startActivityForResult(widgets.bindPermissionIntent(id, provider), REQ_BIND) }
                .onFailure { cancelWidget() }
        }
    }

    @Suppress("DEPRECATION")
    private fun configureOrFinish(id: Int) {
        val info = widgets.infoFor(id)
        if (info == null) {
            cancelWidget()
            return
        }
        val optional = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            (info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL) != 0
        if (info.configure != null && !optional) {
            val started = runCatching { widgets.host.startAppWidgetConfigureActivityForResult(this, id, 0, REQ_CONFIGURE, null) }.isSuccess
            if (started) return
        }
        acceptWidget(id)
    }

    private fun acceptWidget(id: Int) {
        val accepted = request.accept(Bundle().apply { putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, id) })
        if (!accepted) {
            cancelWidget()
            return
        }
        pendingWidgetId = -1
        ondaApp.settings.addWidget(id)
        finishWith(R.string.pin_added)
    }

    private fun cancelWidget() {
        if (pendingWidgetId != -1) widgets.deleteId(pendingWidgetId)
        pendingWidgetId = -1
        finishWith(R.string.pin_failed)
    }

    private fun finishWith(message: Int) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }

    @Deprecated("Usato per i flussi dei widget che richiedono requestCode")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val id = pendingWidgetId
        when {
            id == -1 -> finish()
            resultCode != Activity.RESULT_OK -> cancelWidget()
            requestCode == REQ_BIND -> configureOrFinish(id)
            requestCode == REQ_CONFIGURE -> acceptWidget(id)
        }
    }

    private companion object {
        const val REQ_BIND = 4201
        const val REQ_CONFIGURE = 4202
        const val KEY_PENDING = "pending_widget"
        const val PREVIEW_PX = 480
    }
}
