// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.gabrielepiccione.onda.ui.settings

import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.KeyboardArrowDown
import com.gabrielepiccione.onda.data.ProfileSuggestion
import com.gabrielepiccione.onda.ui.home.ProfileSuggestionCard
import com.gabrielepiccione.onda.ui.home.suggestionCondition
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.service.UsageReader
import com.gabrielepiccione.onda.data.UsageReminders
import com.gabrielepiccione.onda.data.UsageProfiles
import androidx.compose.runtime.LaunchedEffect
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.data.ContextProfile
import com.gabrielepiccione.onda.data.ContextProfiles
import com.gabrielepiccione.onda.data.LaunchLog
import com.gabrielepiccione.onda.data.RuleSuggestion
import com.gabrielepiccione.onda.data.Settings
import com.gabrielepiccione.onda.data.Trigger
import com.gabrielepiccione.onda.data.TriggerGroup
import com.gabrielepiccione.onda.data.TriggerType
import com.gabrielepiccione.onda.ui.components.AppIcon
import com.gabrielepiccione.onda.ui.theme.AccentChoices
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

/** Quello che la pagina chiede al telefono: letture del contesto e permessi. */
interface ContextEnvironment {
    fun currentWifi(): String?
    fun currentLocation(): Pair<Double, Double>?
    fun bondedDevices(): List<String>
    fun requestLocation()
    fun requestCalendar()
    fun requestBluetooth()
}

/** Pagina dei preferiti contestuali: slot fissi, profili con le loro regole e regole proposte. */
@Composable
internal fun ContextProfilesPage(
    settings: Settings,
    allApps: List<AppInfo>,
    active: ContextProfile?,
    env: ContextEnvironment,
    onEdit: ((Settings) -> Settings) -> Unit,
    /** Profilo proposto da "Modifica": si apre già nell'editor. */
    startWith: ContextProfile? = null,
    usageAccess: Boolean = false,
    onUsageAccess: () -> Unit = {},
) {
    val colors = LocalOndaColors.current
    val context = LocalContext.current
    var editing by remember { mutableStateOf(startWith) }
    var explain by remember { mutableStateOf(false) }
    // Cosa ha visto Onda finora: spiega perché una proposta c'è o non c'è ancora
    var diagnosis by remember { mutableStateOf<UsageProfiles.Diagnosis?>(null) }
    // Tutti i suggerimenti, senza il limite della home; «Analizza adesso» rilancia il calcolo
    var pool by remember { mutableStateOf<List<ProfileSuggestion>>(emptyList()) }
    var analysisRun by remember { mutableIntStateOf(0) }
    var removing by remember { mutableStateOf(emptySet<String>()) }
    var ignoredOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val today = java.time.LocalDate.now().toString()
    LaunchedEffect(settings.profileSuggest.enabled, settings.profileSuggest.useWifi, usageAccess, allApps.size, analysisRun) {
        if (settings.profileSuggest.enabled) {
            val now = System.currentTimeMillis()
            val zone = java.time.ZoneId.systemDefault()
            val fixed = settings.favorites.take(settings.fixedFavoriteSlots).map { UsageReminders.packageOf(it) }.toSet() + context.packageName
            val events = UsageReader(context).read(now, allApps.map { it.packageName }.toSet())
            diagnosis = UsageProfiles.diagnose(events, now, zone, fixed, settings.profileSuggest.useWifi)
            pool = UsageProfiles.suggest(events, now, zone, fixed, settings.profileSuggest.useWifi)
            onEdit { it.copy(profileSuggest = UsageProfiles.analyzed(it.profileSuggest, now)) }
        } else {
            diagnosis = null
            pool = emptyList()
        }
    }
    /** Le app del suggerimento come chiavi: la prima app con quel pacchetto. */
    fun profileFrom(s: ProfileSuggestion): ContextProfile =
        UsageProfiles.toProfile(s, java.util.UUID.randomUUID().toString())
            .let { p -> p.copy(apps = p.apps.mapNotNull { pkg -> allApps.firstOrNull { it.packageName == pkg }?.key }) }
    /** La scheda esce con l'animazione, poi si applica la scelta (che la toglie anche dalla home). */
    fun leave(s: ProfileSuggestion, change: (Settings) -> Settings) {
        removing = removing + s.key
        scope.launch {
            delay(LEAVE_MS)
            onEdit(change)
            removing = removing - s.key
        }
    }
    val byKey = remember(allApps) { allApps.associateBy { it.key } }
    val newProfileName = stringResource(R.string.ctx_new_profile)
    val meetingName = stringResource(R.string.ctx_meeting_profile)

    Card(stringResource(R.string.ctx_how_title)) {
        Text(stringResource(R.string.ctx_how), color = colors.onWallpaper, style = MaterialTheme.typography.bodyMedium)
        Text(
            if (active != null) stringResource(R.string.ctx_active_now, active.name) else stringResource(R.string.ctx_active_none),
            color = colors.accent,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 12.dp),
        )
    }

    // Suggerisci profili: spento di default; il permesso si chiede qui, dopo la spiegazione, e si può revocare
    Card(stringResource(R.string.suggest_title)) {
        Text(stringResource(R.string.suggest_sub), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
        SettingsSwitch(stringResource(R.string.suggest_enable), null, settings.profileSuggest.enabled) { on ->
            if (on && !usageAccess) explain = true else onEdit { it.copy(profileSuggest = it.profileSuggest.copy(enabled = on)) }
        }
        if (!settings.profileSuggest.enabled) SettingsSupporting(stringResource(R.string.suggest_list_off))
        if (settings.profileSuggest.enabled) {
            SettingsSwitch(stringResource(R.string.suggest_wifi), stringResource(R.string.suggest_wifi_sub), settings.profileSuggest.useWifi) { on ->
                onEdit { it.copy(profileSuggest = it.profileSuggest.copy(useWifi = on)) }
            }
            val visible = UsageProfiles.forSettings(settings.profileSuggest, pool, today)
            if (!usageAccess) {
                // Senza il permesso dei dati d'uso: invito ad attivarlo, non un elenco vuoto muto
                Text(stringResource(R.string.suggest_need_access), color = colors.onWallpaper, style = MaterialTheme.typography.bodyMedium)
                FilledTonalButton(onClick = onUsageAccess, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.suggest_grant_access)) }
            }
            if (visible.isEmpty()) {
                val last = settings.profileSuggest.lastAnalysisAt
                Text(
                    if (last > 0) stringResource(R.string.suggest_empty, analysisTime(last)) else stringResource(R.string.suggest_empty_never),
                    color = colors.onWallpaper,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                diagnosis?.let { d ->
                    SettingsSupporting(
                        when {
                            d.openings == 0 -> stringResource(R.string.suggest_status_no_data)
                            d.historyDays < UsageProfiles.MIN_HISTORY_DAYS ->
                                stringResource(R.string.suggest_status_history, d.openings, d.historyDays, UsageProfiles.MIN_HISTORY_DAYS - d.historyDays)
                            else -> stringResource(R.string.suggest_status_no_pattern, d.openings)
                        },
                    )
                }
            } else {
                Text(
                    stringResource(R.string.suggest_list_count, visible.size - removing.count { k -> visible.any { it.key == k } }),
                    color = colors.onWallpaper,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
                visible.forEach { s ->
                    key(s.key) {
                        ProfileSuggestionCard(
                            suggestion = if (s.key in removing) null else s,
                            apps = s.apps.mapNotNull { p -> allApps.firstOrNull { it.packageName == p } },
                            themedIcons = settings.themedIcons,
                            onCreate = {
                                val profile = profileFrom(s)
                                leave(s) { st -> st.copy(contextProfiles = st.contextProfiles + profile, profileSuggest = UsageProfiles.accepted(st.profileSuggest, s, today)) }
                            },
                            onEdit = {
                                onEdit { st -> st.copy(profileSuggest = UsageProfiles.accepted(st.profileSuggest, s, today)) }
                                editing = profileFrom(s)
                            },
                            onIgnore = { leave(s) { st -> st.copy(profileSuggest = UsageProfiles.ignore(st.profileSuggest, s, today)) } },
                            horizontalInset = 0.dp,
                        )
                    }
                }
            }
            val canAnalyze = UsageProfiles.canAnalyzeNow(settings.profileSuggest, System.currentTimeMillis())
            FilledTonalButton(onClick = { analysisRun++ }, enabled = canAnalyze, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.suggest_analyze_now))
            }
            if (!canAnalyze) SettingsSupporting(stringResource(R.string.suggest_analyze_wait))
            // Ignorati: sezione compressa, solo se ce ne sono
            val ignored = UsageProfiles.ignoredKeys(settings.profileSuggest, today)
            if (ignored.isNotEmpty()) {
                SettingsItem(
                    headline = stringResource(R.string.suggest_ignored, ignored.size),
                    onClick = { ignoredOpen = !ignoredOpen },
                    trailing = { Icon(if (ignoredOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null) },
                )
                if (ignoredOpen) {
                    ignored.forEach { (key, _) ->
                        val parts = UsageProfiles.describe(key)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(suggestionCondition(parts.days, parts.slot, parts.signal), color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    parts.apps.joinToString(", ") { p -> allApps.firstOrNull { it.packageName == p }?.label ?: p },
                                    color = colors.onWallpaperMuted,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            TextButton(onClick = { onEdit { st -> st.copy(profileSuggest = UsageProfiles.restore(st.profileSuggest, key)) } }) {
                                Text(stringResource(R.string.suggest_restore))
                            }
                        }
                    }
                }
            }
            SettingsItem(
                headline = stringResource(R.string.suggest_revoke),
                supporting = stringResource(if (usageAccess) R.string.suggest_revoke_sub else R.string.suggest_no_access),
                onClick = onUsageAccess,
            )
        }
    }

    if (explain) {
        AlertDialog(
            onDismissRequest = { explain = false },
            title = { Text(stringResource(R.string.suggest_explain_title)) },
            text = { Text(stringResource(R.string.suggest_explain_body)) },
            confirmButton = {
                TextButton(onClick = {
                    explain = false
                    onEdit { it.copy(profileSuggest = it.profileSuggest.copy(enabled = true)) }
                    onUsageAccess()
                }) { Text(stringResource(R.string.suggest_explain_go)) }
            },
            dismissButton = { TextButton(onClick = { explain = false }) { Text(stringResource(R.string.breaker_cancel)) } },
        )
    }

    Card(stringResource(R.string.ctx_fixed_title)) {
        Text(stringResource(R.string.ctx_fixed_sub), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
        val fixedNames = settings.favorites.take(settings.fixedFavoriteSlots).map { byKey[it]?.label ?: "…" }
        Text(
            if (fixedNames.isEmpty()) stringResource(R.string.ctx_fixed_none) else fixedNames.joinToString(" · "),
            color = colors.onWallpaper,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp),
        )
        Slider(
            value = settings.fixedFavoriteSlots.toFloat(),
            onValueChange = { v -> onEdit { it.copy(fixedFavoriteSlots = v.roundToInt().coerceIn(0, ContextProfiles.MAX_FIXED_SLOTS)) } },
            valueRange = 0f..ContextProfiles.MAX_FIXED_SLOTS.toFloat(),
            steps = ContextProfiles.MAX_FIXED_SLOTS - 1,
        )
    }

    Card(stringResource(R.string.ctx_profiles_title)) {
        if (settings.contextProfiles.isEmpty()) {
            Text(stringResource(R.string.ctx_profiles_none), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
        }
        settings.contextProfiles.forEach { p ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { editing = p }
                    .padding(vertical = 8.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        p.name + if (active?.id == p.id) "  •" else "",
                        color = colors.onWallpaper,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(profileSummary(p), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
                }
                Switch(
                    checked = p.enabled,
                    onCheckedChange = { v -> onEdit { s -> s.copy(contextProfiles = s.contextProfiles.map { if (it.id == p.id) it.copy(enabled = v) else it }) } },
                    modifier = Modifier.semantics { contentDescription = p.name },
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            FilledTonalButton(onClick = { editing = ContextProfile(UUID.randomUUID().toString(), newProfileName, priority = 1) }) {
                Text(stringResource(R.string.ctx_add))
            }
            OutlinedButton(onClick = {
                val installed = ContextProfiles.MEETING_PACKAGES.mapNotNull { pkg -> allApps.firstOrNull { it.packageName == pkg }?.key }
                editing = ContextProfiles.meetingTemplate(UUID.randomUUID().toString(), meetingName, installed)
            }) { Text(stringResource(R.string.ctx_add_meeting)) }
        }
    }

    val suggestions = remember(settings.contextProfiles, settings.dismissedSuggestions) {
        ContextProfiles.suggest(LaunchLog.read(context), System.currentTimeMillis(), ZoneId.systemDefault(), settings.contextProfiles, settings.dismissedSuggestions)
            .filter { byKey.containsKey(it.appKey) }
            .take(3)
    }
    if (suggestions.isNotEmpty()) {
        Card(stringResource(R.string.ctx_suggestions_title)) {
            Text(stringResource(R.string.ctx_suggestions_sub), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
            suggestions.forEach { s ->
                val label = byKey[s.appKey]?.label ?: s.appKey
                val day = dayLong(s.dayOfWeek)
                Text(
                    stringResource(R.string.ctx_suggestion, day, s.hour, label, s.weeks),
                    color = colors.onWallpaper,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val name = stringResource(R.string.ctx_suggestion_name, label, day, s.hour)
                    TextButton(onClick = { onEdit { st -> st.copy(dismissedSuggestions = st.dismissedSuggestions + s.id) } }) {
                        Text(stringResource(R.string.ctx_suggestion_ignore))
                    }
                    FilledTonalButton(onClick = { editing = ContextProfiles.fromSuggestion(s, name, UUID.randomUUID().toString()) }) {
                        Text(stringResource(R.string.ctx_suggestion_create))
                    }
                }
            }
        }
    }

    editing?.let { profile ->
        ProfileEditor(
            initial = profile,
            isNew = settings.contextProfiles.none { it.id == profile.id },
            allApps = allApps,
            env = env,
            onSave = { saved ->
                onEdit { s ->
                    val exists = s.contextProfiles.any { it.id == saved.id }
                    s.copy(contextProfiles = if (exists) s.contextProfiles.map { if (it.id == saved.id) saved else it } else s.contextProfiles + saved)
                }
                editing = null
            },
            onDelete = {
                onEdit { s -> s.copy(contextProfiles = s.contextProfiles.filterNot { it.id == profile.id }) }
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) = SettingsSection(title) { content() }

/** Le regole in una riga: "Wi-Fi Casa e Lun–Ven 9:00–18:00, oppure In carica". */
@Composable
private fun profileSummary(p: ContextProfile): String {
    val or = stringResource(R.string.ctx_or)
    val and = stringResource(R.string.ctx_and)
    val groups = p.groups.filter { it.triggers.isNotEmpty() }.map { g -> g.triggers.map { triggerSummary(it) }.joinToString(" $and ") }
    val rules = if (groups.isEmpty()) stringResource(R.string.ctx_no_conditions) else groups.joinToString(", $or ")
    return stringResource(R.string.ctx_summary, rules, p.priority, p.apps.size)
}

@Composable
private fun triggerSummary(t: Trigger): String {
    val base = when (t.type) {
        TriggerType.WIFI -> stringResource(R.string.ctx_t_wifi_is, t.text.ifBlank { "?" })
        TriggerType.BLUETOOTH -> stringResource(R.string.ctx_t_bt_is, t.text.ifBlank { "?" })
        TriggerType.PLACE -> stringResource(R.string.ctx_t_place_is, t.radiusMeters)
        TriggerType.TIME -> stringResource(R.string.ctx_t_time_is, daysText(t.days), minuteText(t.startMinute), minuteText(t.endMinute))
        TriggerType.CALENDAR -> if (t.text.isBlank()) stringResource(R.string.ctx_t_calendar_any) else stringResource(R.string.ctx_t_calendar_is, t.text)
        TriggerType.CHARGING -> stringResource(R.string.ctx_t_charging)
        TriggerType.HEADPHONES -> stringResource(R.string.ctx_t_headphones)
        TriggerType.FOCUS -> stringResource(R.string.ctx_t_focus)
    }
    return if (t.negate) stringResource(R.string.ctx_not, base) else base
}

@Composable
private fun typeName(type: TriggerType): String = stringResource(
    when (type) {
        TriggerType.WIFI -> R.string.ctx_type_wifi
        TriggerType.BLUETOOTH -> R.string.ctx_type_bluetooth
        TriggerType.PLACE -> R.string.ctx_type_place
        TriggerType.TIME -> R.string.ctx_type_time
        TriggerType.CALENDAR -> R.string.ctx_type_calendar
        TriggerType.CHARGING -> R.string.ctx_type_charging
        TriggerType.HEADPHONES -> R.string.ctx_type_headphones
        TriggerType.FOCUS -> R.string.ctx_type_focus
    },
)

private fun minuteText(m: Int): String = "%d:%02d".format(m / 60 % 24, m % 60)

private fun dayShort(day: Int): String = ContextProfiles.dayName(day).getDisplayName(TextStyle.SHORT, Locale.getDefault())

private fun dayLong(day: Int): String = ContextProfiles.dayName(day).getDisplayName(TextStyle.FULL, Locale.getDefault())

@Composable
private fun daysText(days: Set<Int>): String =
    if (days.isEmpty() || days.size == 7) stringResource(R.string.ctx_every_day) else days.sorted().joinToString(", ") { dayShort(it) }

/** Modifica completa di un profilo, a schermo intero. */
@Composable
private fun ProfileEditor(
    initial: ContextProfile,
    isNew: Boolean,
    allApps: List<AppInfo>,
    env: ContextEnvironment,
    onSave: (ContextProfile) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalOndaColors.current
    var p by remember(initial.id) { mutableStateOf(initial.copy(groups = initial.groups.ifEmpty { listOf(TriggerGroup()) })) }
    var picking by remember { mutableStateOf(false) }
    val byKey = remember(allApps) { allApps.associateBy { it.key } }

    fun updateGroup(index: Int, group: TriggerGroup) {
        p = p.copy(groups = p.groups.mapIndexed { i, g -> if (i == index) group else g })
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.scrim)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            TopAppBar(
                title = { Text(stringResource(if (isNew) R.string.ctx_editor_new else R.string.ctx_editor_edit)) },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, stringResource(R.string.close)) } },
                actions = {
                    TextButton(onClick = { onSave(p.copy(name = p.name.ifBlank { initial.name }, groups = p.groups.filter { it.triggers.isNotEmpty() })) }) {
                        Text(stringResource(R.string.wellbeing_limit_save))
                    }
                },
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.scrim,
                    titleContentColor = colors.onWallpaper,
                    navigationIconContentColor = colors.onWallpaper,
                ),
            )
            Column(
                Modifier
                    .weight(1f)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                OutlinedTextField(
                    value = p.name,
                    onValueChange = { p = p.copy(name = it) },
                    label = { Text(stringResource(R.string.ctx_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.ctx_priority), color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.ctx_priority_sub), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = { p = p.copy(priority = (p.priority - 1).coerceAtLeast(0)) }) { Text("−") }
                    Text("${p.priority}", color = colors.onWallpaper, style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { p = p.copy(priority = (p.priority + 1).coerceAtMost(99)) }) { Text("+") }
                }

                Card(stringResource(R.string.ctx_conditions)) {
                    p.groups.forEachIndexed { gi, group ->
                        if (gi > 0) {
                            Text(stringResource(R.string.ctx_or_upper), color = colors.accent, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 8.dp))
                        }
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.panelHigh)
                                .padding(12.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.ctx_group_all), color = colors.onWallpaperMuted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                                if (p.groups.size > 1) {
                                    IconButton(onClick = { p = p.copy(groups = p.groups.filterIndexed { i, _ -> i != gi }) }) {
                                        Icon(Icons.Filled.Delete, stringResource(R.string.ctx_remove_group), tint = colors.onWallpaperMuted)
                                    }
                                }
                            }
                            group.triggers.forEachIndexed { ti, t ->
                                TriggerEditor(
                                    trigger = t,
                                    env = env,
                                    onChange = { changed -> updateGroup(gi, group.copy(triggers = group.triggers.mapIndexed { i, x -> if (i == ti) changed else x })) },
                                    onRemove = { updateGroup(gi, group.copy(triggers = group.triggers.filterIndexed { i, _ -> i != ti })) },
                                )
                            }
                            AddTrigger { type ->
                                when (type) {
                                    TriggerType.BLUETOOTH -> env.requestBluetooth()
                                    TriggerType.WIFI, TriggerType.PLACE -> env.requestLocation()
                                    TriggerType.CALENDAR -> env.requestCalendar()
                                    else -> Unit
                                }
                                updateGroup(gi, group.copy(triggers = group.triggers + Trigger(type)))
                            }
                        }
                    }
                    TextButton(onClick = { p = p.copy(groups = p.groups + TriggerGroup()) }, modifier = Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.ctx_add_group))
                    }
                }

                Card(stringResource(R.string.ctx_apps)) {
                    Text(stringResource(R.string.ctx_apps_sub), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
                    p.apps.forEachIndexed { i, key ->
                        val app = byKey[key]
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            if (app != null) AppIcon(app, 32.dp, false) else Spacer(Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(app?.label ?: key, color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            if (i > 0) {
                                IconButton(onClick = { p = p.copy(apps = p.apps.toMutableList().apply { add(i - 1, removeAt(i)) }) }) {
                                    Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.move_up), tint = colors.onWallpaperMuted)
                                }
                            }
                            IconButton(onClick = { p = p.copy(apps = p.apps - key) }) {
                                Icon(Icons.Filled.Close, stringResource(R.string.remove), tint = colors.onWallpaperMuted)
                            }
                        }
                    }
                    TextButton(onClick = { picking = true }) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.ctx_add_app))
                    }
                }

                Card(stringResource(R.string.ctx_accent)) {
                    Text(stringResource(R.string.ctx_accent_sub), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
                    FilterChip(selected = p.accent == 0L, onClick = { p = p.copy(accent = 0L) }, label = { Text(stringResource(R.string.ctx_accent_none)) })
                    ColorSwatches(AccentChoices, p.accent) { argb -> p = p.copy(accent = argb) }
                }

                if (!isNew) {
                    TextButton(onClick = onDelete, modifier = Modifier.padding(vertical = 16.dp)) {
                        Icon(Icons.Filled.Delete, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.ctx_delete))
                    }
                }
            }
        }
    }

    if (picking) {
        AppPicker(allApps.filterNot { it.key in p.apps }, onPick = { a -> p = p.copy(apps = p.apps + a.key); picking = false }, onDismiss = { picking = false })
    }
}

@Composable
private fun AddTrigger(onAdd: (TriggerType) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Icon(Icons.Filled.Add, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.ctx_add_condition))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            TriggerType.entries.forEach { type ->
                DropdownMenuItem(text = { Text(typeName(type)) }, onClick = { open = false; onAdd(type) })
            }
        }
    }
}

/** Una condizione e i suoi parametri, secondo il tipo. */
@Composable
private fun TriggerEditor(trigger: Trigger, env: ContextEnvironment, onChange: (Trigger) -> Unit, onRemove: () -> Unit) {
    val colors = LocalOndaColors.current
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(typeName(trigger.type), color = colors.onWallpaper, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, stringResource(R.string.remove), tint = colors.onWallpaperMuted) }
        }
        when (trigger.type) {
            TriggerType.WIFI -> {
                OutlinedTextField(trigger.text, { onChange(trigger.copy(text = it)) }, label = { Text(stringResource(R.string.ctx_wifi_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { env.currentWifi()?.let { onChange(trigger.copy(text = it)) } ?: env.requestLocation() }) {
                    Text(stringResource(R.string.ctx_use_current_wifi))
                }
            }
            TriggerType.BLUETOOTH -> {
                OutlinedTextField(trigger.text, { onChange(trigger.copy(text = it)) }, label = { Text(stringResource(R.string.ctx_bt_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                val devices = remember { env.bondedDevices() }
                if (devices.isEmpty()) {
                    TextButton(onClick = { env.requestBluetooth() }) { Text(stringResource(R.string.ctx_bt_permission)) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    devices.forEach { d -> FilterChip(selected = trigger.text == d, onClick = { onChange(trigger.copy(text = d)) }, label = { Text(d) }) }
                }
            }
            TriggerType.PLACE -> {
                val set = trigger.latitude != 0.0 || trigger.longitude != 0.0
                Text(
                    stringResource(if (set) R.string.ctx_place_set else R.string.ctx_place_unset),
                    color = colors.onWallpaperMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = {
                    env.currentLocation()?.let { (lat, lon) -> onChange(trigger.copy(latitude = lat, longitude = lon)) } ?: env.requestLocation()
                }) { Text(stringResource(R.string.ctx_use_current_place)) }
                Text(stringResource(R.string.ctx_radius, trigger.radiusMeters), color = colors.onWallpaper, style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = trigger.radiusMeters.toFloat(),
                    onValueChange = { onChange(trigger.copy(radiusMeters = ((it / 100f).roundToInt() * 100).coerceIn(100, 2000))) },
                    valueRange = 100f..2000f,
                )
            }
            TriggerType.TIME -> {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..7).forEach { d ->
                        FilterChip(
                            selected = d in trigger.days,
                            onClick = { onChange(trigger.copy(days = if (d in trigger.days) trigger.days - d else trigger.days + d)) },
                            label = { Text(dayShort(d)) },
                        )
                    }
                }
                Text(stringResource(R.string.ctx_from, minuteText(trigger.startMinute)), color = colors.onWallpaper, style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = trigger.startMinute.toFloat(),
                    onValueChange = { onChange(trigger.copy(startMinute = ((it / 15f).roundToInt() * 15).coerceIn(0, 1425))) },
                    valueRange = 0f..1425f,
                )
                Text(stringResource(R.string.ctx_to, minuteText(trigger.endMinute)), color = colors.onWallpaper, style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = trigger.endMinute.toFloat(),
                    onValueChange = { onChange(trigger.copy(endMinute = ((it / 15f).roundToInt() * 15).coerceIn(0, 1440))) },
                    valueRange = 0f..1440f,
                )
            }
            TriggerType.CALENDAR -> {
                OutlinedTextField(trigger.text, { onChange(trigger.copy(text = it)) }, label = { Text(stringResource(R.string.ctx_calendar_word)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.ctx_calendar_hint), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
            }
            TriggerType.CHARGING, TriggerType.HEADPHONES, TriggerType.FOCUS -> Unit
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = trigger.negate, role = Role.Checkbox) { onChange(trigger.copy(negate = it)) },
        ) {
            Checkbox(checked = trigger.negate, onCheckedChange = null)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.ctx_negate), color = colors.onWallpaper, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
internal fun AppPicker(apps: List<AppInfo>, onPick: (AppInfo) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val shown = remember(apps, query) { apps.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ctx_add_app)) },
        text = {
            Column {
                OutlinedTextField(query, { query = it }, singleLine = true, placeholder = { Text(stringResource(R.string.search)) }, modifier = Modifier.fillMaxWidth())
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(shown, key = { it.key }) { app ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { onPick(app) }.heightIn(min = 48.dp).padding(vertical = 4.dp),
                        ) {
                            AppIcon(app, 32.dp, false)
                            Spacer(Modifier.width(12.dp))
                            Text(app.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

/** Per i suggerimenti mostrati altrove: il testo della regola proposta. */
@Composable
internal fun suggestionText(s: RuleSuggestion, appLabel: String): String =
    stringResource(R.string.ctx_suggestion, dayLong(s.dayOfWeek), s.hour, appLabel, s.weeks)

/** Durata dell'uscita di una scheda prima di applicare Crea o Ignora. */
private const val LEAVE_MS = 320L

/** Data e ora dell'ultima analisi, nel formato breve della lingua del telefono. */
private fun analysisTime(ms: Long): String =
    java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.SHORT))
