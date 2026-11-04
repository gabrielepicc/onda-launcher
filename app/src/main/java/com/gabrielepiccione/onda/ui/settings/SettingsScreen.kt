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
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.clearAndSetSemantics
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
    val contactsPermission: Boolean,
)

interface SettingsActions {
    fun edit(transform: (Settings) -> Settings)
    fun requestDefault()
    fun notificationAccess()
    fun calendarPermission()
    fun contactsPermission()
    fun accessibility()
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
    fun close()
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
    var hiddenUnlocked by remember { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    var widgetToRemove by remember { mutableStateOf<Int?>(null) }
    val onEdit = actions::edit
    BackHandler(enabled = page != null) { page = null }

    Column(Modifier.fillMaxSize().blockTouches()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.scrim)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp).clip(CircleShape).clickable { if (page != null) page = null else actions.close() }
            ) {
                @Suppress("DEPRECATION")
                Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.onWallpaper)
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(page?.titleRes ?: R.string.settings), style = MaterialTheme.typography.headlineMedium, color = colors.onWallpaper)
        }

        // Anteprima dal vivo sopra lo sfondo vero: lista e orologio reagiscono mentre si modifica
        if (page == SettingsPage.LOOK && LocalConfiguration.current.screenHeightDp >= MIN_HEIGHT_FOR_PREVIEW) {
            SettingsPreview(settings, allApps)
        }

        key(page) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(colors.scrim)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
        if (page == null) {
            Group(stringResource(R.string.section_setup)) {
                StatusRow(stringResource(R.string.default_launcher), status.isDefaultLauncher, stringResource(R.string.default_launcher_ok), stringResource(R.string.default_launcher_ko)) { actions.requestDefault() }
                StatusRow(stringResource(R.string.notif_access), status.notificationAccess, stringResource(R.string.notif_access_ok), stringResource(R.string.notif_access_ko)) { actions.notificationAccess() }
                StatusRow(stringResource(R.string.calendar), status.calendarPermission, stringResource(R.string.calendar_ok), stringResource(R.string.calendar_ko)) { actions.calendarPermission() }
            }
            Group(stringResource(R.string.page_customize)) {
                SettingsPage.entries.forEach { p ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { page = p }
                            .padding(vertical = 14.dp),
                    ) {
                        Icon(p.icon, null, tint = colors.accent)
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(p.titleRes), color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Text(
                stringResource(R.string.version_line, com.gabrielepiccione.onda.BuildConfig.VERSION_NAME),
                color = colors.onWallpaperMuted,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(vertical = 28.dp, horizontal = 8.dp),
            )
        }

        if (page == SettingsPage.LOOK) Group(stringResource(R.string.section_styles)) {
            Text(stringResource(R.string.styles_subtitle), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
            FilledTonalButton(onClick = { actions.openStyles() }, modifier = Modifier.padding(top = 10.dp)) { Text(stringResource(R.string.styles_open)) }
            Label(stringResource(R.string.clock_style_setting))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                com.gabrielepiccione.onda.data.ClockStyle.entries.forEach { st ->
                    FilterChip(selected = settings.clockStyle == st, onClick = { onEdit { it.copy(clockStyle = st) } }, label = { Text(stringResource(st.labelRes)) })
                }
            }
        }

        if (page == SettingsPage.LOOK) Group(stringResource(R.string.section_style)) {
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
            ValueSlider(stringResource(R.string.text_size), percentText(settings.textScale), settings.textScale, Settings().textScale, 0.8f..1.3f, steps = 4) { v -> onEdit { it.copy(textScale = v) } }
            ValueSlider(stringResource(R.string.wallpaper_dim), percentText(settings.wallpaperDim), settings.wallpaperDim, Settings().wallpaperDim, 0f..0.8f) { v -> onEdit { it.copy(wallpaperDim = v) } }
            TextButton(onClick = { actions.wallpaper() }) { Text(stringResource(R.string.change_wallpaper_ellipsis)) }
        }

        if (page == SettingsPage.LOOK) Group(stringResource(R.string.section_text)) {
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
            ValueSlider(
                stringResource(R.string.font_weight),
                settings.listFontWeight.toString(),
                settings.listFontWeight.toFloat(),
                Settings().listFontWeight.toFloat(),
                100f..900f,
                steps = 7,
            ) { v -> onEdit { it.copy(listFontWeight = (v / 100).roundToInt() * 100) } }
            ValueSlider(
                stringResource(R.string.letter_spacing),
                percentText(settings.listLetterSpacing),
                settings.listLetterSpacing,
                Settings().listLetterSpacing,
                -0.05f..0.2f,
            ) { v -> onEdit { it.copy(listLetterSpacing = v) } }
            SwitchRow(stringResource(R.string.text_shadow), stringResource(R.string.text_shadow_sub), settings.textShadow) { v -> onEdit { it.copy(textShadow = v) } }
            SwitchRow(stringResource(R.string.show_battery), null, settings.showBattery) { v -> onEdit { it.copy(showBattery = v) } }
            Label(stringResource(R.string.custom_fonts))
            FontRow(stringResource(R.string.font_for_list), settings.customFontList, onImport = { actions.importFont(false) }, onClear = { onEdit { it.copy(customFontList = "") } })
            FontRow(stringResource(R.string.font_for_clock), settings.customFontClock, onImport = { actions.importFont(true) }, onClear = { onEdit { it.copy(customFontClock = "") } })
        }

        if (page == SettingsPage.LOOK) Group(stringResource(R.string.section_icons)) {
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
            ValueSlider(stringResource(R.string.icon_size), percentText(settings.iconSize), settings.iconSize, Settings().iconSize, 0.75f..1.4f) { v -> onEdit { it.copy(iconSize = v) } }
        }

        if (page == SettingsPage.HOME) Group(stringResource(R.string.section_home)) {
            Label(stringResource(R.string.home_position))
            HomePositionControl(settings) { onEdit(it) }
            SwitchRow(stringResource(R.string.clock_date), null, settings.showClock) { v -> onEdit { it.copy(showClock = v) } }
            SwitchRow(stringResource(R.string.next_event), stringResource(R.string.next_event_sub), settings.showCalendar) { v -> onEdit { it.copy(showCalendar = v) } }
            SwitchRow(stringResource(R.string.music_player), stringResource(R.string.music_player_sub), settings.showMedia) { v -> onEdit { it.copy(showMedia = v) } }
            SwitchRow(stringResource(R.string.alphabet_left), stringResource(R.string.alphabet_left_sub), settings.leftHanded) { v -> onEdit { it.copy(leftHanded = v) } }
            SwitchRow(stringResource(R.string.hide_alphabet), stringResource(R.string.hide_alphabet_sub), settings.hideAlphabet) { v -> onEdit { it.copy(hideAlphabet = v) } }
            SwitchRow(stringResource(R.string.contacts_in_alphabet), stringResource(R.string.contacts_in_alphabet_sub), settings.contactsInAlphabet) { v ->
                onEdit { it.copy(contactsInAlphabet = v) }
                if (v && !status.contactsPermission) actions.contactsPermission()
            }
            SwitchRow(stringResource(R.string.hide_fav_names), stringResource(R.string.hide_fav_names_sub), settings.hideFavoriteNames) { v -> onEdit { it.copy(hideFavoriteNames = v) } }
            SwitchRow(stringResource(R.string.hide_status_bar), null, settings.hideStatusBar) { v -> onEdit { it.copy(hideStatusBar = v) } }
            SwitchRow(stringResource(R.string.agenda_title), stringResource(R.string.agenda_sub), settings.showAgenda) { v ->
                onEdit { it.copy(showAgenda = v) }
                if (v && !status.calendarPermission) actions.calendarPermission()
            }
            SwitchRow(stringResource(R.string.summary_title), stringResource(R.string.summary_sub), settings.showNotificationSummary) { v ->
                onEdit { it.copy(showNotificationSummary = v) }
                if (v && !status.notificationAccess) actions.notificationAccess()
            }
            SwitchRow(stringResource(R.string.layout_lock), stringResource(R.string.layout_lock_sub), settings.lockLayout) { v -> onEdit { it.copy(lockLayout = v) } }
        }

        if (page == SettingsPage.GESTURES) Group(stringResource(R.string.section_gestures)) {
            Text(stringResource(R.string.swipe_down_fixed), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
            Label(stringResource(R.string.swipe_up))
            GestureChips(settings.swipeUp) { g -> onEdit { it.copy(swipeUp = g) } }
            SwitchRow(
                stringResource(R.string.double_tap_lock),
                if (status.accessibilityEnabled) stringResource(R.string.double_tap_lock_on) else stringResource(R.string.double_tap_lock_off),
                settings.doubleTapLock,
            ) { v ->
                onEdit { it.copy(doubleTapLock = v) }
                if (v && !status.accessibilityEnabled) actions.accessibility()
            }
            SwitchRow(stringResource(R.string.haptics), stringResource(R.string.haptics_sub), settings.haptics) { v -> onEdit { it.copy(haptics = v) } }
            Text(
                stringResource(R.string.gestures_help),
                color = colors.onWallpaperMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        if (page == SettingsPage.SEARCH) Group(stringResource(R.string.section_search)) {
            Label(stringResource(R.string.search_engine))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                FilterChip(selected = settings.searchEngine == "SYSTEM", onClick = { onEdit { it.copy(searchEngine = "SYSTEM") } }, label = { Text(stringResource(R.string.engine_system)) })
                listOf("DUCKDUCKGO" to "DuckDuckGo", "STARTPAGE" to "Startpage", "GOOGLE" to "Google", "ECOSIA" to "Ecosia", "BRAVE" to "Brave").forEach { (id, name) ->
                    FilterChip(selected = settings.searchEngine == id, onClick = { onEdit { it.copy(searchEngine = id) } }, label = { Text(name) })
                }
            }
            Text(stringResource(R.string.search_prefix_hint), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }

        if (page == SettingsPage.APPS) Group(stringResource(R.string.section_plug_play)) {
            Text(stringResource(R.string.plug_play_sub), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
            val current = allApps.firstOrNull { it.key == settings.plugPlayApp }
            ClickRow(stringResource(R.string.plug_play_choose), current?.label ?: stringResource(R.string.plug_play_none)) { plugDialog = true }
        }

        if (page == SettingsPage.APPS) Group(stringResource(R.string.section_breaker)) {
            Text(stringResource(R.string.breaker_sub), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
            Label(stringResource(R.string.breaker_seconds, settings.breakerSeconds))
            Slider(
                value = settings.breakerSeconds.toFloat(),
                onValueChange = { v -> onEdit { it.copy(breakerSeconds = v.roundToInt()) } },
                valueRange = 3f..60f,
            )
            val breakerApps = allApps.filter { it.key in settings.breakerApps }
            if (breakerApps.isEmpty()) {
                Text(stringResource(R.string.breaker_none), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall)
            }
            breakerApps.forEach { app ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(app.label, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onEdit { com.gabrielepiccione.onda.data.UsageBreaker.toggle(it, app.key) } }) { Text(stringResource(R.string.remove)) }
                }
            }
        }

        if (page == SettingsPage.HOME) Group(stringResource(R.string.section_widgets)) {
            val homeWidgets = settings.widgetIds + settings.widgetStacks.flatMap { it.widgets }
            if (homeWidgets.isEmpty()) {
                Text(stringResource(R.string.no_home_widgets), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
            }
            homeWidgets.forEach { id ->
                val info = widgets.infoFor(id)
                val label: String? = info?.let { i -> runCatching { i.loadLabel(pm) }.getOrNull() }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(label ?: stringResource(R.string.widget_number, id), color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { widgetToRemove = id }) { Text(stringResource(R.string.remove)) }
                }
            }
            FilledTonalButton(onClick = { actions.addWidget() }, modifier = Modifier.padding(top = 6.dp)) { Text(stringResource(R.string.add_widget)) }
        }

        if (page == SettingsPage.APPS) Group(stringResource(R.string.section_hidden)) {
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

        if (page == SettingsPage.BACKUP) Group(stringResource(R.string.section_backup)) {
            Text(stringResource(R.string.backup_desc), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                FilledTonalButton(onClick = { actions.exportBackup() }) { Text(stringResource(R.string.export)) }
                OutlinedButton(onClick = { actions.importBackup() }) { Text(stringResource(R.string.restore)) }
            }
        }
        }
        }
    }

    widgetToRemove?.let { id ->
        ConfirmDeleteWidgetDialog(onConfirm = { actions.removeWidget(id); widgetToRemove = null }, onDismiss = { widgetToRemove = null })
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

/** Pagine delle impostazioni, nell'ordine mostrato nell'indice. */
private enum class SettingsPage(@StringRes val titleRes: Int, val icon: ImageVector) {
    LOOK(R.string.page_look, Icons.Filled.Palette),
    HOME(R.string.section_home, Icons.Filled.Home),
    GESTURES(R.string.section_gestures, Icons.Filled.TouchApp),
    SEARCH(R.string.section_search, Icons.Filled.Search),
    APPS(R.string.page_apps, Icons.Filled.Apps),
    BACKUP(R.string.section_backup, Icons.Filled.Backup),
}

private const val MIN_HEIGHT_FOR_PREVIEW = 600

/** Anteprima con orologio e preferiti veri: usa gli stessi componenti della home, non è toccabile. */
@Composable
private fun SettingsPreview(settings: Settings, allApps: List<AppInfo>) {
    val apps = remember(settings.favorites, allApps) {
        allApps.filter { it.key in settings.favorites }.take(2).ifEmpty { allApps.take(2) }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp)
            .clipToBounds()
            .clearAndSetSemantics { }
            .padding(horizontal = 28.dp, vertical = 8.dp),
    ) {
        Column {
            if (settings.showClock) {
                ClockHeader(
                    event = null,
                    onTime = {},
                    onDate = {},
                    onEvent = {},
                    style = settings.clockStyle,
                    scale = settings.clockScale,
                    align = settings.clockAlign,
                    showBattery = settings.showBattery,
                )
            }
            apps.forEach { app ->
                FavoriteRow(
                    app = app,
                    swipeTarget = null,
                    notifications = emptyList(),
                    showIcon = settings.showIcons,
                    themedIcons = settings.themedIcons,
                    hideName = settings.hideFavoriteNames,
                    iconScale = settings.iconSize,
                    onClick = {},
                    onLongClick = {},
                    onSwipe = {},
                    onNotificationClick = {},
                    onNotificationDismiss = {},
                )
            }
        }
        Box(Modifier.matchParentSize().blockTouches())
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
