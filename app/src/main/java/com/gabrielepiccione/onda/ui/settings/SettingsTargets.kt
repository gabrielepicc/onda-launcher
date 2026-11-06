// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.settings

import com.gabrielepiccione.onda.R

/** Le voci delle impostazioni a cui portano il benvenuto e i suggerimenti ("Mostrami"). */
internal object SettingsTargets {
    private fun by(titleRes: Int): SettingEntry = SETTING_ENTRIES.first { it.titleRes == titleRes }

    val alphabet: SettingEntry get() = by(R.string.alphabet_left)
    val gestures: SettingEntry get() = by(R.string.section_gestures_custom)
    val home: SettingEntry get() = by(R.string.music_player)
    val search: SettingEntry get() = by(R.string.search_engine)
    val icons: SettingEntry get() = by(R.string.show_icons)
    val profiles: SettingEntry get() = by(R.string.page_contexts)
    val wellbeing: SettingEntry get() = by(R.string.page_wellbeing)
    val backup: SettingEntry get() = by(R.string.auto_backup)
    val favorites: SettingEntry get() = by(R.string.hide_fav_names)
    val player: SettingEntry get() = by(R.string.music_player)
    val plugPlay: SettingEntry get() = by(R.string.section_plug_play)
    val grayscale: SettingEntry get() = by(R.string.section_grayscale)
}
