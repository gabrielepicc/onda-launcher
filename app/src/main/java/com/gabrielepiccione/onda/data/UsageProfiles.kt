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
    /** Giorno dell'ultimo suggerimento mostrato, per mostrarne al massimo uno a settimana. */
    val lastShownDay: String = "",
    /** Chiave dell'ultimo suggerimento mostrato (resta finché non lo si chiude). */
    val lastKey: String = "",
)

/** Ricava profili proposti dall'uso reale del telefono, senza dipendenze Android (testabile su JVM). */
object UsageProfiles {
    /** Servono almeno questi giorni di storia per proporre qualcosa. */
    const val MIN_HISTORY_DAYS = 14
    const val WINDOW_DAYS = 28

    /** Giorni diversi in cui un'app deve comparire nella stessa fascia per entrare in un profilo. */
    const val MIN_RECURRENCE_DAYS = 5
    const val MIN_APPS = 3
    const val MAX_APPS = 6

    /** Dopo "Ignora" la stessa proposta tace per 30 giorni; in generale al massimo una proposta a settimana. */
    const val SILENCE_DAYS = 30
    const val DAYS_BETWEEN_SUGGESTIONS = 7

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
        for (kind in DayKind.entries) for (slot in DaySlot.entries) {
            val inSlot = recent.filter { belongs(it, kind, slot, zone) }
            pick(inSlot, zone)?.let { (apps, strength) -> out += ProfileSuggestion(slotName(kind, slot), apps, kind, slot, null, strength) }
        }
        // Per segnale, in qualsiasi ora
        fun bySignal(signal: SuggestedSignal, name: String, filter: (UsageEventCtx) -> Boolean) {
            pick(recent.filter(filter), zone)?.let { (apps, strength) -> out += ProfileSuggestion(name, apps, null, null, signal, strength) }
        }
        bySignal(SuggestedSignal.HEADPHONES, "Con le cuffie") { it.headphones }
        bySignal(SuggestedSignal.CHARGING, "In carica") { it.charging }
        bySignal(SuggestedSignal.CAR, "In auto") { it.car }
        if (useWifi) {
            recent.mapNotNull { it.wifiHash }.distinct().forEach { hash ->
                pick(recent.filter { it.wifiHash == hash }, zone)?.let { (apps, strength) ->
                    out += ProfileSuggestion("Su questo Wi-Fi", apps, null, null, SuggestedSignal.WIFI, strength, wifiHash = hash)
                }
            }
        }
        return out.sortedByDescending { it.strength }.distinctBy { it.apps.toSet() }
    }

    /** Le app che ricorrono in [events] in almeno [MIN_RECURRENCE_DAYS] giorni diversi, se sono abbastanza. */
    private fun pick(events: List<UsageEventCtx>, zone: ZoneId): Pair<List<String>, Int>? {
        val daysByApp = events.groupBy { it.packageName }.mapValues { (_, list) -> list.map { day(it.time, zone) }.toSet().size }
        val apps = daysByApp.filterValues { it >= MIN_RECURRENCE_DAYS }.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(MAX_APPS)
        if (apps.size < MIN_APPS) return null
        return apps.map { it.key } to apps.sumOf { it.value }
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

    // --- Quando proporre ---

    /** Una proposta ignorata resta in silenzio per [SILENCE_DAYS] giorni. */
    fun silenced(state: ProfileSuggestState, key: String, today: String): Boolean {
        val ignoredOn = state.ignored[key] ?: return false
        return daysBetween(ignoredOn, today) in 0 until SILENCE_DAYS
    }

    /**
     * La proposta da mostrare oggi, o null: spenta, già una nei sette giorni, oppure tutte ignorate di recente.
     * Quella già mostrata (e non chiusa) resta finché non si decide.
     */
    fun next(state: ProfileSuggestState, candidates: List<ProfileSuggestion>, today: String): ProfileSuggestion? {
        if (!state.enabled) return null
        val usable = candidates.filterNot { silenced(state, it.key, today) }
        state.lastKey.takeIf { it.isNotEmpty() }?.let { key -> usable.firstOrNull { it.key == key }?.let { return it } }
        if (state.lastShownDay.isNotEmpty() && daysBetween(state.lastShownDay, today) < DAYS_BETWEEN_SUGGESTIONS) return null
        return usable.firstOrNull()
    }

    fun shown(state: ProfileSuggestState, s: ProfileSuggestion, today: String): ProfileSuggestState =
        if (state.lastKey == s.key) state else state.copy(lastKey = s.key, lastShownDay = today)

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
