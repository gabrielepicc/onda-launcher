// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Un'apertura di app con il contesto in cui è avvenuta, se lo si conosce. Il Wi-Fi è solo un codice (hash locale). */
data class UsageEventCtx(
    val packageName: String,
    val time: Long,
    val headphones: Boolean = false,
    val charging: Boolean = false,
    val car: Boolean = false,
    val wifiHash: String? = null,
)

/** Fasce della giornata usate per raggruppare l'uso. */
enum class DaySlot(val startMinute: Int, val endMinute: Int) {
    MORNING(6 * 60, 12 * 60),
    AFTERNOON(12 * 60, 18 * 60),
    EVENING(18 * 60, 23 * 60),
    NIGHT(23 * 60, 6 * 60),
    ;

    fun contains(minute: Int): Boolean =
        if (startMinute < endMinute) minute in startMinute until endMinute else minute >= startMinute || minute < endMinute
}

/** Giorni a cui si riferisce un profilo: lavorativi o fine settimana. */
enum class DayKind(val days: Set<Int>) {
    WEEKDAYS(setOf(1, 2, 3, 4, 5)),
    WEEKEND(setOf(6, 7)),
}

/** Segnale in più che accompagna la fascia, se l'uso lo mostra con costanza. */
enum class SuggestedSignal { HEADPHONES, CHARGING, CAR, WIFI }

/** Un profilo proposto: nome, 3–6 app, fascia e, se c'è, il segnale. Non si attiva mai da solo. */
data class ProfileSuggestion(
    val name: String,
    val apps: List<String>,
    val days: DayKind?,
    val slot: DaySlot?,
    val signal: SuggestedSignal?,
    /** In quanti giorni diversi si è visto questo comportamento: più è alto, più è solido. */
    val strength: Int,
    /** Codice del Wi-Fi, solo per il segnale WIFI. */
    val wifiHash: String? = null,
    /** In quanti giorni almeno tre di queste app si sono usate insieme in quella fascia o condizione. */
    val togetherDays: Int = 0,
    /** Su quanti giorni di storia (al massimo [UsageProfiles.WINDOW_DAYS]). */
    val historyDays: Int = 0,
) {
    /** Identifica la proposta per ricordare un rifiuto: fascia, segnale e app. */
    val key: String
        get() = listOf(days?.name.orEmpty(), slot?.name.orEmpty(), signal?.name.orEmpty(), apps.sorted().joinToString(",")).joinToString("|")
}

/** Ricordi dell'utente sui suggerimenti di profili: nuovi campi con default, nessuna migrazione. */
@Serializable
data class ProfileSuggestState(
    /** Spento di default: si attiva dalla voce "Suggerisci profili", dopo la spiegazione. */
    val enabled: Boolean = false,
    /** Il Wi-Fi come segnale (hash locale, nessuna posizione): spento di default. */
    val useWifi: Boolean = false,
    /** Proposte ignorate: chiave -> giorno ("AAAA-MM-GG") dell'ultimo "Ignora". */
    val ignored: Map<String, String> = emptyMap(),
    /** Non più usato (la regola ora è [lastShownAt]); resta per leggere i vecchi file. */
    val lastShownDay: String = "",
    /** Chiave dell'ultimo suggerimento mostrato (resta finché non lo si chiude). */
    val lastKey: String = "",
    /** Giorno dell'ultima notifica che ha avvisato di un profilo suggerito (al massimo una a settimana). */
    val lastNotifiedDay: String = "",
    /** Momento (ms) in cui la scheda in home è stata mostrata l'ultima volta: la prossima dopo [UsageProfiles.SUGGESTION_MIN_GAP_MS]. */
    val lastShownAt: Long = 0L,
    /** Momento (ms) dell'ultima analisi dell'uso, mostrato nelle impostazioni. */
    val lastAnalysisAt: Long = 0L,
)

/** Ricava profili proposti dall'uso reale del telefono, senza dipendenze Android (testabile su JVM). */
object UsageProfiles {
    /**
     * Servono almeno questi giorni di storia per proporre qualcosa. Android conserva le aperture per una decina
     * di giorni: con il permesso appena concesso la storia c'è già, e il primo suggerimento può arrivare subito.
     */
    const val MIN_HISTORY_DAYS = 6
    const val WINDOW_DAYS = 28

    /** Giorni diversi in cui un'app deve comparire nella stessa fascia per entrare in un profilo. */
    const val MIN_RECURRENCE_DAYS = 3
    const val MIN_APPS = 3

    /** Un'app entra in un profilo solo se in quella fascia si apre almeno il 20% più spesso che nel resto. */
    const val MIN_CONCENTRATION = 1.2

    /** Oltre questa quota di tutta l'attività la fascia non si confronta con il resto. */
    const val COMPARISON_LIMIT = 0.8
    const val MAX_APPS = 6

    /** Dopo "Ignora" la stessa proposta tace per 30 giorni; in generale al massimo una proposta a settimana. */
    const val SILENCE_DAYS = 30
    /** Notifiche dei profili suggeriti: al massimo una ogni sette giorni. */
    const val DAYS_BETWEEN_SUGGESTIONS = 7

    /** In home la scheda di un nuovo suggerimento compare al massimo una volta ogni 4 ore. */
    const val SUGGESTION_MIN_GAP_MS = 4 * 3_600_000L

    /** «Analizza adesso» nelle impostazioni: al massimo una volta ogni 15 minuti. */
    const val ANALYZE_MIN_GAP_MS = 15 * 60_000L

    /** Quante app devono comparire nello stesso giorno per dire che si sono usate «insieme». */
    private const val TOGETHER_APPS = 3

    /** Due aperture della stessa app a meno di così sono la stessa. */
    private const val DEDUP_MS = 2 * 60_000L

    /**
     * Le proposte, dalla più solida. [exclude] sono app che non si propongono (il launcher, quelle di sistema,
     * i preferiti fissi). [useWifi] decide se il Wi-Fi vale come segnale.
     */
    fun suggest(
        events: List<UsageEventCtx>,
        now: Long,
        zone: ZoneId,
        exclude: Set<String> = emptySet(),
        useWifi: Boolean = false,
    ): List<ProfileSuggestion> {
        val windowStart = now - WINDOW_DAYS * DAY_MS
        val recent = dedupe(events.filter { it.time in windowStart..now && it.packageName !in exclude })
        if (recent.isEmpty()) return emptyList()
        // Storia troppo corta: meglio tacere che proporre su quattro giorni
        val firstDay = recent.minOf { day(it.time, zone) }
        if (day(now, zone).toEpochDay() - firstDay.toEpochDay() < MIN_HISTORY_DAYS) return emptyList()

        val out = mutableListOf<ProfileSuggestion>()
        // Per fascia: lavorativi e fine settimana, mattina/pomeriggio/sera/notte
        for (kind in DayKind.entries) {
            // Il confronto è con gli stessi giorni (lavorativi o weekend), non con tutta la settimana
            val inKind = recent.filter { e -> DaySlot.entries.any { belongs(e, kind, it, zone) } }
            for (slot in DaySlot.entries) {
                val inSlot = inKind.filter { belongs(it, kind, slot, zone) }
                pick(inSlot, inKind, zone)?.let { p -> out += ProfileSuggestion(slotName(kind, slot), p.apps, kind, slot, null, p.strength, togetherDays = p.together) }
            }
        }
        // Per segnale, in qualsiasi ora
        fun bySignal(signal: SuggestedSignal, name: String, filter: (UsageEventCtx) -> Boolean) {
            pick(recent.filter(filter), recent, zone)?.let { p -> out += ProfileSuggestion(name, p.apps, null, null, signal, p.strength, togetherDays = p.together) }
        }
        bySignal(SuggestedSignal.HEADPHONES, "Con le cuffie") { it.headphones }
        bySignal(SuggestedSignal.CHARGING, "In carica") { it.charging }
        bySignal(SuggestedSignal.CAR, "In auto") { it.car }
        if (useWifi) {
            recent.mapNotNull { it.wifiHash }.distinct().forEach { hash ->
                pick(recent.filter { it.wifiHash == hash }, recent, zone)?.let { p ->
                    out += ProfileSuggestion("Su questo Wi-Fi", p.apps, null, null, SuggestedSignal.WIFI, p.strength, wifiHash = hash, togetherDays = p.together)
                }
            }
        }
        val history = (day(now, zone).toEpochDay() - firstDay.toEpochDay()).toInt().coerceAtMost(WINDOW_DAYS)
        return out.sortedByDescending { it.strength }.distinctBy { it.apps.toSet() }.map { it.copy(historyDays = history) }
    }

    /**
     * Le app che ricorrono in [events] in almeno [MIN_RECURRENCE_DAYS] giorni diversi, se sono abbastanza.
     * Le app usate dappertutto (messaggi, browser) non dicono nulla di una fascia: [all] serve a tenere solo quelle
     * che in questa fascia si usano più che nel resto della giornata ([MIN_CONCENTRATION]).
     */
    private fun pick(events: List<UsageEventCtx>, all: List<UsageEventCtx>, zone: ZoneId): Picked? {
        if (events.isEmpty()) return null
        val daysByApp = events.groupBy { it.packageName }.mapValues { (_, list) -> list.map { day(it.time, zone) }.toSet().size }
        val apps = daysByApp.filterValues { it >= MIN_RECURRENCE_DAYS }.entries
            .filter { distinctive(it.key, events, all) }
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(MAX_APPS)
        if (apps.size < MIN_APPS) return null
        val chosen = apps.map { it.key }
        return Picked(chosen, apps.sumOf { it.value }, togetherDays(events, chosen, zone))
    }

    private data class Picked(val apps: List<String>, val strength: Int, val together: Int)

    /** Giorni in cui almeno [TOGETHER_APPS] delle app scelte compaiono negli stessi eventi (fascia o condizione). */
    private fun togetherDays(events: List<UsageEventCtx>, apps: List<String>, zone: ZoneId): Int {
        val need = minOf(TOGETHER_APPS, apps.size)
        return events.filter { it.packageName in apps }
            .groupBy { day(it.time, zone) }
            .count { (_, list) -> list.map { it.packageName }.toSet().size >= need }
    }

    /** Quanto più spesso un'app si apre in questa fascia rispetto al resto: 1 = uguale, 2 = il doppio. */
    internal fun concentration(pkg: String, events: List<UsageEventCtx>, all: List<UsageEventCtx>): Double {
        val total = all.count { it.packageName == pkg }
        if (total == 0 || all.isEmpty()) return 0.0
        val slotShare = events.size.toDouble() / all.size
        return (events.count { it.packageName == pkg }.toDouble() / total) / slotShare
    }

    private fun distinctive(pkg: String, events: List<UsageEventCtx>, all: List<UsageEventCtx>): Boolean {
        // Se la fascia contiene quasi tutta l'attività non c'è nulla con cui confrontarla
        if (events.size.toDouble() / all.size >= COMPARISON_LIMIT) return true
        return concentration(pkg, events, all) >= MIN_CONCENTRATION
    }

    internal fun belongs(e: UsageEventCtx, kind: DayKind, slot: DaySlot, zone: ZoneId): Boolean {
        val t = Instant.ofEpochMilli(e.time).atZone(zone)
        // La notte che passa la mezzanotte conta per il giorno in cui è iniziata
        val minute = t.hour * 60 + t.minute
        val dayOfWeek = if (slot == DaySlot.NIGHT && minute < slot.endMinute) t.minusDays(1).dayOfWeek.value else t.dayOfWeek.value
        return dayOfWeek in kind.days && slot.contains(minute)
    }

    private fun dedupe(events: List<UsageEventCtx>): List<UsageEventCtx> {
        val last = HashMap<String, Long>()
        return events.sortedBy { it.time }.filter { e ->
            val prev = last[e.packageName]
            last[e.packageName] = e.time
            prev == null || e.time - prev >= DEDUP_MS
        }
    }

    private fun day(time: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(time).atZone(zone).toLocalDate()

    private fun slotName(kind: DayKind, slot: DaySlot): String {
        val w = if (kind == DayKind.WEEKDAYS) "feriali" else "weekend"
        val s = when (slot) {
            DaySlot.MORNING -> "Mattina"
            DaySlot.AFTERNOON -> "Pomeriggio"
            DaySlot.EVENING -> "Sera"
            DaySlot.NIGHT -> "Notte"
        }
        return "$s $w"
    }

    /** Cosa ha visto Onda finora, per spiegare perché una proposta non c'è. */
    data class Diagnosis(val openings: Int, val historyDays: Int, val candidates: Int)

    fun diagnose(
        events: List<UsageEventCtx>,
        now: Long,
        zone: ZoneId,
        exclude: Set<String> = emptySet(),
        useWifi: Boolean = false,
    ): Diagnosis {
        val windowStart = now - WINDOW_DAYS * DAY_MS
        val recent = events.filter { it.time in windowStart..now && it.packageName !in exclude }
        val history = if (recent.isEmpty()) 0 else (day(now, zone).toEpochDay() - recent.minOf { day(it.time, zone) }.toEpochDay()).toInt()
        return Diagnosis(recent.size, history, suggest(events, now, zone, exclude, useWifi).size)
    }

    // --- Quando proporre ---

    /** Una proposta ignorata resta in silenzio per [SILENCE_DAYS] giorni. */
    fun silenced(state: ProfileSuggestState, key: String, today: String): Boolean {
        val ignoredOn = state.ignored[key] ?: return false
        return daysBetween(ignoredOn, today) in 0 until SILENCE_DAYS
    }

    /**
     * La proposta da mostrare in home, o null: spenta, un'altra mostrata meno di 4 ore fa, oppure tutte ignorate di recente.
     * Quella già mostrata (e non chiusa) resta finché non si decide.
     */
    fun next(state: ProfileSuggestState, candidates: List<ProfileSuggestion>, today: String, now: Long): ProfileSuggestion? {
        if (!state.enabled) return null
        val usable = forSettings(state, candidates, today)
        // Quella già mostrata (e non chiusa) resta finché non si decide
        state.lastKey.takeIf { it.isNotEmpty() }?.let { key -> usable.firstOrNull { it.key == key }?.let { return it } }
        // Una nuova al massimo ogni 4 ore dall'ultima mostrata; poi la più forte
        if (state.lastShownAt > 0 && now - state.lastShownAt < SUGGESTION_MIN_GAP_MS) return null
        return usable.firstOrNull()
    }

    /** L'elenco completo per le impostazioni: tutte le proposte non ignorate, dalla più solida. Nessun limite. */
    fun forSettings(state: ProfileSuggestState, candidates: List<ProfileSuggestion>, today: String): List<ProfileSuggestion> =
        candidates.filterNot { silenced(state, it.key, today) }.sortedByDescending { it.strength }

    /** Le proposte ignorate ancora in silenzio, dalla più recente: chiave e giorno. */
    fun ignoredKeys(state: ProfileSuggestState, today: String): List<Pair<String, String>> =
        state.ignored.filter { (key, _) -> silenced(state, key, today) }.toList().sortedByDescending { it.second }

    /** «Ripristina» un suggerimento ignorato: torna a poter comparire. */
    fun restore(state: ProfileSuggestState, key: String): ProfileSuggestState = state.copy(ignored = state.ignored - key)

    /** Le parti di una chiave, per descrivere un suggerimento ignorato senza ricalcolarlo. */
    data class KeyParts(val days: DayKind?, val slot: DaySlot?, val signal: SuggestedSignal?, val apps: List<String>)

    fun describe(key: String): KeyParts {
        val parts = key.split('|')
        return KeyParts(
            days = DayKind.entries.firstOrNull { it.name == parts.getOrNull(0) },
            slot = DaySlot.entries.firstOrNull { it.name == parts.getOrNull(1) },
            signal = SuggestedSignal.entries.firstOrNull { it.name == parts.getOrNull(2) },
            apps = parts.getOrNull(3).orEmpty().split(',').filter { it.isNotBlank() },
        )
    }

    fun canAnalyzeNow(state: ProfileSuggestState, now: Long): Boolean =
        state.lastAnalysisAt <= 0 || now - state.lastAnalysisAt >= ANALYZE_MIN_GAP_MS

    fun analyzed(state: ProfileSuggestState, now: Long): ProfileSuggestState = state.copy(lastAnalysisAt = now)

    /** Una notifica per avvisare del profilo, al massimo ogni [DAYS_BETWEEN_SUGGESTIONS] giorni. */
    fun shouldNotify(state: ProfileSuggestState, today: String): Boolean =
        state.enabled && (state.lastNotifiedDay.isEmpty() || daysBetween(state.lastNotifiedDay, today) >= DAYS_BETWEEN_SUGGESTIONS)

    fun notified(state: ProfileSuggestState, today: String): ProfileSuggestState = state.copy(lastNotifiedDay = today)

    fun shown(state: ProfileSuggestState, s: ProfileSuggestion, now: Long): ProfileSuggestState =
        if (state.lastKey == s.key) state else state.copy(lastKey = s.key, lastShownAt = now)

    fun ignore(state: ProfileSuggestState, s: ProfileSuggestion, today: String): ProfileSuggestState =
        state.copy(ignored = state.ignored + (s.key to today), lastKey = "")

    /** Dopo "Crea" o "Modifica" la proposta non si ripresenta. */
    fun accepted(state: ProfileSuggestState, s: ProfileSuggestion, today: String): ProfileSuggestState =
        state.copy(ignored = state.ignored + (s.key to today), lastKey = "")

    /** Il profilo vero che nasce dalla proposta, con le condizioni già impostate. L'utente lo può modificare. */
    fun toProfile(s: ProfileSuggestion, id: String): ContextProfile {
        val triggers = buildList {
            if (s.days != null && s.slot != null) {
                add(Trigger(TriggerType.TIME, days = s.days.days, startMinute = s.slot.startMinute, endMinute = s.slot.endMinute))
            }
            when (s.signal) {
                SuggestedSignal.HEADPHONES -> add(Trigger(TriggerType.HEADPHONES))
                SuggestedSignal.CHARGING -> add(Trigger(TriggerType.CHARGING))
                else -> Unit
            }
        }
        return ContextProfile(id = id, name = s.name, priority = 1, groups = listOf(TriggerGroup(triggers)), apps = s.apps)
    }

    private fun daysBetween(from: String, to: String): Int = DiscoveryTips.daysBetween(from, to)

    private const val DAY_MS = 24 * 3_600_000L
}
