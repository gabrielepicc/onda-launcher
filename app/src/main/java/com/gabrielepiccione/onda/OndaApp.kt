// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda

import android.app.Application
import com.gabrielepiccione.onda.data.AppRepository
import com.gabrielepiccione.onda.data.Backups
import com.gabrielepiccione.onda.data.CalendarRepository
import com.gabrielepiccione.onda.data.MediaRepository
import com.gabrielepiccione.onda.data.SettingsRepository
import com.gabrielepiccione.onda.data.WidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.gabrielepiccione.onda.service.AutomationEngine
import com.gabrielepiccione.onda.service.ProfileSuggestionWorker

class OndaApp : Application() {

    lateinit var settings: SettingsRepository
        private set
    lateinit var apps: AppRepository
        private set
    lateinit var widgets: WidgetManager
        private set
    lateinit var media: MediaRepository
        private set
    lateinit var calendar: CalendarRepository
        private set
    lateinit var audio: com.gabrielepiccione.onda.data.AudioDevices
        private set
    lateinit var automation: AutomationEngine
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        apps = AppRepository(this, settings.settings)
        widgets = WidgetManager(this)
        media = MediaRepository(this)
        calendar = CalendarRepository(this)
        audio = com.gabrielepiccione.onda.data.AudioDevices(this)
        // Automazioni «se… allora»: girano solo se ce n'è una accesa
        automation = AutomationEngine(this).also { it.start() }
        // Il promemoria dei profili suggeriti esiste solo con "Suggerisci profili" acceso
        scope.launch {
            settings.settings.map { it.profileSuggest.enabled }.distinctUntilChanged().collect { ProfileSuggestionWorker.syncSchedule(this@OndaApp, it) }
        }
        // Solo chi ha attivato il backup automatico ha un controllo periodico
        if (settings.current.autoBackupUri.isNotBlank()) Backups.syncSchedule(this, enabled = true)
    }
}
