// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.settings

import com.gabrielepiccione.onda.ui.theme.*
import com.gabrielepiccione.onda.ui.components.*
import com.gabrielepiccione.onda.ui.home.*
import com.gabrielepiccione.onda.ui.drawer.*
import com.gabrielepiccione.onda.ui.search.*
import com.gabrielepiccione.onda.ui.sheets.*

import androidx.compose.ui.res.stringResource
import com.gabrielepiccione.onda.R
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.data.FontChoice
import com.gabrielepiccione.onda.core.icons.IconPacks
import com.gabrielepiccione.onda.data.Settings
import com.gabrielepiccione.onda.data.ThemeMode
import com.gabrielepiccione.onda.data.WidgetManager

data class SettingsStatus(
    val isDefaultLauncher: Boolean,
    val notificationAccess: Boolean,
    val calendarPermission: Boolean,
    val accessibilityEnabled: Boolean,
    val usageAccess: Boolean = false,
    val contactsPermission: Boolean = false,
)

interface SettingsActions {
    fun edit(transform: (Settings) -> Settings)
    fun requestDefault()
    fun notificationAccess()
    fun calendarPermission()
    fun accessibility()
    fun usageAccess()
    fun contactsPermission()
    fun addWidget()
    fun removeWidget(id: Int)
    fun unhide(app: AppInfo)
    fun authenticate(onSuccess: () -> Unit)
    fun wallpaper()
    fun openStyles()
    /** Importa un font da file per la lista (`forClock = false`) o per l'orologio. */
    fun importFont(forClock: Boolean)
    fun exportBackup()
    fun importBackup()
    fun chooseBackupFolder()
    fun disableAutoBackup()
    fun close()
    fun showIntro()
}

@Composable
fun SettingsScreen(
    settings: Settings,
    hiddenApps: List<AppInfo>,
    allApps: List<AppInfo> = emptyList(),
    status: SettingsStatus,
    widgets: WidgetManager,
    actions: SettingsActions,
) {
    val colors = LocalOndaColors.current
    val context = LocalContext.current
    val pm = context.packageManager
    var iconPackDialog by remember { mutableStateOf(false) }
    var plugDialog by remember { mutableStateOf(false) }
    var breakDialog by remember { mutableStateOf(false) }
    var digestDialog by remember { mutableStateOf(false) }
    var gestureAppFor by remember { mutableStateOf<String?>(null) }
    var hiddenUnlocked by remember { mutableStateOf(false) }
    val onEdit = actions::edit

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .blockTouches()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp).clip(CircleShape).clickable { actions.close() }
            ) {
                @Suppress("DEPRECATION")
                Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.onWallpaper)
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium, color = colors.onWallpaper)
        }

        Group(stringResource(R.string.section_setup)) {
            StatusRow(stringResource(R.string.default_launcher), status.isDefaultLauncher, stringResource(R.string.default_launcher_ok), stringResource(R.string.default_launcher_ko)) { actions.requestDefault() }
            StatusRow(stringResource(R.string.notif_access), status.notificationAccess, stringResource(R.string.notif_access_ok), stringResource(R.string.notif_access_ko)) { actions.notificationAccess() }
            StatusRow(stringResource(R.string.calendar), status.calendarPermission, stringResource(R.string.calendar_ok), stringResource(R.string.calendar_ko)) { actions.calendarPermission() }
        }

        Group(stringResource(R.string.section_styles)) {
            Text(stringResource(R.string.styles_subtitle), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
            FilledTonalButton(onClick = { actions.openStyles() }, modifier = Modifier.padding(top = 10.dp)) { Text(stringResource(R.string.styles_open)) }
            Label(stringResource(R.string.clock_style_setting))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                com.gabrielepiccione.onda.data.ClockStyle.entries.forEach { st ->
                    FilterChip(selected = settings.clockStyle == st, onClick = { onEdit { it.copy(clockStyle = st) } }, label = { Text(stringResource(st.labelRes)) })
                }
            }
        }

        Group(stringResource(R.string.section_style)) {
            Label(stringResource(R.string.theme))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ThemeMode.SYSTEM to stringResource(R.string.theme_system), ThemeMode.LIGHT to stringResource(R.string.theme_light), ThemeMode.DARK to stringResource(R.string.theme_dark)).forEach { (mode, label) ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { onEdit { it.copy(themeMode = mode) } },
                        label = { Text(label) },
                    )
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchRow(stringResource(R.string.dynamic_color), stringResource(R.string.dynamic_color_sub), settings.dynamicColor) { v -> onEdit { it.copy(dynamicColor = v) } }
            }
            if (!settings.dynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                Label(stringResource(R.string.accent_color))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    AccentChoices.forEach { argb ->
                        val selected = settings.accent == argb
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .border(BorderStroke(if (selected) 3.dp else 0.dp, colors.onWallpaper), CircleShape)
                                .clickable { onEdit { it.copy(accent = argb) } }
                        ) {
                            if (selected) Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            Label(stringResource(R.string.font))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                FontChoice.entries.forEach { f ->
                    FilterChip(selected = settings.font == f, onClick = { onEdit { it.copy(font = f) } }, label = { Text(stringResource(f.labelRes)) })
                }
            }
            Spacer(Modifier.height(8.dp))
            Label(stringResource(R.string.text_size))
            Slider(value = settings.textScale, onValueChange = { v -> onEdit { it.copy(textScale = v) } }, valueRange = 0.8f..1.3f, steps = 4)
            Label(stringResource(R.string.wallpaper_dim))
            Slider(value = settings.wallpaperDim, onValueChange = { v -> onEdit { it.copy(wallpaperDim = v) } }, valueRange = 0f..0.8f)
            TextButton(onClick = { actions.wallpaper() }) { Text(stringResource(R.string.change_wallpaper_ellipsis)) }
        }

        Group(stringResource(R.string.section_text)) {
            Label(stringResource(R.string.text_color))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 6.dp)) {
                FilterChip(selected = settings.textColor == 0L, onClick = { onEdit { it.copy(textColor = 0) } }, label = { Text(stringResource(R.string.text_color_auto)) })
                listOf(0xFFFFFFFF, 0xFFF2EDE4, 0xFF1A1C1E, 0xFF7FE3D1, 0xFFC3A6FF, 0xFFFFD27F, 0xFFFFAB91).forEach { argb ->
                    val sel = settings.textColor == argb
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .border(BorderStroke(if (sel) 3.dp else 1.dp, if (sel) colors.accent else colors.panelBorder), CircleShape)
                            .clickable { onEdit { it.copy(textColor = argb) } }
                    ) {
                        if (sel) Icon(Icons.Filled.Check, null, tint = if (Color(argb).luminance() > 0.5f) Color.Black else Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Label(stringResource(R.string.list_alignment))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.gabrielepiccione.onda.data.HAlign.entries.forEach { a ->
                    FilterChip(selected = settings.listAlign == a, onClick = { onEdit { it.copy(listAlign = a) } }, label = { Text(stringResource(a.labelRes)) })
                }
            }
            Label(stringResource(R.string.font_weight))
            Slider(
                value = settings.listFontWeight.toFloat(),
                onValueChange = { v -> onEdit { it.copy(listFontWeight = (v / 100).roundToInt() * 100) } },
                valueRange = 100f..900f,
                steps = 7,
            )
            Label(stringResource(R.string.letter_spacing))
            Slider(
                value = settings.listLetterSpacing,
                onValueChange = { v -> onEdit { it.copy(listLetterSpacing = v) } },
                valueRange = -0.05f..0.2f,
            )
            SwitchRow(stringResource(R.string.text_shadow), stringResource(R.string.text_shadow_sub), settings.textShadow) { v -> onEdit { it.copy(textShadow = v) } }
            SwitchRow(stringResource(R.string.show_battery), null, settings.showBattery) { v -> onEdit { it.copy(showBattery = v) } }
            Label(stringResource(R.string.custom_fonts))
            FontRow(stringResource(R.string.font_for_list), settings.customFontList, onImport = { actions.importFont(false) }, onClear = { onEdit { it.copy(customFontList = "") } })
            FontRow(stringResource(R.string.font_for_clock), settings.customFontClock, onImport = { actions.importFont(true) }, onClear = { onEdit { it.copy(customFontClock = "") } })
        }

        Group(stringResource(R.string.section_icons)) {
            SwitchRow(stringResource(R.string.show_icons), null, settings.showIcons) { v -> onEdit { it.copy(showIcons = v) } }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SwitchRow(stringResource(R.string.themed_icons), stringResource(R.string.themed_icons_sub), settings.themedIcons) { v -> onEdit { it.copy(themedIcons = v) } }
            }
            val systemDefault = stringResource(R.string.system_default)
            val packLabel = remember(settings.iconPack, systemDefault) {
                if (settings.iconPack.isBlank()) systemDefault
                else runCatching { pm.getApplicationLabel(pm.getApplicationInfo(settings.iconPack, 0)).toString() }.getOrDefault(settings.iconPack)
            }
            ClickRow(stringResource(R.string.icon_pack), packLabel) { iconPackDialog = true }
            Label(stringResource(R.string.icon_style))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                listOf(
                    "ORIGINAL" to R.string.style_original, "MATERIAL" to R.string.style_material, "OUTLINE" to R.string.style_outline,
                    "STICKER" to R.string.style_sticker, "WATERCOLOR" to R.string.style_watercolor, "MARBLE" to R.string.style_marble,
                    "DOTS" to R.string.style_dots,
                ).forEach { (id, res) ->
                    FilterChip(selected = settings.iconStyle == id, onClick = { onEdit { it.copy(iconStyle = id) } }, label = { Text(stringResource(res)) })
                }
            }
            Label(stringResource(R.string.icon_shape))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                listOf(
                    "SYSTEM" to R.string.shape_system, "CIRCLE" to R.string.shape_circle, "SQUIRCLE" to R.string.shape_squircle,
                    "ROUNDED_SQUARE" to R.string.shape_rounded, "TEARDROP" to R.string.shape_teardrop,
                    "CYLINDER" to R.string.shape_cylinder, "PETAL" to R.string.shape_petal,
                ).forEach { (id, res) ->
                    FilterChip(selected = settings.iconShape == id, onClick = { onEdit { it.copy(iconShape = id) } }, label = { Text(stringResource(res)) })
                }
            }
            Text(stringResource(R.string.icon_shape_hint), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall)
            Label(stringResource(R.string.icon_size))
            Slider(value = settings.iconSize, onValueChange = { v -> onEdit { it.copy(iconSize = v) } }, valueRange = 0.75f..1.4f)
        }

        Group(stringResource(R.string.section_home)) {
            SwitchRow(stringResource(R.string.clock_date), null, settings.showClock) { v -> onEdit { it.copy(showClock = v) } }
            SwitchRow(stringResource(R.string.next_event), stringResource(R.string.next_event_sub), settings.showCalendar) { v -> onEdit { it.copy(showCalendar = v) } }
            SwitchRow(stringResource(R.string.music_player), stringResource(R.string.music_player_sub), settings.showMedia) { v -> onEdit { it.copy(showMedia = v) } }
            SwitchRow(stringResource(R.string.alphabet_left), stringResource(R.string.alphabet_left_sub), settings.leftHanded) { v -> onEdit { it.copy(leftHanded = v) } }
            SwitchRow(stringResource(R.string.hide_alphabet), stringResource(R.string.hide_alphabet_sub), settings.hideAlphabet) { v -> onEdit { it.copy(hideAlphabet = v) } }
            SwitchRow(stringResource(R.string.alphabet_contacts), stringResource(R.string.alphabet_contacts_sub), settings.alphabetContacts) { v ->
                onEdit { it.copy(alphabetContacts = v) }
                if (v && !status.contactsPermission) actions.contactsPermission()
            }
            SwitchRow(stringResource(R.string.hide_fav_names), stringResource(R.string.hide_fav_names_sub), settings.hideFavoriteNames) { v -> onEdit { it.copy(hideFavoriteNames = v) } }
            SwitchRow(stringResource(R.string.hide_status_bar), null, settings.hideStatusBar) { v -> onEdit { it.copy(hideStatusBar = v) } }
            SwitchRow(stringResource(R.string.agenda_title), stringResource(R.string.agenda_sub), settings.showAgenda) { v ->
                onEdit { it.copy(showAgenda = v) }
                if (v && !status.calendarPermission) actions.calendarPermission()
            }
            SwitchRow(stringResource(R.string.layout_lock), stringResource(R.string.layout_lock_sub), settings.lockLayout) { v -> onEdit { it.copy(lockLayout = v) } }
            Label(stringResource(R.string.orientation))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = settings.orientation != "PORTRAIT", onClick = { onEdit { it.copy(orientation = "AUTO") } }, label = { Text(stringResource(R.string.orientation_auto)) })
                FilterChip(selected = settings.orientation == "PORTRAIT", onClick = { onEdit { it.copy(orientation = "PORTRAIT") } }, label = { Text(stringResource(R.string.orientation_portrait)) })
            }
            Label(stringResource(R.string.home_offset))
            Text(stringResource(R.string.home_offset_sub), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall)
            com.gabrielepiccione.onda.ui.home.HomeOffsetControl(settings.homeOffset) { v -> onEdit { it.copy(homeOffset = v) } }
        }

        Group(stringResource(R.string.section_gestures)) {
            Text(stringResource(R.string.swipe_down_fixed), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
            Label(stringResource(R.string.swipe_up))
            GestureChips(settings.swipeUp) { g ->
                onEdit { it.copy(swipeUp = g) }
                if (g.needsAccessibility && !status.accessibilityEnabled) actions.accessibility()
                if (g == com.gabrielepiccione.onda.data.GestureAction.OPEN_APP) gestureAppFor = "up"
            }
            if (settings.swipeUp == com.gabrielepiccione.onda.data.GestureAction.OPEN_APP) {
                ClickRow(stringResource(R.string.gesture_app), allApps.firstOrNull { it.key == settings.swipeUpApp }?.label ?: stringResource(R.string.plug_play_none)) { gestureAppFor = "up" }
            }
            Label(stringResource(R.string.double_tap))
            GestureChips(settings.doubleTap) { g ->
                onEdit { it.copy(doubleTap = g) }
                if (g.needsAccessibility && !status.accessibilityEnabled) actions.accessibility()
                if (g == com.gabrielepiccione.onda.data.GestureAction.OPEN_APP) gestureAppFor = "double"
            }
            if (settings.doubleTap == com.gabrielepiccione.onda.data.GestureAction.OPEN_APP) {
                ClickRow(stringResource(R.string.gesture_app), allApps.firstOrNull { it.key == settings.doubleTapApp }?.label ?: stringResource(R.string.plug_play_none)) { gestureAppFor = "double" }
            }
            Text(
                if (status.accessibilityEnabled) stringResource(R.string.double_tap_lock_on) else stringResource(R.string.double_tap_lock_off),
                color = colors.onWallpaperMuted,
                style = MaterialTheme.typography.bodySmall,
            )
            SwitchRow(stringResource(R.string.haptics), stringResource(R.string.haptics_sub), settings.haptics) { v -> onEdit { it.copy(haptics = v) } }
            Label(stringResource(R.string.open_animation))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                val names = mapOf(
                    "SYSTEM" to R.string.anim_system, "SCALE" to R.string.anim_scale, "REVEAL" to R.string.anim_reveal,
                    "SLIDE" to R.string.anim_slide, "FADE" to R.string.anim_fade,
                )
                com.gabrielepiccione.onda.util.LaunchAnimation.ALL.forEach { id ->
                    FilterChip(
                        selected = settings.openAnimation == id,
                        onClick = { onEdit { it.copy(openAnimation = id) } },
                        label = { Text(stringResource(names.getValue(id))) },
                    )
                }
            }
            Text(
                stringResource(R.string.gestures_help),
                color = colors.onWallpaperMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Group(stringResource(R.string.section_search)) {
            Label(stringResource(R.string.search_engine))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                FilterChip(selected = settings.searchEngine == "SYSTEM", onClick = { onEdit { it.copy(searchEngine = "SYSTEM") } }, label = { Text(stringResource(R.string.engine_system)) })
                listOf("DUCKDUCKGO" to "DuckDuckGo", "STARTPAGE" to "Startpage", "GOOGLE" to "Google", "ECOSIA" to "Ecosia", "BRAVE" to "Brave").forEach { (id, name) ->
                    FilterChip(selected = settings.searchEngine == id, onClick = { onEdit { it.copy(searchEngine = id) } }, label = { Text(name) })
                }
            }
            Text(stringResource(R.string.search_prefix_hint), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }

        Group(stringResource(R.string.summary_title)) {
            SwitchRow(stringResource(R.string.summary_show), stringResource(R.string.summary_show_sub), settings.showSummary) { v ->
                onEdit { it.copy(showSummary = v) }
                if (v && !status.notificationAccess) actions.notificationAccess()
            }
            Text(stringResource(R.string.digest_sub), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            val digestLabels = allApps.filter { it.packageName in settings.digestApps }.map { it.label }.distinct()
            ClickRow(
                stringResource(R.string.digest_apps),
                if (digestLabels.isEmpty()) stringResource(R.string.plug_play_none) else digestLabels.joinToString(),
            ) { digestDialog = true }
            Label(stringResource(R.string.digest_times))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                com.gabrielepiccione.onda.util.DigestSchedule.PRESETS.forEach { m ->
                    val on = m in settings.digestTimes
                    FilterChip(
                        selected = on,
                        onClick = {
                            onEdit { s ->
                                val next = if (on) s.digestTimes - m else (s.digestTimes + m).sorted()
                                // Almeno un orario: senza orari le notifiche non verrebbero rinviate
                                if (next.isEmpty()) s else s.copy(digestTimes = next)
                            }
                        },
                        label = { Text(com.gabrielepiccione.onda.util.DigestSchedule.format(m)) },
                    )
                }
            }
        }

        Group(stringResource(R.string.section_break)) {
            Text(stringResource(R.string.break_settings_sub), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
            val breakApps = allApps.filter { it.key in settings.breakApps }
            ClickRow(
                stringResource(R.string.break_apps),
                if (breakApps.isEmpty()) stringResource(R.string.plug_play_none) else breakApps.joinToString { it.label },
            ) { breakDialog = true }
            Label(stringResource(R.string.break_seconds, settings.breakSeconds))
            Slider(
                value = settings.breakSeconds.toFloat(),
                onValueChange = { v -> onEdit { it.copy(breakSeconds = v.roundToInt().coerceIn(3, 30)) } },
                valueRange = 3f..30f,
            )
            Label(
                if (settings.breakDailyLimit == 0) stringResource(R.string.break_limit_none)
                else stringResource(R.string.break_limit, settings.breakDailyLimit)
            )
            Slider(
                value = settings.breakDailyLimit.toFloat(),
                onValueChange = { v -> onEdit { it.copy(breakDailyLimit = v.roundToInt().coerceIn(0, 30)) } },
                valueRange = 0f..30f,
            )
            StatusRow(
                stringResource(R.string.usage_access),
                status.usageAccess,
                stringResource(R.string.usage_access_ok),
                stringResource(R.string.usage_access_ko),
            ) { actions.usageAccess() }
        }

        Group(stringResource(R.string.section_plug_play)) {
            Text(stringResource(R.string.plug_play_sub), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
            val current = allApps.firstOrNull { it.key == settings.plugPlayApp }
            ClickRow(stringResource(R.string.plug_play_choose), current?.label ?: stringResource(R.string.plug_play_none)) { plugDialog = true }
        }

        Group(stringResource(R.string.section_widgets)) {
            if (settings.widgetIds.isEmpty()) {
                Text(stringResource(R.string.no_home_widgets), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
            }
            settings.widgetIds.forEach { id ->
                val info = widgets.infoFor(id)
                val label: String? = info?.let { i -> runCatching { i.loadLabel(pm) }.getOrNull() }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(label ?: stringResource(R.string.widget_number, id), color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { actions.removeWidget(id) }) { Text(stringResource(R.string.remove)) }
                }
            }
            FilledTonalButton(onClick = { actions.addWidget() }, modifier = Modifier.padding(top = 6.dp)) { Text(stringResource(R.string.add_widget)) }
        }

        Group(stringResource(R.string.section_hidden)) {
            SwitchRow(stringResource(R.string.protect_hidden), stringResource(R.string.protect_hidden_sub), settings.protectHidden) { v ->
                if (!v) actions.authenticate { onEdit { it.copy(protectHidden = false) } } else onEdit { it.copy(protectHidden = true) }
            }
            if (settings.protectHidden && !hiddenUnlocked && hiddenApps.isNotEmpty()) {
                FilledTonalButton(onClick = { actions.authenticate { hiddenUnlocked = true } }) { Text(stringResource(R.string.protect_show_hidden)) }
                return@Group
            }
            if (hiddenApps.isEmpty()) {
                Text(stringResource(R.string.no_hidden_apps), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
            }
            hiddenApps.forEach { app ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Image(app.icon, null, Modifier.size(32.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(app.label, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { actions.unhide(app) }) { Text(stringResource(R.string.show)) }
                }
            }
        }

        Group(stringResource(R.string.section_backup)) {
            Text(stringResource(R.string.backup_desc), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                FilledTonalButton(onClick = { actions.exportBackup() }) { Text(stringResource(R.string.export)) }
                OutlinedButton(onClick = { actions.importBackup() }) { Text(stringResource(R.string.restore)) }
            }
            SwitchRow(
                stringResource(R.string.auto_backup),
                if (settings.autoBackupUri.isBlank()) stringResource(R.string.auto_backup_sub)
                else if (settings.lastAutoBackup == 0L) stringResource(R.string.auto_backup_pending)
                else stringResource(
                    R.string.auto_backup_last,
                    java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(java.util.Date(settings.lastAutoBackup)),
                ),
                settings.autoBackupUri.isNotBlank(),
            ) { v -> if (v) actions.chooseBackupFolder() else actions.disableAutoBackup() }
        }

        TextButton(onClick = { actions.showIntro() }, modifier = Modifier.padding(top = 12.dp)) { Text(stringResource(R.string.intro_again)) }
        Text(
            stringResource(R.string.version_line, com.gabrielepiccione.onda.BuildConfig.VERSION_NAME),
            color = colors.onWallpaperMuted,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(vertical = 28.dp, horizontal = 8.dp),
        )
    }

    gestureAppFor?.let { which ->
        AlertDialog(
            onDismissRequest = { gestureAppFor = null },
            title = { Text(stringResource(R.string.gesture_app)) },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(Modifier.androidxHeightIn()) {
                    items(allApps.size) { i ->
                        val app = allApps[i]
                        val selected = app.key == (if (which == "up") settings.swipeUpApp else settings.doubleTapApp)
                        ChoiceRow(app.label, selected) {
                            onEdit { if (which == "up") it.copy(swipeUpApp = app.key) else it.copy(doubleTapApp = app.key) }
                            gestureAppFor = null
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { gestureAppFor = null }) { Text(stringResource(R.string.close)) } },
        )
    }

    if (digestDialog) {
        val apps = remember(allApps) { allApps.distinctBy { it.packageName } }
        AlertDialog(
            onDismissRequest = { digestDialog = false },
            title = { Text(stringResource(R.string.digest_apps)) },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(Modifier.androidxHeightIn()) {
                    items(apps.size) { i ->
                        val pkg = apps[i].packageName
                        val on = pkg in settings.digestApps
                        ChoiceRow(apps[i].label, on) {
                            onEdit { it.copy(digestApps = if (on) it.digestApps - pkg else it.digestApps + pkg) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { digestDialog = false }) { Text(stringResource(R.string.close)) } },
        )
    }

    if (breakDialog) {
        AlertDialog(
            onDismissRequest = { breakDialog = false },
            title = { Text(stringResource(R.string.break_apps)) },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(Modifier.androidxHeightIn()) {
                    items(allApps.size) { i ->
                        val app = allApps[i]
                        val on = app.key in settings.breakApps
                        ChoiceRow(app.label, on) {
                            onEdit { it.copy(breakApps = if (on) it.breakApps - app.key else it.breakApps + app.key) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { breakDialog = false }) { Text(stringResource(R.string.close)) } },
        )
    }

    if (plugDialog) {
        AlertDialog(
            onDismissRequest = { plugDialog = false },
            title = { Text(stringResource(R.string.plug_play_choose)) },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(Modifier.androidxHeightIn()) {
                    item {
                        ChoiceRow(stringResource(R.string.plug_play_none), settings.plugPlayApp.isBlank()) {
                            onEdit { it.copy(plugPlayApp = "") }; plugDialog = false
                        }
                    }
                    items(allApps.size) { i ->
                        val app = allApps[i]
                        ChoiceRow(app.label, settings.plugPlayApp == app.key) {
                            onEdit { it.copy(plugPlayApp = app.key) }; plugDialog = false
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { plugDialog = false }) { Text(stringResource(R.string.close)) } },
        )
    }

    if (iconPackDialog) {
        val packs = remember { IconPacks.installed(context) }
        AlertDialog(
            onDismissRequest = { iconPackDialog = false },
            title = { Text(stringResource(R.string.icon_pack)) },
            text = {
                Column {
                    ChoiceRow(stringResource(R.string.system_default), settings.iconPack.isBlank()) {
                        onEdit { it.copy(iconPack = "") }; iconPackDialog = false
                    }
                    packs.forEach { p ->
                        ChoiceRow(p.label, settings.iconPack == p.packageName) {
                            onEdit { it.copy(iconPack = p.packageName) }; iconPackDialog = false
                        }
                    }
                    if (packs.isEmpty()) {
                        Text(
                            stringResource(R.string.no_icon_packs),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { iconPackDialog = false }) { Text(stringResource(R.string.close)) } },
        )
    }
}

@Composable
private fun GestureChips(selected: com.gabrielepiccione.onda.data.GestureAction, onPick: (com.gabrielepiccione.onda.data.GestureAction) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 6.dp)) {
        com.gabrielepiccione.onda.data.GestureAction.entries.forEach { g ->
            FilterChip(selected = selected == g, onClick = { onPick(g) }, label = { Text(stringResource(g.labelRes)) })
        }
    }
}

private fun Modifier.androidxHeightIn(): Modifier = this.then(Modifier.heightIn(max = 420.dp))

@Composable
private fun FontRow(title: String, path: String, onImport: () -> Unit, onClear: () -> Unit) {
    val colors = LocalOndaColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (path.isBlank()) stringResource(R.string.font_from_theme) else java.io.File(path).name.substringAfter('_'),
                color = colors.onWallpaperMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (path.isNotBlank()) TextButton(onClick = onClear) { Text(stringResource(R.string.remove)) }
        TextButton(onClick = onImport) { Text(stringResource(R.string.font_import)) }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    val colors = LocalOndaColors.current
    Text(
        title,
        color = colors.accent,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = 8.dp, top = 24.dp, bottom = 8.dp),
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.panel)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) { content() }
}

@Composable
private fun Label(text: String) {
    Text(text, color = LocalOndaColors.current.onWallpaper, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
}

@Composable
private fun SwitchRow(text: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = LocalOndaColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onChange(!checked) }
            .padding(vertical = 8.dp)
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge)
            if (sub != null) Text(sub, color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ClickRow(text: String, value: String, onClick: () -> Unit) {
    val colors = LocalOndaColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Text(text, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge)
        Text(value, color = colors.accent, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        androidx.compose.material3.RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun StatusRow(title: String, ok: Boolean, okText: String, koText: String, onClick: () -> Unit) {
    val colors = LocalOndaColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (ok) colors.accentContainer else MaterialTheme.colorScheme.errorContainer)
        ) {
            if (ok) Icon(Icons.Filled.Check, null, tint = colors.onAccentContainer, modifier = Modifier.size(16.dp))
            else Text("!", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge)
            Text(if (ok) okText else koText, color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
