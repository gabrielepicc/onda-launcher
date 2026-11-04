// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.io.OutputStream

/** Serializer DataStore: il file contiene il JSON prodotto da [SettingsCodec]. */
object SettingsSerializer : Serializer<Settings> {
    override val defaultValue: Settings = Settings()

    override suspend fun readFrom(input: InputStream): Settings =
        try {
            SettingsCodec.decode(input.readBytes().decodeToString())
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("Impostazioni illeggibili", e)
        }

    override suspend fun writeTo(t: Settings, output: OutputStream) {
        output.write(SettingsCodec.encode(t).encodeToByteArray())
    }
}

/** Importa una sola volta le impostazioni salvate da Onda 0.2/0.3 in SharedPreferences. */
private class LegacyPrefsMigration(context: Context) : DataMigration<Settings> {
    private val prefs = context.getSharedPreferences("onda_settings", Context.MODE_PRIVATE)

    override suspend fun shouldMigrate(currentData: Settings): Boolean = prefs.contains(LEGACY_KEY)

    override suspend fun migrate(currentData: Settings): Settings =
        prefs.getString(LEGACY_KEY, null)
            ?.let { runCatching { SettingsCodec.decode(it) }.getOrNull() }
            ?: currentData

    override suspend fun cleanUp() {
        prefs.edit().remove(LEGACY_KEY).apply()
    }

    companion object {
        const val LEGACY_KEY = "settings_json"
    }
}

/**
 * Preferenze persistenti del launcher.
 *
 * La UI legge uno [StateFlow] sincrono (nessuna attesa al primo frame); le scritture su disco
 * avvengono in background tramite DataStore, raggruppando le modifiche ravvicinate (es. slider).
 */
class SettingsRepository(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val store: DataStore<Settings> = DataStoreFactory.create(
        serializer = SettingsSerializer,
        migrations = listOf(LegacyPrefsMigration(context)),
        scope = scope,
        produceFile = { context.dataStoreFile("settings.json") },
    )

    // Lettura iniziale bloccante ma minuscola (un file JSON di pochi KB), così il primo frame
    // ha già i preferiti corretti.
    private val _settings = MutableStateFlow(runBlocking { store.data.first() })
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    val current: Settings get() = _settings.value

    init {
        scope.launch {
            _settings.drop(1).collectLatest { snapshot ->
                delay(WRITE_DEBOUNCE_MS)
                store.updateData { snapshot }
            }
        }
    }

    fun edit(transform: (Settings) -> Settings) {
        _settings.update(transform)
    }

    // --- Operazioni di comodo ---

    fun toggleFavorite(key: String) = edit { s ->
        if (key in s.favorites) s.copy(favorites = s.favorites - key)
        else s.copy(favorites = s.favorites + key)
    }

    fun moveFavorite(key: String, delta: Int) = edit { s ->
        val list = s.favorites.toMutableList()
        val from = list.indexOf(key)
        if (from < 0) return@edit s
        val to = (from + delta).coerceIn(0, list.lastIndex)
        list.removeAt(from)
        list.add(to, key)
        s.copy(favorites = list)
    }

    fun setHidden(key: String, hidden: Boolean) = edit { s ->
        s.copy(
            hidden = if (hidden) s.hidden + key else s.hidden - key,
            favorites = if (hidden) s.favorites - key else s.favorites,
        )
    }

    fun rename(key: String, newLabel: String?) = edit { s ->
        val map = s.renames.toMutableMap()
        if (newLabel.isNullOrBlank()) map.remove(key) else map[key] = newLabel.trim()
        s.copy(renames = map)
    }

    fun setSwipeAction(key: String, target: String?) = edit { s ->
        val map = s.swipeActions.toMutableMap()
        if (target == null) map.remove(key) else map[key] = target
        s.copy(swipeActions = map)
    }

    fun toggleMuted(packageName: String) = edit { s ->
        s.copy(
            mutedNotifications = if (packageName in s.mutedNotifications) s.mutedNotifications - packageName
            else s.mutedNotifications + packageName
        )
    }

    fun addWidget(id: Int) = edit { it.copy(widgetIds = it.widgetIds + id) }

    // --- Layout della home ---

    /** Sposta un blocco della home (orologio, media, widget, app) di [delta] posizioni. */
    fun moveBlock(block: String, delta: Int) = edit { s ->
        val list = HomeBlocks.resolve(s).toMutableList()
        val from = list.indexOf(block)
        if (from < 0) return@edit s
        val to = (from + delta).coerceIn(0, list.lastIndex)
        list.removeAt(from)
        list.add(to, block)
        s.copy(homeLayout = list)
    }

    fun setWidgetHeight(id: Int, dp: Int) = edit { it.copy(widgetHeights = it.widgetHeights + (id to dp)) }

    /** Sostituisce un widget della home con un altro nella stessa posizione e con la stessa altezza. */
    fun replaceWidget(oldId: Int, newId: Int) = edit { s ->
        if (s.widgetStacks.any { oldId in it.widgets }) return@edit WidgetStacks.replace(s, oldId, newId)
        val layout = HomeBlocks.resolve(s).map { if (it == HomeBlocks.widget(oldId)) HomeBlocks.widget(newId) else it }
        val height = s.widgetHeights[oldId]
        s.copy(
            widgetIds = s.widgetIds.map { if (it == oldId) newId else it },
            homeLayout = layout,
            widgetHeights = (s.widgetHeights - oldId).let { m -> if (height != null) m + (newId to height) else m },
        )
    }

    /** Aggiunge un widget a una pila, oppure trasforma un widget singolo in una pila. */
    fun addToStack(block: String, newId: Int) = edit {
        WidgetStacks.add(it, block, newId, "s" + System.currentTimeMillis().toString(36))
    }

    fun setStackHeight(stackId: String, dp: Int) = edit { WidgetStacks.setHeight(it, stackId, dp) }

    /** Mette un widget al posto dell'orologio (che viene nascosto). */
    fun replaceClockWithWidget(newId: Int) = edit { s ->
        val layout = HomeBlocks.resolve(s).map { if (it == HomeBlocks.CLOCK) HomeBlocks.widget(newId) else it } + HomeBlocks.CLOCK
        s.copy(widgetIds = s.widgetIds + newId, homeLayout = layout, showClock = false)
    }

    // --- Cartelle ---

    fun createFolder(name: String, firstApp: String?): String {
        val id = "f" + System.currentTimeMillis().toString(36)
        edit { s ->
            val apps = listOfNotNull(firstApp)
            s.copy(
                folders = s.folders + (id to Folder(id, name.ifBlank { "Cartella" }, apps)),
                favorites = (s.favorites - apps.toSet()) + (FOLDER_PREFIX + id),
            )
        }
        return id
    }

    fun renameFolder(id: String, name: String) = edit { s ->
        val f = s.folders[id] ?: return@edit s
        s.copy(folders = s.folders + (id to f.copy(name = name.ifBlank { f.name })))
    }

    /** Elimina la cartella; le app tornano tra i preferiti. Restituisce i widget da rilasciare. */
    fun deleteFolder(id: String): List<Int> {
        val widgets = current.folders[id]?.widgets.orEmpty()
        edit { s ->
            val f = s.folders[id] ?: return@edit s
            val list = s.favorites.toMutableList()
            val at = list.indexOf(FOLDER_PREFIX + id).coerceAtLeast(0)
            list.remove(FOLDER_PREFIX + id)
            list.addAll(at.coerceAtMost(list.size), f.apps.filter { it !in list })
            s.copy(folders = s.folders - id, favorites = list)
        }
        return widgets
    }

    /** Sposta un'app in una cartella (null = fuori da tutte, tra i preferiti). */
    fun moveAppToFolder(appKey: String, folderId: String?) = edit { s ->
        val cleaned = s.folders.mapValues { (_, f) -> f.copy(apps = f.apps - appKey) }
        if (folderId == null) {
            s.copy(folders = cleaned, favorites = if (appKey in s.favorites) s.favorites else s.favorites + appKey)
        } else {
            val target = cleaned[folderId] ?: return@edit s
            s.copy(
                folders = cleaned + (folderId to target.copy(apps = target.apps + appKey)),
                favorites = s.favorites - appKey,
            )
        }
    }

    fun addWidgetToFolder(folderId: String, widgetId: Int) = edit { s ->
        val f = s.folders[folderId] ?: return@edit s
        s.copy(folders = s.folders + (folderId to f.copy(widgets = f.widgets + widgetId)))
    }

    fun removeWidgetFromFolder(folderId: String, widgetId: Int) = edit { s ->
        val f = s.folders[folderId] ?: return@edit s
        s.copy(folders = s.folders + (folderId to f.copy(widgets = f.widgets - widgetId)))
    }

    fun setAppWidget(appKey: String, widgetId: Int?) = edit { s ->
        s.copy(appWidgets = if (widgetId == null) s.appWidgets - appKey else s.appWidgets + (appKey to widgetId))
    }

    fun setIconOverride(appKey: String, packPackage: String?, drawable: String?) = edit { s ->
        s.copy(
            iconOverrides = if (packPackage == null || drawable == null) s.iconOverrides - appKey
            else s.iconOverrides + (appKey to "$packPackage|$drawable"),
        )
    }

    // --- Theme Studio ---

    fun saveCurrentAsTheme(name: String): LookTheme {
        val theme = LookTheme.capture(current, "t" + System.currentTimeMillis().toString(36), name.ifBlank { "Tema" })
        edit { it.copy(savedThemes = it.savedThemes + theme, stylePreset = "theme:${theme.id}") }
        return theme
    }

    fun applyTheme(theme: LookTheme) = edit { theme.applyTo(it) }

    fun renameTheme(id: String, name: String) = edit { s ->
        s.copy(savedThemes = s.savedThemes.map { if (it.id == id) it.copy(name = name.ifBlank { it.name }) else it })
    }

    fun duplicateTheme(id: String, copySuffix: String) = edit { s ->
        val src = s.savedThemes.firstOrNull { it.id == id } ?: return@edit s
        s.copy(savedThemes = s.savedThemes + src.copy(id = "t" + System.currentTimeMillis().toString(36), name = "${src.name} $copySuffix"))
    }

    fun deleteTheme(id: String) = edit { s -> s.copy(savedThemes = s.savedThemes.filterNot { it.id == id }) }

    fun addImportedTheme(theme: LookTheme) = edit { s ->
        s.copy(savedThemes = s.savedThemes + theme.copy(id = "t" + System.currentTimeMillis().toString(36)))
    }

    fun toggleProtected(appKey: String) = edit { s ->
        s.copy(protectedApps = if (appKey in s.protectedApps) s.protectedApps - appKey else s.protectedApps + appKey)
    }

    fun recordLaunch(appKey: String) = edit { s ->
        s.copy(launchCounts = s.launchCounts + (appKey to (s.launchCounts[appKey] ?: 0) + 1))
    }
    fun removeWidget(id: Int) = edit { s ->
        WidgetStacks.remove(s, id).let {
            it.copy(
                widgetIds = it.widgetIds - id,
                homeLayout = it.homeLayout - HomeBlocks.widget(id),
                widgetHeights = it.widgetHeights - id,
            )
        }
    }

    // --- Backup ---

    /** JSON delle impostazioni, senza i widget (gli id sono validi solo su questo dispositivo). */
    fun exportJson(): String = SettingsCodec.encode(
        current.copy(
            widgetIds = emptyList(),
            widgetStacks = emptyList(),
            appWidgets = emptyMap(),
            folders = current.folders.mapValues { (_, f) -> f.copy(widgets = emptyList()) },
        ),
        prettyPrint = true,
    )

    /** Importa un backup (formato attuale o di Onda 0.2/0.3). I widget presenti restano. */
    fun importJson(json: String): Boolean {
        val parsed = runCatching { SettingsCodec.decode(json) }.getOrNull() ?: return false
        edit { now ->
            parsed.copy(
                widgetIds = now.widgetIds,
                widgetStacks = now.widgetStacks,
                appWidgets = now.appWidgets,
                folders = parsed.folders.mapValues { (id, f) -> f.copy(widgets = now.folders[id]?.widgets.orEmpty()) },
                onboardingDone = true,
            )
        }
        return true
    }

    private companion object {
        const val WRITE_DEBOUNCE_MS = 150L
    }
}
