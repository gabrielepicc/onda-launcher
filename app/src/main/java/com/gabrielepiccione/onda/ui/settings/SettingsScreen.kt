// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.settings

import androidx.compose.material.icons.filled.HourglassEmpty
import com.gabrielepiccione.onda.data.UsageStatsRepository
import com.gabrielepiccione.onda.util.HapticEvent
import com.gabrielepiccione.onda.util.HapticSupport
import com.gabrielepiccione.onda.util.LocalOndaHaptics
import com.gabrielepiccione.onda.util.OndaHaptics
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import com.gabrielepiccione.onda.data.SettingsSearch
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
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
import com.gabrielepiccione.onda.data.GestureAction
import com.gabrielepiccione.onda.data.HomeSuggestions
import com.gabrielepiccione.onda.data.UsageReminders
import com.gabrielepiccione.onda.core.icons.IconPacks
import com.gabrielepiccione.onda.data.Settings
import com.gabrielepiccione.onda.data.WidgetManager

data class SettingsStatus(
    val isDefaultLauncher: Boolean,
    val notificationAccess: Boolean,
    val calendarPermission: Boolean,
    val accessibilityEnabled: Boolean,
    val contactsPermission: Boolean,
    val weatherInstalled: Boolean = false,
    val weatherPermission: Boolean = false,
    val usageAccess: Boolean = false,
    val postNotifications: Boolean = false,
)

interface SettingsActions {
    fun edit(transform: (Settings) -> Settings)
    fun requestDefault()
    fun notificationAccess()
    fun calendarPermission()
    fun contactsPermission()
    fun weatherPermission()
    fun postNotifications()
    fun usageAccess()
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
    fun chooseBackupFolder()
    fun disableAutoBackup()
    fun shareBackup()
    fun close()
}

@Composable
fun SettingsScreen(
    settings: Settings,
    hiddenApps: List<AppInfo>,
    allApps: List<AppInfo> = emptyList(),
    hasPrivateSpace: Boolean = false,
    status: SettingsStatus,
    widgets: WidgetManager,
    actions: SettingsActions,
    usageStats: UsageStatsRepository? = null,
) {
    val colors = LocalOndaColors.current
    val context = LocalContext.current
    val pm = context.packageManager
    var iconPackDialog by remember { mutableStateOf(false) }
    var plugDialog by remember { mutableStateOf(false) }
    var hiddenUnlocked by remember { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var widgetToRemove by remember { mutableStateOf<Int?>(null) }
    // Dove salvare l'app scelta per un gesto; null = scelta chiusa
    var gestureAppPick by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var gesturePick by remember { mutableStateOf<GesturePick?>(null) }
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
            Text(stringResource(page?.titleRes ?: R.string.settings), style = MaterialTheme.typography.titleLarge, color = colors.onWallpaper)
        }

        // Anteprima dal vivo sopra lo sfondo vero: resta ferma mentre sotto si scorre e si modifica
        if (LocalConfiguration.current.screenHeightDp >= MIN_HEIGHT_FOR_PREVIEW) {
            when (page) {
                SettingsPage.STYLE -> StylePreview(settings, allApps)
                SettingsPage.CLOCK -> ClockPreview(settings)
                SettingsPage.TEXT, SettingsPage.ICONS -> RowsPreview(settings, allApps)
                SettingsPage.HOME -> PhonePreview(
                    settings,
                    listOf(stringResource(if (settings.homeTopFraction >= 0f) R.string.phone_hint_manual else R.string.phone_hint_auto), stringResource(R.string.phone_hint_drag)),
                    onPosition = { v -> onEdit { it.copy(homeTopFraction = v) } },
                )
                SettingsPage.ALPHABET -> PhonePreview(
                    settings,
                    listOf(
                        stringResource(if (settings.hideAlphabet) R.string.phone_letters_hidden else R.string.phone_letters),
                        stringResource(R.string.phone_invisible_side),
                        stringResource(if (settings.showSearchButton) R.string.phone_search_on else R.string.phone_search_off),
                    ),
                    emphasizeAlphabet = true,
                )
                else -> Unit
            }
        }

        key(page) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(colors.scrim)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 16.dp + com.gabrielepiccione.onda.data.ScreenLayout.sideInsetDp(LocalConfiguration.current.screenWidthDp).dp,
                    vertical = 8.dp,
                )
        ) {
        if (page == null) {
            // Solo ciò che manca: quando è tutto pronto, l'indice parte dalle personalizzazioni
            val calendarUsed = settings.showCalendar || settings.showAgenda
            val remindersUsed = settings.reminderApps.isNotEmpty()
            val setupMissing = !status.isDefaultLauncher || !status.notificationAccess || (calendarUsed && !status.calendarPermission) ||
                (remindersUsed && (!status.usageAccess || !status.postNotifications))
            if (setupMissing) Group(stringResource(R.string.section_setup)) {
                if (!status.isDefaultLauncher) StatusRow(stringResource(R.string.default_launcher), false, stringResource(R.string.default_launcher_ok), stringResource(R.string.default_launcher_ko)) { actions.requestDefault() }
                if (!status.notificationAccess) StatusRow(stringResource(R.string.notif_access), false, stringResource(R.string.notif_access_ok), stringResource(R.string.notif_access_ko)) { actions.notificationAccess() }
                if (calendarUsed && !status.calendarPermission) StatusRow(stringResource(R.string.calendar), false, stringResource(R.string.calendar_ok), stringResource(R.string.calendar_ko)) { actions.calendarPermission() }
                if (remindersUsed && !status.usageAccess) StatusRow(stringResource(R.string.usage_access), false, stringResource(R.string.usage_access_ok), stringResource(R.string.usage_access_ko)) { actions.usageAccess() }
                if (remindersUsed && !status.postNotifications) StatusRow(stringResource(R.string.post_notifications), false, stringResource(R.string.post_notifications_ok), stringResource(R.string.post_notifications_ko)) { actions.postNotifications() }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.settings_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.settings_search_clear)) }
                    }
                },
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            if (query.isNotBlank()) {
                val found = SETTING_ENTRIES.filter { e ->
                    SettingsSearch.matches(query, stringResource(e.titleRes), stringResource(e.page.titleRes), stringResource(e.keywordsRes))
                }
                Group(stringResource(R.string.settings_search_results)) {
                    if (found.isEmpty()) {
                        Text(stringResource(R.string.settings_search_none), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                    found.forEach { e ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { page = e.page; query = "" }
                                .padding(vertical = 10.dp),
                        ) {
                            Text(stringResource(e.titleRes), color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge)
                            Text(stringResource(e.page.titleRes), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (query.isBlank()) Group(stringResource(R.string.page_customize)) {
                SettingsPage.entries.forEach { p ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { page = p }
                            .padding(vertical = 10.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(colors.accentContainer),
                        ) {
                            Icon(p.icon, null, tint = colors.onAccentContainer, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(p.titleRes), color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge)
                            Text(stringResource(p.subRes), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (query.isBlank()) Text(
                stringResource(R.string.version_line, com.gabrielepiccione.onda.BuildConfig.VERSION_NAME),
                color = colors.onWallpaperMuted,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(vertical = 28.dp, horizontal = 8.dp),
            )
        }

        if (page == SettingsPage.STYLE) Group(stringResource(R.string.section_styles)) {
            Text(stringResource(R.string.styles_subtitle), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
            FilledTonalButton(onClick = { actions.openStyles() }, modifier = Modifier.padding(top = 10.dp)) { Text(stringResource(R.string.styles_open)) }
        }

        if (page == SettingsPage.STYLE) Group(stringResource(R.string.section_style)) {
            Label(stringResource(R.string.theme))
            ThemeTiles(settings.themeMode) { mode -> onEdit { it.copy(themeMode = mode) } }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchRow(stringResource(R.string.dynamic_color), stringResource(R.string.dynamic_color_sub), settings.dynamicColor) { v -> onEdit { it.copy(dynamicColor = v) } }
            }
            if (!settings.dynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                Label(stringResource(R.string.accent_color))
                ColorSwatches(AccentChoices, settings.accent) { argb -> onEdit { it.copy(accent = argb) } }
            }
            ValueSlider(stringResource(R.string.wallpaper_dim), percentText(settings.wallpaperDim), settings.wallpaperDim, Settings().wallpaperDim, 0f..0.8f) { v -> onEdit { it.copy(wallpaperDim = v) } }
            TextButton(onClick = { actions.wallpaper() }) { Text(stringResource(R.string.change_wallpaper_ellipsis)) }
        }

        if (page == SettingsPage.CLOCK) Group(stringResource(R.string.edit_clock)) {
            SwitchRow(stringResource(R.string.clock_date), null, settings.showClock) { v -> onEdit { it.copy(showClock = v) } }
            if (settings.showClock) {
                Label(stringResource(R.string.clock_style_setting))
                ClockStyleTiles(settings.clockStyle) { st -> onEdit { it.copy(clockStyle = st) } }
                ValueSlider(stringResource(R.string.edit_size), percentText(settings.clockScale), settings.clockScale, Settings().clockScale, 0.6f..1.6f) { v -> onEdit { it.copy(clockScale = v) } }
                Label(stringResource(R.string.edit_alignment))
                AlignTiles(settings.clockAlign) { a -> onEdit { it.copy(clockAlign = a) } }
            }
        }

        if (page == SettingsPage.CLOCK && settings.showClock) Group(stringResource(R.string.section_under_clock)) {
            SwitchRow(stringResource(R.string.next_event), stringResource(R.string.next_event_sub), settings.showCalendar) { v ->
                onEdit { it.copy(showCalendar = v) }
                if (v && !status.calendarPermission) actions.calendarPermission()
            }
            SwitchRow(stringResource(R.string.show_battery), null, settings.showBattery) { v -> onEdit { it.copy(showBattery = v) } }
            SwitchRow(
                stringResource(R.string.weather_title),
                when {
                    settings.showWeather && !status.weatherInstalled -> stringResource(R.string.weather_needs_app)
                    settings.showWeather && !status.weatherPermission -> stringResource(R.string.weather_needs_permission)
                    else -> stringResource(R.string.weather_sub)
                },
                settings.showWeather,
            ) { v ->
                onEdit { it.copy(showWeather = v) }
                if (v && status.weatherInstalled && !status.weatherPermission) actions.weatherPermission()
            }
        }

        if (page == SettingsPage.TEXT) Group(stringResource(R.string.section_text)) {
            Label(stringResource(R.string.font))
            FontTiles(settings.font) { f -> onEdit { it.copy(font = f) } }
            ValueSlider(stringResource(R.string.text_size), percentText(settings.textScale), settings.textScale, Settings().textScale, 0.8f..1.3f, steps = 4) { v -> onEdit { it.copy(textScale = v) } }
            Label(stringResource(R.string.text_color))
            FilterChip(selected = settings.textColor == 0L, onClick = { onEdit { it.copy(textColor = 0) } }, label = { Text(stringResource(R.string.text_color_auto)) })
            ColorSwatches(TextColorChoices, settings.textColor) { argb -> onEdit { it.copy(textColor = argb) } }
            Label(stringResource(R.string.list_alignment))
            AlignTiles(settings.listAlign) { a -> onEdit { it.copy(listAlign = a) } }
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
        }

        if (page == SettingsPage.TEXT) Group(stringResource(R.string.custom_fonts)) {
            FontRow(stringResource(R.string.font_for_list), settings.customFontList, onImport = { actions.importFont(false) }, onClear = { onEdit { it.copy(customFontList = "") } })
            FontRow(stringResource(R.string.font_for_clock), settings.customFontClock, onImport = { actions.importFont(true) }, onClear = { onEdit { it.copy(customFontClock = "") } })
        }

        if (page == SettingsPage.ICONS) Group(stringResource(R.string.section_icons)) {
            SwitchRow(stringResource(R.string.show_icons), null, settings.showIcons) { v -> onEdit { it.copy(showIcons = v) } }
            if (settings.showIcons) {
                ValueSlider(stringResource(R.string.icon_size), percentText(settings.iconSize), settings.iconSize, Settings().iconSize, 0.75f..1.4f) { v -> onEdit { it.copy(iconSize = v) } }
                Label(stringResource(R.string.icon_style))
                val sample = remember(allApps) { allApps.firstOrNull { !it.hasCustomIcon } }
                IconStyleTiles(settings.iconStyle, sample) { id -> onEdit { it.copy(iconStyle = id) } }
                Label(stringResource(R.string.icon_shape))
                IconShapeTiles(settings.iconShape) { id -> onEdit { it.copy(iconShape = id) } }
                Text(stringResource(R.string.icon_shape_hint), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    SwitchRow(stringResource(R.string.themed_icons), stringResource(R.string.themed_icons_sub), settings.themedIcons) { v -> onEdit { it.copy(themedIcons = v) } }
                }
                val systemDefault = stringResource(R.string.system_default)
                val packLabel = remember(settings.iconPack, systemDefault) {
                    if (settings.iconPack.isBlank()) systemDefault
                    else runCatching { pm.getApplicationLabel(pm.getApplicationInfo(settings.iconPack, 0)).toString() }.getOrDefault(settings.iconPack)
                }
                ClickRow(stringResource(R.string.icon_pack), packLabel) { iconPackDialog = true }
            }
        }

        if (page == SettingsPage.HOME) Group(stringResource(R.string.home_position)) {
            HomePositionControl(settings) { onEdit(it) }
        }

        if (page == SettingsPage.HOME) Group(stringResource(R.string.section_blocks)) {
            SwitchRow(stringResource(R.string.music_player), stringResource(R.string.music_player_sub), settings.showMedia) { v -> onEdit { it.copy(showMedia = v) } }
            SwitchRow(stringResource(R.string.agenda_title), stringResource(R.string.agenda_sub), settings.showAgenda) { v ->
                onEdit { it.copy(showAgenda = v) }
                if (v && !status.calendarPermission) actions.calendarPermission()
            }
            SwitchRow(stringResource(R.string.summary_title), stringResource(R.string.summary_sub), settings.showNotificationSummary) { v ->
                onEdit { it.copy(showNotificationSummary = v) }
                if (v && !status.notificationAccess) actions.notificationAccess()
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

        if (page == SettingsPage.HOME) Group(stringResource(R.string.section_favorites)) {
            SwitchRow(stringResource(R.string.hide_fav_names), stringResource(R.string.hide_fav_names_sub), settings.hideFavoriteNames) { v -> onEdit { it.copy(hideFavoriteNames = v) } }
            Label(stringResource(R.string.notif_images))
            TextChoices(
                com.gabrielepiccione.onda.data.NotificationImages.entries.map { it to stringResource(it.labelRes) },
                settings.notificationImages,
                columns = 3,
            ) { v -> onEdit { it.copy(notificationImages = v) } }
            Text(stringResource(R.string.notif_images_sub), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall)
            SwitchRow(stringResource(R.string.suggested_setting), stringResource(R.string.suggested_setting_sub), settings.suggestInHome) { v -> onEdit { it.copy(suggestInHome = v) } }
            allApps.filter { it.key in settings.suggestionsDismissed }.forEach { app ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(app.label, color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onEdit { HomeSuggestions.restore(it, app.key) } }) { Text(stringResource(R.string.suggested_restore)) }
                }
            }
        }

        if (page == SettingsPage.HOME) Group(stringResource(R.string.section_layout)) {
            SwitchRow(stringResource(R.string.layout_lock), stringResource(R.string.layout_lock_sub), settings.lockLayout) { v -> onEdit { it.copy(lockLayout = v) } }
            SwitchRow(stringResource(R.string.hide_status_bar), null, settings.hideStatusBar) { v -> onEdit { it.copy(hideStatusBar = v) } }
        }

        if (page == SettingsPage.ALPHABET) Group(stringResource(R.string.section_alphabet)) {
            SwitchRow(stringResource(R.string.alphabet_left), stringResource(R.string.alphabet_left_sub), settings.leftHanded) { v -> onEdit { it.copy(leftHanded = v) } }
            SwitchRow(stringResource(R.string.hide_alphabet), stringResource(R.string.hide_alphabet_sub), settings.hideAlphabet) { v -> onEdit { it.copy(hideAlphabet = v) } }
            val restText = stringResource(
                when {
                    settings.alphabetRest >= 0.95f -> R.string.alphabet_height_bottom
                    settings.alphabetRest <= 0.05f -> R.string.alphabet_height_top
                    settings.alphabetRest in 0.45f..0.55f -> R.string.alphabet_height_middle
                    else -> R.string.alphabet_height_custom
                },
            )
            ValueSlider(stringResource(R.string.alphabet_height), restText, settings.alphabetRest, Settings().alphabetRest, 0f..1f) { v -> onEdit { it.copy(alphabetRest = v) } }
            ValueSlider(stringResource(R.string.alphabet_size), percentText(settings.alphabetScale), settings.alphabetScale, Settings().alphabetScale, 0.8f..1.4f) { v -> onEdit { it.copy(alphabetScale = v) } }
            ValueSlider(
                stringResource(R.string.alphabet_spacing),
                stringResource(R.string.value_dp, settings.alphabetStepDp),
                settings.alphabetStepDp.toFloat(),
                Settings().alphabetStepDp.toFloat(),
                13f..24f,
                steps = 10,
            ) { v -> onEdit { it.copy(alphabetStepDp = v.roundToInt()) } }
            SwitchRow(stringResource(R.string.contacts_in_alphabet), stringResource(R.string.contacts_in_alphabet_sub), settings.contactsInAlphabet) { v ->
                onEdit { it.copy(contactsInAlphabet = v) }
                if (v && !status.contactsPermission) actions.contactsPermission()
            }
        }

        if (page == SettingsPage.ALPHABET) Group(stringResource(R.string.section_search)) {
            SwitchRow(stringResource(R.string.search_button), stringResource(R.string.search_button_sub), settings.showSearchButton) { v -> onEdit { it.copy(showSearchButton = v) } }
            if (settings.showSearchButton) {
                Label(stringResource(R.string.search_button_color))
                SearchButtonColorChoices(settings.searchButtonColor) { c -> onEdit { it.copy(searchButtonColor = c) } }
                Label(stringResource(R.string.search_button_icon))
                SearchButtonIconChoices(settings.searchButtonIcon) { n -> onEdit { it.copy(searchButtonIcon = n) } }
                GestureSetting(
                    title = stringResource(R.string.search_button_tap),
                    subtitle = stringResource(R.string.search_button_gesture_sub),
                    action = settings.searchButtonTap,
                    appKey = settings.searchButtonTapApp,
                    allApps = allApps,
                    accessibilityEnabled = status.accessibilityEnabled,
                    onOpen = {
                        gesturePick = GesturePick(R.string.search_button_tap, settings.searchButtonTap) { g ->
                            onEdit { it.copy(searchButtonTap = g) }
                            if (g == GestureAction.LOCK_SCREEN && !status.accessibilityEnabled) actions.accessibility()
                        }
                    },
                    onChooseApp = { gestureAppPick = { key -> onEdit { it.copy(searchButtonTapApp = key) } } },
                )
                GestureSetting(
                    title = stringResource(R.string.search_button_long),
                    subtitle = stringResource(R.string.search_button_gesture_sub),
                    action = settings.searchButtonLong,
                    appKey = settings.searchButtonLongApp,
                    allApps = allApps,
                    accessibilityEnabled = status.accessibilityEnabled,
                    onOpen = {
                        gesturePick = GesturePick(R.string.search_button_long, settings.searchButtonLong) { g ->
                            onEdit { it.copy(searchButtonLong = g) }
                            if (g == GestureAction.LOCK_SCREEN && !status.accessibilityEnabled) actions.accessibility()
                        }
                    },
                    onChooseApp = { gestureAppPick = { key -> onEdit { it.copy(searchButtonLongApp = key) } } },
                )
                GestureSetting(
                    title = stringResource(R.string.search_button_swipe),
                    subtitle = stringResource(R.string.search_button_gesture_sub),
                    action = settings.searchButtonSwipe,
                    appKey = settings.searchButtonSwipeApp,
                    allApps = allApps,
                    accessibilityEnabled = status.accessibilityEnabled,
                    onOpen = {
                        gesturePick = GesturePick(R.string.search_button_swipe, settings.searchButtonSwipe) { g ->
                            onEdit { it.copy(searchButtonSwipe = g) }
                            if (g == GestureAction.LOCK_SCREEN && !status.accessibilityEnabled) actions.accessibility()
                        }
                    },
                    onChooseApp = { gestureAppPick = { key -> onEdit { it.copy(searchButtonSwipeApp = key) } } },
                )
            }
            Label(stringResource(R.string.search_engine))
            TextChoices(
                listOf(
                    "SYSTEM" to stringResource(R.string.engine_system), "DUCKDUCKGO" to "DuckDuckGo", "STARTPAGE" to "Startpage",
                    "GOOGLE" to "Google", "ECOSIA" to "Ecosia", "BRAVE" to "Brave",
                ),
                settings.searchEngine,
            ) { id -> onEdit { it.copy(searchEngine = id) } }
            Text(stringResource(R.string.search_prefix_hint), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }

        if (page == SettingsPage.GESTURES) Group(stringResource(R.string.section_gestures_custom)) {
            GestureSetting(
                title = stringResource(R.string.swipe_up),
                subtitle = stringResource(R.string.swipe_up_sub),
                action = settings.swipeUp,
                appKey = settings.swipeUpApp,
                allApps = allApps,
                accessibilityEnabled = status.accessibilityEnabled,
                onOpen = {
                    gesturePick = GesturePick(R.string.swipe_up, settings.swipeUp) { g ->
                        onEdit { it.copy(swipeUp = g) }
                        if (g == GestureAction.LOCK_SCREEN && !status.accessibilityEnabled) actions.accessibility()
                    }
                },
                onChooseApp = { gestureAppPick = { key -> onEdit { it.copy(swipeUpApp = key) } } },
            )
            GestureSetting(
                title = stringResource(R.string.double_tap),
                subtitle = stringResource(R.string.double_tap_sub),
                action = settings.doubleTapAction,
                appKey = settings.doubleTapApp,
                allApps = allApps,
                accessibilityEnabled = status.accessibilityEnabled,
                onOpen = {
                    gesturePick = GesturePick(R.string.double_tap, settings.doubleTapAction) { g ->
                        onEdit { it.copy(doubleTapAction = g) }
                        if (g == GestureAction.LOCK_SCREEN && !status.accessibilityEnabled) actions.accessibility()
                    }
                },
                onChooseApp = { gestureAppPick = { key -> onEdit { it.copy(doubleTapApp = key) } } },
            )
        }

        if (page == SettingsPage.GESTURES) Group(stringResource(R.string.section_gestures_fixed)) {
            listOf(
                R.string.swipe_down_fixed, R.string.gesture_help_right, R.string.gesture_help_left,
                R.string.gesture_help_hold, R.string.gesture_help_edges, R.string.gesture_help_alphabet,
            ).forEach { res ->
                Text(stringResource(res), color = colors.onWallpaper, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 6.dp))
            }
        }

        if (page == SettingsPage.GESTURES) Group(stringResource(R.string.section_feedback)) {
            Label(stringResource(R.string.open_animation))
            TextChoices(com.gabrielepiccione.onda.data.OpenAnimation.entries.map { it to stringResource(it.labelRes) }, settings.openAnimation, columns = 2) { a ->
                onEdit { it.copy(openAnimation = a) }
            }
            SwitchRow(stringResource(R.string.haptics), stringResource(R.string.haptics_sub), settings.haptics) { v -> onEdit { it.copy(haptics = v) } }
            if (settings.haptics) {
                // Prova: fa sentire i colpi principali e dice se il telefono ha le vibrazioni leggere
                val hapticContext = LocalContext.current
                val support = remember { OndaHaptics.support(hapticContext) }
                val tester = LocalOndaHaptics.current
                val testScope = rememberCoroutineScope()
                ClickRow(
                    stringResource(R.string.haptics_test),
                    stringResource(
                        when (support) {
                            HapticSupport.LIGHT -> R.string.haptics_support_light
                            HapticSupport.BASIC -> R.string.haptics_support_basic
                            HapticSupport.UNKNOWN -> R.string.haptics_support_unknown
                            HapticSupport.NONE -> R.string.haptics_support_none
                        },
                    ),
                ) {
                    testScope.launch {
                        listOf(HapticEvent.LETTER, HapticEvent.LETTER, HapticEvent.LETTER, HapticEvent.EDGE, HapticEvent.THRESHOLD_ON, HapticEvent.CONFIRM)
                            .forEach { e -> tester?.perform(e); delay(if (e == HapticEvent.LETTER) 90 else 420) }
                    }
                }
            }
        }

        if (page == SettingsPage.APPS) Group(stringResource(R.string.section_plug_play)) {
            Text(stringResource(R.string.plug_play_sub), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
            val current = allApps.firstOrNull { it.key == settings.plugPlayApp }
            ClickRow(stringResource(R.string.plug_play_choose), current?.label ?: stringResource(R.string.plug_play_none)) { plugDialog = true }
        }

        if (page == SettingsPage.WELLBEING && usageStats != null) {
            WellbeingDashboard(
                repo = usageStats,
                hasAccess = status.usageAccess,
                apps = allApps,
                limits = settings.appLimits,
                onRequestAccess = { actions.usageAccess() },
                onSetLimit = { pkg, minutes ->
                    onEdit { s -> s.copy(appLimits = if (minutes > 0) s.appLimits + (pkg to minutes) else s.appLimits - pkg) }
                },
            )
        }

        if (page == SettingsPage.WELLBEING) Group(stringResource(R.string.section_limits)) {
            Text(stringResource(R.string.limits_sub), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
            if (settings.appLimits.isNotEmpty() && !status.postNotifications) {
                StatusRow(stringResource(R.string.post_notifications), false, stringResource(R.string.post_notifications_ok), stringResource(R.string.post_notifications_ko)) { actions.postNotifications() }
            }
            if (settings.appLimits.isEmpty()) {
                Text(stringResource(R.string.breaker_none), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall)
            }
            settings.appLimits.forEach { (pkg, minutes) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(allApps.firstOrNull { it.packageName == pkg }?.label ?: pkg, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge)
                        Text(durationText(minutes * 60_000L), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { onEdit { it.copy(appLimits = it.appLimits - pkg) } }) { Text(stringResource(R.string.remove)) }
                }
            }
        }

        if (page == SettingsPage.WELLBEING) Group(stringResource(R.string.section_breaker)) {
            Text(stringResource(R.string.breaker_sub), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
            if ((settings.breakerApps.isNotEmpty() || settings.appLimits.isNotEmpty()) && !status.accessibilityEnabled) {
                StatusRow(stringResource(R.string.breaker_everywhere), false, stringResource(R.string.breaker_everywhere_ok), stringResource(R.string.breaker_everywhere_ko)) { actions.accessibility() }
            }
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

        if (page == SettingsPage.WELLBEING) Group(stringResource(R.string.section_reminders)) {
            Text(stringResource(R.string.reminder_sub), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
            Label(stringResource(R.string.reminder_start))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UsageReminders.START_CHOICES.forEach { m ->
                    FilterChip(
                        selected = settings.reminderStartMinutes == m,
                        onClick = { onEdit { it.copy(reminderStartMinutes = m) } },
                        label = { Text(stringResource(R.string.reminder_minutes, m)) },
                    )
                }
            }
            val reminderApps = allApps.filter { it.key in settings.reminderApps }
            if (reminderApps.isEmpty()) {
                Text(stringResource(R.string.breaker_none), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall)
            }
            reminderApps.forEach { app ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(app.label, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onEdit { UsageReminders.toggle(it, app.key) } }) { Text(stringResource(R.string.remove)) }
                }
            }
        }

        if (page == SettingsPage.APPS && hasPrivateSpace) Group(stringResource(R.string.private_space)) {
            SwitchRow(stringResource(R.string.private_hide), stringResource(R.string.private_hide_sub), settings.hidePrivateSpace) { v -> onEdit { it.copy(hidePrivateSpace = v) } }
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
            TextButton(onClick = { actions.shareBackup() }) { Text(stringResource(R.string.backup_share)) }
            Label(stringResource(R.string.auto_backup))
            Text(stringResource(R.string.auto_backup_sub), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall)
            if (settings.autoBackupUri.isNotBlank()) {
                Text(
                    if (settings.autoBackupLastDay.isBlank()) stringResource(R.string.auto_backup_pending) else stringResource(R.string.auto_backup_last, settings.autoBackupLastDay),
                    color = colors.accent,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                FilledTonalButton(onClick = { actions.chooseBackupFolder() }) {
                    Text(stringResource(if (settings.autoBackupUri.isBlank()) R.string.auto_backup_enable else R.string.auto_backup_change))
                }
                if (settings.autoBackupUri.isNotBlank()) {
                    OutlinedButton(onClick = { actions.disableAutoBackup() }) { Text(stringResource(R.string.auto_backup_disable)) }
                }
            }
        }
        }
        }
    }

    gesturePick?.let { pick ->
        AlertDialog(
            onDismissRequest = { gesturePick = null },
            title = { Text(stringResource(pick.titleRes)) },
            text = {
                Column {
                    GestureAction.entries.forEach { g ->
                        ChoiceRow(stringResource(g.labelRes), pick.current == g) {
                            pick.onPick(g)
                            gesturePick = null
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { gesturePick = null }) { Text(stringResource(R.string.close)) } },
        )
    }

    widgetToRemove?.let { id ->
        ConfirmDeleteWidgetDialog(onConfirm = { actions.removeWidget(id); widgetToRemove = null }, onDismiss = { widgetToRemove = null })
    }

    gestureAppPick?.let { save ->
        AlertDialog(
            onDismissRequest = { gestureAppPick = null },
            title = { Text(stringResource(R.string.gesture_choose_app)) },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(Modifier.androidxHeightIn()) {
                    items(allApps.size, key = { allApps[it].key }) { i ->
                        val app = allApps[i]
                        ChoiceRow(app.label, false) {
                            save(app.key)
                            gestureAppPick = null
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { gestureAppPick = null }) { Text(stringResource(R.string.close)) } },
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
                    items(allApps.size, key = { allApps[it].key }) { i ->
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
private enum class SettingsPage(@StringRes val titleRes: Int, @StringRes val subRes: Int, val icon: ImageVector) {
    STYLE(R.string.section_style, R.string.page_style_sub, Icons.Filled.Palette),
    CLOCK(R.string.edit_clock, R.string.page_clock_sub, Icons.Filled.Schedule),
    TEXT(R.string.section_text, R.string.page_text_sub, Icons.Filled.TextFields),
    ICONS(R.string.section_icons, R.string.page_icons_sub, Icons.Filled.Category),
    HOME(R.string.section_home, R.string.page_home_sub, Icons.Filled.Home),
    ALPHABET(R.string.page_alphabet, R.string.page_alphabet_sub, Icons.Filled.Search),
    GESTURES(R.string.section_gestures, R.string.page_gestures_sub, Icons.Filled.TouchApp),
    WELLBEING(R.string.page_wellbeing, R.string.page_wellbeing_sub, Icons.Filled.HourglassEmpty),
    APPS(R.string.page_apps, R.string.page_apps_sub, Icons.Filled.Apps),
    BACKUP(R.string.section_backup, R.string.page_backup_sub, Icons.Filled.Backup),
}

private const val MIN_HEIGHT_FOR_PREVIEW = 600

/** Una voce cercabile: il titolo, la pagina che la contiene e parole in più per trovarla. */
private class SettingEntry(@StringRes val titleRes: Int, val page: SettingsPage, @StringRes val keywordsRes: Int)

private val SETTING_ENTRIES = listOf(
    SettingEntry(R.string.section_style, SettingsPage.STYLE, R.string.kw_style),
    SettingEntry(R.string.dynamic_color, SettingsPage.STYLE, R.string.kw_style),
    SettingEntry(R.string.accent_color, SettingsPage.STYLE, R.string.kw_style),
    SettingEntry(R.string.clock_date, SettingsPage.CLOCK, R.string.kw_clock),
    SettingEntry(R.string.clock_style_setting, SettingsPage.CLOCK, R.string.kw_clock),
    SettingEntry(R.string.show_battery, SettingsPage.CLOCK, R.string.kw_clock),
    SettingEntry(R.string.next_event, SettingsPage.CLOCK, R.string.kw_clock),
    SettingEntry(R.string.font, SettingsPage.TEXT, R.string.kw_text),
    SettingEntry(R.string.text_color, SettingsPage.TEXT, R.string.kw_text),
    SettingEntry(R.string.list_alignment, SettingsPage.TEXT, R.string.kw_text),
    SettingEntry(R.string.text_shadow, SettingsPage.TEXT, R.string.kw_text),
    SettingEntry(R.string.custom_fonts, SettingsPage.TEXT, R.string.kw_text),
    SettingEntry(R.string.show_icons, SettingsPage.ICONS, R.string.kw_icons),
    SettingEntry(R.string.icon_style, SettingsPage.ICONS, R.string.kw_icons),
    SettingEntry(R.string.icon_shape, SettingsPage.ICONS, R.string.kw_icons),
    SettingEntry(R.string.themed_icons, SettingsPage.ICONS, R.string.kw_icons),
    SettingEntry(R.string.icon_pack, SettingsPage.ICONS, R.string.kw_icons),
    SettingEntry(R.string.home_position, SettingsPage.HOME, R.string.kw_home),
    SettingEntry(R.string.music_player, SettingsPage.HOME, R.string.kw_home),
    SettingEntry(R.string.agenda_title, SettingsPage.HOME, R.string.kw_home),
    SettingEntry(R.string.summary_title, SettingsPage.HOME, R.string.kw_home),
    SettingEntry(R.string.section_widgets, SettingsPage.HOME, R.string.kw_home),
    SettingEntry(R.string.hide_fav_names, SettingsPage.HOME, R.string.kw_home),
    SettingEntry(R.string.notif_images, SettingsPage.HOME, R.string.kw_notif),
    SettingEntry(R.string.suggested_setting, SettingsPage.HOME, R.string.kw_home),
    SettingEntry(R.string.layout_lock, SettingsPage.HOME, R.string.kw_home),
    SettingEntry(R.string.hide_status_bar, SettingsPage.HOME, R.string.kw_home),
    SettingEntry(R.string.alphabet_left, SettingsPage.ALPHABET, R.string.kw_alphabet),
    SettingEntry(R.string.hide_alphabet, SettingsPage.ALPHABET, R.string.kw_alphabet),
    SettingEntry(R.string.contacts_in_alphabet, SettingsPage.ALPHABET, R.string.kw_alphabet),
    SettingEntry(R.string.search_button, SettingsPage.ALPHABET, R.string.kw_search),
    SettingEntry(R.string.search_button_color, SettingsPage.ALPHABET, R.string.kw_search),
    SettingEntry(R.string.search_button_icon, SettingsPage.ALPHABET, R.string.kw_search),
    SettingEntry(R.string.search_engine, SettingsPage.ALPHABET, R.string.kw_search),
    SettingEntry(R.string.section_gestures_custom, SettingsPage.GESTURES, R.string.kw_gestures),
    SettingEntry(R.string.section_gestures_fixed, SettingsPage.GESTURES, R.string.kw_gestures),
    SettingEntry(R.string.open_animation, SettingsPage.GESTURES, R.string.kw_gestures),
    SettingEntry(R.string.haptics, SettingsPage.GESTURES, R.string.kw_haptics),
    SettingEntry(R.string.section_plug_play, SettingsPage.APPS, R.string.kw_apps),
    SettingEntry(R.string.section_breaker, SettingsPage.WELLBEING, R.string.kw_wellbeing),
    SettingEntry(R.string.section_reminders, SettingsPage.WELLBEING, R.string.kw_wellbeing),
    SettingEntry(R.string.page_wellbeing, SettingsPage.WELLBEING, R.string.kw_wellbeing),
    SettingEntry(R.string.section_limits, SettingsPage.WELLBEING, R.string.kw_wellbeing),
    SettingEntry(R.string.private_space, SettingsPage.APPS, R.string.kw_apps),
    SettingEntry(R.string.section_hidden, SettingsPage.APPS, R.string.kw_apps),
    SettingEntry(R.string.protect_hidden, SettingsPage.APPS, R.string.kw_apps),
    SettingEntry(R.string.section_backup, SettingsPage.BACKUP, R.string.kw_backup),
    SettingEntry(R.string.auto_backup, SettingsPage.BACKUP, R.string.kw_backup),
)

/** Scelta in corso per un gesto: si apre in un dialogo con tutte le azioni in vista. */
private class GesturePick(@StringRes val titleRes: Int, val current: GestureAction, val onPick: (GestureAction) -> Unit)

/** Azione di un gesto: dove si fa, cosa fa ora, e l'avviso sull'accessibilità quando serve. */
@Composable
private fun GestureSetting(
    title: String,
    subtitle: String,
    action: GestureAction,
    appKey: String,
    allApps: List<AppInfo>,
    accessibilityEnabled: Boolean,
    onOpen: () -> Unit,
    onChooseApp: () -> Unit,
) {
    val colors = LocalOndaColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .padding(vertical = 10.dp),
    ) {
        Text(title, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge)
        Text(subtitle, color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall)
        Text(stringResource(action.labelRes), color = colors.accent, style = MaterialTheme.typography.bodyMedium)
    }
    if (action == GestureAction.OPEN_APP) {
        ClickRow(stringResource(R.string.gesture_choose_app), allApps.firstOrNull { it.key == appKey }?.label ?: stringResource(R.string.gesture_app_none), onChooseApp)
    }
    if (action == GestureAction.LOCK_SCREEN && !accessibilityEnabled) {
        Text(stringResource(R.string.double_tap_lock_off), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodySmall)
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
