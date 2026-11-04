// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrielepiccione.onda.data.NotificationStore
import com.gabrielepiccione.onda.data.WidgetManager
import com.gabrielepiccione.onda.service.OndaAccessibilityService
import com.gabrielepiccione.onda.ui.home.HomeCallbacks
import com.gabrielepiccione.onda.ui.home.HomeScreen
import com.gabrielepiccione.onda.ui.home.HomeStatus
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import com.gabrielepiccione.onda.ui.theme.OndaTheme
import com.gabrielepiccione.onda.ui.home.WidgetTarget
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.gabrielepiccione.onda.util.SystemActions
import java.time.LocalDate

class MainActivity : androidx.fragment.app.FragmentActivity(), HomeCallbacks {

    private val vm: LauncherViewModel by viewModels()
    private val ondaApp: OndaApp get() = application as OndaApp
    private val widgets: WidgetManager get() = ondaApp.widgets

    private var status by mutableStateOf(HomeStatus(false, false, false, false, false))

    /** Id del widget in attesa di consenso/configurazione. */
    private var pendingWidgetId = -1
    private var pendingTarget: WidgetTarget = WidgetTarget.Home

    private val contactsLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refreshStatus()
        if (granted) vm.updateSearch(vm.search.value.query)
    }

    private val calendarLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refreshStatus()
        if (granted) vm.refreshCalendar()
    }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        val ok = runCatching {
            contentResolver.openOutputStream(uri)?.use { it.write(vm.settingsRepo.exportJson().toByteArray()) }
        }.isSuccess
        toast(getString(if (ok) R.string.toast_backup_saved else R.string.toast_backup_failed))
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        val text = runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }.getOrNull()
        val ok = text != null && vm.settingsRepo.importJson(text)
        toast(getString(if (ok) R.string.toast_restored else R.string.toast_restore_failed))
    }

    private var fontForClock = false

    /** Font scelto dall'utente: copiato nella memoria privata e usato dal tema. */
    private val fontLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        val name = runCatching {
            contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: "font.ttf"
        val dir = java.io.File(filesDir, "fonts").apply { mkdirs() }
        val target = java.io.File(dir, "${System.currentTimeMillis()}_${name.replace(Regex("[^A-Za-z0-9._-]"), "_")}")
        val ok = runCatching {
            contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
            // Verifica che sia davvero un font leggibile
            android.graphics.Typeface.createFromFile(target)
        }.isSuccess
        if (!ok) {
            target.delete()
            toast(getString(R.string.font_invalid))
            return@registerForActivityResult
        }
        vm.updateSettings {
            if (fontForClock) it.copy(customFontClock = target.absolutePath) else it.copy(customFontList = target.absolutePath)
        }
        toast(getString(R.string.font_imported))
    }

    private var themeToExport: com.gabrielepiccione.onda.data.LookTheme? = null

    /** Esporta un tema come file .onda-theme, con i font incorporati. */
    private val themeExportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        val theme = themeToExport ?: return@registerForActivityResult
        uri ?: return@registerForActivityResult
        val fonts = listOf(theme.customFontList, theme.customFontClock).filter { it.isNotBlank() }.mapNotNull { path ->
            val f = java.io.File(path)
            if (f.exists()) f.name to android.util.Base64.encodeToString(f.readBytes(), android.util.Base64.NO_WRAP) else null
        }.toMap()
        val ok = runCatching {
            val text = com.gabrielepiccione.onda.data.ThemeFile.encode(com.gabrielepiccione.onda.data.ThemeFile(theme = theme, fonts = fonts))
            contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
        }.isSuccess
        toast(getString(if (ok) R.string.studio_exported else R.string.toast_backup_failed))
    }

    /** Importa un file .onda-theme: salva i font nella memoria privata e aggiunge il tema. */
    private val themeImportLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        val text = runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }.getOrNull()
        val file = text?.let { com.gabrielepiccione.onda.data.ThemeFile.decode(it) }
        if (file == null) {
            toast(getString(R.string.studio_invalid)); return@registerForActivityResult
        }
        val dir = java.io.File(filesDir, "fonts").apply { mkdirs() }
        val written = file.fonts.mapNotNull { (name, b64) ->
            runCatching {
                val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
                val target = java.io.File(dir, "${System.currentTimeMillis()}_$safe")
                target.writeBytes(android.util.Base64.decode(b64, android.util.Base64.DEFAULT))
                name to target.absolutePath
            }.getOrNull()
        }.toMap()
        fun remap(path: String) = if (path.isBlank()) "" else written[java.io.File(path).name] ?: ""
        vm.settingsRepo.addImportedTheme(
            file.theme.copy(customFontList = remap(file.theme.customFontList), customFontClock = remap(file.theme.customFontClock))
        )
        toast(getString(R.string.studio_imported))
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        // Punto di partenza dell'animazione di apertura delle app
        if (ev.actionMasked == android.view.MotionEvent.ACTION_DOWN) com.gabrielepiccione.onda.util.LaunchAnimation.onTouch(ev.x, ev.y)
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        com.gabrielepiccione.onda.util.LaunchAnimation.attach(window.decorView)
        pendingWidgetId = savedInstanceState?.getInt(KEY_PENDING, -1) ?: -1

        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            OndaTheme(state.settings) {
                val dark = LocalOndaColors.current.dark
                val hideBar = state.settings.hideStatusBar
                val orientation = state.settings.orientation
                SideEffect {
                    requestedOrientation = if (orientation == "PORTRAIT") {
                        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    } else {
                        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                        if (hideBar) {
                            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            hide(WindowInsetsCompat.Type.statusBars())
                        } else {
                            show(WindowInsetsCompat.Type.statusBars())
                        }
                    }
                }
                HomeScreen(vm = vm, callbacks = this, status = status)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        widgets.startListening()
    }

    override fun onStop() {
        super.onStop()
        widgets.stopListening()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        if (status.notificationAccess) ondaApp.media.start()
        vm.refreshCalendar()
    }

    private fun refreshStatus() {
        status = HomeStatus(
            notificationAccess = NotificationStore.isPermissionGranted(this),
            isDefaultLauncher = SystemActions.isDefaultLauncher(this),
            contactsPermission = vm.contacts.hasPermission(),
            calendarPermission = vm.calendar.hasPermission(),
            accessibilityEnabled = OndaAccessibilityService.isEnabled,
            usageAccess = com.gabrielepiccione.onda.util.UsageAccess.granted(this),
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Tasto Home premuto mentre il launcher è già in primo piano
        if (intent.action == Intent.ACTION_MAIN) vm.onHomePressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PENDING, pendingWidgetId)
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    // ---------------- HomeCallbacks ----------------

    override fun requestContactsPermission() = contactsLauncher.launch(Manifest.permission.READ_CONTACTS)

    override fun requestCalendarPermission() = calendarLauncher.launch(Manifest.permission.READ_CALENDAR)

    override fun requestDefaultLauncher() = SystemActions.requestDefaultLauncher(this)

    override fun openNotificationAccess() {
        runCatching { startActivity(NotificationStore.permissionIntent(this)) }
    }

    override fun openOwnAppInfo() {
        runCatching {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }

    override fun openAccessibilitySettings() {
        toast(getString(R.string.toast_enable_a11y))
        runCatching { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
    }

    override fun importFont(forClock: Boolean) {
        fontForClock = forClock
        fontLauncher.launch(arrayOf("font/ttf", "font/otf", "font/*", "application/x-font-ttf", "application/x-font-otf", "application/octet-stream"))
    }

    override fun exportTheme(theme: com.gabrielepiccione.onda.data.LookTheme) {
        themeToExport = theme
        themeExportLauncher.launch("${theme.name.replace(Regex("[^A-Za-z0-9 _-]"), "")}.onda-theme")
    }

    override fun importTheme() = themeImportLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))

    /** Chiede impronta, volto o PIN del telefono; [onSuccess] solo se l'utente si autentica. */
    override fun authenticate(title: String, onSuccess: () -> Unit) {
        val authenticators = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or
            androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val canAuth = androidx.biometric.BiometricManager.from(this).canAuthenticate(authenticators)
        if (canAuth != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
            // Nessun blocco schermo configurato: non c'è nulla da verificare
            toast(getString(R.string.protect_no_lock))
            onSuccess()
            return
        }
        val prompt = androidx.biometric.BiometricPrompt(
            this,
            androidx.core.content.ContextCompat.getMainExecutor(this),
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) = onSuccess()
            },
        )
        prompt.authenticate(
            androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setAllowedAuthenticators(authenticators)
                .build()
        )
    }

    override fun exportBackup() = exportLauncher.launch("onda-backup-${LocalDate.now()}.json")

    override fun importBackup() = importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))

    @Suppress("DEPRECATION")
    override fun pickWidget(provider: WidgetManager.Provider, target: WidgetTarget) {
        val id = widgets.allocateId()
        pendingWidgetId = id
        pendingTarget = target
        if (widgets.bindIfAllowed(id, provider)) {
            configureOrAdd(id)
        } else {
            runCatching { startActivityForResult(widgets.bindPermissionIntent(id, provider), REQ_BIND) }
                .onFailure { cancelPending() }
        }
    }

    @Suppress("DEPRECATION")
    private fun configureOrAdd(id: Int) {
        val info = widgets.infoFor(id)
        if (info == null) {
            cancelPending(); return
        }
        if (info.configure != null) {
            val started = runCatching {
                widgets.host.startAppWidgetConfigureActivityForResult(this, id, 0, REQ_CONFIGURE, null)
            }.isSuccess
            if (started) return
        }
        addPending(id)
    }

    private fun addPending(id: Int) {
        when (val t = pendingTarget) {
            WidgetTarget.Home -> vm.settingsRepo.addWidget(id)
            is WidgetTarget.InFolder -> vm.settingsRepo.addWidgetToFolder(t.folderId, id)
            is WidgetTarget.Replace -> {
                widgets.deleteId(t.oldId)
                vm.settingsRepo.replaceWidget(t.oldId, id)
            }
            WidgetTarget.ReplaceClock -> vm.settingsRepo.replaceClockWithWidget(id)
            is WidgetTarget.Stack -> vm.settingsRepo.addToStack(t.block, id)
            is WidgetTarget.ForApp -> {
                vm.settingsRepo.current.appWidgets[t.appKey]?.let { old -> widgets.deleteId(old) }
                vm.settingsRepo.setAppWidget(t.appKey, id)
            }
        }
        pendingWidgetId = -1
        pendingTarget = WidgetTarget.Home
    }

    private fun cancelPending() {
        if (pendingWidgetId != -1) widgets.deleteId(pendingWidgetId)
        pendingWidgetId = -1
    }

    @Deprecated("Usato per i flussi dei widget che richiedono requestCode")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val id = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId) ?: pendingWidgetId
        when (requestCode) {
            REQ_BIND -> if (resultCode == Activity.RESULT_OK && id != -1) {
                pendingWidgetId = id
                configureOrAdd(id)
            } else cancelPending()
            REQ_CONFIGURE -> if (resultCode == Activity.RESULT_OK && id != -1) addPending(id) else cancelPending()
        }
    }

    private companion object {
        const val REQ_BIND = 4101
        const val REQ_CONFIGURE = 4102
        const val KEY_PENDING = "pending_widget"
    }
}
