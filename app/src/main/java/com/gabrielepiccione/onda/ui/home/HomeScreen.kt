// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(ExperimentalMaterial3Api::class)

package com.gabrielepiccione.onda.ui.home

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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import com.gabrielepiccione.onda.data.GestureAction
import com.gabrielepiccione.onda.data.NotificationStore
import com.gabrielepiccione.onda.data.WidgetManager
import com.gabrielepiccione.onda.service.OndaAccessibilityService
import com.gabrielepiccione.onda.util.IcuSectionIndex
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
}

/** Azioni che richiedono l'Activity (permessi, risultati di altre schermate). */
interface HomeCallbacks {
    fun requestContactsPermission()
    fun requestCalendarPermission()
    fun requestWeatherPermission()
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
    val usageAccess: Boolean = false,
    val postNotifications: Boolean = false,
)

@Composable
fun HomeScreen(vm: LauncherViewModel, callbacks: HomeCallbacks, status: HomeStatus) {
    val state by vm.state.collectAsStateWithLifecycle()
    val search by vm.search.collectAsStateWithLifecycle()
    val media by vm.media.media.collectAsStateWithLifecycle()
    val nextEvent by vm.nextEvent.collectAsStateWithLifecycle()
    val agenda by vm.agenda.collectAsStateWithLifecycle()
    val headphones by vm.audio.headphonesConnected.collectAsStateWithLifecycle()
    val settings = state.settings
    val colors = LocalOndaColors.current
    val context = LocalContext.current
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

    var breakerFor by remember { mutableStateOf<AppInfo?>(null) }

    fun startApp(app: AppInfo) {
        val go = {
            vm.launch(app)
            scope.launch { delay(380); closeAll() }
        }
        // App protette: prima lo sblocco (impronta, volto o PIN)
        if (app.key in vm.settingsRepo.current.protectedApps) callbacks.authenticate(app.label) { go() } else go()
    }

    fun launchApp(app: AppInfo) {
        // Usage Breaker: prima la pausa, poi l'eventuale sblocco
        if (app.key in vm.settingsRepo.current.breakerApps) breakerFor = app else startApp(app)
    }

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

    fun perform(action: GestureAction, appKey: String = "") {
        when (action) {
            GestureAction.SEARCH -> overlay = Overlay.SEARCH
            GestureAction.NOTIFICATIONS -> SystemActions.expandNotifications(context)
            GestureAction.ALL_APPS -> overlay = Overlay.ALL_APPS
            GestureAction.LOCK_SCREEN -> if (!OndaAccessibilityService.lockScreen()) withRestrictedHelp(callbacks::openAccessibilitySettings)
            GestureAction.SETTINGS -> overlay = Overlay.SETTINGS
            GestureAction.OPEN_APP -> state.app(appKey)?.let { launchApp(it) }
            GestureAction.NONE -> Unit
        }
    }

    LaunchedEffect(state.loaded) { if (state.loaded) vm.seedFavoritesIfNeeded() }
    LaunchedEffect(settings.showAgenda, settings.agendaDays, status.calendarPermission) { vm.refreshCalendar() }
    LaunchedEffect(settings.contactsInAlphabet, status.contactsPermission) { vm.refreshAlphabetContacts() }
    LaunchedEffect(settings.showWeather, status.weatherPermission, status.weatherInstalled) { vm.refreshWeather(force = true) }

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
        androidx.compose.animation.core.tween(140, easing = Motion.Standard),
        label = "listMode",
    )
    val panelScale = 1f - 0.1f * backProgress
    val panelCorner = (32 * backProgress).dp

    // Voci della home (preferiti e cartelle), inserite nella posizione del blocco "app"
    fun androidx.compose.foundation.lazy.LazyListScope.homeEntries() {
        items(state.home, key = { it.key }) { entry ->
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
                        onOpenPanel = { openQuick(app, "h_" + app.key) },
                        onSwipeLeft = { NotificationStore.dismissAll(app.packageName) },
                        onNotificationClick = { openQuick(app, "h_" + app.key) },
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
                    modifier = itemMod.then(rowTracker("g_" + entry.folder.id)),
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
    var summaryOpen by remember { mutableStateOf(false) }
    val digest = remember(state.notifications) { com.gabrielepiccione.onda.data.NotificationDigest.of(state.notifications) }
    val packageLabel: (String) -> String = { pkg -> state.allApps.firstOrNull { it.packageName == pkg }?.label ?: pkg }
    // Il blocco in modifica può sparire (es. una pila ridotta a un solo widget)
    LaunchedEffect(blocks, editing) { if (editing != null && editing !in blocks) editing = null }

    val dimColor = if (colors.dark) Color.Black else Color.White
    Box(
        Modifier
            .fillMaxSize()
            .background(dimColor.copy(alpha = settings.wallpaperDim))
    ) {
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
            verticalArrangement = Arrangement.spacedBy(2.dp, if (manualTop) Alignment.Top else Alignment.Bottom),
            // Orologio, widget e player: stesso margine a destra e a sinistra.
            // Solo le righe delle app si tengono lontane dalle lettere (vedi homeEntries).
            contentPadding = PaddingValues(
                start = hiddenSide,
                end = hiddenSide,
                top = bars.calculateTopPadding() + 24.dp + chosenTop,
                bottom = bars.calculateBottomPadding() + 28.dp,
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
            // Blocchi della home nell'ordine scelto dall'utente (orologio, musica, widget, app)
            for (block in blocks) {
                when {
                    block == HomeBlocks.CLOCK -> if (settings.showClock) {
                        item(key = "clock") {
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
                            modifier = Modifier.animateItem(placementSpec = Motion.offsetEnter).padding(vertical = 8.dp),
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
                                        highlighted = editing == block,
                                        onLongPress = { if (!settings.lockLayout) editing = block },
                                    )
                                }
                            }
                        }
                    }
                    block == HomeBlocks.APPS -> {
                        item(key = "spacer") { Spacer(Modifier.size(12.dp)) }
                        // Plug & Play: con cuffie o casse collegate, l'app scelta compare in cima
                        val plug = if (headphones) state.app(settings.plugPlayApp) else null
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
                                    onNotificationClick = { openQuick(app, rowId) },
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
                    when (action) {
                        GestureAction.SEARCH -> Icons.Filled.Search
                        GestureAction.NOTIFICATIONS -> Icons.Filled.Notifications
                        GestureAction.ALL_APPS -> Icons.Filled.List
                        GestureAction.LOCK_SCREEN -> Icons.Filled.Lock
                        GestureAction.SETTINGS -> Icons.Filled.Settings
                        GestureAction.OPEN_APP -> Icons.Filled.Apps
                        GestureAction.NONE -> Icons.Filled.Close
                    },
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
                .graphicsLayer { alpha = listMode }
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
        // L'inizio della lettera scelta si porta all'altezza del dito (non in cima allo schermo),
        // così la prima app è subito sotto il pollice; vicino agli estremi l'elenco si ferma da solo.
        val listTopPx = with(density) { (bars.calculateTopPadding() + 16.dp).toPx() }
        val headerHalfPx = with(density) { 24.dp.toPx() }
        val onLetter: (String, Float) -> Unit = onLetter@{ letter, fingerY ->
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
            if (target != null) {
                val lift = (fingerY - listTopPx - headerHalfPx).coerceAtLeast(0f)
                scope.launch {
                    allAppsList.scrollToItem(target)
                    allAppsList.scrollBy(-lift)
                }
            }
        }
        if (scrollerAlpha > 0.01f) {
            AlphabetWaveScroller(
                letters = letters,
                leftSide = settings.leftHanded,
                showLetters = lettersVisible,
                letterScale = settings.alphabetScale,
                maxLetterStep = settings.alphabetStepDp.dp,
                modifier = Modifier
                    .align(if (settings.leftHanded) Alignment.CenterStart else Alignment.CenterEnd)
                    .zIndex(2f)
                    .graphicsLayer { alpha = scrollerAlpha }
                    .then(scrollerPadding)
                    .padding(horizontal = 6.dp),
                onTouchStart = { overlay = Overlay.ALL_APPS },
                onLetter = onLetter,
                onDoubleTap = lockFromAlphabet,
            )
            AlphabetWaveScroller(
                letters = letters,
                leftSide = !settings.leftHanded,
                showLetters = false,
                maxLetterStep = settings.alphabetStepDp.dp,
                modifier = Modifier
                    .align(if (settings.leftHanded) Alignment.CenterEnd else Alignment.CenterStart)
                    .zIndex(2f)
                    .then(scrollerPadding),
                onTouchStart = { overlay = Overlay.ALL_APPS },
                onLetter = onLetter,
                onDoubleTap = lockFromAlphabet,
            )
            if (settings.showSearchButton) {
                Box(
                    contentAlignment = Alignment.Center,
                    // Fluttua accanto alla colonna dell'alfabeto, staccato dai bordi e velato sullo sfondo
                    modifier = Modifier
                        .align(if (settings.leftHanded) Alignment.BottomStart else Alignment.BottomEnd)
                        .zIndex(3f)
                        .graphicsLayer { alpha = scrollerAlpha }
                        .padding(
                            start = if (settings.leftHanded) letterSide + 4.dp else 0.dp,
                            end = if (settings.leftHanded) 0.dp else letterSide + 4.dp,
                            bottom = bars.calculateBottomPadding() + 16.dp,
                        )
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(colors.accentContainer.copy(alpha = 0.6f))
                        .border(1.dp, colors.onWallpaper.copy(alpha = 0.15f), CircleShape)
                        .clickable { overlay = Overlay.SEARCH },
                ) {
                    Icon(Icons.Filled.Search, stringResource(R.string.search), tint = colors.onAccentContainer)
                }
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
                    suggestions = state.suggestions,
                    showIcons = settings.showIcons,
                    themedIcons = settings.themedIcons,
                    contactsPermission = status.contactsPermission,
                    onQueryChange = vm::updateSearch,
                    onApp = { launchApp(it) },
                    onAppLongClick = { openQuick(it) },
                    onContact = { vm.contacts.open(it); scope.launch { delay(380); closeAll() } },
                    onWebSearch = { SystemActions.webSearch(context, it, settings.searchEngine); scope.launch { delay(380); closeAll() } },
                    onSettingsShortcut = { sc ->
                        if (sc.action == com.gabrielepiccione.onda.util.SettingsShortcuts.ONDA) {
                            vm.clearSearch(); overlay = Overlay.SETTINGS
                        } else {
                            com.gabrielepiccione.onda.util.SettingsShortcuts.open(context, sc)
                            scope.launch { delay(380); closeAll() }
                        }
                    },
                    onRequestContacts = callbacks::requestContactsPermission,
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
                        usageAccess = status.usageAccess,
                        postNotifications = status.postNotifications,
                    ),
                    widgets = vm.widgets,
                    actions = object : SettingsActions {
                        override fun edit(transform: (com.gabrielepiccione.onda.data.Settings) -> com.gabrielepiccione.onda.data.Settings) = vm.updateSettings(transform)
                        override fun requestDefault() = callbacks.requestDefaultLauncher()
                        override fun notificationAccess() = withRestrictedHelp(callbacks::openNotificationAccess)
                        override fun calendarPermission() = callbacks.requestCalendarPermission()
                        override fun contactsPermission() = callbacks.requestContactsPermission()
                        override fun weatherPermission() = callbacks.requestWeatherPermission()
                        override fun postNotifications() = callbacks.requestPostNotifications()
                        override fun usageAccess() = callbacks.openUsageAccess()
                        override fun accessibility() = withRestrictedHelp(callbacks::openAccessibilitySettings)
                        override fun addWidget() = openWidgetPicker(null, WidgetTarget.Home)
                        override fun removeWidget(id: Int) = vm.removeWidget(id)
                        override fun unhide(app: AppInfo) = vm.setHidden(app, false)
                        override fun authenticate(onSuccess: () -> Unit) = callbacks.authenticate(context.getString(R.string.section_hidden), onSuccess)
                        override fun wallpaper() = SystemActions.openWallpaperPicker(context)
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
            onDone = { vm.updateSettings { it.copy(welcomeDone = true) } },
        )
    }

    // ---------------- POPUP DELL'APP (copre tutto, all'altezza della riga) ----------------
    quickFor?.let { requested ->
        val app = state.app(requested.key) ?: requested
        val shortcuts = remember(app.key) { vm.shortcuts(app) }
        val popup = AppPopups.of(settings, app.key)
        AppQuickPopup(
            app = app,
            anchor = quickAnchor,
            shortcuts = shortcuts.filterNot { it.id in popup.hiddenShortcuts },
            shortcutIcon = { vm.appsRepo.shortcutIcon(it) },
            notifications = state.notifications[app.packageName].orEmpty(),
            widgetIds = AppPopups.widgetsOf(settings, app.key),
            relatedApps = popup.relatedApps.mapNotNull { state.app(it) },
            onRelatedApp = { launchApp(it) },
            onEdit = { popupEditFor = app },
            widgets = vm.widgets,
            themedIcons = settings.themedIcons,
            onOpenApp = { launchApp(app) },
            onShortcut = vm::startShortcut,
            onNotification = { NotificationStore.open(context, it) },
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
            },
            onDismiss = { popupEditFor = null },
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
            onShortcut = vm::startShortcut,
            onNotification = { NotificationStore.open(context, it) },
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
                swipeTarget = state.app(app.swipeTarget),
                allApps = state.allApps.filter { it.key != app.key },
                themedIcons = settings.themedIcons,
                onSetSwipe = { vm.setSwipeAction(app, it) },
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
                onShortcut = vm::startShortcut,
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

    shortcutMenu?.let { entry ->
        val favoriteId = SHORTCUT_PREFIX + entry.pinned.key
        val index = settings.favorites.indexOf(favoriteId)
        ModalBottomSheet(
            onDismissRequest = { shortcutMenu = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.panel,
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
                onPick = { vm.settingsRepo.setFolderIcon(id, it) },
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
            containerColor = colors.panel,
            contentColor = colors.onWallpaper,
        ) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
                SheetRow(Icons.Filled.Settings, stringResource(R.string.settings)) { homeMenu = false; overlay = Overlay.SETTINGS }
                if (!settings.lockLayout) {
                    SheetRow(Icons.Filled.SwapVert, stringResource(R.string.reorder_home)) { homeMenu = false; reorderOpen = true }
                    SheetRow(Icons.Filled.CreateNewFolder, stringResource(R.string.new_folder)) { newFolder = true }
                    SheetRow(Icons.Filled.Widgets, stringResource(R.string.add_widget)) { homeMenu = false; openWidgetPicker(null, WidgetTarget.Home) }
                    if (!settings.showNotificationSummary) {
                        SheetRow(Icons.Filled.Notifications, stringResource(R.string.summary_add)) {
                            homeMenu = false
                            vm.updateSettings { it.copy(showNotificationSummary = true) }
                        }
                    }
                    if (!settings.showAgenda) {
                        SheetRow(Icons.Filled.Event, stringResource(R.string.agenda_add)) {
                            homeMenu = false
                            vm.updateSettings { it.copy(showAgenda = true) }
                            if (!status.calendarPermission) callbacks.requestCalendarPermission()
                        }
                    }
                }
                SheetRow(if (settings.lockLayout) Icons.Filled.LockOpen else Icons.Filled.Lock, stringResource(if (settings.lockLayout) R.string.layout_unlock else R.string.layout_lock)) {
                    homeMenu = false
                    vm.updateSettings { it.copy(lockLayout = !it.lockLayout) }
                }
                SheetRow(Icons.Filled.Search, stringResource(R.string.search)) { homeMenu = false; overlay = Overlay.SEARCH }
                if (!settings.showClock) {
                    SheetRow(Icons.Filled.Schedule, stringResource(R.string.show_clock_again)) {
                        homeMenu = false
                        vm.updateSettings { it.copy(showClock = true) }
                    }
                }
                SheetRow(Icons.Filled.Wallpaper, stringResource(R.string.change_wallpaper)) { homeMenu = false; SystemActions.openWallpaperPicker(context) }
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
            onNotification = { NotificationStore.open(context, it) },
            onDismissAll = { digest.packages.forEach(NotificationStore::dismissAll); summaryOpen = false },
            onDismiss = { summaryOpen = false },
        )
    }

    breakerFor?.let { app ->
        val today = remember(app.key) { java.time.LocalDate.now().toString() }
        UsageBreakerDialog(
            app = app,
            seconds = settings.breakerSeconds,
            opensToday = com.gabrielepiccione.onda.data.UsageBreaker.opensToday(settings, app.key, today),
            onOpen = {
                breakerFor = null
                vm.updateSettings { com.gabrielepiccione.onda.data.UsageBreaker.recordOpen(it, app.key, today) }
                startApp(app)
            },
            onDismiss = { breakerFor = null },
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
private fun HintChip(icon: ImageVector, text: String, onClick: () -> Unit) {
    val colors = LocalOndaColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.accentContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Icon(icon, null, tint = colors.onAccentContainer, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = colors.onAccentContainer, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, onClick: () -> Unit) {
    val colors = LocalOndaColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick)
    ) {
        Icon(icon, null, tint = colors.onWallpaper)
    }
}
