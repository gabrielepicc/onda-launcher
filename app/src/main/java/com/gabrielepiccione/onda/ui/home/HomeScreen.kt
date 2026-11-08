// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(ExperimentalMaterial3Api::class)

package com.gabrielepiccione.onda.ui.home

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.gabrielepiccione.onda.data.SearchRanking
import com.gabrielepiccione.onda.data.ContextProfile
import com.gabrielepiccione.onda.data.ProfileSuggestion
import com.gabrielepiccione.onda.data.UsageProfiles
import com.gabrielepiccione.onda.service.UsageReader
import com.gabrielepiccione.onda.data.DiscoveryTips
import com.gabrielepiccione.onda.data.GrayscaleMode
import com.gabrielepiccione.onda.data.TipSignals
import com.gabrielepiccione.onda.ui.settings.SettingEntry
import com.gabrielepiccione.onda.data.WidgetSize
import com.gabrielepiccione.onda.data.WidgetSizing
import com.gabrielepiccione.onda.ui.components.actionIcon
import com.gabrielepiccione.onda.ui.components.actionSummary
import com.gabrielepiccione.onda.ui.components.WidgetSizeControls
import com.gabrielepiccione.onda.data.PlugPlay
import com.gabrielepiccione.onda.data.MEDIA_PREFIX
import com.gabrielepiccione.onda.data.MediaShortcuts
import android.content.ComponentName
import androidx.compose.material.icons.filled.Palette
import androidx.compose.foundation.indication
import androidx.compose.material3.ripple
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItem
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.material.icons.filled.Edit
import com.gabrielepiccione.onda.data.SpotifyLinks
import com.gabrielepiccione.onda.ui.components.OndaChip
import com.gabrielepiccione.onda.ui.components.MusicBackdrop
import androidx.compose.material3.Switch
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import com.gabrielepiccione.onda.ui.settings.ContextEnvironment
import com.gabrielepiccione.onda.data.HomeLights
import com.gabrielepiccione.onda.data.HomePresence
import com.gabrielepiccione.onda.data.HAlign
import com.gabrielepiccione.onda.ui.components.HostedWidget
import com.gabrielepiccione.onda.data.UsageStatsMath
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import com.gabrielepiccione.onda.data.FolderIcons
import com.gabrielepiccione.onda.util.HapticEvent
import com.gabrielepiccione.onda.util.LocalOndaHaptics
import com.gabrielepiccione.onda.ui.theme.*
import com.gabrielepiccione.onda.ui.components.*
import com.gabrielepiccione.onda.ui.drawer.*
import com.gabrielepiccione.onda.ui.search.*
import com.gabrielepiccione.onda.ui.settings.*
import com.gabrielepiccione.onda.ui.sheets.*

import androidx.compose.ui.res.stringResource
import com.gabrielepiccione.onda.R
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrielepiccione.onda.HomeEntry
import com.gabrielepiccione.onda.LauncherViewModel
import com.gabrielepiccione.onda.data.AppInfo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import com.gabrielepiccione.onda.data.AppPopups
import com.gabrielepiccione.onda.data.CalendarEvent
import com.gabrielepiccione.onda.data.FOLDER_PREFIX
import com.gabrielepiccione.onda.data.HomeSuggestions
import com.gabrielepiccione.onda.data.UsageReminders
import com.gabrielepiccione.onda.data.SHORTCUT_PREFIX
import com.gabrielepiccione.onda.data.HomeBlocks
import com.gabrielepiccione.onda.data.Folder
import com.gabrielepiccione.onda.data.Actions
import com.gabrielepiccione.onda.data.HomeCarousel
import com.gabrielepiccione.onda.data.Holidays
import com.gabrielepiccione.onda.data.HolidayRepository
import com.gabrielepiccione.onda.data.BlockHit
import com.gabrielepiccione.onda.data.AppBlocks
import com.gabrielepiccione.onda.data.GestureAction
import com.gabrielepiccione.onda.data.NotificationItem
import com.gabrielepiccione.onda.data.NotificationStore
import com.gabrielepiccione.onda.data.WidgetManager
import com.gabrielepiccione.onda.service.OndaAccessibilityService
import com.gabrielepiccione.onda.util.IcuSectionIndex
import com.gabrielepiccione.onda.util.ActionRunner
import com.gabrielepiccione.onda.util.SystemActions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

enum class Overlay { NONE, ALL_APPS, SEARCH, SETTINGS, STYLES }

private const val UNDO_VISIBLE_MS = 8000L
private const val AGENDA_SHEET_DAYS = 7

/** Dove finisce un widget appena scelto. */
sealed interface WidgetTarget {
    data object Home : WidgetTarget
    data class InFolder(val folderId: String) : WidgetTarget
    data class ForApp(val appKey: String) : WidgetTarget
    data class Replace(val oldId: Int) : WidgetTarget
    data object ReplaceClock : WidgetTarget
    /** Nella pila [block], o in una pila nuova se [block] è un widget singolo. */
    data class ToStack(val block: String) : WidgetTarget
    /** Nel popup dell'app [appKey]. */
    data class ToPopup(val appKey: String) : WidgetTarget
    /** Il widget delle luci di casa, accanto all'orologio. */
    data object HomeLights : WidgetTarget
}

/** Azioni che richiedono l'Activity (permessi, risultati di altre schermate). */
interface HomeCallbacks {
    fun requestContactsPermission()
    fun requestCalendarPermission()
    fun requestWeatherPermission()
    fun requestLocationPermission()
    fun requestBluetoothPermission()
    fun requestMicrophonePermission()
    fun openSystemGrayscale()
    fun restartLauncher()
    fun requestPostNotifications()
    fun openUsageAccess()
    fun requestDefaultLauncher()
    fun openNotificationAccess()
    fun openAccessibilitySettings()
    fun openOwnAppInfo()
    fun pickWidget(provider: WidgetManager.Provider, target: WidgetTarget)
    fun exportBackup()
    fun importBackup()
    fun chooseBackupFolder()
    fun disableAutoBackup()
    fun shareBackup()
    fun importFont(forClock: Boolean)
    fun pickIconImage(appKey: String)
    /** Sceglie l'immagine di base dello sfondo musicale; con [thenApply] apre subito l'anteprima di sistema. */
    fun pickWallpaperBase(thenApply: Boolean)
    fun exportTheme(theme: com.gabrielepiccione.onda.data.LookTheme)
    fun authenticate(title: String, onSuccess: () -> Unit)
    fun importTheme()
}

data class HomeStatus(
    val notificationAccess: Boolean,
    val isDefaultLauncher: Boolean,
    val contactsPermission: Boolean,
    val calendarPermission: Boolean,
    val accessibilityEnabled: Boolean,
    val weatherInstalled: Boolean = false,
    val weatherPermission: Boolean = false,
    val locationPermission: Boolean = false,
    val microphonePermission: Boolean = false,
    val usageAccess: Boolean = false,
    val postNotifications: Boolean = false,
)

@Composable
fun HomeScreen(vm: LauncherViewModel, callbacks: HomeCallbacks, status: HomeStatus) {
    val state by vm.state.collectAsStateWithLifecycle()
    val search by vm.search.collectAsStateWithLifecycle()
    val media by vm.media.media.collectAsStateWithLifecycle()
    val headphoneNames by vm.audio.connectedNames.collectAsStateWithLifecycle()
    val nextEvent by vm.nextEvent.collectAsStateWithLifecycle()
    val agenda by vm.agenda.collectAsStateWithLifecycle()
    val headphones by vm.audio.headphonesConnected.collectAsStateWithLifecycle()
    val settings = state.settings
    val searchHistory by vm.searchHistory.history.collectAsStateWithLifecycle()
    // A ricerca vuota: «Cercate spesso», oppure le più usate se le cercate sono meno di due
    val searchPick = remember(searchHistory, state.allApps, state.suggestions, settings.searchSuggestions, settings.protectedApps) {
        SearchRanking.pick(
            searchHistory, System.currentTimeMillis(), settings.searchSuggestions, state.allApps, state.suggestions,
            settings.protectedApps.map { UsageReminders.packageOf(it) }.toSet(),
        ) { it.packageName }
    }
    val colors = LocalOndaColors.current
    val context = LocalContext.current
    val usageReader = remember { UsageReader(context) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val ondaHaptics = LocalOndaHaptics.current

    var overlay by rememberSaveable { mutableStateOf(Overlay.NONE) }
    var actionsFor by remember { mutableStateOf<AppInfo?>(null) }
    var panelFor by remember { mutableStateOf<AppInfo?>(null) }
    // Popup dell'app e riga da cui è stato aperto
    var quickFor by remember { mutableStateOf<AppInfo?>(null) }
    var quickAnchor by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val rowBounds = remember { HashMap<String, androidx.compose.ui.geometry.Rect>() }
    fun rowTracker(id: String): Modifier = Modifier.onGloballyPositioned { rowBounds[id] = it.boundsInRoot() }
    fun openQuick(app: AppInfo, rowId: String? = null) {
        quickAnchor = rowId?.let { rowBounds[it] }
        quickFor = app
    }
    var iconPickerFor by remember { mutableStateOf<AppInfo?>(null) }
    var folderSheet by remember { mutableStateOf<Folder?>(null) }
    var shortcutMenu by remember { mutableStateOf<HomeEntry.Shortcut?>(null) }
    var shortcutIconFor by remember { mutableStateOf<HomeEntry.Shortcut?>(null) }
    var lightsEdit by remember { mutableStateOf(false) }
    // Widget delle luci accanto all'orologio: le sue misure come ogni altro widget; pressione lunga per cambiarle
    val lightsWidget: @Composable () -> Unit = {
        HostedWidget(
            settings.homeLightsWidget,
            vm.widgets,
            heightDp = settings.widgetHeights[settings.homeLightsWidget] ?: LIGHTS_HEIGHT,
            size = settings.widgetSizes[WidgetSizing.widgetKey(settings.homeLightsWidget)],
            onLongPress = { if (!settings.lockLayout) lightsEdit = true },
        )
    }
    var mediaMenu by remember { mutableStateOf<HomeEntry.Media?>(null) }
    var mediaRename by remember { mutableStateOf<HomeEntry.Media?>(null) }
    var popupEditFor by remember { mutableStateOf<AppInfo?>(null) }
    var agendaOpen by remember { mutableStateOf(false) }
    var agendaWeek by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    val weather by vm.weather.collectAsStateWithLifecycle()
    LaunchedEffect(agendaOpen, status.calendarPermission) {
        if (agendaOpen) agendaWeek = vm.calendar.upcoming(AGENDA_SHEET_DAYS)
    }
    var folderIconFor by remember { mutableStateOf<String?>(null) }
    var moveFor by remember { mutableStateOf<AppInfo?>(null) }
    var homeMenu by remember { mutableStateOf(false) }
    var widgetPicker by remember { mutableStateOf(false) }
    var widgetFilter by remember { mutableStateOf<String?>(null) }
    var widgetTarget by remember { mutableStateOf<WidgetTarget>(WidgetTarget.Home) }
    var backProgress by remember { mutableFloatStateOf(0f) }

    val homeList = rememberLazyListState()
    val allAppsList = rememberLazyListState()
    val privateSpace by vm.appsRepo.privateSpace.collectAsStateWithLifecycle()
    val alphabetContacts by vm.alphabetContacts.collectAsStateWithLifecycle()
    // Alfabeto secondo le lingue del telefono, nel loro ordine
    val phoneLocales = androidx.compose.ui.platform.LocalConfiguration.current.locales
    val alphabet = remember(phoneLocales) { IcuSectionIndex.of(phoneLocales) }
    val sections = remember(state.allApps, settings.launchCounts, alphabetContacts, alphabet) {
        buildSections(state.allApps, settings.launchCounts, alphabetContacts, alphabet)
    }
    // Il segno finale porta a cartelle e voci di Onda in fondo all'elenco
    val letters = remember(sections) { listOf(FAVORITES_MARK) + sections.map { it.letter } + END_OF_LIST_MARK }
    var recentSheet by remember { mutableStateOf(false) }
    val iconScale = settings.iconSize

    fun closeAll() {
        overlay = Overlay.NONE
        vm.clearSearch()
    }

    // Usage Breaker: qualunque cosa apra l'app (riga, notifica, scorciatoia, gesto) passa da qui
    var breakerFor by remember { mutableStateOf<BreakerRequest?>(null) }
    // Blocchi delle app: i giorni festivi vengono dal calendario «Festività in Italia» (o dall'elenco interno)
    val holidayRepo = remember { HolidayRepository(context) }
    var holidays by remember { mutableStateOf<Set<java.time.LocalDate>?>(null) }
    LaunchedEffect(status.calendarPermission) { holidays = holidayRepo.around(java.time.LocalDate.now()) }
    var blockedNotice by remember { mutableStateOf<Pair<String, BlockHit>?>(null) }
    fun guarded(app: AppInfo?, go: () -> Unit) {
        if (app == null) return go()
        val current = vm.settingsRepo.current
        // Un blocco (a fascia oraria o a mano) vale prima di ogni pausa
        AppBlocks.hit(current, app.key, java.time.LocalDateTime.now()) { Holidays.isHoliday(it, holidays) }?.let { hit ->
            blockedNotice = app.label to hit
            return
        }
        val limit = current.appLimits[app.packageName] ?: 0
        when {
            app.key in current.breakerApps -> breakerFor = BreakerRequest(app, go)
            // Limite giornaliero: si legge l'uso di oggi solo se serve, poi pausa o apertura diretta
            limit > 0 && status.usageAccess -> scope.launch {
                val used = vm.usageStats.todayMs(app.packageName)
                if (UsageStatsMath.overLimit(used, limit)) breakerFor = BreakerRequest(app, go) else go()
            }
            else -> go()
        }
    }

    fun startApp(app: AppInfo) {
        val go = {
            vm.launch(app)
            scope.launch { delay(380); closeAll() }
        }
        // App protette: prima lo sblocco (impronta, volto o PIN)
        if (app.key in vm.settingsRepo.current.protectedApps) callbacks.authenticate(app.label) { go() } else go()
    }

    fun markSwiped() {
        if (!vm.settingsRepo.current.tips.swiped) vm.updateSettings { it.copy(tips = it.tips.copy(swiped = true)) }
    }

    fun launchApp(app: AppInfo) {
        // Usage Breaker: prima la pausa, poi l'eventuale sblocco
        guarded(app) { startApp(app) }
    }

    /** Apre la notifica come farebbe la tendina, ma con la pausa di Usage Breaker se l'app ce l'ha. */
    fun openNotification(item: NotificationItem) {
        val app = state.allApps.firstOrNull { it.packageName == item.packageName }
        guarded(app) {
            NotificationStore.open(context, item)
            scope.launch { delay(380); closeAll() }
        }
    }

    // Impostazioni aperte su una voce precisa (da benvenuto, suggerimenti e ricerca)
    var settingsFocus by remember { mutableStateOf<SettingEntry?>(null) }
    var profileToEdit by remember { mutableStateOf<ContextProfile?>(null) }
    fun openSettingsAt(entry: SettingEntry) {
        settingsFocus = entry
        vm.clearSearch()
        overlay = Overlay.SETTINGS
    }
    LaunchedEffect(overlay) { if (overlay != Overlay.SETTINGS) { settingsFocus = null; profileToEdit = null } }

    fun openWidgetPicker(filter: String?, target: WidgetTarget) {
        widgetFilter = filter
        widgetTarget = target
        widgetPicker = true
    }

    // Da Android 13 l'accesso alle notifiche/accessibilità per le app installate da APK è
    // "con limitazioni": prima di aprire le impostazioni mostriamo come sbloccarlo.
    var restrictedHelp by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun withRestrictedHelp(open: () -> Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) restrictedHelp = open else open()
    }

    fun perform(action: GestureAction, arg: String = "") {
        when (action) {
            GestureAction.SEARCH -> overlay = Overlay.SEARCH
            GestureAction.ALL_APPS -> overlay = Overlay.ALL_APPS
            GestureAction.SETTINGS -> overlay = Overlay.SETTINGS
            GestureAction.OPEN_APP -> state.app(arg)?.let { launchApp(it) }
            GestureAction.OPEN_SHORTCUT -> Actions.parseShortcut(arg)?.let { (pkg, id) -> vm.startShortcutById(pkg, id) }
            GestureAction.NONE -> Unit
            // Azioni di sistema del catalogo: il permesso mancante si chiede qui, con la guida
            else -> when (ActionRunner.run(context, action)) {
                ActionRunner.Outcome.NEEDS_ACCESSIBILITY -> withRestrictedHelp(callbacks::openAccessibilitySettings)
                ActionRunner.Outcome.NEEDS_POLICY_ACCESS -> SystemActions.openPolicyAccessSettings(context)
                else -> Unit
            }
        }
    }

    /** Esegue un'azione salvata nel formato di [Actions.encode] (swipe, popup, cartelle, ricerca). */
    fun performEncoded(encoded: String) {
        val choice = Actions.decode(encoded)
        perform(choice.action, choice.arg)
    }

    // Luci di casa: si controlla ogni minuto (e a ogni cambio di impostazione) se si è a casa e fa buio
    var lightsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(settings.homeLights, settings.homeWifi, settings.homeLightsWidget, settings.homeLatitude, status.locationPermission) {
        while (true) {
            val now = System.currentTimeMillis()
            val sun = if (settings.homeLatitude == 0.0 && settings.homeLongitude == 0.0) null
            else HomeLights.sunTimes(java.time.LocalDate.now(), settings.homeLatitude, settings.homeLongitude)
            lightsVisible = HomeLights.shouldShow(
                settings.homeLights,
                settings.homeLightsWidget,
                HomePresence.currentSsid(context),
                settings.homeWifi,
                now,
                sun,
                java.time.LocalTime.now().let { it.hour * 60 + it.minute },
            )
            delay(60_000L)
        }
    }

    // Preferiti contestuali: il profilo si ricalcola ogni 30 secondi, e subito se cambiano le regole
    LaunchedEffect(settings.contextProfiles, settings.profileOverride) {
        while (true) {
            vm.refreshContext()
            delay(30_000L)
        }
    }

    LaunchedEffect(state.loaded) { if (state.loaded) vm.seedFavoritesIfNeeded() }
    LaunchedEffect(settings.showAgenda, settings.agendaDays, status.calendarPermission) { vm.refreshCalendar() }
    LaunchedEffect(settings.contactsInAlphabet, status.contactsPermission) { vm.refreshAlphabetContacts() }
    LaunchedEffect(settings.showWeather, status.weatherPermission, status.weatherInstalled) { vm.refreshWeather(force = true) }

    // Pausa chiesta dal servizio di accessibilità: l'app si stava aprendo da fuori (tendina, recenti)
    LaunchedEffect(Unit) {
        vm.blockRequests.collect { notice ->
            val label = state.allApps.firstOrNull { it.packageName == notice.packageName }?.label ?: notice.packageName
            blockedNotice = label to BlockHit(notice.name, notice.until)
        }
    }

    LaunchedEffect(Unit) {
        vm.pauseRequests.collect { pkg ->
            val app = state.allApps.firstOrNull { it.packageName == pkg } ?: return@collect
            breakerFor = BreakerRequest(app) { startApp(app) }
        }
    }

    LaunchedEffect(Unit) {
        vm.homePressed.collect {
            if (overlay == Overlay.NONE && actionsFor == null && panelFor == null && quickFor == null) homeList.animateScrollToItem(0)
            closeAll()
            quickFor = null
            actionsFor = null
            panelFor = null
            folderSheet = null
            homeMenu = false
            widgetPicker = false
            breakerFor = null
            agendaOpen = false
        }
    }

    // Back predittivo (Android 14+): il pannello si rimpicciolisce seguendo il gesto
    PredictiveBackHandler(enabled = overlay != Overlay.NONE) { progress ->
        try {
            progress.collect { backProgress = it.progress }
            closeAll()
        } catch (e: CancellationException) {
            // gesto annullato
        } finally {
            backProgress = 0f
        }
    }

    // ---------- Gesti verticali interattivi ----------
    // "pull" segue il dito quando la lista è già al bordo; rilasciando oltre la soglia parte l'azione.
    val threshold = with(density) { 96.dp.toPx() }
    val pull = remember { Animatable(0f) }
    var armed by remember { mutableStateOf(false) }
    val currentSettings by rememberUpdatedState(settings)
    val gestureConnection = remember(threshold) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Se stiamo "tirando" e il dito torna indietro, consumiamo prima il tiro
                val p = pull.value
                if (source == NestedScrollSource.UserInput && p != 0f && sign(available.y) != sign(p)) {
                    val newP = if (abs(available.y) > abs(p)) 0f else p + available.y
                    val consumed = newP - p
                    scope.launch { pull.snapTo(newP) }
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y == 0f) return Offset.Zero
                val action = if (available.y > 0) GestureAction.NOTIFICATIONS else currentSettings.swipeUp
                if (action == GestureAction.NONE) return Offset.Zero
                val next = pull.value + available.y
                scope.launch { pull.snapTo(next) }
                val nowArmed = abs(next) >= threshold
                if (nowArmed != armed) {
                    armed = nowArmed
                    ondaHaptics?.perform(if (nowArmed) HapticEvent.THRESHOLD_ON else HapticEvent.THRESHOLD_OFF)
                }
                return available
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val p = pull.value
                if (p == 0f) return Velocity.Zero
                val fire = abs(p) >= threshold || (abs(p) > threshold / 3 && abs(available.y) > 2200f && sign(available.y) == sign(p))
                armed = false
                if (fire) {
                    if (p > 0) perform(GestureAction.NOTIFICATIONS) else perform(currentSettings.swipeUp, currentSettings.swipeUpApp)
                }
                scope.launch { pull.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f)) }
                return available
            }
        }
    }
    // Resistenza elastica: il contenuto si muove meno del dito
    fun rubber(p: Float): Float = sign(p) * sqrt(abs(p)) * 9f

    // Profondità: quando si apre un pannello la home arretra e sfuma
    // Profondità solo per ricerca e impostazioni; l'elenco app resta sullo stesso piano
    val depth by animateFloatAsState(
        if (overlay == Overlay.SEARCH || overlay == Overlay.SETTINGS || overlay == Overlay.STYLES) 1f else 0f,
        Motion.enter(450),
        label = "depth",
    )
    val listMode by animateFloatAsState(
        if (overlay == Overlay.ALL_APPS) 1f else 0f,
        androidx.compose.animation.core.tween(150, easing = Motion.Standard),
        label = "listMode",
    )
    val panelScale = 1f - 0.1f * backProgress
    val panelCorner = (32 * backProgress).dp

    // Voci della home (preferiti e cartelle), inserite nella posizione del blocco "app"
    fun androidx.compose.foundation.lazy.LazyListScope.homeEntries() {
        items(state.home, key = { it.key }, contentType = { it::class }) { entry ->
            val clearOfLetters = 48.dp - 28.dp
            val itemMod = Modifier
                .animateItem(
                    fadeInSpec = Motion.enter(),
                    placementSpec = Motion.offsetEnter,
                    fadeOutSpec = Motion.exit(),
                )
                .padding(
                    start = if (settings.leftHanded) clearOfLetters else 0.dp,
                    end = if (settings.leftHanded) 0.dp else clearOfLetters,
                )
            when (entry) {
                is HomeEntry.App -> {
                    val app = entry.app
                    val target = state.app(app.swipeTarget)
                    FavoriteRow(
                        app = app,
                        swipeTarget = target,
                        notifications = state.notifications[app.packageName].orEmpty(),
                        showIcon = settings.showIcons,
                        themedIcons = settings.themedIcons,
                        hideName = settings.hideFavoriteNames,
                        iconScale = iconScale,
                        onClick = { launchApp(app) },
                        onLongClick = { openQuick(app, "h_" + app.key) },
                        onSwipe = { openQuick(app, "h_" + app.key) },
                        onOpenPanel = {
                            markSwiped()
                            // Azione scelta per lo swipe dell'app (catalogo unico); senza, il pannello
                            if (app.swipeTarget.isNullOrBlank()) openQuick(app, "h_" + app.key) else performEncoded(app.swipeTarget)
                        },
                        onSwipeLeft = { markSwiped(); NotificationStore.dismissAll(app.packageName) },
                        onNotificationClick = { openNotification(it) },
                        onNotificationDismiss = { NotificationStore.dismiss(it) },
                        onNotificationReply = { n, text -> NotificationStore.reply(context, n, text) },
                        onNotificationAction = { n, a -> NotificationStore.runAction(context, n, a) },
                        modifier = itemMod.then(rowTracker("h_" + app.key)),
                    )
                }
                is HomeEntry.Group -> FolderRow(
                    folder = entry.folder,
                    apps = entry.apps,
                    widgets = vm.widgets,
                    notifications = state.notifications,
                    showIcons = settings.showIcons,
                    themedIcons = settings.themedIcons,
                    iconScale = iconScale,
                    onApp = { launchApp(it) },
                    onAppLongClick = { openQuick(it, "g_" + entry.folder.id) },
                    onLongClick = { folderSheet = entry.folder },
                    hideName = settings.hideFavoriteNames,
                    onPerform = { markSwiped(); performEncoded(it) },
                    modifier = itemMod.then(rowTracker("g_" + entry.folder.id)),
                )
                is HomeEntry.Media -> PinnedShortcutRow(
                    label = entry.shortcut.name,
                    icon = entry.app?.icon,
                    showIcon = settings.showIcons,
                    hideName = settings.hideFavoriteNames,
                    iconScale = iconScale,
                    onClick = { vm.openMediaShortcut(entry.shortcut) },
                    onLongClick = { if (!settings.lockLayout) mediaMenu = entry },
                    modifier = itemMod,
                )
                is HomeEntry.Shortcut -> PinnedShortcutRow(
                    label = entry.info.label,
                    icon = entry.info.icon,
                    showIcon = settings.showIcons,
                    hideName = settings.hideFavoriteNames,
                    iconScale = iconScale,
                    onClick = { vm.startShortcut(entry.info.shortcut) },
                    onLongClick = { if (!settings.lockLayout) shortcutMenu = entry },
                    modifier = itemMod,
                )
            }
        }
    }

    val blocks = remember(settings) { HomeBlocks.resolve(settings) }
    var editing by remember { mutableStateOf<String?>(null) }
    // Annulla dopo un preset: aspetto precedente e nome del preset che era attivo
    var undoLook by remember { mutableStateOf<Pair<com.gabrielepiccione.onda.data.LookTheme, String>?>(null) }
    var undoKeepsWallpaper by remember { mutableStateOf(false) }
    var reorderOpen by remember { mutableStateOf(false) }
    // "Lo sapevi?": una scheda piccola al giorno, mai mentre c'è un pannello o una modifica in corso
    val today = remember { java.time.LocalDate.now().toString() }
    val minuteNow = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
    val inFolders = remember(settings.folders) { settings.folders.values.flatMap { it.apps }.toSet() }
    val tipSignals = TipSignals(
        today = today,
        minuteOfDay = minuteNow,
        daysSinceFirst = DiscoveryTips.daysBetween(settings.tips.firstDay, today),
        oftenOpenedNotFavorite = settings.launchCounts.any { (k, c) -> c >= DiscoveryTips.OFTEN_OPENED && k !in settings.favorites && k !in inFolders },
        hasNotifications = state.notifications.isNotEmpty(),
        playerSeen = media != null,
        headphonesSeen = headphones,
        grayscaleOn = settings.grayscaleMode != GrayscaleMode.OFF,
        hasProfiles = settings.contextProfiles.isNotEmpty(),
        busy = overlay != Overlay.NONE || quickFor != null || panelFor != null || actionsFor != null || editing != null || reorderOpen || breakerFor != null,
    )
    // Profili proposti dall'uso: si ricalcolano quando la funzione è accesa, ogni volta che si torna in home
    var suggestionPool by remember { mutableStateOf<List<ProfileSuggestion>>(emptyList()) }
    val launchablePackages = remember(state.allApps) { state.allApps.map { it.packageName }.toSet() }
    // Anche appena concesso l'accesso ai dati d'uso: il primo suggerimento non aspetta
    LaunchedEffect(settings.profileSuggest.enabled, settings.profileSuggest.useWifi, launchablePackages, overlay == Overlay.NONE, status.usageAccess) {
        suggestionPool = if (settings.profileSuggest.enabled && state.loaded && overlay == Overlay.NONE) {
            val fixed = settings.favorites.take(settings.fixedFavoriteSlots).toSet()
            UsageProfiles.suggest(
                events = usageReader.read(System.currentTimeMillis(), launchablePackages),
                now = System.currentTimeMillis(),
                zone = java.time.ZoneId.systemDefault(),
                exclude = fixed.map { UsageReminders.packageOf(it) }.toSet() + context.packageName,
                useWifi = settings.profileSuggest.useWifi,
            )
        } else {
            emptyList()
        }
    }
    // Il profilo proposto ha la precedenza su "Lo sapevi?": è la cosa più utile da vedere
    val suggestion = if (!tipSignals.busy && settings.welcomeDone) UsageProfiles.next(settings.profileSuggest, suggestionPool, today, System.currentTimeMillis()) else null
    val tip = if (state.loaded && settings.welcomeDone && suggestion == null) DiscoveryTips.next(settings.tips, tipSignals) else null
    LaunchedEffect(suggestion?.key) { suggestion?.let { s -> vm.updateSettings { it.copy(profileSuggest = UsageProfiles.shown(it.profileSuggest, s, System.currentTimeMillis())) } } }
    LaunchedEffect(state.loaded) {
        if (state.loaded && settings.tips.firstDay.isEmpty()) vm.updateSettings { it.copy(tips = it.tips.copy(firstDay = today)) }
    }
    LaunchedEffect(tip) { if (tip != null) vm.updateSettings { it.copy(tips = DiscoveryTips.shown(it.tips, tip, today)) } }
    LaunchedEffect(overlay) { if (overlay == Overlay.ALL_APPS) vm.updateSettings { it.copy(tips = it.tips.copy(listOpens = it.tips.listOpens + 1)) } }

    var summaryOpen by remember { mutableStateOf(false) }
    val digest = remember(state.notifications) { com.gabrielepiccione.onda.data.NotificationDigest.of(state.notifications) }
    // Nome dell'app, mai il pacchetto quando il sistema conosce un nome (es. Servizi Google Play)
    val packageLabel = rememberAppLabel { pkg -> state.allApps.firstOrNull { it.packageName == pkg } }
    val cardsCallbacks = CardsCallbacks(
        onMediaOpen = { vm.media.open() },
        onPlayPause = { vm.media.playPause() },
        onPrev = { vm.media.previous() },
        onNext = { vm.media.next() },
        onMediaAction = { vm.media.customAction(it) },
        onEvent = { vm.calendar.open(it) },
        onAgendaEmpty = { if (!status.calendarPermission) callbacks.requestCalendarPermission() else SystemActions.openCalendar(context) },
        onSummary = { summaryOpen = true },
        onLongPress = { block -> if (!settings.lockLayout) editing = block },
    )
    // Il blocco in modifica può sparire (es. una pila ridotta a un solo widget)
    LaunchedEffect(blocks, editing) { if (editing != null && editing !in blocks) editing = null }

    val dimColor = if (colors.dark) Color.Black else Color.White
    Box(Modifier.fillMaxSize()) {
        // Sfondo con la musica: sotto la velatura, così testo e icone restano leggibili
        val backdropHere = when (overlay) {
            Overlay.NONE -> settings.musicBackdropHome
            Overlay.ALL_APPS -> settings.musicBackdropList
            Overlay.SEARCH -> settings.musicBackdropSearch
            else -> false
        }
        if (backdropHere && media != null) {
            MusicBackdrop(
                art = media?.art,
                playing = media?.playing == true,
                mode = settings.musicBackdrop,
                intensity = settings.musicBackdropIntensity,
                dark = colors.dark,
                modifier = Modifier.matchParentSize(),
                beatAllowed = status.microphonePermission,
            )
        }
        Box(Modifier.matchParentSize().background(dimColor.copy(alpha = settings.wallpaperDim)))

        val bars = WindowInsets.systemBars.asPaddingValues()
        // Su tablet e in orizzontale il contenuto resta centrato, con la stessa larghezza leggibile
        val wideInset = com.gabrielepiccione.onda.data.ScreenLayout.sideInsetDp(androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp).dp
        val letterSide = 48.dp + wideInset
        val hiddenSide = 28.dp + wideInset
        // Posizione scelta dall'utente: il primo blocco parte da lì e il resto lo segue
        val manualTop = settings.homeTopFraction >= 0f
        val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
        val chosenTop = if (manualTop) screenHeight * settings.homeTopFraction else 0.dp

        // ---------------- HOME ----------------
        LazyColumn(
            state = homeList,
            verticalArrangement = Arrangement.spacedBy(settings.homeBlockSpacing.dp, if (manualTop) Alignment.Top else Alignment.Bottom),
            // Orologio, widget e player: stesso margine a destra e a sinistra.
            // Solo le righe delle app si tengono lontane dalle lettere (vedi homeEntries).
            contentPadding = PaddingValues(
                start = hiddenSide,
                end = hiddenSide,
                top = bars.calculateTopPadding() + 24.dp + chosenTop,
                // Con il pulsante di ricerca in basso le ultime righe restano sopra di lui
                bottom = bars.calculateBottomPadding() + if (settings.showSearchButton) 88.dp else 28.dp,
            ),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = 1f - 0.06f * depth
                    scaleX = s; scaleY = s
                    val pr = (abs(pull.value) / threshold).coerceIn(0f, 1f)
                    alpha = (1f - depth) * (1f - 0.35f * pr) * (1f - listMode)
                    translationY = rubber(pull.value)
                }
                .nestedScroll(gestureConnection)
                .pointerInput(settings.doubleTapAction, settings.doubleTapApp) {
                    detectTapGestures(
                        onLongPress = {
                            ondaHaptics?.perform(HapticEvent.LONG_PRESS)
                            homeMenu = true
                        },
                        onDoubleTap = if (settings.doubleTapAction != GestureAction.NONE) {
                            { _: Offset -> perform(settings.doubleTapAction, settings.doubleTapApp) }
                        } else null,
                    )
                }
        ) {
            // Orologio sostituito da un widget: la regola delle luci di casa vale lo stesso, in cima alla home
            if (!settings.showClock && lightsVisible) {
                item(key = "lights_alone") {
                    Box(Modifier.animateItem(placementSpec = Motion.offsetEnter).padding(horizontal = 4.dp)) { lightsWidget() }
                }
            }
            // Blocchi della home nell'ordine scelto dall'utente (orologio, musica, widget, app)
            for (block in blocks) {
                when {
                    block == HomeBlocks.CLOCK -> if (settings.showClock) {
                        item(key = "clock") {
                            // Luci di casa: il widget di Google Home sta accanto all'orologio, dalla parte libera
                            Column(Modifier.animateItem(placementSpec = Motion.offsetEnter)) {
                            // Scorrere di lato sull'orologio sceglie a mano il profilo dei preferiti per un'ora
                            val swipePx = with(density) { 64.dp.toPx() }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.pointerInput(settings.contextProfiles.isNotEmpty()) {
                                    if (settings.contextProfiles.isEmpty()) return@pointerInput
                                    var dragged = 0f
                                    detectHorizontalDragGestures(
                                        onDragStart = { dragged = 0f },
                                        onDragEnd = {
                                            if (kotlin.math.abs(dragged) > swipePx) {
                                                ondaHaptics?.perform(HapticEvent.CONFIRM)
                                                vm.cycleProfile()
                                            }
                                        },
                                    ) { _, delta -> dragged += delta }
                                },
                            ) {
                            if (lightsVisible && settings.clockAlign == HAlign.END) {
                                Box(Modifier.weight(1f).padding(end = 8.dp)) { lightsWidget() }
                            }
                            ClockHeader(
                                event = if (settings.showCalendar) nextEvent else null,
                                onTime = { SystemActions.openAlarms(context) },
                                onDate = { agendaOpen = true },
                                temperature = weather?.temperature,
                                onEvent = { vm.calendar.open(it) },
                                style = settings.clockStyle,
                                scale = settings.clockScale,
                                align = settings.clockAlign,
                                highlighted = editing == HomeBlocks.CLOCK,
                                onLongPress = { if (!settings.lockLayout) editing = HomeBlocks.CLOCK },
                                showBattery = settings.showBattery,
                                innerSpacing = settings.clockInnerSpacing,
                                modifier = if (lightsVisible) Modifier.weight(1f) else Modifier,
                            )
                            if (lightsVisible && settings.clockAlign != HAlign.END) {
                                Box(Modifier.weight(1f).padding(start = 8.dp)) { lightsWidget() }
                            }
                            }
                            val manual = settings.profileOverride
                            if (settings.contextProfiles.isNotEmpty() && manual.until > System.currentTimeMillis()) {
                                Text(
                                    stringResource(
                                        R.string.ctx_manual_label,
                                        state.activeProfile?.name ?: stringResource(R.string.ctx_manual_none),
                                        java.time.Instant.ofEpochMilli(manual.until).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
                                            .format(java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT)),
                                    ),
                                    color = LocalOndaColors.current.onWallpaperMuted,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                )
                            }
                            }
                        }
                    }
                    // Player, agenda e riepilogo in un solo carosello, al posto del primo dei tre blocchi
                    settings.cardsCarousel && block in HomeCarousel.CARD_BLOCKS -> if (block == HomeCarousel.anchorBlock(blocks)) {
                        item(key = "cards") {
                            HomeCardsCarousel(
                                pages = HomeCarousel.pages(blocks, hasMedia = settings.showMedia && media != null, hasAgenda = true, hasNotifications = digest.total > 0),
                                media = media,
                                events = agenda,
                                digest = digest,
                                appOf = { pkg -> state.allApps.firstOrNull { it.packageName == pkg } },
                                labelOf = packageLabel,
                                themedIcons = settings.themedIcons,
                                highlighted = editing,
                                callbacks = cardsCallbacks,
                                modifier = Modifier.animateItem(placementSpec = Motion.offsetEnter),
                            )
                        }
                    }
                    block == HomeBlocks.AGENDA -> item(key = "agenda") {
                        AgendaCard(
                            events = agenda,
                            onEvent = { vm.calendar.open(it) },
                            highlighted = editing == HomeBlocks.AGENDA,
                            onLongPress = { if (!settings.lockLayout) editing = HomeBlocks.AGENDA },
                            onEmptyClick = { if (!status.calendarPermission) callbacks.requestCalendarPermission() else SystemActions.openCalendar(context) },
                            modifier = Modifier.animateItem(placementSpec = Motion.offsetEnter),
                        )
                    }
                    block == HomeBlocks.NOTIFICATIONS -> item(key = "notifications") {
                        AnimatedVisibility(
                            visible = digest.total > 0,
                            enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
                            exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit()),
                            modifier = Modifier.animateItem(placementSpec = Motion.offsetEnter),
                        ) {
                            NotificationSummaryCard(
                                digest = digest,
                                appLabel = packageLabel,
                                highlighted = editing == HomeBlocks.NOTIFICATIONS,
                                onClick = { summaryOpen = true },
                                onLongPress = { if (!settings.lockLayout) editing = HomeBlocks.NOTIFICATIONS },
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                            )
                        }
                    }
                    block == HomeBlocks.MEDIA -> item(key = "media") {
                        val m = media
                        AnimatedVisibility(
                            visible = settings.showMedia && m != null,
                            enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
                            exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit()),
                            modifier = Modifier.animateItem(placementSpec = Motion.offsetEnter),
                        ) {
                            if (m != null) {
                                MediaCard(
                                    media = m,
                                    onOpen = { vm.media.open() },
                                    onPlayPause = { vm.media.playPause() },
                                    onPrev = { vm.media.previous() },
                                    onNext = { vm.media.next() },
                                    onSeek = { vm.media.seekTo(it) },
                                    onAction = { vm.media.customAction(it) },
                                    compact = settings.mediaCompact,
                                    scale = settings.mediaScale,
                                    highlighted = editing == HomeBlocks.MEDIA,
                                    onLongPress = { if (!settings.lockLayout) editing = HomeBlocks.MEDIA },
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                                )
                            }
                        }
                    }
                    block.startsWith(HomeBlocks.STACK_PREFIX) -> {
                        val stack = settings.widgetStacks.firstOrNull { it.id == HomeBlocks.stackId(block) }
                        if (stack != null) {
                            item(key = block) {
                                WidgetStackCard(
                                    stack = stack,
                                    widgets = vm.widgets,
                                    highlighted = editing == block,
                                    onLongPress = { if (!settings.lockLayout) editing = block },
                                    modifier = Modifier.animateItem(placementSpec = Motion.offsetEnter).padding(horizontal = 4.dp),
                                )
                            }
                        }
                    }
                    block.startsWith(HomeBlocks.WIDGET_PREFIX) -> {
                        val id = HomeBlocks.widgetId(block)
                        if (id != null) {
                            item(key = block) {
                                Box(Modifier.animateItem(placementSpec = Motion.offsetEnter).padding(horizontal = 4.dp)) {
                                    HostedWidget(
                                        id = id,
                                        widgets = vm.widgets,
                                        heightDp = settings.widgetHeights[id],
                                        size = settings.widgetSizes[WidgetSizing.widgetKey(id)],
                                        highlighted = editing == block,
                                        onLongPress = { if (!settings.lockLayout) editing = block },
                                    )
                                }
                            }
                        }
                    }
                    block == HomeBlocks.APPS -> {
                        item(key = "tip") {
                            TipCard(
                                tip = tip,
                                onShowMe = { entry -> openSettingsAt(entry) },
                                onNotNow = { tip?.let { t -> vm.updateSettings { it.copy(tips = DiscoveryTips.notNow(it.tips, t, today)) } } },
                                onNever = { tip?.let { t -> vm.updateSettings { it.copy(tips = DiscoveryTips.never(it.tips, t)) } } },
                                modifier = Modifier.animateItem(fadeInSpec = Motion.enter(), placementSpec = Motion.offsetEnter, fadeOutSpec = Motion.exit()),
                            )
                        }
                        item(key = "profile_suggestion") {
                            ProfileSuggestionCard(
                                suggestion = suggestion,
                                apps = suggestion?.apps.orEmpty().mapNotNull { p -> state.allApps.firstOrNull { it.packageName == p } },
                                themedIcons = settings.themedIcons,
                                onCreate = {
                                    suggestion?.let { s ->
                                        vm.updateSettings { st ->
                                            // Le app del profilo si salvano come chiavi: la prima app con quel pacchetto
                                            val profile = UsageProfiles.toProfile(s, java.util.UUID.randomUUID().toString())
                                                .let { p -> p.copy(apps = p.apps.mapNotNull { pkg -> state.allApps.firstOrNull { it.packageName == pkg }?.key }) }
                                            st.copy(
                                                contextProfiles = st.contextProfiles + profile,
                                                profileSuggest = UsageProfiles.accepted(st.profileSuggest, s, today),
                                            )
                                        }
                                    }
                                },
                                onEdit = {
                                    suggestion?.let { s ->
                                        profileToEdit = UsageProfiles.toProfile(s, java.util.UUID.randomUUID().toString())
                                            .let { p -> p.copy(apps = p.apps.mapNotNull { pkg -> state.allApps.firstOrNull { it.packageName == pkg }?.key }) }
                                        vm.updateSettings { it.copy(profileSuggest = UsageProfiles.accepted(it.profileSuggest, s, today)) }
                                        settingsFocus = null
                                        overlay = Overlay.SETTINGS
                                    }
                                },
                                onIgnore = { suggestion?.let { s -> vm.updateSettings { it.copy(profileSuggest = UsageProfiles.ignore(it.profileSuggest, s, today)) } } },
                                modifier = Modifier.animateItem(fadeInSpec = Motion.enter(), placementSpec = Motion.offsetEnter, fadeOutSpec = Motion.exit()),
                            )
                        }
                        // Plug & Play: con cuffie o casse collegate, l'app scelta compare in cima
                        val plug = if (headphones && PlugPlay.matches(settings.plugPlayDevice, headphoneNames)) state.app(settings.plugPlayApp) else null
                        if (plug != null) {
                            item(key = "plugplay") {
                                Column(Modifier.animateItem(fadeInSpec = Motion.enter(), placementSpec = Motion.offsetEnter, fadeOutSpec = Motion.exit())) {
                                    Text(
                                        stringResource(R.string.plug_play_connected),
                                        color = colors.accent,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                                    )
                                    FavoriteRow(
                                        app = plug,
                                        swipeTarget = null,
                                        notifications = state.notifications[plug.packageName].orEmpty(),
                                        showIcon = settings.showIcons,
                                        themedIcons = settings.themedIcons,
                                        hideName = false,
                                        iconScale = iconScale,
                                        onClick = { launchApp(plug) },
                                        onLongClick = { openQuick(plug, "plug") },
                                        onSwipe = { openQuick(plug, "plug") },
                                        onOpenPanel = { openQuick(plug, "plug") },
                                        onSwipeLeft = { NotificationStore.dismissAll(plug.packageName) },
                                        onNotificationClick = { openQuick(plug, "plug") },
                                        onNotificationDismiss = { NotificationStore.dismiss(it) },
                                        modifier = rowTracker("plug"),
                                    )
                                }
                            }
                        }
                        homeEntries()
                        if (state.homeSuggestions.isNotEmpty()) {
                            item(key = "suggested_label") {
                                Text(
                                    stringResource(R.string.suggested_label),
                                    color = colors.onWallpaperMuted,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier
                                        .animateItem(fadeInSpec = Motion.enter(), placementSpec = Motion.offsetEnter, fadeOutSpec = Motion.exit())
                                        .padding(start = 12.dp, top = 12.dp),
                                )
                            }
                            items(state.homeSuggestions, key = { "suggested_" + it.key }) { app ->
                                val rowId = "suggested_" + app.key
                                FavoriteRow(
                                    app = app,
                                    swipeTarget = null,
                                    notifications = state.notifications[app.packageName].orEmpty(),
                                    showIcon = settings.showIcons,
                                    themedIcons = settings.themedIcons,
                                    hideName = settings.hideFavoriteNames,
                                    iconScale = iconScale,
                                    onClick = { launchApp(app) },
                                    onLongClick = { openQuick(app, rowId) },
                                    onSwipe = { openQuick(app, rowId) },
                                    onOpenPanel = { openQuick(app, rowId) },
                                    onSwipeLeft = { NotificationStore.dismissAll(app.packageName) },
                                    onNotificationClick = { openNotification(it) },
                                    onNotificationDismiss = { NotificationStore.dismiss(it) },
                                    modifier = Modifier
                                        .animateItem(fadeInSpec = Motion.enter(), placementSpec = Motion.offsetEnter, fadeOutSpec = Motion.exit())
                                        .then(rowTracker(rowId)),
                                )
                            }
                        }
                    }
                }
            }
            if (state.loaded && state.home.isEmpty() && state.homeSuggestions.isEmpty()) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.home_empty_hint),
                        color = colors.onWallpaperMuted,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
            if (!status.notificationAccess || !status.isDefaultLauncher) {
                item(key = "hints") {
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!status.notificationAccess) {
                            HintChip(Icons.Filled.Notifications, stringResource(R.string.hint_notifications)) { withRestrictedHelp(callbacks::openNotificationAccess) }
                        }
                        if (!status.isDefaultLauncher) {
                            HintChip(Icons.Filled.Settings, stringResource(R.string.hint_default)) { callbacks.requestDefaultLauncher() }
                        }
                    }
                }
            }
        }

        // Indicatore del gesto verticale (cresce col dito, si "accende" oltre la soglia)
        val pv = pull.value
        if (pv != 0f) {
            val action = if (pv > 0) GestureAction.NOTIFICATIONS else settings.swipeUp
            val progress = (abs(pv) / threshold).coerceIn(0f, 1.2f)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(if (pv > 0) Alignment.TopCenter else Alignment.BottomCenter)
                    .padding(top = bars.calculateTopPadding() + 12.dp, bottom = bars.calculateBottomPadding() + 12.dp)
                    .graphicsLayer {
                        alpha = progress.coerceIn(0f, 1f)
                        val s = 0.6f + 0.4f * progress.coerceIn(0f, 1f)
                        scaleX = s; scaleY = s
                        translationY = rubber(pv) * 0.5f
                    }
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (armed) colors.accent else colors.panelHigh)
            ) {
                Icon(
                    actionIcon(action),
                    null,
                    tint = if (armed) colors.onAccent else colors.onWallpaper,
                )
            }
        }

        // ---------------- TUTTE LE APP (stessa pagina, nessun pannello) ----------------
        // L'elenco è sempre composto (pronto all'istante) e sostituisce i preferiti con una
        // dissolvenza breve, sopra lo stesso sfondo: si resta "nella stessa pagina".
        val appsFloor = remember(threshold) {
            object : NestedScrollConnection {
                var pullDown = 0f
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (source == NestedScrollSource.UserInput && available.y > 0f) pullDown += available.y
                    return Offset.Zero
                }
                override suspend fun onPreFling(available: Velocity): Velocity {
                    // In cima all'elenco, tirando giù si torna ai preferiti
                    if (pullDown > threshold || (pullDown > threshold / 3 && available.y > 2200f)) closeAll()
                    pullDown = 0f
                    return Velocity.Zero
                }
            }
        }
        AllAppsPanel(
            sections = sections,
            listState = allAppsList,
            activeLetter = null,
            notifications = state.notifications,
            showIcons = settings.showIcons,
            themedIcons = settings.themedIcons,
            onClick = { launchApp(it) },
            onLongClick = { openQuick(it, "a_" + it.key) },
            onOpenPanel = { openQuick(it, "a_" + it.key) },
            onNotificationClick = { openNotification(it) },
            rowModifier = { rowTracker("a_" + it.key) },
            privateLocked = privateSpace?.locked?.takeUnless { settings.hidePrivateSpace },
            privateApps = state.privateApps,
            onTogglePrivateLock = { privateSpace?.let { vm.appsRepo.setPrivateSpaceLocked(!it.locked) } },
            onDismissNotifications = { NotificationStore.dismissAll(it.packageName) },
            onContact = { vm.contacts.open(it); scope.launch { delay(380); closeAll() } },
            folders = state.folders.map { it.folder },
            folderRow = { folder ->
                val group = state.folders.firstOrNull { it.folder.id == folder.id }
                if (group != null) {
                    FolderRow(
                        folder = group.folder,
                        apps = group.apps,
                        widgets = vm.widgets,
                        notifications = state.notifications,
                        showIcons = settings.showIcons,
                        themedIcons = settings.themedIcons,
                        iconScale = 1f,
                        onApp = { launchApp(it) },
                        onAppLongClick = { openQuick(it, "fa_" + folder.id) },
                        onLongClick = { folderSheet = group.folder },
                        onPerform = { performEncoded(it) },
                        modifier = rowTracker("fa_" + folder.id),
                    )
                }
            },
            onRecentlyInstalled = { recentSheet = true },
            onLauncherSettings = { overlay = Overlay.SETTINGS },
            contentPadding = PaddingValues(
                start = if (settings.leftHanded) letterSide else hiddenSide,
                end = if (settings.leftHanded) hiddenSide else letterSide,
                top = bars.calculateTopPadding() + 16.dp,
                bottom = bars.calculateBottomPadding() + if (settings.showSearchButton) 88.dp else 24.dp,
            ),
            modifier = Modifier
                .zIndex(if (overlay == Overlay.ALL_APPS) 1f else -1f)
                .graphicsLayer {
                    alpha = listMode
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                // Le righe svaniscono salendo sotto la barra di stato, così non si sovrappongono alle sue icone
                .drawWithContent {
                    drawContent()
                    val statusBar = bars.calculateTopPadding().toPx()
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black),
                            startY = statusBar * 0.6f,
                            endY = statusBar + TOP_FADE.toPx(),
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                }
                .nestedScroll(appsFloor),
        )

        // ---------------- SCROLLER (lettere visibili + lato opposto invisibile) ----------------
        val scrollerActive = overlay == Overlay.NONE || overlay == Overlay.ALL_APPS
        val lettersVisible = overlay == Overlay.ALL_APPS || !settings.hideAlphabet
        val scrollerAlpha by animateFloatAsState(if (scrollerActive) 1f else 0f, Motion.enter(250), label = "scroller")
        val scrollerPadding = Modifier.padding(
            top = bars.calculateTopPadding() + 16.dp,
            bottom = bars.calculateBottomPadding() + 40.dp,
        )
        // Doppio tocco sull'alfabeto (visibile o invisibile): stessa azione del doppio tocco in home
        val lockFromAlphabet: (() -> Unit)? = if (settings.doubleTapAction != GestureAction.NONE) {
            {
                closeAll()
                perform(settings.doubleTapAction, settings.doubleTapApp)
            }
        } else null
        // La prima app della lettera scelta si porta al centro dello schermo, qualunque sia la lettera;
        // agli estremi dell'elenco si ferma da solo.
        val listTopPx = with(density) { (bars.calculateTopPadding() + 16.dp).toPx() }
        val firstRowCenterPx = with(density) { FIRST_ROW_CENTER.toPx() }
        val onLetter: (String) -> Unit = onLetter@{ letter ->
            if (!settings.tips.alphabetUsed) vm.updateSettings { it.copy(tips = it.tips.copy(alphabetUsed = true)) }
            // La stella riporta ai preferiti; scendendo sulle lettere l'elenco torna subito
            if (letter == FAVORITES_MARK) {
                overlay = Overlay.NONE
                scope.launch { homeList.animateScrollToItem(0) }
                return@onLetter
            }
            overlay = Overlay.ALL_APPS
            val target = if (letter == END_OF_LIST_MARK) {
                val privateShown = privateSpace?.takeUnless { settings.hidePrivateSpace } != null
                val privateRows = if (privateShown) 1 + state.privateApps.size else 0
                endOfListIndex(sections, privateRows)
            } else {
                sections.firstOrNull { it.letter == letter }?.firstIndex
            }
            if (target != null && letter == END_OF_LIST_MARK) {
                // Il fondo dell'elenco (cartelle, installate di recente, impostazioni) si mostra intero:
                // la sezione va in cima e, se è più corta dello schermo, l'elenco si ferma in fondo da solo
                scope.launch { allAppsList.scrollToItem(target) }
            } else if (target != null) {
                val center = allAppsList.layoutInfo.viewportSize.height / 2f
                val lift = (center - listTopPx - firstRowCenterPx).coerceAtLeast(0f)
                scope.launch {
                    allAppsList.scrollToItem(target)
                    allAppsList.scrollBy(-lift)
                }
            }
        }
        // Mentre il dito scorre sull'alfabeto, l'onda delle lettere passerebbe sopra il pulsante: si fa da parte
        var alphabetTouched by remember { mutableStateOf(false) }
        val searchButtonAlpha by animateFloatAsState(if (alphabetTouched) 0f else 1f, Motion.enter(150), label = "searchButtonAway")
        if (scrollerAlpha > 0.01f) {
            AlphabetWaveScroller(
                letters = letters,
                leftSide = settings.leftHanded,
                showLetters = lettersVisible,
                letterScale = settings.alphabetScale,
                maxLetterStep = settings.alphabetStepDp.dp,
                restFraction = settings.alphabetRest,
                hotAreaWidth = settings.alphabetHotAreaDp.dp,
                modifier = Modifier
                    .align(if (settings.leftHanded) Alignment.CenterStart else Alignment.CenterEnd)
                    .zIndex(2f)
                    .graphicsLayer { alpha = scrollerAlpha }
                    .then(scrollerPadding)
                    .padding(horizontal = 6.dp),
                onTouchStart = { overlay = Overlay.ALL_APPS; alphabetTouched = true },
                onLetter = onLetter,
                onTouchEnd = { alphabetTouched = false },
                onDoubleTap = lockFromAlphabet,
            )
            AlphabetWaveScroller(
                letters = letters,
                leftSide = !settings.leftHanded,
                showLetters = false,
                maxLetterStep = settings.alphabetStepDp.dp,
                restFraction = settings.alphabetRest,
                hotAreaWidth = settings.alphabetHotAreaDp.dp,
                modifier = Modifier
                    .align(if (settings.leftHanded) Alignment.CenterEnd else Alignment.CenterStart)
                    .zIndex(2f)
                    .then(scrollerPadding),
                onTouchStart = { overlay = Overlay.ALL_APPS },
                onLetter = onLetter,
                onDoubleTap = lockFromAlphabet,
            )
            if (settings.showSearchButton) {
                // Fluttua accanto alla colonna dell'alfabeto, staccato dai bordi e velato sullo sfondo
                SearchButton(
                    settings = settings,
                    onAction = { action, appKey -> perform(action, appKey) },
                    modifier = Modifier
                        .align(if (settings.leftHanded) Alignment.BottomStart else Alignment.BottomEnd)
                        .zIndex(3f)
                        .graphicsLayer { alpha = scrollerAlpha * searchButtonAlpha }
                        .padding(
                            start = if (settings.leftHanded) letterSide + 4.dp else 0.dp,
                            end = if (settings.leftHanded) 0.dp else letterSide + 4.dp,
                            bottom = bars.calculateBottomPadding() + SEARCH_BUTTON_BOTTOM,
                        ),
                )
            }
        }

        // ---------------- RICERCA ----------------
        AnimatedVisibility(
            visible = overlay == Overlay.SEARCH,
            enter = fadeIn(Motion.enter(260)) + slideInVertically(Motion.offsetEnter) { -it / 12 },
            exit = fadeOut(Motion.exit()) + slideOutVertically(Motion.offsetExit) { -it / 16 },
        ) {
            Box(
                Modifier
                    .graphicsLayer { scaleX = panelScale; scaleY = panelScale }
                    .clip(RoundedCornerShape(panelCorner))
            ) {
                SearchPanel(
                    results = search,
                    suggestions = searchPick.first,
                    suggestionsTitle = if (searchPick.second) R.string.search_frequent else R.string.search_most_used,
                    showIcons = settings.showIcons,
                    themedIcons = settings.themedIcons,
                    contactsPermission = status.contactsPermission,
                    onQueryChange = vm::updateSearch,
                    onApp = { vm.recordSearchOpen(it); launchApp(it) },
                    onAppLongClick = { openQuick(it) },
                    onContact = { vm.contacts.open(it); scope.launch { delay(380); closeAll() } },
                    onWebSearch = { SystemActions.webSearch(context, it, settings.searchEngine); scope.launch { delay(380); closeAll() } },
                    onOndaSetting = { entry -> openSettingsAt(entry) },
                    onSettingsShortcut = { sc ->
                        if (sc.action == com.gabrielepiccione.onda.util.SettingsShortcuts.ONDA) {
                            vm.clearSearch(); overlay = Overlay.SETTINGS
                        } else {
                            com.gabrielepiccione.onda.util.SettingsShortcuts.open(context, sc)
                            scope.launch { delay(380); closeAll() }
                        }
                    },
                    onRequestContacts = callbacks::requestContactsPermission,
                    onAction = { action ->
                        // Le azioni che aprono schermate di Onda chiudono prima la ricerca
                        vm.clearSearch()
                        perform(action)
                        if (action != GestureAction.ALL_APPS && action != GestureAction.SETTINGS && action != GestureAction.SEARCH) {
                            scope.launch { delay(380); closeAll() }
                        }
                    },
                    musicAppLabel = vm.musicApp().takeIf { it.isNotBlank() }?.let { pkg -> state.allApps.firstOrNull { it.packageName == pkg }?.label ?: pkg },
                    spotifyInstalled = state.allApps.any { it.packageName == SpotifyLinks.PACKAGE },
                    onPlay = { text ->
                        val pkg = vm.musicApp()
                        if (!vm.media.playFromSearch(pkg, text)) {
                            android.widget.Toast.makeText(context, R.string.music_play_unsupported, android.widget.Toast.LENGTH_SHORT).show()
                        }
                        scope.launch { delay(380); closeAll() }
                    },
                    onAddMedia = { item, name ->
                        vm.addMediaShortcut(item, name)
                        android.widget.Toast.makeText(context, R.string.media_added, android.widget.Toast.LENGTH_SHORT).show()
                        closeAll()
                    },
                    onSpotifySearch = { text ->
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(SpotifyLinks.search(text)))
                            .setPackage(SpotifyLinks.PACKAGE)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(intent) }
                        scope.launch { delay(380); closeAll() }
                    },
                    onClose = { closeAll() },
                )
            }
        }

        // ---------------- IMPOSTAZIONI ----------------
        AnimatedVisibility(
            visible = overlay == Overlay.SETTINGS,
            enter = fadeIn(Motion.enter(300)) + slideInVertically(Motion.offsetEnter) { it / 8 },
            exit = fadeOut(Motion.exit()) + slideOutVertically(Motion.offsetExit) { it / 12 },
        ) {
            Box(
                Modifier
                    .graphicsLayer { scaleX = panelScale; scaleY = panelScale }
                    .clip(RoundedCornerShape(panelCorner))
            ) {
                SettingsScreen(
                    settings = settings,
                    hiddenApps = state.hiddenApps,
                    allApps = state.allApps,
                    hasPrivateSpace = privateSpace != null,
                    status = SettingsStatus(
                        isDefaultLauncher = status.isDefaultLauncher,
                        notificationAccess = status.notificationAccess,
                        calendarPermission = status.calendarPermission,
                        accessibilityEnabled = status.accessibilityEnabled,
                        contactsPermission = status.contactsPermission,
                        weatherInstalled = status.weatherInstalled,
                        weatherPermission = status.weatherPermission,
                        microphonePermission = status.microphonePermission,
                        locationPermission = status.locationPermission,
                        usageAccess = status.usageAccess,
                        postNotifications = status.postNotifications,
                    ),
                    widgets = vm.widgets,
                    usageStats = vm.usageStats,
                    focus = settingsFocus,
                    suggestedProfile = profileToEdit,
                    connectedHeadphones = headphoneNames,
                    activeProfile = state.activeProfile,
                    contextEnvironment = object : ContextEnvironment {
                        override fun currentWifi() = HomePresence.currentSsid(context)
                        override fun currentLocation() = HomePresence.lastLocation(context)
                        override fun bondedDevices() = vm.contextReader.bondedDevices()
                        override fun requestLocation() { if (!status.locationPermission) callbacks.requestLocationPermission() }
                        override fun requestCalendar() { if (!status.calendarPermission) callbacks.requestCalendarPermission() }
                        override fun requestBluetooth() = callbacks.requestBluetoothPermission()
                    },
                    actions = object : SettingsActions {
                        override fun edit(transform: (com.gabrielepiccione.onda.data.Settings) -> com.gabrielepiccione.onda.data.Settings) = vm.updateSettings(transform)
                        override fun requestDefault() = callbacks.requestDefaultLauncher()
                        override fun notificationAccess() = withRestrictedHelp(callbacks::openNotificationAccess)
                        override fun calendarPermission() = callbacks.requestCalendarPermission()
                        override fun contactsPermission() = callbacks.requestContactsPermission()
                        override fun weatherPermission() = callbacks.requestWeatherPermission()
                        override fun locationPermission() = callbacks.requestLocationPermission()
                        override fun bluetoothPermission() = callbacks.requestBluetoothPermission()
                        override fun microphonePermission() = callbacks.requestMicrophonePermission()
                        override fun systemGrayscale() = callbacks.openSystemGrayscale()
                        override fun pickLightsWidget() = openWidgetPicker(HomePresence.GOOGLE_HOME_PACKAGE, WidgetTarget.HomeLights)
                        override fun currentWifi(): String? = HomePresence.currentSsid(context)
                        override fun saveHome(ssid: String) {
                            vm.updateSettings { it.copy(homeWifi = ssid) }
                            // La posizione serve una volta sola, per gli orari del sole: se non c'è, la si chiede ora
                            scope.launch {
                                HomePresence.locate(context)?.let { (lat, lon) ->
                                    vm.updateSettings { it.copy(homeLatitude = lat, homeLongitude = lon) }
                                }
                            }
                        }
                        override fun locateHome() {
                            scope.launch {
                                HomePresence.locate(context)?.let { (lat, lon) ->
                                    vm.updateSettings { it.copy(homeLatitude = lat, homeLongitude = lon) }
                                }
                            }
                        }
                        override fun restartLauncher() = callbacks.restartLauncher()
                        override fun postNotifications() = callbacks.requestPostNotifications()
                        override fun usageAccess() = callbacks.openUsageAccess()
                        override fun accessibility() = withRestrictedHelp(callbacks::openAccessibilitySettings)
                        override fun clearSearchHistory() = vm.clearSearchHistory()
                        override fun shortcutsOf(app: AppInfo) = vm.shortcuts(app)
                        override fun connectedDevices() = vm.audio.connectedNames.value
                        override fun addWidget() = openWidgetPicker(null, WidgetTarget.Home)
                        override fun removeWidget(id: Int) = vm.removeWidget(id)
                        override fun unhide(app: AppInfo) = vm.setHidden(app, false)
                        override fun authenticate(onSuccess: () -> Unit) = callbacks.authenticate(context.getString(R.string.section_hidden), onSuccess)
                        override fun wallpaper() = SystemActions.openWallpaperPicker(context)
                        // Senza immagine di base si sceglie prima quella: a musica spenta si vedrà lei
                        override fun musicWallpaper() {
                            if (vm.settingsRepo.current.musicWallpaperBase.isBlank()) callbacks.pickWallpaperBase(true) else SystemActions.openMusicWallpaper(context)
                        }
                        override fun pickWallpaperBase() = callbacks.pickWallpaperBase(false)
                        override fun openStyles() { overlay = Overlay.STYLES }
                        override fun importFont(forClock: Boolean) = callbacks.importFont(forClock)
                        override fun exportBackup() = callbacks.exportBackup()
                        override fun importBackup() = callbacks.importBackup()
                        override fun chooseBackupFolder() = callbacks.chooseBackupFolder()
                        override fun disableAutoBackup() = callbacks.disableAutoBackup()
                        override fun shareBackup() = callbacks.shareBackup()
                        override fun close() = closeAll()
                    },
                )
            }
        }

        // ---------------- ANNULLA DOPO UN PRESET ----------------
        undoLook?.let { (look, previousPreset) ->
            LaunchedEffect(look) {
                delay(UNDO_VISIBLE_MS)
                undoLook = null
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(3f)
                    .padding(start = 16.dp, end = 16.dp, bottom = bars.calculateBottomPadding() + 16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.panelHigh)
                    .padding(start = 20.dp, end = 8.dp),
            ) {
                Text(
                    stringResource(if (undoKeepsWallpaper) R.string.styles_applied_wallpaper else R.string.styles_applied),
                    color = colors.onWallpaper,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f, fill = false),
                )
                androidx.compose.material3.TextButton(onClick = {
                    vm.updateSettings { look.applyTo(it).copy(stylePreset = previousPreset) }
                    undoLook = null
                }) { Text(stringResource(R.string.undo)) }
            }
        }
    }

    // ---------------- GALLERIA STILI (fuori dal Box: copre tutto) ----------------
    AnimatedVisibility(
        visible = overlay == Overlay.STYLES,
        enter = fadeIn(Motion.enter(300)) + slideInVertically(Motion.offsetEnter) { it / 8 },
        exit = fadeOut(Motion.exit()) + slideOutVertically(Motion.offsetExit) { it / 12 },
    ) {
        StyleGallery(
            currentPresetId = settings.stylePreset,
            onApply = { preset, withWallpaper ->
                val before = vm.settingsRepo.current
                undoLook = com.gabrielepiccione.onda.data.LookTheme.capture(before, "undo", "undo") to before.stylePreset
                undoKeepsWallpaper = withWallpaper
                vm.updateSettings { preset.applyTo(it) }
                if (withWallpaper) {
                    scope.launch {
                        com.gabrielepiccione.onda.data.StylePresets.applyWallpaper(context, preset)
                    }
                }
                overlay = Overlay.NONE
            },
            onClose = { overlay = Overlay.SETTINGS },
            savedThemes = settings.savedThemes,
            studio = object : ThemeStudioActions {
                override fun saveCurrent(name: String) { vm.settingsRepo.saveCurrentAsTheme(name) }
                override fun apply(theme: com.gabrielepiccione.onda.data.LookTheme) = vm.settingsRepo.applyTheme(theme)
                override fun rename(id: String, name: String) = vm.settingsRepo.renameTheme(id, name)
                override fun duplicate(id: String) = vm.settingsRepo.duplicateTheme(id, context.getString(R.string.studio_copy_suffix))
                override fun delete(id: String) = vm.settingsRepo.deleteTheme(id)
                override fun export(theme: com.gabrielepiccione.onda.data.LookTheme) = callbacks.exportTheme(theme)
                override fun import() = callbacks.importTheme()
            },
        )
    }

    // ---------------- BENVENUTO AL PRIMO AVVIO ----------------
    if (state.loaded && !settings.welcomeDone) {
        OnboardingScreen(
            isDefaultLauncher = status.isDefaultLauncher,
            notificationAccess = status.notificationAccess,
            onRequestDefault = callbacks::requestDefaultLauncher,
            onNotificationAccess = { withRestrictedHelp(callbacks::openNotificationAccess) },
            onShow = { entry ->
                // "Mostrami": il benvenuto si chiude e le impostazioni si aprono sulla voce, che lampeggia
                vm.updateSettings { it.copy(welcomeDone = true) }
                openSettingsAt(entry)
            },
            onDone = { vm.updateSettings { it.copy(welcomeDone = true) } },
        )
    }

    // ---------------- POPUP DELL'APP (copre tutto, all'altezza della riga) ----------------
    quickFor?.let { requested ->
        val app = state.app(requested.key) ?: requested
        val shortcuts = remember(app.key) { vm.shortcuts(app) }
        val popup = AppPopups.of(settings, app.key)
        AppQuickPopup(
            collapseExtras = settings.hideShortcutsWithNotifications,
            app = app,
            anchor = quickAnchor,
            shortcuts = shortcuts.filterNot { it.id in popup.hiddenShortcuts },
            shortcutIcon = { vm.appsRepo.shortcutIcon(it) },
            notifications = state.notifications[app.packageName].orEmpty(),
            widgetIds = AppPopups.widgetsOf(settings, app.key),
            relatedApps = popup.relatedApps.mapNotNull { state.app(it) },
            onRelatedApp = { launchApp(it) },
            customActions = popup.actions.map { it to actionSummary(Actions.decode(it), state.allApps, "") },
            onCustomAction = { performEncoded(it) },
            onEdit = { popupEditFor = app },
            widgets = vm.widgets,
            themedIcons = settings.themedIcons,
            onOpenApp = { launchApp(app) },
            onShortcut = { sc -> guarded(app) { vm.startShortcut(sc) } },
            onNotification = { openNotification(it) },
            onDismissNotification = { NotificationStore.dismiss(it) },
            onNotificationAction = { n, act -> NotificationStore.runAction(context, n, act) },
            onReply = { panelFor = app },
            onToggleFavorite = {
                val folder = settings.folders.values.firstOrNull { app.key in it.apps }
                if (folder != null) vm.moveToFolder(app, null) else vm.toggleFavorite(app)
            },
            onInfo = { vm.appsRepo.openAppInfo(app) },
            onUninstall = { SystemActions.uninstall(context, app) },
            onMoreOptions = { actionsFor = app },
            onStopSuggesting = if (state.homeSuggestions.any { it.key == app.key }) {
                { vm.updateSettings { HomeSuggestions.dismiss(it, app.key) } }
            } else {
                null
            },
            onDismiss = { quickFor = null },
        )
    }

    popupEditFor?.let { requested ->
        val app = state.app(requested.key) ?: requested
        val shortcuts = remember(app.key) { vm.shortcuts(app) }
        AppPopupEditSheet(
            app = app,
            popup = AppPopups.of(settings, app.key),
            widgetIds = AppPopups.widgetsOf(settings, app.key),
            canAddWidget = AppPopups.canAddWidget(settings, app.key),
            widgetLabel = { id -> vm.widgets.infoFor(id)?.let { i -> runCatching { i.loadLabel(context.packageManager) }.getOrNull() } },
            allApps = state.allApps,
            appOf = { state.app(it) },
            shortcuts = shortcuts,
            themedIcons = settings.themedIcons,
            actions = object : AppPopupEditActions {
                override fun addRelated(key: String) = vm.updateSettings { AppPopups.addRelated(it, app.key, key) }
                override fun removeRelated(key: String) = vm.updateSettings { AppPopups.removeRelated(it, app.key, key) }
                override fun addWidget() {
                    popupEditFor = null
                    openWidgetPicker(null, WidgetTarget.ToPopup(app.key))
                }
                override fun removeWidget(id: Int) = vm.removePopupWidget(app, id)
                override fun toggleShortcut(id: String) = vm.updateSettings { AppPopups.toggleShortcut(it, app.key, id) }
                override fun addAction(encoded: String) = vm.updateSettings { AppPopups.addAction(it, app.key, encoded) }
                override fun removeAction(encoded: String) = vm.updateSettings { AppPopups.removeAction(it, app.key, encoded) }
            },
            onDismiss = { popupEditFor = null },
            shortcutsOf = { vm.shortcuts(it) },
            accessibilityEnabled = status.accessibilityEnabled,
            onAccessibility = { withRestrictedHelp(callbacks::openAccessibilitySettings) },
        )
    }

    // ---------------- FOGLI MODALI ----------------
    panelFor?.let { app ->
        val shortcuts = remember(app.key) { vm.shortcuts(app) }
        AppPanelSheet(
            app = app,
            shortcuts = shortcuts,
            shortcutIcon = { vm.appsRepo.shortcutIcon(it) },
            notifications = state.notifications[app.packageName].orEmpty(),
            widgetId = settings.appWidgets[app.key],
            widgets = vm.widgets,
            themedIcons = settings.themedIcons,
            onOpenApp = { launchApp(app) },
            onShortcut = { sc -> guarded(app) { vm.startShortcut(sc) } },
            onNotification = { openNotification(it) },
            onDismissNotification = { NotificationStore.dismiss(it) },
            onReplyNotification = { n, text -> NotificationStore.reply(context, n, text) },
            onNotificationAction = { n, act -> NotificationStore.runAction(context, n, act) },
            onAddWidget = { openWidgetPicker(app.packageName, WidgetTarget.ForApp(app.key)) },
            onRemoveWidget = { vm.removeAppWidget(app) },
            onMoreOptions = { actionsFor = app },
            onDismiss = { panelFor = null },
        )
    }

    actionsFor?.let { app ->
        val favIndex = settings.favorites.indexOf(app.key)
        val shortcuts = remember(app.key) { vm.shortcuts(app) }
        val inFolder = settings.folders.values.firstOrNull { app.key in it.apps }
        AppActionsSheet(
            app = app,
            actions = AppActions(
                shortcuts = shortcuts,
                shortcutIcon = { vm.appsRepo.shortcutIcon(it) },
                swipe = Actions.decode(app.swipeTarget),
                allApps = state.allApps.filter { it.key != app.key },
                themedIcons = settings.themedIcons,
                onSetSwipe = { vm.setSwipeAction(app, Actions.encode(it)) },
                shortcutsOf = { vm.shortcuts(it) },
                accessibilityEnabled = status.accessibilityEnabled,
                onAccessibility = { withRestrictedHelp(callbacks::openAccessibilitySettings) },
                folderName = inFolder?.name,
                onMoveToFolder = { moveFor = app },
                onChangeIcon = { iconPickerFor = app },
                isProtected = app.key in settings.protectedApps,
                onToggleProtected = {
                    // Togliere la protezione richiede comunque lo sblocco
                    if (app.key in settings.protectedApps) callbacks.authenticate(app.label) { vm.settingsRepo.toggleProtected(app.key) }
                    else vm.settingsRepo.toggleProtected(app.key)
                },
                hasBreaker = app.key in settings.breakerApps,
                onToggleBreaker = { vm.updateSettings { com.gabrielepiccione.onda.data.UsageBreaker.toggle(it, app.key) } },
                hasReminder = app.key in settings.reminderApps,
                onToggleReminder = {
                    vm.updateSettings { UsageReminders.toggle(it, app.key) }
                    // Attivandola si passa dalle impostazioni, dove si vede quali permessi mancano
                    if (app.key !in settings.reminderApps && !(status.usageAccess && status.postNotifications && status.notificationAccess)) {
                        overlay = Overlay.SETTINGS
                    }
                },
                isMuted = app.packageName in settings.mutedNotifications,
                favoriteIndex = favIndex,
                favoriteCount = settings.favorites.size,
                onShortcut = { sc -> guarded(app) { vm.startShortcut(sc) } },
                onToggleFavorite = {
                    if (inFolder != null) vm.moveToFolder(app, null) else vm.toggleFavorite(app)
                },
                onMove = { delta -> vm.moveFavorite(app, delta) },
                onRename = { vm.rename(app, it) },
                onToggleHidden = { vm.setHidden(app, !app.isHidden) },
                onToggleMuted = { vm.toggleMuted(app) },
                onAddWidget = { openWidgetPicker(app.packageName, WidgetTarget.Home) },
                onInfo = { vm.appsRepo.openAppInfo(app) },
                onUninstall = { SystemActions.uninstall(context, app) },
            ),
            onDismiss = { actionsFor = null },
        )
    }

    iconPickerFor?.let { requested ->
        val app = state.app(requested.key) ?: requested
        IconPickerSheet(
            app = app,
            packs = vm.appsRepo.packs,
            onPick = { pkg, name -> vm.settingsRepo.setIconOverride(app.key, pkg, name) },
            onReset = {
                vm.settingsRepo.setIconOverride(app.key, null, null)
                vm.settingsRepo.setIconShape(app.key, null)
            },
            onDismiss = { iconPickerFor = null },
            currentShape = settings.iconShapeOverrides[app.key],
            onShape = { vm.settingsRepo.setIconShape(app.key, it) },
            onPickImage = { callbacks.pickIconImage(app.key) },
            themedIcons = settings.themedIcons,
        )
    }

    // Icona di una scorciatoia o web app: stessa schermata e stessa logica delle app, con la chiave della scorciatoia
    shortcutIconFor?.let { entry ->
        val key = entry.pinned.key
        val stand = remember(entry) {
            AppInfo(
                key = key,
                component = ComponentName(entry.pinned.packageName, SHORTCUT_COMPONENT),
                user = android.os.Process.myUserHandle(),
                label = entry.info.label,
                originalLabel = entry.info.label,
                icon = entry.info.icon ?: ImageBitmap(ICON_FALLBACK_PX, ICON_FALLBACK_PX),
                mono = null,
                isWorkProfile = false,
                isFavorite = true,
                isHidden = false,
                swipeTarget = null,
            )
        }
        IconPickerSheet(
            app = stand,
            packs = vm.appsRepo.packs,
            onPick = { pkg, name -> vm.settingsRepo.setIconOverride(key, pkg, name) },
            onReset = {
                vm.settingsRepo.setIconOverride(key, null, null)
                vm.settingsRepo.setIconShape(key, null)
            },
            onDismiss = { shortcutIconFor = null },
            currentShape = settings.iconShapeOverrides[key],
            onShape = { vm.settingsRepo.setIconShape(key, it) },
            onPickImage = { callbacks.pickIconImage(key) },
            themedIcons = settings.themedIcons,
        )
    }

    if (reorderOpen) {
        HomeReorderSheet(
            blocks = blocks.map { block ->
                val widgetId = HomeBlocks.widgetId(block)
                block to when {
                    block == HomeBlocks.CLOCK -> stringResource(R.string.edit_clock)
                    block == HomeBlocks.MEDIA -> stringResource(R.string.edit_media)
                    block == HomeBlocks.AGENDA -> stringResource(R.string.agenda_title)
                    block == HomeBlocks.NOTIFICATIONS -> stringResource(R.string.summary_title)
                    block == HomeBlocks.APPS -> stringResource(R.string.reorder_favorites)
                    block.startsWith(HomeBlocks.STACK_PREFIX) -> stringResource(R.string.edit_stack)
                    widgetId != null ->
                        vm.widgets.infoFor(widgetId)?.let { i -> runCatching { i.loadLabel(context.packageManager) }.getOrNull() }
                            ?: stringResource(R.string.widget_number, widgetId)
                    else -> block
                }
            },
            entries = state.home.map { entry ->
                when (entry) {
                    is HomeEntry.App -> entry.app.key to entry.app.label
                    is HomeEntry.Group -> (FOLDER_PREFIX + entry.folder.id) to entry.folder.name
                    is HomeEntry.Shortcut -> (SHORTCUT_PREFIX + entry.pinned.key) to entry.info.label
                    is HomeEntry.Media -> (MEDIA_PREFIX + entry.shortcut.id) to entry.shortcut.name
                }
            },
            onBlocks = { vm.settingsRepo.setHomeLayout(it) },
            onEntries = { vm.settingsRepo.setHomeEntries(it) },
            onDismiss = { reorderOpen = false },
        )
    }

    moveFor?.let { app ->
        MoveToFolderDialog(
            folders = settings.folders.values.toList(),
            currentFolderId = settings.folders.values.firstOrNull { app.key in it.apps }?.id,
            onPick = { id -> vm.moveToFolder(app, id); moveFor = null },
            onCreate = { name ->
                vm.createFolder(name, app)
                moveFor = null
            },
            onDismiss = { moveFor = null },
        )
    }

    if (recentSheet) {
        RecentAppsSheet(
            apps = state.recentlyInstalled,
            themedIcons = settings.themedIcons,
            onApp = { recentSheet = false; launchApp(it) },
            onDismiss = { recentSheet = false },
        )
    }

    if (agendaOpen) {
        AgendaSheet(
            weather = weather,
            weatherHint = when {
                !settings.showWeather -> null
                !status.weatherInstalled -> stringResource(R.string.weather_needs_app)
                !status.weatherPermission -> stringResource(R.string.weather_needs_permission)
                else -> stringResource(R.string.weather_no_data)
            },
            events = agendaWeek,
            onEvent = { vm.calendar.open(it) },
            onEmptyAgenda = { if (!status.calendarPermission) callbacks.requestCalendarPermission() else SystemActions.openCalendar(context) },
            onOpenWeather = { vm.weatherRepo.open() },
            onOpenCalendar = { SystemActions.openCalendar(context) },
            onDismiss = { agendaOpen = false },
        )
    }

    // Voce multimediale (playlist, album…): sposta, rinomina, togli
    if (lightsEdit && settings.homeLightsWidget > 0) {
        val id = settings.homeLightsWidget
        val key = WidgetSizing.widgetKey(id)
        ModalBottomSheet(
            onDismissRequest = { lightsEdit = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = colors.onWallpaper,
            scrimColor = Color.Transparent,
        ) {
            Column(Modifier.navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 16.dp)) {
                Text(stringResource(R.string.section_lights), style = MaterialTheme.typography.headlineSmall)
                WidgetSizeControls(
                    size = settings.widgetSizes[key] ?: WidgetSize(),
                    height = settings.widgetHeights[id] ?: LIGHTS_HEIGHT,
                    defaultHeight = LIGHTS_HEIGHT,
                    maxWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp / 2,
                    onSize = { size -> vm.updateSettings { WidgetSizing.set(it, key, size) } },
                    onHeight = { h -> vm.updateSettings { s -> s.copy(widgetHeights = if (h == null) s.widgetHeights - id else s.widgetHeights + (id to h)) } },
                )
            }
        }
    }

    mediaMenu?.let { entry ->
        val favoriteId = MEDIA_PREFIX + entry.shortcut.id
        val index = settings.favorites.indexOf(favoriteId)
        ModalBottomSheet(
            onDismissRequest = { mediaMenu = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = colors.onWallpaper,
        ) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
                Text(entry.shortcut.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                if (index > 0) SheetRow(Icons.Filled.KeyboardArrowUp, stringResource(R.string.move_up)) { vm.moveEntry(favoriteId, -1) }
                if (index in 0 until settings.favorites.lastIndex) {
                    SheetRow(Icons.Filled.KeyboardArrowDown, stringResource(R.string.move_down)) { vm.moveEntry(favoriteId, 1) }
                }
                SheetRow(Icons.Filled.Edit, stringResource(R.string.rename)) { mediaRename = entry; mediaMenu = null }
                SheetRow(Icons.Filled.Delete, stringResource(R.string.pin_remove)) {
                    vm.updateSettings { MediaShortcuts.remove(it, entry.shortcut.id) }
                    mediaMenu = null
                }
            }
        }
    }
    mediaRename?.let { entry ->
        NameDialog(stringResource(R.string.rename), entry.shortcut.name, onDismiss = { mediaRename = null }) { name ->
            vm.updateSettings { MediaShortcuts.rename(it, entry.shortcut.id, name) }
            mediaRename = null
        }
    }

    shortcutMenu?.let { entry ->
        val favoriteId = SHORTCUT_PREFIX + entry.pinned.key
        val index = settings.favorites.indexOf(favoriteId)
        ModalBottomSheet(
            onDismissRequest = { shortcutMenu = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = colors.onWallpaper,
        ) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
                Text(
                    entry.info.label,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                if (index > 0) SheetRow(Icons.Filled.KeyboardArrowUp, stringResource(R.string.move_up)) { vm.moveEntry(favoriteId, -1) }
                if (index in 0 until settings.favorites.lastIndex) {
                    SheetRow(Icons.Filled.KeyboardArrowDown, stringResource(R.string.move_down)) { vm.moveEntry(favoriteId, 1) }
                }
                SheetRow(Icons.Filled.Palette, stringResource(R.string.icon_change)) {
                    shortcutIconFor = entry
                    shortcutMenu = null
                }
                SheetRow(Icons.Filled.Delete, stringResource(R.string.pin_remove)) {
                    vm.removePinnedShortcut(entry.pinned)
                    shortcutMenu = null
                }
            }
        }
    }

    folderSheet?.let { f ->
        val live = settings.folders[f.id] ?: f
        val idx = settings.favorites.indexOf(FOLDER_PREFIX + f.id)
        FolderSheet(
            folder = live,
            widgets = vm.widgets,
            canMoveUp = idx > 0,
            canMoveDown = idx in 0 until settings.favorites.lastIndex,
            onRename = { vm.renameFolder(f.id, it) },
            onMove = { vm.moveEntry(FOLDER_PREFIX + f.id, it) },
            onAddWidget = { openWidgetPicker(null, WidgetTarget.InFolder(f.id)) },
            onRemoveWidget = { vm.removeFolderWidget(f.id, it) },
            onDelete = { vm.deleteFolder(f.id) },
            onDismiss = { folderSheet = null },
            onChangeIcon = { folderIconFor = f.id },
            onWidgetHeight = { vm.settingsRepo.setFolderWidgetHeight(f.id, it) },
            allApps = state.allApps,
            shortcutsOf = { vm.shortcuts(it) },
            themedIcons = settings.themedIcons,
            accessibilityEnabled = status.accessibilityEnabled,
            onAccessibility = { withRestrictedHelp(callbacks::openAccessibilitySettings) },
            onSwipe = { vm.settingsRepo.setFolderSwipe(f.id, Actions.encode(it)) },
        )
    }

    folderIconFor?.let { id ->
        val folder = settings.folders[id]
        if (folder == null) {
            folderIconFor = null
        } else {
            FolderIconSheet(
                folder = folder,
                apps = state.folders.firstOrNull { it.folder.id == id }?.apps.orEmpty(),
                themedIcons = settings.themedIcons,
                packs = vm.appsRepo.packs,
                onPick = { vm.settingsRepo.setFolderIcon(id, it) },
                onPickPackIcon = { pkg, name ->
                    scope.launch {
                        savePackIconForFolder(context, vm.appsRepo.packs, id, pkg, name)?.let { vm.settingsRepo.setFolderIcon(id, it) }
                    }
                    folderIconFor = null
                },
                onPickImage = { callbacks.pickIconImage(FolderIcons.IMAGE_KEY_PREFIX + id) },
                onDismiss = { folderIconFor = null },
            )
        }
    }

    if (homeMenu) {
        var newFolder by remember { mutableStateOf(false) }
        ModalBottomSheet(
            onDismissRequest = { homeMenu = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = colors.onWallpaper,
        ) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
                Text(
                    stringResource(R.string.home_menu_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onWallpaper,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                // Le tre azioni più usate, di pari larghezza su una riga (la ricerca ha il suo pulsante)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MenuTile(Icons.Filled.Widgets, stringResource(R.string.home_menu_widget), Modifier.weight(1f), enabled = !settings.lockLayout) {
                        homeMenu = false
                        openWidgetPicker(null, WidgetTarget.Home)
                    }
                    MenuTile(Icons.Filled.Wallpaper, stringResource(R.string.home_menu_wallpaper), Modifier.weight(1f)) {
                        homeMenu = false
                        SystemActions.openWallpaperPicker(context)
                    }
                    MenuTile(Icons.Filled.Settings, stringResource(R.string.settings), Modifier.weight(1f)) { homeMenu = false; overlay = Overlay.SETTINGS }
                }
                if (!settings.lockLayout) {
                    MenuSection(stringResource(R.string.home_menu_add))
                    MenuItem(Icons.Filled.CreateNewFolder, stringResource(R.string.new_folder), stringResource(R.string.home_menu_folder_sub)) { newFolder = true }
                    if (!settings.showClock) {
                        MenuItem(Icons.Filled.Schedule, stringResource(R.string.show_clock_again), null) {
                            homeMenu = false
                            vm.updateSettings { it.copy(showClock = true) }
                        }
                    }
                    if (!settings.showNotificationSummary) {
                        MenuItem(Icons.Filled.Notifications, stringResource(R.string.summary_add), stringResource(R.string.summary_sub)) {
                            homeMenu = false
                            vm.updateSettings { it.copy(showNotificationSummary = true) }
                        }
                    }
                    if (!settings.showAgenda) {
                        MenuItem(Icons.Filled.Event, stringResource(R.string.agenda_add), stringResource(R.string.agenda_sub)) {
                            homeMenu = false
                            vm.updateSettings { it.copy(showAgenda = true) }
                            if (!status.calendarPermission) callbacks.requestCalendarPermission()
                        }
                    }
                }
                MenuSection(stringResource(R.string.home_menu_layout))
                if (!settings.lockLayout) {
                    MenuItem(Icons.Filled.SwapVert, stringResource(R.string.reorder_home), stringResource(R.string.home_menu_reorder_sub)) {
                        homeMenu = false
                        reorderOpen = true
                    }
                }
                MenuItem(
                    Icons.Filled.Lock,
                    stringResource(R.string.layout_lock),
                    stringResource(R.string.layout_lock_sub),
                    trailing = { Switch(checked = settings.lockLayout, onCheckedChange = null) },
                ) { vm.updateSettings { it.copy(lockLayout = !it.lockLayout) } }
            }
        }
        if (newFolder) {
            NameDialog(stringResource(R.string.new_folder), "", onDismiss = { newFolder = false }) { name ->
                vm.createFolder(name, null)
                newFolder = false
                homeMenu = false
            }
        }
    }

    editing?.let { block ->
        val density = LocalDensity.current
        HomeEditSheet(
            block = block,
            settings = settings,
            blocks = blocks,
            defaultWidgetHeight = { id ->
                vm.widgets.infoFor(id)?.let { with(density) { it.minHeight.toDp().value.toInt() } }?.coerceIn(72, 320) ?: 160
            },
            widgetLabel = { id -> vm.widgets.infoFor(id)?.let { i -> runCatching { i.loadLabel(context.packageManager) }.getOrNull() } },
            actions = object : HomeEditActions {
                override fun edit(transform: (com.gabrielepiccione.onda.data.Settings) -> com.gabrielepiccione.onda.data.Settings) = vm.updateSettings(transform)
                override fun move(block: String, delta: Int) = vm.settingsRepo.moveBlock(block, delta)
                override fun setWidgetHeight(id: Int, dp: Int) = vm.settingsRepo.setWidgetHeight(id, dp)
                override fun replaceWidget(id: Int) { editing = null; openWidgetPicker(null, WidgetTarget.Replace(id)) }
                override fun replaceClock() { editing = null; openWidgetPicker(null, WidgetTarget.ReplaceClock) }
                override fun addToStack(block: String) { editing = null; openWidgetPicker(null, WidgetTarget.ToStack(block)) }
                override fun setStackHeight(stackId: String, dp: Int) = vm.settingsRepo.setStackHeight(stackId, dp)
                override fun deleteWidget(id: Int) = vm.removeWidget(id)
                override fun done() { editing = null }
            },
        )
    }

    if (summaryOpen) {
        NotificationSummarySheet(
            notifications = state.notifications,
            appLabel = packageLabel,
            onNotification = { openNotification(it) },
            onDismissAll = { digest.packages.forEach(NotificationStore::dismissAll); summaryOpen = false },
            onDismiss = { summaryOpen = false },
        )
    }

    breakerFor?.let { request ->
        val app = request.app
        val today = remember(app.key) { java.time.LocalDate.now().toString() }
        var usedToday by remember(app.key) { mutableStateOf<Long?>(null) }
        LaunchedEffect(app.key) { if (status.usageAccess) usedToday = vm.usageStats.todayMs(app.packageName) }
        UsageBreakerDialog(
            app = app,
            seconds = settings.breakerSeconds,
            opensToday = com.gabrielepiccione.onda.data.UsageBreaker.opensToday(settings, app.key, today),
            usedTodayMs = usedToday,
            limitMinutes = settings.appLimits[app.packageName] ?: 0,
            onOpen = {
                breakerFor = null
                vm.updateSettings { com.gabrielepiccione.onda.data.UsageBreaker.recordOpen(it, app.key, today) }
                // L'app sta per aprirsi dopo la pausa: il servizio non deve chiederla di nuovo
                OndaAccessibilityService.allow(app.packageName)
                request.go()
            },
            onDismiss = { breakerFor = null },
        )
    }

    blockedNotice?.let { (label, hit) ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { blockedNotice = null },
            title = { Text(stringResource(R.string.block_title)) },
            text = {
                Text(
                    when {
                        hit.until != null -> stringResource(R.string.block_until, label, hit.name, AppBlocks.timeText(hit.until))
                        hit.name.isNotBlank() -> stringResource(R.string.block_all_day, label, hit.name)
                        else -> stringResource(R.string.block_manual, label)
                    },
                )
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { blockedNotice = null }) { Text(stringResource(R.string.ok)) } },
        )
    }

    restrictedHelp?.let { open ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { restrictedHelp = null },
            title = { Text(stringResource(R.string.restricted_title)) },
            text = { Text(stringResource(R.string.restricted_body)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { restrictedHelp = null; open() }) { Text(stringResource(R.string.try_again)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { restrictedHelp = null; callbacks.openOwnAppInfo() }) { Text(stringResource(R.string.open_app_info)) }
            },
        )
    }

    if (widgetPicker) {
        WidgetPickerSheet(
            widgets = vm.widgets,
            packageFilter = widgetFilter,
            onPick = { p -> widgetPicker = false; callbacks.pickWidget(p, widgetTarget) },
            onDismiss = { widgetPicker = false },
        )
    }
}

@Composable
private fun HintChip(icon: ImageVector, text: String, onClick: () -> Unit) = OndaChip(text, onClick, primary = true, icon = icon)

/** Dall'inizio della sezione al centro della sua prima app: lettera più metà riga. */
private val FIRST_ROW_CENTER = 72.dp

/** Altezza della sfumatura sotto la barra di stato, in cima all'elenco delle app. */
private val TOP_FADE = 40.dp

/** Apertura in attesa della pausa di Usage Breaker: cosa fare quando l'utente conferma. */
private class BreakerRequest(val app: AppInfo, val go: () -> Unit)

/** Nome fittizio del componente di una scorciatoia nel selettore delle icone. */
private const val SHORTCUT_COMPONENT = "shortcut"

/** Lato dell'icona vuota quando il sistema non fornisce quella della scorciatoia. */
private const val ICON_FALLBACK_PX = 96

/** Altezza del widget delle luci accanto all'orologio. */
private const val LIGHTS_HEIGHT = 120

/** Azione principale del pannello della home: riquadro tonale con icona ed etichetta. */
@Composable
private fun MenuTile(icon: ImageVector, label: String, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = LocalOndaColors.current
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Molla breve sull'indicatore quando lo si preme
    val scale by animateFloatAsState(if (pressed) PRESSED_SCALE else 1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium), label = "menuTile")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = label }
            .graphicsLayer { alpha = if (enabled) 1f else DISABLED_ALPHA }
            .padding(vertical = 8.dp),
    ) {
        // Indicatore a pillola di Material 3: 64×40, colore secondario tonale
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 64.dp, height = 40.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(scheme.secondaryContainer)
                .indication(interaction, ripple()),
        ) {
            Icon(icon, null, tint = scheme.onSecondaryContainer, modifier = Modifier.size(24.dp))
        }
        // Etichetta intera su una riga: mai troncata
        Text(label, color = colors.onWallpaper, style = MaterialTheme.typography.labelLarge, softWrap = false, textAlign = TextAlign.Center)
    }
}

/** Titolo di una sezione del pannello: etichetta grande nel colore primario. */
@Composable
private fun MenuSection(title: String) {
    Text(
        title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 4.dp),
    )
}

/** Voce del pannello (ListItem di Material 3): icona in un cerchio tonale da 40dp, titolo, descrizione e controllo. */
@Composable
private fun MenuItem(icon: ImageVector, title: String, supporting: String?, trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    val colors = LocalOndaColors.current
    val scheme = MaterialTheme.colorScheme
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = supporting?.let { { Text(it, style = MaterialTheme.typography.bodyMedium) } },
        leadingContent = { TonalIcon(icon) },
        trailingContent = trailing,
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = colors.onWallpaper,
            supportingColor = colors.onWallpaperMuted,
        ),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp),
    )
}

private const val PRESSED_SCALE = 0.92f
private const val DISABLED_ALPHA = 0.38f
