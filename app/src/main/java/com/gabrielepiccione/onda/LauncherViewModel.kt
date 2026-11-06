// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda

import com.gabrielepiccione.onda.data.PlugPlay
import com.gabrielepiccione.onda.data.MEDIA_PREFIX
import com.gabrielepiccione.onda.data.MediaShortcut
import com.gabrielepiccione.onda.data.MediaShortcuts
import com.gabrielepiccione.onda.data.SpotifyItem
import com.gabrielepiccione.onda.data.PrefixKind
import com.gabrielepiccione.onda.data.SearchPrefixes
import com.gabrielepiccione.onda.data.ContextProfile
import com.gabrielepiccione.onda.data.ContextProfiles
import com.gabrielepiccione.onda.data.ContextReader
import com.gabrielepiccione.onda.data.LaunchLog
import com.gabrielepiccione.onda.data.TriggerType
import com.gabrielepiccione.onda.data.UsageStatsRepository
import android.app.Application
import android.content.pm.ShortcutInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.data.AppPopups
import com.gabrielepiccione.onda.data.AppRepository
import com.gabrielepiccione.onda.data.HomeSuggestions
import com.gabrielepiccione.onda.data.PinnedShortcut
import com.gabrielepiccione.onda.data.PinnedShortcuts
import com.gabrielepiccione.onda.data.SHORTCUT_PREFIX
import com.gabrielepiccione.onda.data.WeatherRepository
import com.gabrielepiccione.onda.data.WeatherSnapshot
import com.gabrielepiccione.onda.data.CalendarEvent
import com.gabrielepiccione.onda.data.ContactHit
import com.gabrielepiccione.onda.data.ContactsRepository
import com.gabrielepiccione.onda.data.NotificationItem
import com.gabrielepiccione.onda.data.NotificationStore
import com.gabrielepiccione.onda.data.Settings
import com.gabrielepiccione.onda.data.normalizedForSearch
import com.gabrielepiccione.onda.util.Calculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.gabrielepiccione.onda.data.RecentInstalls
import java.text.Collator
import java.util.Locale

private const val WEATHER_MIN_INTERVAL_MS = 15 * 60_000L

/** Voce della home: un'app preferita, una cartella oppure una scorciatoia. */
sealed interface HomeEntry {
    val key: String
    data class App(val app: AppInfo) : HomeEntry { override val key get() = "f_" + app.key }
    data class Group(val folder: com.gabrielepiccione.onda.data.Folder, val apps: List<AppInfo>) : HomeEntry {
        override val key get() = "g_" + folder.id
    }
    data class Shortcut(val pinned: PinnedShortcut, val info: AppRepository.PinnedInfo) : HomeEntry {
        override val key get() = "s_" + pinned.key
    }
    /** Playlist, album, artista o brano: si apre nell'app musicale; l'icona è quella dell'app. */
    data class Media(val shortcut: MediaShortcut, val app: AppInfo?) : HomeEntry {
        override val key get() = "m_" + shortcut.id
    }
}

data class LauncherState(
    val allApps: List<AppInfo> = emptyList(),     // visibili, ordinate A-Z
    val privateApps: List<AppInfo> = emptyList(), // Spazio privato sbloccato, ordinate A-Z
    val favorites: List<AppInfo> = emptyList(),   // solo app preferite (fuori dalle cartelle), in ordine
    val home: List<HomeEntry> = emptyList(),      // app + cartelle, nell'ordine scelto
    val homeSuggestions: List<AppInfo> = emptyList(), // usate spesso, sotto i preferiti
    val suggestions: List<AppInfo> = emptyList(), // app più usate
    val hiddenApps: List<AppInfo> = emptyList(),
    val folders: List<HomeEntry.Group> = emptyList(),      // tutte le cartelle, per il fondo dell'elenco
    val recentlyInstalled: List<AppInfo> = emptyList(),   // dalla più recente
    val notifications: Map<String, List<NotificationItem>> = emptyMap(),
    val settings: Settings = Settings(),
    val loaded: Boolean = false,
    /** Profilo dei preferiti contestuali attivo adesso, o null. */
    val activeProfile: ContextProfile? = null,
) {
    private val byKey: Map<String, AppInfo> by lazy { (allApps + hiddenApps + privateApps).associateBy { it.key } }
    fun app(key: String?): AppInfo? = key?.let { byKey[it] }
}

/** Modalità di ricerca scelta con il prefisso: "=" calcolo, "@" contatti, "/" impostazioni. */
enum class SearchMode { ALL, CALC, CONTACTS, SETTINGS, PLAY, SPOTIFY }

data class SearchResults(
    val query: String = "",
    val calc: String? = null,
    val apps: List<AppInfo> = emptyList(),
    val contacts: List<ContactHit> = emptyList(),
    val mode: SearchMode = SearchMode.ALL,
    val settingsHits: List<com.gabrielepiccione.onda.util.SettingsShortcut> = emptyList(),
    /** Il testo cercato senza il prefisso. */
    val term: String = "",
)

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    private val ondaApp = app as OndaApp
    val appsRepo = ondaApp.apps
    val settingsRepo = ondaApp.settings
    val widgets = ondaApp.widgets
    val media = ondaApp.media
    val calendar = ondaApp.calendar
    val audio = ondaApp.audio
    val contacts = ContactsRepository(app)
    val weatherRepo = WeatherRepository(app)
    val usageStats = UsageStatsRepository(app)

    private val collator = Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }

    /** Aggiunge ai preferiti un contenuto di Spotify riconosciuto da un link. */
    fun addMediaShortcut(item: SpotifyItem, name: String) {
        settingsRepo.edit { MediaShortcuts.add(it, MediaShortcut(java.util.UUID.randomUUID().toString(), item.uri, name, item.type.name.lowercase())) }
    }

    /**
     * Apre la voce nell'app: se Spotify ha già una sessione, le si chiede di far partire subito il
     * contenuto; altrimenti si apre la sua pagina.
     */
    fun openMediaShortcut(m: MediaShortcut) {
        if (media.playFromUri(m.packageName, m.uri)) return
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(m.uri))
            .setPackage(m.packageName)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { getApplication<android.app.Application>().startActivity(intent) }
    }

    /** L'app musicale per "p": quella scelta, oppure l'ultima che ha suonato. */
    fun musicApp(): String = settingsRepo.current.let { it.musicApp.ifBlank { it.lastMusicApp } }

    init {
        // Plug & Play: collegando le cuffie, se richiesto, riprende la musica dell'app scelta
        viewModelScope.launch {
            var before = audio.headphonesConnected.value
            audio.headphonesConnected.collect { now ->
                val justConnected = now && !before
                before = now
                if (!justConnected) return@collect
                // Il nome del dispositivo può arrivare un istante dopo il collegamento
                delay(1_500)
                val s = settingsRepo.current
                if (PlugPlay.shouldResume(true, s.plugPlayResume, s.plugPlayDevice, audio.connectedNames.value, inCall = false, somethingPlaying = audio.busy())) {
                    val pkg = appsRepo.apps.value.firstOrNull { it.key == s.plugPlayApp }?.component?.packageName ?: musicApp()
                    if (pkg.isNotBlank()) media.resume(pkg)
                }
            }
        }
    }

    init {
        // Ricorda l'ultima app che ha avuto una sessione multimediale, per la riproduzione dalla ricerca
        viewModelScope.launch {
            media.media.collect { m ->
                if (m != null && m.packageName != settingsRepo.current.lastMusicApp) settingsRepo.edit { it.copy(lastMusicApp = m.packageName) }
            }
        }
    }

    // --- Preferiti contestuali ---
    private val activeProfileFlow = MutableStateFlow<ContextProfile?>(null)
    val contextReader = ContextReader(app, ondaApp.audio, ondaApp.calendar)

    /** Rilegge il contesto e sceglie il profilo; senza profili non legge nulla. */
    fun refreshContext() {
        viewModelScope.launch {
            val s = settingsRepo.current
            if (s.contextProfiles.none { it.enabled }) {
                activeProfileFlow.value = null
                return@launch
            }
            contextReader.start()
            val types = s.contextProfiles.flatMap { p -> p.groups.flatMap { g -> g.triggers.map { it.type } } }.toSet()
            val snapshot = contextReader.snapshot(TriggerType.PLACE in types, TriggerType.CALENDAR in types)
            activeProfileFlow.value = ContextProfiles.resolve(s.contextProfiles, snapshot, s.profileOverride, System.currentTimeMillis())
        }
    }

    /** Gesto sull'orologio: passa al profilo successivo per un'ora, poi si torna al profilo automatico. */
    fun cycleProfile() {
        val now = System.currentTimeMillis()
        settingsRepo.edit { s -> s.copy(profileOverride = ContextProfiles.nextOverride(s.contextProfiles, activeProfileFlow.value?.id, now)) }
        viewModelScope.launch {
            delay(150)
            refreshContext()
        }
    }

    val state: StateFlow<LauncherState> = combine(
        appsRepo.apps,
        settingsRepo.settings,
        NotificationStore.byPackage,
        appsRepo.pinned,
        activeProfileFlow,
    ) { raw, settings, notifs, pinned, profile ->
        val all = raw.map { r ->
            AppInfo(
                key = r.key,
                component = r.component,
                user = r.user,
                label = settings.renames[r.key] ?: r.label,
                originalLabel = r.label,
                icon = r.icon,
                mono = r.mono,
                isWorkProfile = r.isWorkProfile,
                category = r.category,
                color = r.color,
                isFavorite = r.key in settings.favorites,
                isHidden = r.key in settings.hidden,
                swipeTarget = settings.swipeActions[r.key],
                hasCustomIcon = r.key in settings.iconOverrides || r.key in settings.iconShapeOverrides,
                isPrivate = r.isPrivate,
                installedAt = r.installedAt,
            )
        }.sortedWith { a, b -> collator.compare(a.label, b.label) }

        val byKey = all.associateBy { it.key }
        // Preferiti contestuali: i primi slot restano, gli altri vengono dal profilo attivo
        val home = ContextProfiles.favorites(settings.favorites, settings.fixedFavoriteSlots, profile).mapNotNull { entry ->
            if (entry.startsWith(com.gabrielepiccione.onda.data.FOLDER_PREFIX)) {
                val f = settings.folders[entry.removePrefix(com.gabrielepiccione.onda.data.FOLDER_PREFIX)] ?: return@mapNotNull null
                HomeEntry.Group(f, f.apps.mapNotNull { byKey[it] })
            } else if (entry.startsWith(MEDIA_PREFIX)) {
                val id = entry.removePrefix(MEDIA_PREFIX)
                settings.mediaShortcuts.firstOrNull { it.id == id }?.let { m ->
                    HomeEntry.Media(m, all.firstOrNull { it.packageName == m.packageName })
                }
            } else if (entry.startsWith(SHORTCUT_PREFIX)) {
                val key = entry.removePrefix(SHORTCUT_PREFIX)
                val shortcut = settings.pinnedShortcuts.firstOrNull { it.key == key } ?: return@mapNotNull null
                // Non più nel sistema (app disinstallata, Onda non predefinito): la voce non si mostra
                pinned[key]?.let { HomeEntry.Shortcut(shortcut, it) }
            } else {
                byKey[entry]?.takeIf { !it.isHidden }?.let { HomeEntry.App(it) }
            }
        }
        // Lo Spazio privato ha il suo contenitore: fuori da alfabeto, ricerca e suggerimenti
        val visible = all.filterNot { it.isHidden || it.isPrivate }
        LauncherState(
            allApps = visible,
            privateApps = all.filter { it.isPrivate },
            favorites = home.filterIsInstance<HomeEntry.App>().map { it.app },
            home = home,
            homeSuggestions = HomeSuggestions.pick(settings, visible.map { it.key }.toSet()).mapNotNull { byKey[it] },
            suggestions = settings.launchCounts.entries
                .sortedByDescending { it.value }
                .mapNotNull { byKey[it.key]?.takeIf { a -> !a.isHidden && !a.isPrivate } }
                .take(8)
                .ifEmpty { visible.take(8) },
            hiddenApps = all.filter { it.isHidden && !it.isPrivate },
            // In fondo all'elenco: tutte le cartelle, in ordine alfabetico
            folders = settings.folders.values
                .sortedWith { a, b -> collator.compare(a.name, b.name) }
                .map { f -> HomeEntry.Group(f, f.apps.mapNotNull { byKey[it]?.takeIf { a -> !a.isHidden } }) },
            recentlyInstalled = RecentInstalls.pick(visible) { it.installedAt },
            notifications = notifs.filterKeys { it !in settings.mutedNotifications },
            settings = settings,
            loaded = raw.isNotEmpty(),
            activeProfile = profile,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LauncherState())

    // --- Meteo (da Breezy Weather) ---
    private val _weather = MutableStateFlow<WeatherSnapshot?>(null)
    val weather: StateFlow<WeatherSnapshot?> = _weather.asStateFlow()
    private var weatherLoadedAt = 0L

    /** Rilegge il meteo; senza [force] al massimo ogni 15 minuti, perché il launcher riprende spesso. */
    fun refreshWeather(force: Boolean = false) {
        if (!settingsRepo.current.showWeather) {
            _weather.value = null
            return
        }
        val now = System.currentTimeMillis()
        if (!force && _weather.value != null && now - weatherLoadedAt < WEATHER_MIN_INTERVAL_MS) return
        viewModelScope.launch {
            _weather.value = weatherRepo.load()
            weatherLoadedAt = now
        }
    }

    // --- Calendario ---
    private val _nextEvent = MutableStateFlow<CalendarEvent?>(null)
    val nextEvent: StateFlow<CalendarEvent?> = _nextEvent.asStateFlow()

    private val _agenda = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val agenda: StateFlow<List<CalendarEvent>> = _agenda.asStateFlow()

    fun refreshCalendar() {
        viewModelScope.launch {
            _nextEvent.value = calendar.nextEvent()
            val s = settingsRepo.current
            _agenda.value = if (s.showAgenda) calendar.upcoming(s.agendaDays) else emptyList()
        }
    }

    // --- Contatti nell'elenco alfabetico (letti solo se l'opzione è attiva) ---
    private val _alphabetContacts = MutableStateFlow<List<ContactHit>>(emptyList())
    val alphabetContacts: StateFlow<List<ContactHit>> = _alphabetContacts.asStateFlow()

    fun refreshAlphabetContacts() {
        viewModelScope.launch {
            _alphabetContacts.value = if (settingsRepo.current.contactsInAlphabet) contacts.all() else emptyList()
        }
    }

    // --- Tasto Home premuto: chiude pannelli e torna in cima ---
    private val _homePressed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val homePressed: SharedFlow<Unit> = _homePressed
    fun onHomePressed() { _homePressed.tryEmit(Unit) }

    private val _pauseRequests = MutableSharedFlow<String>(extraBufferCapacity = 1)
    /** Pacchetti aperti fuori dal launcher per cui mostrare la pausa di Usage Breaker. */
    val pauseRequests: SharedFlow<String> = _pauseRequests
    fun requestPause(packageName: String) { _pauseRequests.tryEmit(packageName) }

    // --- Ricerca ---
    private val _search = MutableStateFlow(SearchResults())
    val search: StateFlow<SearchResults> = _search.asStateFlow()
    private var searchJob: Job? = null

    fun updateSearch(query: String) {
        searchJob?.cancel()
        // Prefissi scelti dall'utente: simboli subito, parole ("p ", "sp ") solo con lo spazio dopo
        val (kind, term) = SearchPrefixes.parse(query, settingsRepo.current.searchPrefixes)
        val mode = when (kind) {
            PrefixKind.CALC -> SearchMode.CALC
            PrefixKind.CONTACTS -> SearchMode.CONTACTS
            PrefixKind.SETTINGS -> SearchMode.SETTINGS
            PrefixKind.PLAY -> SearchMode.PLAY
            PrefixKind.SPOTIFY -> SearchMode.SPOTIFY
            null -> SearchMode.ALL
        }
        _search.value = SearchResults(
            query = query,
            term = term,
            mode = mode,
            apps = if (mode == SearchMode.ALL) filterApps(term) else emptyList(),
            calc = when (mode) {
                SearchMode.ALL -> Calculator.evaluate(term)
                // Con "=" anche un numero semplice è un risultato valido
                SearchMode.CALC -> Calculator.evaluate(term) ?: term.trim().takeIf { it.isNotEmpty() && it.all { c -> c.isDigit() || c == ',' || c == '.' } }
                else -> null
            },
            settingsHits = if (mode == SearchMode.SETTINGS) com.gabrielepiccione.onda.util.SettingsShortcuts.search(getApplication(), term) else emptyList(),
            contacts = _search.value.contacts.takeIf { mode == SearchMode.ALL || mode == SearchMode.CONTACTS }.orEmpty(),
        )
        if (mode == SearchMode.ALL || mode == SearchMode.CONTACTS) {
            searchJob = viewModelScope.launch {
                delay(120)
                _search.value = _search.value.copy(contacts = contacts.search(term, limit = if (mode == SearchMode.CONTACTS) 20 else 5))
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _search.value = SearchResults()
    }

    /** Ricerca app: prefisso > inizio parola > iniziali > contiene. Include le app nascoste. */
    private fun filterApps(query: String): List<AppInfo> {
        val q = query.normalizedForSearch().trim()
        if (q.isEmpty()) return emptyList()
        val s = state.value
        return (s.allApps + s.hiddenApps).mapNotNull { app ->
            val label = app.label.normalizedForSearch()
            val words = label.split(' ', '-', '.', '_', '(', ')').filter { it.isNotEmpty() }
            val initials = words.joinToString("") { it.take(1) }
            val score = when {
                label.startsWith(q) -> 0
                words.any { it.startsWith(q) } -> 1
                q.length >= 2 && initials.startsWith(q) -> 2
                label.contains(q) -> 3
                q.length >= 3 && app.packageName.contains(q) -> 4
                else -> null
            }
            score?.let { it to app }
        }.sortedWith(compareBy<Pair<Int, AppInfo>> { it.first }.thenBy { it.second.label.lowercase() })
            .map { it.second }
            .take(8)
    }

    // --- Azioni sulle app ---
    fun launch(app: AppInfo) {
        appsRepo.launch(app)
        LaunchLog.record(getApplication(), app.key)
        viewModelScope.launch { delay(600); settingsRepo.recordLaunch(app.key) }
    }

    // --- Cartelle ---
    fun createFolder(name: String, first: AppInfo?) = settingsRepo.createFolder(name, first?.key)
    fun renameFolder(id: String, name: String) = settingsRepo.renameFolder(id, name)
    fun deleteFolder(id: String) = settingsRepo.deleteFolder(id).forEach { widgets.deleteId(it) }
    fun moveToFolder(app: AppInfo, folderId: String?) = settingsRepo.moveAppToFolder(app.key, folderId)
    fun moveEntry(entryKey: String, delta: Int) = settingsRepo.moveFavorite(entryKey, delta)
    fun removeFolderWidget(folderId: String, id: Int) {
        widgets.deleteId(id)
        settingsRepo.removeWidgetFromFolder(folderId, id)
    }
    fun removePopupWidget(app: AppInfo, id: Int) {
        widgets.deleteId(id)
        settingsRepo.edit { AppPopups.removeWidget(it, app.key, id) }
    }
    fun removeAppWidget(app: AppInfo) {
        settingsRepo.current.appWidgets[app.key]?.let { widgets.deleteId(it) }
        settingsRepo.setAppWidget(app.key, null)
    }
    fun toggleFavorite(app: AppInfo) = settingsRepo.toggleFavorite(app.key)
    fun moveFavorite(app: AppInfo, delta: Int) = settingsRepo.moveFavorite(app.key, delta)
    fun setHidden(app: AppInfo, hidden: Boolean) = settingsRepo.setHidden(app.key, hidden)
    fun rename(app: AppInfo, label: String?) = settingsRepo.rename(app.key, label)
    fun setSwipeAction(app: AppInfo, target: AppInfo?) = settingsRepo.setSwipeAction(app.key, target?.key)
    fun toggleMuted(app: AppInfo) = settingsRepo.toggleMuted(app.packageName)
    fun shortcuts(app: AppInfo): List<ShortcutInfo> = appsRepo.shortcutsFor(app)
    fun startShortcut(s: ShortcutInfo) = appsRepo.startShortcut(s)

    /** Toglie la scorciatoia dalla home e dal sistema. */
    fun removePinnedShortcut(pinned: PinnedShortcut) {
        settingsRepo.edit { PinnedShortcuts.remove(it, pinned.key) }
        appsRepo.unpinShortcut(pinned.packageName, pinned.userSerial, PinnedShortcuts.idsFor(settingsRepo.current, pinned.packageName, pinned.userSerial))
    }
    fun updateSettings(transform: (Settings) -> Settings) = settingsRepo.edit(transform)

    fun removeWidget(id: Int) {
        widgets.deleteId(id)
        settingsRepo.removeWidget(id)
    }

    /** Al primo avvio mettiamo qualche app tra i preferiti per non partire da una home vuota. */
    fun seedFavoritesIfNeeded() {
        viewModelScope.launch {
            if (settingsRepo.current.onboardingDone) return@launch
            val apps = state.value.allApps.ifEmpty { return@launch }
            val wanted = listOf("dialer", "phone", "messag", "camera", "chrome", "browser", "whatsapp", "photos", "gallery")
            val picks = wanted.mapNotNull { w -> apps.firstOrNull { it.packageName.contains(w, ignoreCase = true) } }
                .distinctBy { it.key }
                .take(6)
            settingsRepo.edit { it.copy(favorites = picks.map { a -> a.key }, onboardingDone = true) }
        }
    }
}
