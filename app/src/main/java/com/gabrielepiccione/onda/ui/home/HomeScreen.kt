// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(ExperimentalMaterial3Api::class)

package com.gabrielepiccione.onda.ui.home

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrielepiccione.onda.HomeEntry
import com.gabrielepiccione.onda.LauncherViewModel
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.data.FOLDER_PREFIX
import com.gabrielepiccione.onda.data.HomeBlocks
import com.gabrielepiccione.onda.data.Folder
import com.gabrielepiccione.onda.data.GestureAction
import com.gabrielepiccione.onda.data.NotificationStore
import com.gabrielepiccione.onda.data.WidgetManager
import com.gabrielepiccione.onda.service.OndaAccessibilityService
import com.gabrielepiccione.onda.util.SystemActions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

/** Larghezza massima delle liste su tablet e in orizzontale. */
private val MAX_CONTENT_WIDTH = 560.dp

enum class Overlay { NONE, ALL_APPS, SEARCH, SETTINGS, STYLES }

/** Dove finisce un widget appena scelto. */
sealed interface WidgetTarget {
    data object Home : WidgetTarget
    data class InFolder(val folderId: String) : WidgetTarget
    data class ForApp(val appKey: String) : WidgetTarget
    data class Replace(val oldId: Int) : WidgetTarget
    data object ReplaceClock : WidgetTarget
    /** Aggiunta a una pila (o creazione della pila a partire da un widget singolo). */
    data class Stack(val block: String) : WidgetTarget
}

/** Azioni che richiedono l'Activity (permessi, risultati di altre schermate). */
interface HomeCallbacks {
    fun requestContactsPermission()
    fun requestCalendarPermission()
    fun requestDefaultLauncher()
    fun openNotificationAccess()
    fun openAccessibilitySettings()
    fun openOwnAppInfo()
    fun pickWidget(provider: WidgetManager.Provider, target: WidgetTarget)
    fun exportBackup()
    fun importBackup()
    fun importFont(forClock: Boolean)
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
    val usageAccess: Boolean = false,
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

    var overlay by rememberSaveable { mutableStateOf(Overlay.NONE) }
    var actionsFor by remember { mutableStateOf<AppInfo?>(null) }
    var panelFor by remember { mutableStateOf<AppInfo?>(null) }
    var iconPickerFor by remember { mutableStateOf<AppInfo?>(null) }
    var folderSheet by remember { mutableStateOf<Folder?>(null) }
    var moveFor by remember { mutableStateOf<AppInfo?>(null) }
    var homeMenu by remember { mutableStateOf(false) }
    var widgetPicker by remember { mutableStateOf(false) }
    var widgetFilter by remember { mutableStateOf<String?>(null) }
    var widgetTarget by remember { mutableStateOf<WidgetTarget>(WidgetTarget.Home) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    var breakFor by remember { mutableStateOf<AppInfo?>(null) }
    // Introduzione: al primo avvio (prima che vengano proposti i preferiti) o su richiesta
    var showIntro by rememberSaveable { mutableStateOf(!vm.settingsRepo.current.onboardingDone) }

    val homeList = rememberLazyListState()
    val allAppsList = rememberLazyListState()
    val sections = remember(state.allApps, settings.launchCounts) { buildSections(state.allApps, settings.launchCounts) }
    val starred by vm.starred.collectAsStateWithLifecycle()
    val privateSpace by vm.privateSpace.collectAsStateWithLifecycle()
    val contactsInList = if (settings.alphabetContacts) starred else emptyList()
    // "@" in cima all'alfabeto porta ai contatti preferiti
    val letters = remember(sections, contactsInList.isNotEmpty()) {
        (if (contactsInList.isNotEmpty()) listOf(CONTACTS_LETTER) else emptyList()) + sections.map { it.letter }
    }
    val iconScale = settings.iconSize

    fun closeAll() {
        overlay = Overlay.NONE
        vm.clearSearch()
    }

    fun launchApp(app: AppInfo, skipBreak: Boolean = false) {
        // Pausa consapevole: prima qualche secondo di respiro
        if (!skipBreak && app.key in vm.settingsRepo.current.breakApps) {
            breakFor = app
            return
        }
        val go = {
            vm.launch(app)
            scope.launch { delay(380); closeAll() }
        }
        // App protette: prima lo sblocco (impronta, volto o PIN)
        if (app.key in vm.settingsRepo.current.protectedApps) callbacks.authenticate(app.label) { go() } else go()
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
        val needsService = { ok: Boolean -> if (!ok) withRestrictedHelp(callbacks::openAccessibilitySettings) }
        when (action) {
            GestureAction.SEARCH -> overlay = Overlay.SEARCH
            GestureAction.NOTIFICATIONS -> SystemActions.expandNotifications(context)
            GestureAction.ALL_APPS -> overlay = Overlay.ALL_APPS
            GestureAction.LOCK_SCREEN -> needsService(OndaAccessibilityService.lockScreen())
            GestureAction.QUICK_SETTINGS ->
                if (!OndaAccessibilityService.quickSettings()) SystemActions.expandQuickSettings(context)
            GestureAction.RECENTS -> needsService(OndaAccessibilityService.recents())
            GestureAction.SCREENSHOT -> needsService(OndaAccessibilityService.screenshot())
            GestureAction.ONDA_SETTINGS -> overlay = Overlay.SETTINGS
            GestureAction.OPEN_APP -> state.app(appKey)?.let { launchApp(it) }
            GestureAction.NONE -> Unit
        }
    }

    LaunchedEffect(state.loaded) { if (state.loaded) vm.seedFavoritesIfNeeded() }
    LaunchedEffect(settings.showAgenda, settings.agendaDays, status.calendarPermission) { vm.refreshCalendar() }
    LaunchedEffect(settings.alphabetContacts, status.contactsPermission, overlay == Overlay.ALL_APPS) { vm.refreshStarred() }

    LaunchedEffect(Unit) {
        vm.homePressed.collect {
            if (overlay == Overlay.NONE && actionsFor == null && panelFor == null) homeList.animateScrollToItem(0)
            closeAll()
            actionsFor = null
            panelFor = null
            folderSheet = null
            homeMenu = false
            widgetPicker = false
            breakFor = null
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
                    if (currentSettings.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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
                        onLongClick = { actionsFor = app },
                        onSwipe = { panelFor = app },
                        onOpenPanel = { panelFor = app },
                        onSwipeLeft = { NotificationStore.dismissAll(app.packageName) },
                        onNotificationClick = { panelFor = app },
                        onNotificationDismiss = { NotificationStore.dismiss(it) },
                        onNotificationReply = { n, text -> NotificationStore.reply(context, n, text) },
                        onNotificationAction = { n, a -> NotificationStore.runAction(context, n, a) },
                        modifier = itemMod,
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
                    onAppLongClick = { actionsFor = it },
                    onLongClick = { folderSheet = entry.folder },
                    modifier = itemMod,
                )
            }
        }
    }

    val blocks = remember(settings) { HomeBlocks.resolve(settings) }
    var editing by remember { mutableStateOf<String?>(null) }

    val dimColor = if (colors.dark) Color.Black else Color.White
    Box(
        Modifier
            .fillMaxSize()
            .background(dimColor.copy(alpha = settings.wallpaperDim))
    ) {
        val bars = WindowInsets.systemBars.asPaddingValues()
        val letterSide = 48.dp
        val hiddenSide = 28.dp
        // Tablet e orizzontale: le liste restano di larghezza leggibile, centrate
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val extraSide = ((screenWidth - MAX_CONTENT_WIDTH) / 2).coerceAtLeast(0.dp)
        val usableHeight = LocalConfiguration.current.screenHeightDp.dp - bars.calculateTopPadding() - bars.calculateBottomPadding()
        val anchorHeight by androidx.compose.animation.core.animateDpAsState(
            usableHeight * settings.homeOffset.coerceIn(0f, 0.75f),
            Motion.bouncy(),
            label = "homeOffset",
        )

        // ---------------- HOME ----------------
        LazyColumn(
            state = homeList,
            // Automatico: contenuto in basso; con posizione scelta parte dall'alto dopo uno spazio
            verticalArrangement = Arrangement.spacedBy(2.dp, if (settings.homeOffset >= 0f) Alignment.Top else Alignment.Bottom),
            // Orologio, widget e player: stesso margine a destra e a sinistra.
            // Solo le righe delle app si tengono lontane dalle lettere (vedi homeEntries).
            contentPadding = PaddingValues(
                start = hiddenSide + extraSide,
                end = hiddenSide + extraSide,
                top = bars.calculateTopPadding() + 24.dp,
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
                .pointerInput(settings.doubleTap, settings.doubleTapApp) {
                    detectTapGestures(
                        onLongPress = {
                            if (settings.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            homeMenu = true
                        },
                        onDoubleTap = if (settings.doubleTap != GestureAction.NONE) {
                            { _: Offset -> perform(settings.doubleTap, settings.doubleTapApp) }
                        } else null,
                    )
                }
        ) {
            // Spazio prima del primo blocco, se l'utente ha scelto dove far iniziare la home
            if (settings.homeOffset >= 0f) {
                item(key = "anchor") { Spacer(Modifier.height(anchorHeight)) }
            }
            // Blocchi della home nell'ordine scelto dall'utente (orologio, musica, widget, app)
            for (block in blocks) {
                when {
                    block == HomeBlocks.CLOCK -> if (settings.showClock) {
                        item(key = "clock") {
                            ClockHeader(
                                event = if (settings.showCalendar) nextEvent else null,
                                onTime = { SystemActions.openAlarms(context) },
                                onDate = { SystemActions.openCalendar(context) },
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
                    block == HomeBlocks.SUMMARY -> item(key = "summary") {
                        val groups = state.notifications.entries
                            .filter { it.value.isNotEmpty() }
                            .sortedByDescending { it.value.first().postTime }
                            .map { (pkg, list) ->
                                val app = state.allApps.firstOrNull { it.packageName == pkg }
                                SummaryGroup(pkg, app?.label ?: pkg, app?.icon, list)
                            }
                        val nowMinute = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
                        NotificationSummaryCard(
                            groups = groups,
                            nextDigest = if (settings.digestApps.isEmpty()) null
                            else com.gabrielepiccione.onda.util.DigestSchedule.nextSlot(nowMinute, settings.digestTimes)
                                ?.let(com.gabrielepiccione.onda.util.DigestSchedule::format),
                            onGroup = { g -> state.allApps.firstOrNull { it.packageName == g.packageName }?.let { panelFor = it } },
                            onClearAll = { groups.forEach { g -> NotificationStore.dismissAll(g.packageName) } },
                            highlighted = editing == HomeBlocks.SUMMARY,
                            onLongPress = { if (!settings.lockLayout) editing = HomeBlocks.SUMMARY },
                            modifier = Modifier.animateItem(placementSpec = Motion.offsetEnter).padding(vertical = 8.dp),
                        )
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
                    block.startsWith(HomeBlocks.STACK_PREFIX) -> {
                        val stack = HomeBlocks.stackId(block)?.let { settings.widgetStacks[it] }
                        if (stack != null && stack.widgets.isNotEmpty()) {
                            item(key = block) {
                                WidgetStackView(
                                    stack = stack,
                                    widgets = vm.widgets,
                                    highlighted = editing == block,
                                    onLongPress = { if (!settings.lockLayout) editing = block },
                                    modifier = Modifier.animateItem(placementSpec = Motion.offsetEnter).padding(horizontal = 4.dp),
                                )
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
                                        onLongClick = { actionsFor = plug },
                                        onSwipe = { panelFor = plug },
                                        onOpenPanel = { panelFor = plug },
                                        onSwipeLeft = { NotificationStore.dismissAll(plug.packageName) },
                                        onNotificationClick = { panelFor = plug },
                                        onNotificationDismiss = { NotificationStore.dismiss(it) },
                                    )
                                }
                            }
                        }
                        homeEntries()
                    }
                }
            }
            if (state.loaded && state.home.isEmpty()) {
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
                        GestureAction.QUICK_SETTINGS, GestureAction.ONDA_SETTINGS -> Icons.Filled.Settings
                        GestureAction.RECENTS -> Icons.Filled.Menu
                        GestureAction.SCREENSHOT -> Icons.Filled.Face
                        GestureAction.OPEN_APP -> Icons.Filled.PlayArrow
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
            onLongClick = { actionsFor = it },
            onOpenPanel = { panelFor = it },
            onDismissNotifications = { NotificationStore.dismissAll(it.packageName) },
            contacts = contactsInList,
            onContact = { vm.contacts.open(it); scope.launch { delay(380); closeAll() } },
            loadPhoto = { vm.contacts.photo(it) },
            privateSpace = privateSpace,
            privateApps = state.privateApps,
            onTogglePrivate = { privateSpace?.let { ps -> vm.appsRepo.setPrivateLocked(!ps.locked) } },
            contentPadding = PaddingValues(
                start = (if (settings.leftHanded) letterSide else hiddenSide) + extraSide,
                end = (if (settings.leftHanded) hiddenSide else letterSide) + extraSide,
                top = bars.calculateTopPadding() + 16.dp,
                bottom = bars.calculateBottomPadding() + 24.dp,
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
            top = bars.calculateTopPadding() + 72.dp,
            bottom = bars.calculateBottomPadding() + 40.dp,
        )
        // Doppio tocco sull'alfabeto (visibile o invisibile): stessa azione del doppio tocco in home
        val lockFromAlphabet: (() -> Unit)? = if (settings.doubleTap != GestureAction.NONE) {
            {
                closeAll()
                perform(settings.doubleTap, settings.doubleTapApp)
            }
        } else null
        val contactOffset = contactsRows(contactsInList)
        val onLetter: (Char) -> Unit = { letter ->
            if (letter == CONTACTS_LETTER) {
                scope.launch { allAppsList.scrollToItem(0) }
            } else {
                sections.firstOrNull { it.letter == letter }?.let { s ->
                    scope.launch { allAppsList.scrollToItem(s.firstIndex + contactOffset) }
                }
            }
        }
        if (scrollerAlpha > 0.01f) {
            AlphabetWaveScroller(
                letters = letters,
                leftSide = settings.leftHanded,
                showLetters = lettersVisible,
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
                modifier = Modifier
                    .align(if (settings.leftHanded) Alignment.CenterEnd else Alignment.CenterStart)
                    .zIndex(2f)
                    .then(scrollerPadding),
                onTouchStart = { overlay = Overlay.ALL_APPS },
                onLetter = onLetter,
                onDoubleTap = lockFromAlphabet,
            )
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
                    onAppLongClick = { actionsFor = it },
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
                    status = SettingsStatus(
                        isDefaultLauncher = status.isDefaultLauncher,
                        notificationAccess = status.notificationAccess,
                        calendarPermission = status.calendarPermission,
                        accessibilityEnabled = status.accessibilityEnabled,
                        usageAccess = status.usageAccess,
                        contactsPermission = status.contactsPermission,
                    ),
                    widgets = vm.widgets,
                    actions = object : SettingsActions {
                        override fun edit(transform: (com.gabrielepiccione.onda.data.Settings) -> com.gabrielepiccione.onda.data.Settings) = vm.updateSettings(transform)
                        override fun requestDefault() = callbacks.requestDefaultLauncher()
                        override fun notificationAccess() = withRestrictedHelp(callbacks::openNotificationAccess)
                        override fun calendarPermission() = callbacks.requestCalendarPermission()
                        override fun accessibility() = withRestrictedHelp(callbacks::openAccessibilitySettings)
                        override fun contactsPermission() = callbacks.requestContactsPermission()
                        override fun usageAccess() = com.gabrielepiccione.onda.util.UsageAccess.openSettings(context)
                        override fun addWidget() = openWidgetPicker(null, WidgetTarget.Home)
                        override fun removeWidget(id: Int) = vm.removeWidget(id)
                        override fun unhide(app: AppInfo) = vm.setHidden(app, false)
                        override fun authenticate(onSuccess: () -> Unit) = callbacks.authenticate(context.getString(R.string.section_hidden), onSuccess)
                        override fun wallpaper() = SystemActions.openWallpaperPicker(context)
                        override fun openStyles() { overlay = Overlay.STYLES }
                        override fun importFont(forClock: Boolean) = callbacks.importFont(forClock)
                        override fun exportBackup() = callbacks.exportBackup()
                        override fun importBackup() = callbacks.importBackup()
                        override fun close() = closeAll()
                        override fun showIntro() { closeAll(); showIntro = true }
                    },
                )
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
                vm.updateSettings { preset.applyTo(it) }
                if (withWallpaper) {
                    scope.launch {
                        com.gabrielepiccione.onda.data.StylePresets.applyWallpaper(context, preset)
                    }
                }
                android.widget.Toast.makeText(context, R.string.styles_applied, android.widget.Toast.LENGTH_SHORT).show()
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

    // ---------------- PAUSA CONSAPEVOLE ----------------
    breakFor?.let { app ->
        val minutes = remember(app.key) { com.gabrielepiccione.onda.util.UsageAccess.todayMinutes(context, app.packageName) }
        androidx.activity.compose.BackHandler { breakFor = null }
        UsageBreakScreen(
            app = app,
            seconds = settings.breakSeconds,
            opensToday = remember(app.key) { vm.settingsRepo.opensToday(app.key) },
            dailyLimit = settings.breakDailyLimit,
            minutesToday = minutes,
            onOpen = { breakFor = null; launchApp(app, skipBreak = true) },
            onCancel = { breakFor = null },
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
                hasBreak = app.key in settings.breakApps,
                onToggleBreak = { vm.settingsRepo.toggleBreak(app.key) },
                isProtected = app.key in settings.protectedApps,
                onToggleProtected = {
                    // Togliere la protezione richiede comunque lo sblocco
                    if (app.key in settings.protectedApps) callbacks.authenticate(app.label) { vm.settingsRepo.toggleProtected(app.key) }
                    else vm.settingsRepo.toggleProtected(app.key)
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

    iconPickerFor?.let { app ->
        IconPickerSheet(
            app = app,
            packs = vm.appsRepo.packs,
            onPick = { pkg, name -> vm.settingsRepo.setIconOverride(app.key, pkg, name) },
            onReset = { vm.settingsRepo.setIconOverride(app.key, null, null) },
            onDismiss = { iconPickerFor = null },
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
        )
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
                    SheetRow(Icons.Filled.Add, stringResource(R.string.new_folder)) { newFolder = true }
                    SheetRow(Icons.Filled.Add, stringResource(R.string.add_widget)) { homeMenu = false; openWidgetPicker(null, WidgetTarget.Home) }
                    if (!settings.showSummary) {
                        SheetRow(Icons.Filled.Notifications, stringResource(R.string.summary_add)) {
                            homeMenu = false
                            vm.updateSettings { it.copy(showSummary = true) }
                            if (!status.notificationAccess) withRestrictedHelp(callbacks::openNotificationAccess)
                        }
                    }
                    if (!settings.showAgenda) {
                        SheetRow(Icons.Filled.Add, stringResource(R.string.agenda_add)) {
                            homeMenu = false
                            vm.updateSettings { it.copy(showAgenda = true) }
                            if (!status.calendarPermission) callbacks.requestCalendarPermission()
                        }
                    }
                }
                SheetRow(Icons.Filled.Settings, stringResource(if (settings.lockLayout) R.string.layout_unlock else R.string.layout_lock)) {
                    homeMenu = false
                    vm.updateSettings { it.copy(lockLayout = !it.lockLayout) }
                }
                SheetRow(Icons.Filled.Search, stringResource(R.string.search)) { homeMenu = false; overlay = Overlay.SEARCH }
                if (!settings.showClock) {
                    SheetRow(Icons.Filled.Add, stringResource(R.string.show_clock_again)) {
                        homeMenu = false
                        vm.updateSettings { it.copy(showClock = true) }
                    }
                }
                SheetRow(Icons.Filled.Face, stringResource(R.string.change_wallpaper)) { homeMenu = false; SystemActions.openWallpaperPicker(context) }
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
            actions = object : HomeEditActions {
                override fun edit(transform: (com.gabrielepiccione.onda.data.Settings) -> com.gabrielepiccione.onda.data.Settings) = vm.updateSettings(transform)
                override fun move(block: String, delta: Int) = vm.settingsRepo.moveBlock(block, delta)
                override fun setWidgetHeight(id: Int, dp: Int) = vm.settingsRepo.setWidgetHeight(id, dp)
                override fun replaceWidget(id: Int) { editing = null; openWidgetPicker(null, WidgetTarget.Replace(id)) }
                override fun replaceClock() { editing = null; openWidgetPicker(null, WidgetTarget.ReplaceClock) }
                override fun deleteWidget(id: Int) = vm.removeWidget(id)
                override fun addToStack(block: String) { editing = null; openWidgetPicker(null, WidgetTarget.Stack(block)) }
                override fun removeFromStack(stackId: String, id: Int) = vm.removeFromStack(stackId, id)
                override fun deleteStack(stackId: String) = vm.deleteStack(stackId)
                override fun setStackHeight(stackId: String, dp: Int) = vm.settingsRepo.setStackHeight(stackId, dp)
                override fun widgetLabel(id: Int): String? = vm.widgets.infoFor(id)?.let { i -> runCatching { i.loadLabel(context.packageManager) }.getOrNull() }
                override fun done() { editing = null }
            },
        )
    }

    if (showIntro) {
        OnboardingScreen(
            status = status,
            actions = object : OnboardingActions {
                override fun defaultLauncher() = callbacks.requestDefaultLauncher()
                override fun notificationAccess() = withRestrictedHelp(callbacks::openNotificationAccess)
                override fun contacts() = callbacks.requestContactsPermission()
                override fun calendar() = callbacks.requestCalendarPermission()
                override fun openStyles() { showIntro = false; overlay = Overlay.STYLES }
                override fun done() { showIntro = false }
            },
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
