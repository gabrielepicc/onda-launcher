// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda

import com.gabrielepiccione.onda.data.MusicBackdropMode
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.content.ComponentName
import com.gabrielepiccione.onda.data.BluetoothWatcher
import com.gabrielepiccione.onda.data.HomePresence
import androidx.core.content.FileProvider
import com.gabrielepiccione.onda.data.AppPopups
import com.gabrielepiccione.onda.data.AutoBackup
import com.gabrielepiccione.onda.data.Backups
import com.gabrielepiccione.onda.data.FolderIcons
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
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.viewModels
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrielepiccione.onda.data.NotificationStore
import com.gabrielepiccione.onda.data.WeatherRepository
import com.gabrielepiccione.onda.data.WidgetManager
import com.gabrielepiccione.onda.service.OndaAccessibilityService
import com.gabrielepiccione.onda.service.UsageReminderEngine
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

    private val postNotificationsLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshStatus()
    }

    private val weatherLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refreshStatus()
        if (granted) vm.refreshWeather(force = true)
    }

    private val locationLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refreshStatus()
    }

    private val microphoneLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refreshStatus()
        // Rifiutato: si torna a "colori e movimento", che non chiede nulla
        if (!granted) vm.updateSettings { if (it.musicBackdrop == MusicBackdropMode.BEAT) it.copy(musicBackdrop = MusicBackdropMode.MOTION) else it }
    }

    private val bluetoothLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshStatus()
        vm.refreshContext()
    }

    private val calendarLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refreshStatus()
        if (granted) vm.refreshCalendar()
    }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    checkNotNull(contentResolver.openOutputStream(uri)).use { Backups.write(this@MainActivity, vm.settingsRepo, it) }
                }.isSuccess
            }
            toast(getString(if (ok) R.string.toast_backup_saved else R.string.toast_backup_failed))
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    checkNotNull(contentResolver.openInputStream(uri)).use { Backups.restore(this@MainActivity, vm.settingsRepo, it) }
                }.getOrDefault(false)
            }
            toast(getString(if (ok) R.string.toast_restored else R.string.toast_restore_failed))
        }
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

    /** Cartella del backup automatico: il permesso resta valido anche dopo il riavvio. */
    private val backupFolderLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val ok = runCatching { contentResolver.takePersistableUriPermission(uri, flags) }.isSuccess
        if (!ok) {
            toast(getString(R.string.toast_backup_failed))
            return@registerForActivityResult
        }
        releaseBackupFolder()
        vm.updateSettings { it.copy(autoBackupUri = uri.toString(), autoBackupLastDay = "") }
        Backups.syncSchedule(this, enabled = true)
    }

    private fun releaseBackupFolder() {
        val current = vm.settingsRepo.current.autoBackupUri
        if (current.isBlank()) return
        runCatching {
            contentResolver.releasePersistableUriPermission(
                Uri.parse(current),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }

    /** Backup automatico al massimo una volta al giorno, quando si esce dal launcher. */
    private fun runAutoBackupIfDue() {
        if (!AutoBackup.isDue(vm.settingsRepo.current, LocalDate.now().toString())) return
        val appContext = applicationContext
        lifecycleScope.launch(Dispatchers.IO) { Backups.runAutoIfDue(appContext, vm.settingsRepo) }
    }

    /** Crea il backup nella cache e lo passa all'app scelta dall'utente. */
    override fun shareBackup() {
        lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = java.io.File(cacheDir, "share").apply { mkdirs() }
                    java.io.File(dir, "onda-backup-${LocalDate.now()}.zip").also { target ->
                        target.outputStream().use { Backups.write(this@MainActivity, vm.settingsRepo, it) }
                    }
                }.getOrNull()
            }
            if (file == null) {
                toast(getString(R.string.toast_backup_failed))
                return@launch
            }
            val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.backup", file)
            val send = Intent(Intent.ACTION_SEND)
                .setType("application/zip")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            runCatching { startActivity(Intent.createChooser(send, getString(R.string.backup_share))) }
        }
    }

    private var iconImageFor: String? = null

    /** Immagine scelta come icona: ridotta e copiata nella memoria privata, senza chiedere permessi. */
    private val iconImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        val appKey = iconImageFor ?: return@registerForActivityResult
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            val path = withContext(Dispatchers.IO) { copyIconImage(uri) }
            val folderId = FolderIcons.folderOfImageKey(appKey)
            when {
                path == null -> toast(getString(R.string.icon_image_invalid))
                folderId != null -> vm.settingsRepo.setFolderIcon(folderId, FolderIcons.image(path))
                else -> vm.settingsRepo.setIconImage(appKey, path)
            }
        }
    }

    private fun copyIconImage(uri: Uri): String? = runCatching {
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= ICON_IMAGE_SIZE) sample *= 2
        val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, options) }
            ?: return@runCatching null
        val dir = java.io.File(filesDir, "icons").apply { mkdirs() }
        val target = java.io.File(dir, "${System.currentTimeMillis()}.png")
        target.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        target.absolutePath
    }.getOrNull()

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        pendingWidgetId = savedInstanceState?.getInt(KEY_PENDING, -1) ?: -1

        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            // Il profilo attivo può cambiare appena l'accento: l'unico segnale del cambio, niente popup
            val themed = state.activeProfile?.accent?.takeIf { it != 0L }?.let { state.settings.copy(accent = it, dynamicColor = false) } ?: state.settings
            OndaTheme(themed) {
                val dark = LocalOndaColors.current.dark
                val hideBar = state.settings.hideStatusBar
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
        runAutoBackupIfDue()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        if (status.notificationAccess) ondaApp.media.start()
        vm.refreshCalendar()
        vm.refreshAlphabetContacts()
        vm.refreshWeather()
    }

    private fun refreshStatus() {
        status = HomeStatus(
            notificationAccess = NotificationStore.isPermissionGranted(this),
            isDefaultLauncher = SystemActions.isDefaultLauncher(this),
            contactsPermission = vm.contacts.hasPermission(),
            calendarPermission = vm.calendar.hasPermission(),
            accessibilityEnabled = OndaAccessibilityService.isEnabled,
            weatherInstalled = vm.weatherRepo.isInstalled(),
            weatherPermission = vm.weatherRepo.hasPermission(),
            locationPermission = HomePresence.hasPermission(this),
            microphonePermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
            usageAccess = UsageReminderEngine.hasUsageAccess(this),
            postNotifications = UsageReminderEngine.canNotify(this),
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Tasto Home premuto mentre il launcher è già in primo piano
        if (intent.action == Intent.ACTION_MAIN) vm.onHomePressed()
        // App aperta fuori dal launcher che ha bisogno della pausa di Usage Breaker
        intent.getStringExtra(EXTRA_PAUSE_PACKAGE)?.let { vm.requestPause(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PENDING, pendingWidgetId)
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    // ---------------- HomeCallbacks ----------------

    override fun requestContactsPermission() = contactsLauncher.launch(Manifest.permission.READ_CONTACTS)

    override fun requestCalendarPermission() = calendarLauncher.launch(Manifest.permission.READ_CALENDAR)

    override fun requestWeatherPermission() = weatherLauncher.launch(WeatherRepository.PERMISSION)
    override fun requestLocationPermission() = locationLauncher.launch(HomePresence.PERMISSIONS)
    /** Chiude e riapre Onda da capo: utile se qualcosa resta in uno stato strano. */
    override fun restartLauncher() {
        val restart = Intent.makeRestartActivityTask(ComponentName(this, MainActivity::class.java))
        startActivity(restart)
        Runtime.getRuntime().exit(0)
    }

    override fun requestMicrophonePermission() = microphoneLauncher.launch(android.Manifest.permission.RECORD_AUDIO)

    override fun requestBluetoothPermission() {
        BluetoothWatcher.PERMISSION?.let { bluetoothLauncher.launch(it) }
    }

    override fun requestPostNotifications() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            postNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun openUsageAccess() {
        runCatching { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
    }

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

    override fun pickIconImage(appKey: String) {
        iconImageFor = appKey
        iconImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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

    override fun exportBackup() = exportLauncher.launch("onda-backup-${LocalDate.now()}.zip")

    override fun importBackup() = importLauncher.launch(arrayOf("application/zip", "application/json", "text/plain", "*/*"))

    override fun chooseBackupFolder() = backupFolderLauncher.launch(null)

    override fun disableAutoBackup() {
        releaseBackupFolder()
        vm.updateSettings { it.copy(autoBackupUri = "", autoBackupLastDay = "") }
        Backups.syncSchedule(this, enabled = false)
    }

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
            is WidgetTarget.ToStack -> vm.settingsRepo.addToStack(t.block, id)
            is WidgetTarget.ToPopup -> vm.settingsRepo.edit { AppPopups.addWidget(it, t.appKey, id) }
            WidgetTarget.HomeLights -> {
                vm.settingsRepo.current.homeLightsWidget.takeIf { it > 0 }?.let { old -> widgets.deleteId(old) }
                vm.settingsRepo.edit { it.copy(homeLightsWidget = id) }
            }
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

    companion object {
        /** Pacchetto per cui mostrare la pausa (dal servizio di accessibilità). */
        const val EXTRA_PAUSE_PACKAGE = "onda.pause_package"

        const val REQ_BIND = 4101
        const val REQ_CONFIGURE = 4102
        const val KEY_PENDING = "pending_widget"
        const val ICON_IMAGE_SIZE = 256
    }
}
