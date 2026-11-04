// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda

import android.app.Application
import com.gabrielepiccione.onda.data.AppRepository
import com.gabrielepiccione.onda.data.CalendarRepository
import com.gabrielepiccione.onda.data.MediaRepository
import com.gabrielepiccione.onda.data.SettingsRepository
import com.gabrielepiccione.onda.data.WidgetManager

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

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        apps = AppRepository(this, settings.settings)
        widgets = WidgetManager(this)
        media = MediaRepository(this)
        calendar = CalendarRepository(this)
        audio = com.gabrielepiccione.onda.data.AudioDevices(this)
    }
}
