// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.settings

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import com.gabrielepiccione.onda.data.DiscoveryTips
import com.gabrielepiccione.onda.data.GrayscaleMode
import com.gabrielepiccione.onda.data.GrayscaleSchedule
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.filled.GraphicEq
import com.gabrielepiccione.onda.data.PrefixKind
import com.gabrielepiccione.onda.data.SearchPrefixes
import com.gabrielepiccione.onda.ui.components.OndaChip
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.foundation.layout.imePadding
import com.gabrielepiccione.onda.data.MusicBackdropMode
import com.gabrielepiccione.onda.ui.components.MusicBackdrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import com.gabrielepiccione.onda.data.HomeLights
import com.gabrielepiccione.onda.data.HomePresence
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material.icons.filled.AutoAwesome
import com.gabrielepiccione.onda.data.ContextProfile
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.data.AppInfo
import android.content.pm.ShortcutInfo
import com.gabrielepiccione.onda.data.ActionArg
import com.gabrielepiccione.onda.data.ActionChoice
import com.gabrielepiccione.onda.data.ActionNeed
import com.gabrielepiccione.onda.data.Actions
import com.gabrielepiccione.onda.data.ClockSpacing
import com.gabrielepiccione.onda.data.GestureAction
import com.gabrielepiccione.onda.data.MusicWallpaper
import com.gabrielepiccione.onda.ui.components.ActionPickerDialog
import com.gabrielepiccione.onda.ui.components.TonalIcon
import com.gabrielepiccione.onda.ui.components.actionSummary
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
    val locationPermission: Boolean = false,
    val microphonePermission: Boolean = false,
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
    fun locationPermission()
    fun bluetoothPermission()
    fun microphonePermission()
    fun systemGrayscale()
    /** Nome del Wi-Fi collegato adesso, o null se Android non lo dice. */
    fun currentWifi(): String?
    /** Salva [ssid] come Wi-Fi di casa e, se manca, la posizione approssimativa. */
    fun saveHome(ssid: String)
    /** Rilegge la posizione approssimativa di casa. */
    fun locateHome()
    fun restartLauncher()
    fun pickLightsWidget()
    fun postNotifications()
    fun usageAccess()
    fun accessibility()
    /** Cancella la cronologia delle app aperte dalla ricerca. */
    fun clearSearchHistory()
    fun shortcutsOf(app: AppInfo): List<ShortcutInfo>
    fun connectedDevices(): Set<String>
    fun addWidget()
    fun removeWidget(id: Int)
    fun unhide(app: AppInfo)
    fun authenticate(onSuccess: () -> Unit)
    fun wallpaper()
    fun musicWallpaper()
    fun pickWallpaperBase()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    settings: Settings,
    hiddenApps: List<AppInfo>,
    allApps: List<AppInfo> = emptyList(),
    hasPrivateSpace: Boolean = false,
    status: SettingsStatus,
    widgets: WidgetManager,
    actions: SettingsActions,
    usageStats: UsageStatsRepository? = null,
    /** Nomi delle cuffie collegate adesso, per scegliere quelle di Plug & Play. */
    connectedHeadphones: Set<String> = emptySet(),
    activeProfile: ContextProfile? = null,
    contextEnvironment: ContextEnvironment? = null,
    /** Voce da raggiungere subito: la pagina si apre su di lei e la voce lampeggia. */
    focus: SettingEntry? = null,
    /** Profilo proposto, da modificare subito (da "Modifica" nella scheda di suggerimento). */
    suggestedProfile: ContextProfile? = null,
) {
    val colors = LocalOndaColors.current
    val context = LocalContext.current
    val pm = context.packageManager
    var iconPackDialog by remember { mutableStateOf(false) }
    var plugDialog by remember { mutableStateOf(false) }
    var hiddenUnlocked by remember { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf(focus?.page ?: if (suggestedProfile != null) SettingsPage.CONTEXTS else null) }
    var highlight by remember { mutableStateOf(focus) }
    // Conferma del Wi-Fi di casa: null = chiusa, vuoto = Android non dice il nome
    var wifiConfirm by remember { mutableStateOf<String?>(null) }
    var restartConfirm by remember { mutableStateOf(false) }
    var musicAppPick by remember { mutableStateOf<List<String>?>(null) }
    var deviceDialog by remember { mutableStateOf(false) }
    var beatExplain by remember { mutableStateOf(false) }
    var appPick by remember { mutableStateOf<AppPickTarget?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var widgetToRemove by remember { mutableStateOf<Int?>(null) }
    // Dove salvare l'app scelta per un gesto; null = scelta chiusa
    var gestureAppPick by remember { mutableStateOf<ArgPick?>(null) }
    var gesturePick by remember { mutableStateOf<GesturePick?>(null) }
    val onEdit = actions::edit
    BackHandler(enabled = page != null) { page = null }

    // La voce cercata lampeggia per qualche secondo, poi basta
    LaunchedEffect(highlight) {
        if (highlight != null) {
            kotlinx.coroutines.delay(HIGHLIGHT_MS)
            highlight = null
        }
    }
    val highlightText = highlight?.takeIf { it.page == page }?.let { stringResource(it.titleRes) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    // Ogni pagina parte con la barra grande e il contenuto subito sotto, mai nascosto dietro la barra
    LaunchedEffect(page) {
        scrollBehavior.state.heightOffset = 0f
        scrollBehavior.state.contentOffset = 0f
    }
    CompositionLocalProvider(LocalSettingsHighlight provides highlightText) {
    Column(Modifier.fillMaxSize().blockTouches().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        LargeTopAppBar(
            title = { Text(stringResource(page?.titleRes ?: R.string.settings)) },
            navigationIcon = {
                IconButton(onClick = { if (page != null) page = null else actions.close() }) {
                    @Suppress("DEPRECATION")
                    Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
            colors = TopAppBarDefaults.largeTopAppBarColors(
                containerColor = colors.scrim,
                scrolledContainerColor = colors.panel,
                titleContentColor = colors.onWallpaper,
                navigationIconContentColor = colors.onWallpaper,
            ),
            scrollBehavior = scrollBehavior,
        )

        // Anteprima dal vivo sopra lo sfondo vero: resta ferma mentre sotto si scorre e si modifica, ma solo se
        // lo schermo è abbastanza alto e il testo non è ingrandito; altrimenti scorre con la pagina e non copre nulla
        val config = LocalConfiguration.current
        val fixedPreview = config.screenHeightDp >= MIN_HEIGHT_FOR_PREVIEW && config.fontScale <= MAX_FONT_FOR_FIXED_PREVIEW
        val preview: @Composable () -> Unit = {
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
        if (fixedPreview) preview()

        key(page) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(colors.scrim)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 16.dp + com.gabrielepiccione.onda.data.ScreenLayout.sideInsetDp(LocalConfiguration.current.screenWidthDp).dp,
                    vertical = 8.dp,
                )
        ) {
        if (!fixedPreview) preview()
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
                        SettingsSupporting(stringResource(R.string.settings_search_none))
                    }
                    found.forEach { e ->
                        SettingsItem(stringResource(e.titleRes), stringResource(e.page.titleRes), onClick = { page = e.page; query = "" })
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
                        TonalIcon(p.icon)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(p.titleRes), color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge)
                            SettingsSupporting(stringResource(p.subRes))
                        }
                    }
                }
            }
            if (query.isBlank()) Group(stringResource(R.string.section_tips)) {
                SwitchRow(stringResource(R.string.tips_enable), stringResource(R.string.tips_enable_sub), settings.tips.enabled) { v ->
                    onEdit { it.copy(tips = it.tips.copy(enabled = v)) }
                }
                ClickRow(stringResource(R.string.tips_reset), stringResource(R.string.tips_reset_sub)) {
                    onEdit { it.copy(tips = DiscoveryTips.reset(it.tips)) }
                }
                ClickRow(stringResource(R.string.welcome_again), stringResource(R.string.welcome_again_sub)) {
                    onEdit { it.copy(welcomeDone = false) }
                    actions.close()
                }
            }
            if (query.isBlank()) Group(stringResource(R.string.section_app)) {
                ClickRow(stringResource(R.string.restart_launcher), stringResource(R.string.restart_launcher_sub)) { restartConfirm = true }
            }
            if (query.isBlank()) Text(
                stringResource(R.string.version_line, com.gabrielepiccione.onda.BuildConfig.VERSION_NAME),
                color = colors.onWallpaperMuted,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(vertical = 28.dp, horizontal = 8.dp),
            )
        }

        if (page == SettingsPage.STYLE) Group(stringResource(R.string.section_styles)) {
            SettingsSupporting(stringResource(R.string.styles_subtitle))
            FilledTonalButton(onClick = { actions.openStyles() }, modifier = Modifier.padding(top = 10.dp)) { Text(stringResource(R.string.styles_open)) }
        }

        if (page == SettingsPage.STYLE) Group(stringResource(R.string.section_music_backdrop)) {
            SettingsSupporting(stringResource(R.string.music_backdrop_sub))
            TextChoices(
                listOf(
                    MusicBackdropMode.OFF to stringResource(R.string.music_backdrop_off),
                    MusicBackdropMode.COLORS to stringResource(R.string.music_backdrop_colors),
                    MusicBackdropMode.MOTION to stringResource(R.string.music_backdrop_motion),
                    MusicBackdropMode.BEAT to stringResource(R.string.music_backdrop_beat),
                ),
                settings.musicBackdrop,
                columns = 2,
            ) { m ->
                // "A ritmo" chiede il microfono: prima si spiega perché, poi si chiede
                if (m == MusicBackdropMode.BEAT && !status.microphonePermission) beatExplain = true else onEdit { it.copy(musicBackdrop = m) }
            }
            if (settings.musicBackdrop != MusicBackdropMode.OFF) {
                // Anteprima dal vivo con i colori di esempio: com'è lo strato sotto la velatura
                Box(
                    Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(20.dp)),
                ) {
                    MusicBackdrop(
                        art = null,
                        playing = true,
                        mode = settings.musicBackdrop,
                        intensity = settings.musicBackdropIntensity,
                        dark = colors.dark,
                        modifier = Modifier.matchParentSize(),
                    )
                    Text(
                        stringResource(R.string.music_backdrop_preview),
                        color = colors.onWallpaper,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                ValueSlider(stringResource(R.string.music_backdrop_intensity), percentText(settings.musicBackdropIntensity), settings.musicBackdropIntensity, Settings().musicBackdropIntensity, 0.2f..1f) { v ->
                    onEdit { it.copy(musicBackdropIntensity = v) }
                }
                SwitchRow(stringResource(R.string.music_backdrop_home), null, settings.musicBackdropHome) { v -> onEdit { it.copy(musicBackdropHome = v) } }
                SwitchRow(stringResource(R.string.music_backdrop_list), null, settings.musicBackdropList) { v -> onEdit { it.copy(musicBackdropList = v) } }
                SwitchRow(stringResource(R.string.music_backdrop_search), null, settings.musicBackdropSearch) { v -> onEdit { it.copy(musicBackdropSearch = v) } }
                ClickRow(
                    stringResource(R.string.music_wallpaper_base),
                    stringResource(if (settings.musicWallpaperBase.isBlank()) R.string.music_wallpaper_base_none else R.string.music_wallpaper_base_set),
                ) { actions.pickWallpaperBase() }
                ClickRow(stringResource(R.string.music_wallpaper_set), stringResource(R.string.music_wallpaper_set_sub)) { actions.musicWallpaper() }
                SettingsSupporting(
                    stringResource(
                        when (MusicWallpaper.lockSupport(Build.VERSION.SDK_INT)) {
                            MusicWallpaper.LockSupport.CHOOSE_IN_PREVIEW -> R.string.music_wallpaper_lock_14
                            MusicWallpaper.LockSupport.WITH_HOME_UNLESS_SEPARATE_LOCK -> R.string.music_wallpaper_lock_old
                        },
                    ),
                )
            }
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
            // Il secondo colore del tema: evidenzia interruttori, lettere e selezioni
            Label(stringResource(R.string.theme_accent_color))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = settings.themeAccent == 0L,
                    onClick = { onEdit { it.copy(themeAccent = 0L) } },
                    label = { Text(stringResource(R.string.theme_accent_auto)) },
                )
            }
            ColorSwatches(AccentChoices + ACCENT_EXTRA, settings.themeAccent) { argb -> onEdit { it.copy(themeAccent = argb) } }
            SettingsSupporting(stringResource(R.string.theme_accent_sub))
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
                SettingsSupporting(stringResource(R.string.icon_shape_hint))
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
                SettingsSupporting(stringResource(R.string.no_home_widgets))
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

        if (page == SettingsPage.HOME) Group(stringResource(R.string.section_notifications)) {
            SwitchRow(stringResource(R.string.hide_shortcuts_with_notifications), stringResource(R.string.hide_shortcuts_with_notifications_sub), settings.hideShortcutsWithNotifications) { v ->
                onEdit { it.copy(hideShortcutsWithNotifications = v) }
            }
            Label(stringResource(R.string.notif_images))
            TextChoices(
                com.gabrielepiccione.onda.data.NotificationImages.entries.map { it to stringResource(it.labelRes) },
                settings.notificationImages,
            ) { v -> onEdit { it.copy(notificationImages = v) } }
            SettingsSupporting(stringResource(R.string.notif_images_sub))
        }

        if (page == SettingsPage.HOME) Group(stringResource(R.string.section_favorites)) {
            SwitchRow(stringResource(R.string.hide_fav_names), stringResource(R.string.hide_fav_names_sub), settings.hideFavoriteNames) { v -> onEdit { it.copy(hideFavoriteNames = v) } }
            SwitchRow(stringResource(R.string.suggested_setting), stringResource(R.string.suggested_setting_sub), settings.suggestInHome) { v -> onEdit { it.copy(suggestInHome = v) } }
            allApps.filter { it.key in settings.suggestionsDismissed }.forEach { app ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(app.label, color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onEdit { HomeSuggestions.restore(it, app.key) } }) { Text(stringResource(R.string.suggested_restore)) }
                }
            }
        }

        if (page == SettingsPage.HOME) Group(stringResource(R.string.section_lights)) {
            SettingsSupporting(stringResource(R.string.lights_sub))
            SwitchRow(stringResource(R.string.lights_enable), null, settings.homeLights) { v ->
                onEdit { it.copy(homeLights = v) }
                if (v && !status.locationPermission) actions.locationPermission()
            }
            if (settings.homeLights) {
                if (!status.locationPermission) {
                    StatusRow(stringResource(R.string.lights_permission), false, "", stringResource(R.string.lights_permission_ko)) { actions.locationPermission() }
                }
                ClickRow(
                    stringResource(R.string.lights_wifi),
                    if (settings.homeWifi.isBlank()) stringResource(R.string.lights_wifi_none) else stringResource(R.string.lights_wifi_saved, settings.homeWifi),
                ) { wifiConfirm = actions.currentWifi().orEmpty() }
                ClickRow(
                    stringResource(R.string.lights_widget),
                    stringResource(if (settings.homeLightsWidget > 0) R.string.lights_widget_set else R.string.lights_widget_none),
                ) { actions.pickLightsWidget() }
                LightsStatus(settings, status.locationPermission, onLocate = { actions.locateHome() })
            }
        }

        if (page == SettingsPage.HOME) Group(stringResource(R.string.home_block_spacing)) {
            SettingsSupporting(stringResource(R.string.home_block_spacing_sub))
            ValueSlider(
                stringResource(R.string.home_block_spacing),
                stringResource(
                    when {
                        settings.homeBlockSpacing <= 4 -> R.string.spacing_compact
                        settings.homeBlockSpacing >= 24 -> R.string.spacing_wide
                        else -> R.string.spacing_normal
                    },
                ) + " · " + stringResource(R.string.value_dp, settings.homeBlockSpacing),
                settings.homeBlockSpacing.toFloat(),
                Settings().homeBlockSpacing.toFloat(),
                0f..32f,
                steps = 7,
            ) { v -> onEdit { it.copy(homeBlockSpacing = (v / 4f).roundToInt() * 4) } }
            SwitchRow(stringResource(R.string.cards_carousel), stringResource(R.string.cards_carousel_sub), settings.cardsCarousel) { v -> onEdit { it.copy(cardsCarousel = v) } }
            ValueSlider(
                stringResource(R.string.clock_inner_spacing),
                stringResource(R.string.value_dp, settings.clockInnerSpacing),
                settings.clockInnerSpacing.toFloat(),
                ClockSpacing.DEFAULT.toFloat(),
                ClockSpacing.MIN.toFloat()..ClockSpacing.MAX.toFloat(),
                steps = ClockSpacing.MAX / ClockSpacing.STEP - 1,
            ) { v -> onEdit { it.copy(clockInnerSpacing = ClockSpacing.snap(v)) } }
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
            ValueSlider(
                stringResource(R.string.alphabet_hot_area),
                stringResource(R.string.value_dp, settings.alphabetHotAreaDp),
                settings.alphabetHotAreaDp.toFloat(),
                Settings().alphabetHotAreaDp.toFloat(),
                28f..96f,
            ) { v -> onEdit { it.copy(alphabetHotAreaDp = v.roundToInt()) } }
            SwitchRow(stringResource(R.string.contacts_in_alphabet), stringResource(R.string.contacts_in_alphabet_sub), settings.contactsInAlphabet) { v ->
                onEdit { it.copy(contactsInAlphabet = v) }
                if (v && !status.contactsPermission) actions.contactsPermission()
            }
        }

        if (page == SettingsPage.ALPHABET) Group(stringResource(R.string.section_prefixes)) {
            SettingsSupporting(stringResource(R.string.prefixes_sub))
            PrefixKind.entries.forEach { kind ->
                var text by remember(kind, settings.searchPrefixes) { mutableStateOf(settings.searchPrefixes.of(kind)) }
                val candidate = settings.searchPrefixes.with(kind, text.trim())
                val ok = SearchPrefixes.valid(candidate)
                OutlinedTextField(
                    value = text,
                    onValueChange = { v ->
                        text = v
                        val next = settings.searchPrefixes.with(kind, v.trim())
                        if (SearchPrefixes.valid(next)) onEdit { it.copy(searchPrefixes = next) }
                    },
                    singleLine = true,
                    isError = !ok,
                    label = { Text(stringResource(prefixLabel(kind))) },
                    supportingText = { Text(stringResource(if (ok) prefixExample(kind) else R.string.prefix_invalid)) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
            }
            val musicApps = mediaApps()
            ClickRow(
                stringResource(R.string.music_app),
                settings.musicApp.takeIf { it.isNotBlank() }?.let { pkg -> allApps.firstOrNull { it.packageName == pkg }?.label ?: pkg }
                    ?: stringResource(R.string.music_app_last),
            ) { musicAppPick = musicApps }
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
                            if (Actions.need(g) == ActionNeed.ACCESSIBILITY && !status.accessibilityEnabled) actions.accessibility()
                        }
                    },
                    onChooseApp = { kind -> gestureAppPick = ArgPick(kind) { key -> onEdit { it.copy(searchButtonTapApp = key) } } },
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
                            if (Actions.need(g) == ActionNeed.ACCESSIBILITY && !status.accessibilityEnabled) actions.accessibility()
                        }
                    },
                    onChooseApp = { kind -> gestureAppPick = ArgPick(kind) { key -> onEdit { it.copy(searchButtonLongApp = key) } } },
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
                            if (Actions.need(g) == ActionNeed.ACCESSIBILITY && !status.accessibilityEnabled) actions.accessibility()
                        }
                    },
                    onChooseApp = { kind -> gestureAppPick = ArgPick(kind) { key -> onEdit { it.copy(searchButtonSwipeApp = key) } } },
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
            Text(stringResource(R.string.search_prefix_hint), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        }
        if (page == SettingsPage.ALPHABET) SearchHistorySettings(settings, onEdit, actions::clearSearchHistory)

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
                        if (Actions.need(g) == ActionNeed.ACCESSIBILITY && !status.accessibilityEnabled) actions.accessibility()
                    }
                },
                onChooseApp = { kind -> gestureAppPick = ArgPick(kind) { key -> onEdit { it.copy(swipeUpApp = key) } } },
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
                        if (Actions.need(g) == ActionNeed.ACCESSIBILITY && !status.accessibilityEnabled) actions.accessibility()
                    }
                },
                onChooseApp = { kind -> gestureAppPick = ArgPick(kind) { key -> onEdit { it.copy(doubleTapApp = key) } } },
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
            SettingsSupporting(stringResource(R.string.plug_play_sub))
            val current = allApps.firstOrNull { it.key == settings.plugPlayApp }
            ClickRow(stringResource(R.string.plug_play_choose), current?.label ?: stringResource(R.string.plug_play_none)) { plugDialog = true }
            ClickRow(
                stringResource(R.string.plug_play_device),
                settings.plugPlayDevice.ifBlank { stringResource(R.string.plug_play_device_any) },
            ) { deviceDialog = true }
            SwitchRow(stringResource(R.string.plug_play_resume), stringResource(R.string.plug_play_resume_sub), settings.plugPlayResume) { v ->
                onEdit { it.copy(plugPlayResume = v) }
            }
        }

        if (page == SettingsPage.CONTEXTS && contextEnvironment != null) {
            ContextProfilesPage(settings, allApps, activeProfile, contextEnvironment, onEdit, suggestedProfile, status.usageAccess, actions::usageAccess)
        }

        if (page == SettingsPage.AUTOMATIONS) {
            AutomationsPage(settings, allApps, actions::connectedDevices, actions::shortcutsOf, status.accessibilityEnabled, actions::accessibility, onEdit)
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

        if (page == SettingsPage.WELLBEING) {
            AppBlocksSection(settings, allApps, status.calendarPermission, actions::calendarPermission, onEdit)
        }

        if (page == SettingsPage.WELLBEING) Group(stringResource(R.string.section_limits)) {
            SettingsSupporting(stringResource(R.string.limits_sub))
            if (settings.appLimits.isNotEmpty() && !status.postNotifications) {
                StatusRow(stringResource(R.string.post_notifications), false, stringResource(R.string.post_notifications_ok), stringResource(R.string.post_notifications_ko)) { actions.postNotifications() }
            }
            if (settings.appLimits.isEmpty()) {
                SettingsSupporting(stringResource(R.string.breaker_none))
            }
            settings.appLimits.forEach { (pkg, minutes) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(allApps.firstOrNull { it.packageName == pkg }?.label ?: pkg, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge)
                        Text(durationText(minutes * 60_000L), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = { onEdit { it.copy(appLimits = it.appLimits - pkg) } }) { Text(stringResource(R.string.remove)) }
                }
            }
        }

        if (page == SettingsPage.WELLBEING) Group(stringResource(R.string.section_grayscale)) {
            SettingsSupporting(stringResource(R.string.grayscale_sub))
            TextChoices(
                listOf(
                    GrayscaleMode.OFF to stringResource(R.string.grayscale_off),
                    GrayscaleMode.SCHEDULE to stringResource(R.string.grayscale_schedule),
                    GrayscaleMode.PROFILE to stringResource(R.string.grayscale_profile),
                ),
                settings.grayscaleMode,
            ) { m -> onEdit { it.copy(grayscaleMode = m) } }
            if (settings.grayscaleMode == GrayscaleMode.SCHEDULE) {
                ValueSlider(
                    stringResource(R.string.grayscale_from),
                    minuteLabel(settings.grayscaleStart),
                    settings.grayscaleStart.toFloat(),
                    GrayscaleSchedule.DEFAULT_START.toFloat(),
                    0f..1425f,
                ) { v -> onEdit { it.copy(grayscaleStart = ((v / 15f).roundToInt() * 15).coerceIn(0, 1425)) } }
                ValueSlider(
                    stringResource(R.string.grayscale_to),
                    minuteLabel(settings.grayscaleEnd),
                    settings.grayscaleEnd.toFloat(),
                    GrayscaleSchedule.DEFAULT_END.toFloat(),
                    0f..1425f,
                ) { v -> onEdit { it.copy(grayscaleEnd = ((v / 15f).roundToInt() * 15).coerceIn(0, 1425)) } }
            }
            if (settings.grayscaleMode == GrayscaleMode.PROFILE) {
                if (settings.contextProfiles.isEmpty()) {
                    SettingsSupporting(stringResource(R.string.grayscale_no_profiles))
                } else {
                    Column(Modifier.selectableGroup()) {
                        settings.contextProfiles.forEach { p ->
                            RadioItem(p.name, settings.grayscaleProfile == p.id) { onEdit { it.copy(grayscaleProfile = p.id) } }
                        }
                    }
                }
            }
            SettingsItem(
                headline = stringResource(R.string.grayscale_system),
                supporting = stringResource(R.string.grayscale_system_sub),
                onClick = { actions.systemGrayscale() },
            )
        }

        if (page == SettingsPage.WELLBEING) Group(stringResource(R.string.section_breaker)) {
            SettingsSupporting(stringResource(R.string.breaker_sub))
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
                SettingsSupporting(stringResource(R.string.breaker_none))
            }
            OndaChip(stringResource(R.string.choose_apps), { appPick = AppPickTarget.BREAKER }, icon = Icons.Filled.Add)
            breakerApps.forEach { app ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(app.label, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onEdit { com.gabrielepiccione.onda.data.UsageBreaker.toggle(it, app.key) } }) { Text(stringResource(R.string.remove)) }
                }
            }
        }

        if (page == SettingsPage.WELLBEING) Group(stringResource(R.string.section_reminders)) {
            SettingsSupporting(stringResource(R.string.reminder_sub))
            Label(stringResource(R.string.reminder_start))
            TextChoices(
                UsageReminders.START_CHOICES.map { m -> m to stringResource(R.string.reminder_minutes, m) },
                settings.reminderStartMinutes,
            ) { m -> onEdit { it.copy(reminderStartMinutes = m) } }
            val reminderApps = allApps.filter { it.key in settings.reminderApps }
            if (reminderApps.isEmpty()) {
                SettingsSupporting(stringResource(R.string.breaker_none))
            }
            OndaChip(stringResource(R.string.choose_apps), { appPick = AppPickTarget.REMINDERS }, icon = Icons.Filled.Add)
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
                SettingsSupporting(stringResource(R.string.no_hidden_apps))
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
            SettingsSupporting(stringResource(R.string.backup_desc))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                FilledTonalButton(onClick = { actions.exportBackup() }) { Text(stringResource(R.string.export)) }
                OutlinedButton(onClick = { actions.importBackup() }) { Text(stringResource(R.string.restore)) }
            }
            TextButton(onClick = { actions.shareBackup() }) { Text(stringResource(R.string.backup_share)) }
            Label(stringResource(R.string.auto_backup))
            SettingsSupporting(stringResource(R.string.auto_backup_sub))
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
                    androidx.compose.foundation.lazy.LazyColumn(Modifier.androidxHeightIn()) {
                        item {
                            ChoiceRow(stringResource(GestureAction.NONE.labelRes), pick.current == GestureAction.NONE) {
                                pick.onPick(GestureAction.NONE)
                                gesturePick = null
                            }
                        }
                        // Stesso catalogo di ogni altro ambito (vedi Actions)
                        Actions.catalog.forEach { (group, all) ->
                            // Le azioni delle sole automazioni (blocchi, impostazioni di Onda) non sono gesti
                            val list = all.filterNot { Actions.isAutomationOnly(it) }
                            item(key = "g_" + group.name) { Label(stringResource(group.labelRes)) }
                            items(list.size, key = { list[it].name }) { i ->
                                val g = list[i]
                                ChoiceRow(stringResource(g.labelRes), pick.current == g) {
                                    pick.onPick(g)
                                    gesturePick = null
                                }
                            }
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

    gestureAppPick?.let { pick ->
        ActionPickerDialog(
            title = stringResource(if (pick.kind == ActionArg.SHORTCUT) R.string.gesture_open_shortcut else R.string.gesture_choose_app),
            current = ActionChoice(GestureAction.NONE),
            allApps = allApps,
            shortcutsOf = actions::shortcutsOf,
            themedIcons = settings.themedIcons,
            accessibilityEnabled = status.accessibilityEnabled,
            onAccessibility = {},
            onPick = { pick.save(it.arg) },
            onDismiss = { gestureAppPick = null },
            startAt = pick.kind,
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
    }

    wifiConfirm?.let { ssid ->
        AlertDialog(
            onDismissRequest = { wifiConfirm = null },
            title = { Text(stringResource(R.string.lights_wifi)) },
            text = {
                Text(
                    if (ssid.isBlank()) stringResource(R.string.lights_wifi_unknown) else stringResource(R.string.lights_wifi_confirm, ssid),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = {
                if (ssid.isNotBlank()) {
                    TextButton(onClick = { actions.saveHome(ssid); wifiConfirm = null }) { Text(stringResource(R.string.lights_wifi_use)) }
                } else if (!status.locationPermission) {
                    TextButton(onClick = { actions.locationPermission(); wifiConfirm = null }) { Text(stringResource(R.string.lights_permission)) }
                }
            },
            dismissButton = { TextButton(onClick = { wifiConfirm = null }) { Text(stringResource(R.string.breaker_cancel)) } },
        )
    }

    if (beatExplain) {
        AlertDialog(
            onDismissRequest = { beatExplain = false },
            icon = { Icon(Icons.Filled.GraphicEq, null) },
            title = { Text(stringResource(R.string.music_beat_title)) },
            text = { Text(stringResource(R.string.music_beat_body)) },
            confirmButton = {
                TextButton(onClick = {
                    beatExplain = false
                    onEdit { it.copy(musicBackdrop = MusicBackdropMode.BEAT) }
                    actions.microphonePermission()
                }) { Text(stringResource(R.string.music_beat_allow)) }
            },
            dismissButton = { TextButton(onClick = { beatExplain = false }) { Text(stringResource(R.string.breaker_cancel)) } },
        )
    }

    if (deviceDialog) {
        val connected = connectedHeadphones
        var typed by remember { mutableStateOf(settings.plugPlayDevice) }
        AlertDialog(
            onDismissRequest = { deviceDialog = false },
            title = { Text(stringResource(R.string.plug_play_device)) },
            text = {
                Column {
                    ChoiceRow(stringResource(R.string.plug_play_device_any), settings.plugPlayDevice.isBlank()) {
                        onEdit { it.copy(plugPlayDevice = "") }
                        deviceDialog = false
                    }
                    connected.forEach { name ->
                        ChoiceRow(name, settings.plugPlayDevice.equals(name, ignoreCase = true)) {
                            onEdit { it.copy(plugPlayDevice = name) }
                            deviceDialog = false
                        }
                    }
                    SettingsSupporting(stringResource(R.string.plug_play_device_hint))
                    OutlinedTextField(typed, { typed = it }, singleLine = true, label = { Text(stringResource(R.string.ctx_bt_name)) }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = { onEdit { it.copy(plugPlayDevice = typed.trim()) }; deviceDialog = false }) { Text(stringResource(R.string.wellbeing_limit_save)) }
            },
            dismissButton = { TextButton(onClick = { deviceDialog = false }) { Text(stringResource(R.string.breaker_cancel)) } },
        )
    }

    musicAppPick?.let { pkgs ->
        AlertDialog(
            onDismissRequest = { musicAppPick = null },
            title = { Text(stringResource(R.string.music_app)) },
            text = {
                Column {
                    ChoiceRow(stringResource(R.string.music_app_last), settings.musicApp.isBlank()) { onEdit { it.copy(musicApp = "") }; musicAppPick = null }
                    pkgs.forEach { pkg ->
                        val label = allApps.firstOrNull { it.packageName == pkg }?.label ?: pkg
                        ChoiceRow(label, settings.musicApp == pkg) { onEdit { it.copy(musicApp = pkg) }; musicAppPick = null }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { musicAppPick = null }) { Text(stringResource(R.string.close)) } },
        )
    }

    appPick?.let { target ->
        val chosen = if (target == AppPickTarget.BREAKER) settings.breakerApps else settings.reminderApps
        AppPicker(
            allApps.filterNot { it.key in chosen },
            onPick = { app ->
                onEdit {
                    if (target == AppPickTarget.BREAKER) com.gabrielepiccione.onda.data.UsageBreaker.toggle(it, app.key) else UsageReminders.toggle(it, app.key)
                }
                appPick = null
            },
            onDismiss = { appPick = null },
        )
    }

    if (restartConfirm) {
        AlertDialog(
            onDismissRequest = { restartConfirm = false },
            title = { Text(stringResource(R.string.restart_launcher)) },
            text = { Text(stringResource(R.string.restart_launcher_body)) },
            confirmButton = { TextButton(onClick = { actions.restartLauncher() }) { Text(stringResource(R.string.restart_launcher_go)) } },
            dismissButton = { TextButton(onClick = { restartConfirm = false }) { Text(stringResource(R.string.breaker_cancel)) } },
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
internal enum class SettingsPage(@StringRes val titleRes: Int, @StringRes val subRes: Int, val icon: ImageVector) {
    STYLE(R.string.section_style, R.string.page_style_sub, Icons.Filled.Palette),
    CLOCK(R.string.edit_clock, R.string.page_clock_sub, Icons.Filled.Schedule),
    TEXT(R.string.section_text, R.string.page_text_sub, Icons.Filled.TextFields),
    ICONS(R.string.section_icons, R.string.page_icons_sub, Icons.Filled.Category),
    HOME(R.string.section_home, R.string.page_home_sub, Icons.Filled.Home),
    ALPHABET(R.string.page_alphabet, R.string.page_alphabet_sub, Icons.Filled.Search),
    GESTURES(R.string.section_gestures, R.string.page_gestures_sub, Icons.Filled.TouchApp),
    CONTEXTS(R.string.page_contexts, R.string.page_contexts_sub, Icons.Filled.AutoAwesome),
    AUTOMATIONS(R.string.page_automations, R.string.page_automations_sub, Icons.Filled.Bolt),
    WELLBEING(R.string.page_wellbeing, R.string.page_wellbeing_sub, Icons.Filled.HourglassEmpty),
    APPS(R.string.page_apps, R.string.page_apps_sub, Icons.Filled.Apps),
    BACKUP(R.string.section_backup, R.string.page_backup_sub, Icons.Filled.Backup),
}

private const val HIGHLIGHT_MS = 4_000L
private const val MIN_HEIGHT_FOR_PREVIEW = 720
private const val MAX_FONT_FOR_FIXED_PREVIEW = 1.15f

/** Una voce cercabile: il titolo, la pagina che la contiene e parole in più per trovarla. */
internal class SettingEntry(@StringRes val titleRes: Int, val page: SettingsPage, @StringRes val keywordsRes: Int)

internal val SETTING_ENTRIES = listOf(
    SettingEntry(R.string.section_style, SettingsPage.STYLE, R.string.kw_style),
    SettingEntry(R.string.section_music_backdrop, SettingsPage.STYLE, R.string.kw_music),
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
    SettingEntry(R.string.hide_shortcuts_with_notifications, SettingsPage.HOME, R.string.kw_notif),
    SettingEntry(R.string.suggested_setting, SettingsPage.HOME, R.string.kw_home),
    SettingEntry(R.string.layout_lock, SettingsPage.HOME, R.string.kw_home),
    SettingEntry(R.string.hide_status_bar, SettingsPage.HOME, R.string.kw_home),
    SettingEntry(R.string.home_block_spacing, SettingsPage.HOME, R.string.kw_home),
    SettingEntry(R.string.section_lights, SettingsPage.HOME, R.string.kw_lights),
    SettingEntry(R.string.alphabet_left, SettingsPage.ALPHABET, R.string.kw_alphabet),
    SettingEntry(R.string.hide_alphabet, SettingsPage.ALPHABET, R.string.kw_alphabet),
    SettingEntry(R.string.contacts_in_alphabet, SettingsPage.ALPHABET, R.string.kw_alphabet),
    SettingEntry(R.string.search_button, SettingsPage.ALPHABET, R.string.kw_search),
    SettingEntry(R.string.search_button_color, SettingsPage.ALPHABET, R.string.kw_search),
    SettingEntry(R.string.search_button_icon, SettingsPage.ALPHABET, R.string.kw_search),
    SettingEntry(R.string.search_engine, SettingsPage.ALPHABET, R.string.kw_search),
    SettingEntry(R.string.section_prefixes, SettingsPage.ALPHABET, R.string.kw_search),
    SettingEntry(R.string.music_app, SettingsPage.ALPHABET, R.string.kw_music),
    SettingEntry(R.string.section_gestures_custom, SettingsPage.GESTURES, R.string.kw_gestures),
    SettingEntry(R.string.section_gestures_fixed, SettingsPage.GESTURES, R.string.kw_gestures),
    SettingEntry(R.string.open_animation, SettingsPage.GESTURES, R.string.kw_gestures),
    SettingEntry(R.string.haptics, SettingsPage.GESTURES, R.string.kw_haptics),
    SettingEntry(R.string.section_plug_play, SettingsPage.APPS, R.string.kw_apps),
    SettingEntry(R.string.section_breaker, SettingsPage.WELLBEING, R.string.kw_wellbeing),
    SettingEntry(R.string.section_reminders, SettingsPage.WELLBEING, R.string.kw_wellbeing),
    SettingEntry(R.string.page_wellbeing, SettingsPage.WELLBEING, R.string.kw_wellbeing),
    SettingEntry(R.string.page_contexts, SettingsPage.CONTEXTS, R.string.kw_contexts),
    SettingEntry(R.string.page_automations, SettingsPage.AUTOMATIONS, R.string.kw_automations),
    SettingEntry(R.string.blocks_title, SettingsPage.WELLBEING, R.string.kw_blocks),
    SettingEntry(R.string.suggest_enable, SettingsPage.CONTEXTS, R.string.kw_contexts),
    SettingEntry(R.string.ctx_fixed_title, SettingsPage.CONTEXTS, R.string.kw_contexts),
    SettingEntry(R.string.section_limits, SettingsPage.WELLBEING, R.string.kw_wellbeing),
    SettingEntry(R.string.section_grayscale, SettingsPage.WELLBEING, R.string.kw_wellbeing),
    SettingEntry(R.string.private_space, SettingsPage.APPS, R.string.kw_apps),
    SettingEntry(R.string.section_hidden, SettingsPage.APPS, R.string.kw_apps),
    SettingEntry(R.string.protect_hidden, SettingsPage.APPS, R.string.kw_apps),
    SettingEntry(R.string.section_backup, SettingsPage.BACKUP, R.string.kw_backup),
    SettingEntry(R.string.auto_backup, SettingsPage.BACKUP, R.string.kw_backup),
)

/** Scelta in corso per un gesto: si apre in un dialogo con tutte le azioni in vista. */
/** Scelta in corso dell'app o della scorciatoia di un gesto; [save] riceve l'argomento scelto. */
private class ArgPick(val kind: ActionArg, val save: (String) -> Unit)

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
    onChooseApp: (ActionArg) -> Unit,
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
        Text(subtitle, color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(action.labelRes), color = colors.accent, style = MaterialTheme.typography.bodyMedium)
    }
    if (action == GestureAction.OPEN_APP) {
        ClickRow(stringResource(R.string.gesture_choose_app), allApps.firstOrNull { it.key == appKey }?.label ?: stringResource(R.string.gesture_app_none)) { onChooseApp(ActionArg.APP) }
    }
    if (action == GestureAction.OPEN_SHORTCUT) {
        ClickRow(stringResource(R.string.gesture_open_shortcut), actionSummary(ActionChoice(action, appKey), allApps, stringResource(R.string.gesture_app_none))) { onChooseApp(ActionArg.SHORTCUT) }
    }
    if (Actions.need(action) == ActionNeed.ACCESSIBILITY && !accessibilityEnabled) {
        SettingsSupporting(stringResource(R.string.double_tap_lock_off))
    }
}

/** Colori in più per l'accento, oltre a quelli del primario. */
private val ACCENT_EXTRA = listOf(0xFFE57373, 0xFFF06292, 0xFF4DB6AC, 0xFF9575CD)

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
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (path.isNotBlank()) TextButton(onClick = onClear) { Text(stringResource(R.string.remove)) }
        TextButton(onClick = onImport) { Text(stringResource(R.string.font_import)) }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) = SettingsSection(title) { content() }

@Composable
private fun Label(text: String) = SettingsLabel(text)

@Composable
private fun SwitchRow(text: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit) = SettingsSwitch(text, sub, checked, onChange = onChange)

@Composable
private fun ClickRow(text: String, value: String, onClick: () -> Unit) = SettingsItem(text, value, onClick = onClick)

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) = RadioItem(label, selected, onClick)

@Composable
private fun StatusRow(title: String, ok: Boolean, okText: String, koText: String, onClick: () -> Unit) {
    val colors = LocalOndaColors.current
    SettingsItem(
        headline = title,
        supporting = (if (ok) okText else koText).ifBlank { null },
        onClick = onClick,
        trailing = {
            Icon(
                if (ok) Icons.Filled.CheckCircle else Icons.Filled.Error,
                contentDescription = null,
                tint = if (ok) colors.accent else MaterialTheme.colorScheme.error,
            )
        },
    )
}

/**
 * Stato delle luci di casa in diretta: cosa vede il telefono adesso e, se il widget non c'è, il primo
 * motivo da sistemare. Si aggiorna ogni 5 secondi mentre la pagina è aperta.
 */
@Composable
private fun LightsStatus(settings: Settings, permission: Boolean, onLocate: () -> Unit) {
    val colors = LocalOndaColors.current
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5_000)
            tick++
        }
    }
    val ssid = remember(tick, permission) { HomePresence.currentSsid(context) }
    val locationOn = remember(tick) { HomePresence.locationEnabled(context) }
    val hasPlace = settings.homeLatitude != 0.0 || settings.homeLongitude != 0.0
    val sun = remember(settings.homeLatitude, settings.homeLongitude) {
        if (hasPlace) HomeLights.sunTimes(java.time.LocalDate.now(), settings.homeLatitude, settings.homeLongitude) else null
    }
    val minute = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
    val dark = HomeLights.isDark(System.currentTimeMillis(), sun, minute)
    val missing = HomeLights.missing(settings.homeLights, permission, locationOn, settings.homeLightsWidget, ssid, settings.homeWifi, dark)
    val time = java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT)
    fun clock(ms: Long) = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalTime().format(time)

    Column(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.panelHigh)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.lights_status_title), color = colors.accent, style = MaterialTheme.typography.labelLarge)
        StatusLine(stringResource(R.string.lights_status_wifi_now), ssid ?: stringResource(R.string.lights_status_unknown), ssid != null)
        StatusLine(stringResource(R.string.lights_status_home), stringResource(if (HomeLights.isHome(ssid, settings.homeWifi)) R.string.yes else R.string.no), HomeLights.isHome(ssid, settings.homeWifi))
        StatusLine(
            stringResource(R.string.lights_status_dark),
            if (sun != null) stringResource(R.string.lights_status_sun, clock(sun.sunset - HomeLights.DUSK_MARGIN_MS), clock(sun.sunrise))
            else stringResource(R.string.lights_status_fallback),
            dark,
        )
        StatusLine(stringResource(R.string.lights_widget), stringResource(if (settings.homeLightsWidget > 0) R.string.lights_widget_set else R.string.lights_widget_none), settings.homeLightsWidget > 0)
        Text(
            when (missing) {
                null -> stringResource(R.string.lights_ok)
                HomeLights.Missing.DISABLED -> stringResource(R.string.lights_why_disabled)
                HomeLights.Missing.PERMISSION -> stringResource(R.string.lights_why_permission)
                HomeLights.Missing.LOCATION_OFF -> stringResource(R.string.lights_why_location_off)
                HomeLights.Missing.NO_HOME_WIFI -> stringResource(R.string.lights_why_no_home)
                HomeLights.Missing.NOT_HOME -> stringResource(R.string.lights_why_not_home)
                HomeLights.Missing.NO_WIDGET -> stringResource(R.string.lights_why_no_widget)
                HomeLights.Missing.NOT_DARK -> stringResource(R.string.lights_why_not_dark)
            },
            color = colors.onWallpaper,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (!hasPlace && permission) {
            TextButton(onClick = onLocate) { Text(stringResource(R.string.lights_locate)) }
        }
    }
}

@Composable
private fun StatusLine(label: String, value: String, ok: Boolean) {
    val colors = LocalOndaColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(if (ok) Icons.Filled.Check else Icons.Filled.Close, null, tint = if (ok) colors.accent else colors.onWallpaperMuted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, color = colors.onWallpaper, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun prefixLabel(kind: PrefixKind): Int = when (kind) {
    PrefixKind.CALC -> R.string.prefix_calc
    PrefixKind.CONTACTS -> R.string.prefix_contacts
    PrefixKind.SETTINGS -> R.string.prefix_settings
    PrefixKind.PLAY -> R.string.prefix_play
    PrefixKind.SPOTIFY -> R.string.prefix_spotify
}

private fun prefixExample(kind: PrefixKind): Int = when (kind) {
    PrefixKind.CALC -> R.string.prefix_calc_example
    PrefixKind.CONTACTS -> R.string.prefix_contacts_example
    PrefixKind.SETTINGS -> R.string.prefix_settings_example
    PrefixKind.PLAY -> R.string.prefix_play_example
    PrefixKind.SPOTIFY -> R.string.prefix_spotify_example
}

/** App che sanno far partire un brano da una ricerca (letto qui per non passare il repository alla pagina). */
@Composable
private fun mediaApps(): List<String> {
    val context = LocalContext.current
    return remember {
        runCatching {
            context.packageManager.queryIntentActivities(android.content.Intent(android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH), 0)
                .map { it.activityInfo.packageName }
                .distinct()
        }.getOrDefault(emptyList())
    }
}
/** A quale elenco di Benessere digitale si aggiunge l'app scelta. */
private enum class AppPickTarget { BREAKER, REMINDERS }

/** Minuti dalla mezzanotte come "22:30". */
private fun minuteLabel(minute: Int): String = "%d:%02d".format(minute / 60 % 24, minute % 60)
