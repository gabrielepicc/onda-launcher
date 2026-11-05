// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.IsoFields
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Cosa osserva una condizione. */
@Serializable
enum class TriggerType { WIFI, BLUETOOTH, PLACE, TIME, CALENDAR, CHARGING, HEADPHONES, FOCUS }

/**
 * Una condizione di un profilo. Usa solo i campi che servono al suo tipo:
 * - [text]: nome del Wi-Fi, del dispositivo Bluetooth o parola da cercare nell'evento (vuoto = qualsiasi evento);
 * - [days] (1 = lunedì … 7 = domenica), [startMinute] e [endMinute] (minuti dalla mezzanotte; se la fine è
 *   prima dell'inizio, la fascia passa la mezzanotte);
 * - [latitude], [longitude], [radiusMeters] per la posizione.
 * [negate] la rovescia ("non sul Wi-Fi di casa").
 */
@Serializable
data class Trigger(
    val type: TriggerType,
    val text: String = "",
    val days: Set<Int> = emptySet(),
    val startMinute: Int = 9 * 60,
    val endMinute: Int = 18 * 60,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radiusMeters: Int = 300,
    val negate: Boolean = false,
)

/** Condizioni da avere tutte insieme (E). */
@Serializable
data class TriggerGroup(val triggers: List<Trigger> = emptyList())

/**
 * Un profilo di preferiti contestuali. È attivo quando almeno un gruppo di [groups] è vero (O tra i
 * gruppi, E dentro ogni gruppo). Tra più profili attivi vince quello con [priority] più alta; a parità,
 * quello più in alto nell'elenco. Le regole sono esplicite: nessuna deduzione nascosta.
 */
@Serializable
data class ContextProfile(
    val id: String,
    val name: String,
    val priority: Int = 0,
    val groups: List<TriggerGroup> = emptyList(),
    /** App per gli slot variabili, in ordine. */
    val apps: List<String> = emptyList(),
    /** Accento usato mentre il profilo è attivo, come segnale discreto; 0 = invariato. */
    val accent: Long = 0L,
    val enabled: Boolean = true,
)

/** Quello che il telefono sa del momento, letto da [ContextReader]. */
data class ContextSnapshot(
    val time: LocalDateTime,
    val wifi: String? = null,
    val bluetooth: Set<String> = emptySet(),
    val location: Pair<Double, Double>? = null,
    /** Testo (titolo, luogo, descrizione) degli eventi in corso adesso. */
    val calendarNow: List<String> = emptyList(),
    val charging: Boolean = false,
    val headphones: Boolean = false,
    val focus: Boolean = false,
)

/** Scelta del profilo fatta a mano, valida fino a [until]; [profileId] vuoto = "nessun profilo". */
@Serializable
data class ProfileOverride(val profileId: String = "", val until: Long = 0L)

/** Regola proposta osservando le abitudini: l'utente decide se crearla. */
data class RuleSuggestion(val appKey: String, val dayOfWeek: Int, val hour: Int, val weeks: Int) {
    val id: String get() = "$appKey@$dayOfWeek@$hour"
}

/** Un'apertura di app dal launcher, per i suggerimenti. */
data class LaunchRecord(val appKey: String, val time: Long)

/** Motore dei preferiti contestuali, senza dipendenze Android (testabile su JVM). */
object ContextProfiles {
    const val OVERRIDE_MS = 60 * 60_000L
    const val MAX_FIXED_SLOTS = 4

    /** Settimane diverse in cui l'abitudine si deve ripetere, sulle ultime [SUGGEST_WINDOW_WEEKS], per proporla. */
    const val SUGGEST_MIN_WEEKS = 3
    const val SUGGEST_WINDOW_WEEKS = 4

    fun matches(t: Trigger, s: ContextSnapshot): Boolean {
        val raw = when (t.type) {
            TriggerType.WIFI -> s.wifi != null && s.wifi.equals(t.text.trim(), ignoreCase = true)
            TriggerType.BLUETOOTH -> t.text.isNotBlank() && s.bluetooth.any { it.equals(t.text.trim(), ignoreCase = true) }
            TriggerType.PLACE -> s.location?.let { (lat, lon) ->
                (t.latitude != 0.0 || t.longitude != 0.0) && distanceMeters(lat, lon, t.latitude, t.longitude) <= t.radiusMeters
            } ?: false
            TriggerType.TIME -> inTimeRange(t, s.time)
            TriggerType.CALENDAR -> s.calendarNow.any { t.text.isBlank() || it.contains(t.text.trim(), ignoreCase = true) }
            TriggerType.CHARGING -> s.charging
            TriggerType.HEADPHONES -> s.headphones
            TriggerType.FOCUS -> s.focus
        }
        return raw != t.negate
    }

    /** Vero se il profilo vale adesso: almeno un gruppo con tutte le condizioni vere. Senza gruppi, mai. */
    fun matches(p: ContextProfile, s: ContextSnapshot): Boolean =
        p.enabled && p.groups.any { g -> g.triggers.isNotEmpty() && g.triggers.all { matches(it, s) } }

    /** Il profilo che vince adesso: il più importante tra quelli veri, o null. */
    fun active(profiles: List<ContextProfile>, s: ContextSnapshot): ContextProfile? =
        profiles.withIndex()
            .filter { matches(it.value, s) }
            .maxWithOrNull(compareBy<IndexedValue<ContextProfile>> { it.value.priority }.thenByDescending { it.index })
            ?.value

    /** Come [active], ma una scelta a mano ancora valida vince su tutto. */
    fun resolve(profiles: List<ContextProfile>, s: ContextSnapshot, override: ProfileOverride, now: Long): ContextProfile? {
        if (override.until > now) {
            return if (override.profileId.isEmpty()) null else profiles.firstOrNull { it.id == override.profileId }
        }
        return active(profiles, s)
    }

    /** Il profilo successivo per la scelta a mano: nessuno, poi ognuno in ordine, poi di nuovo nessuno. */
    fun nextOverride(profiles: List<ContextProfile>, currentId: String?, now: Long): ProfileOverride {
        val ids = listOf("") + profiles.filter { it.enabled }.map { it.id }
        val at = ids.indexOf(currentId.orEmpty()).coerceAtLeast(0)
        return ProfileOverride(ids[(at + 1) % ids.size], now + OVERRIDE_MS)
    }

    /**
     * I preferiti da mostrare: i primi [fixedSlots] della home non cambiano mai, gli altri vengono dal
     * profilo. Le cartelle e le scorciatoie restano dove sono negli slot fissi.
     */
    fun favorites(base: List<String>, fixedSlots: Int, profile: ContextProfile?): List<String> {
        if (profile == null) return base
        val fixed = base.take(fixedSlots.coerceIn(0, MAX_FIXED_SLOTS))
        return (fixed + profile.apps.filterNot { it in fixed }).distinct()
    }

    fun inTimeRange(t: Trigger, time: LocalDateTime): Boolean {
        val minute = time.hour * 60 + time.minute
        val day = time.dayOfWeek.value
        return if (t.startMinute <= t.endMinute) {
            (t.days.isEmpty() || day in t.days) && minute >= t.startMinute && minute < t.endMinute
        } else {
            // La fascia passa la mezzanotte: la parte dopo conta per il giorno prima
            val yesterday = if (day == 1) 7 else day - 1
            (minute >= t.startMinute && (t.days.isEmpty() || day in t.days)) ||
                (minute < t.endMinute && (t.days.isEmpty() || yesterday in t.days))
        }
    }

    /** Distanza in metri tra due punti, con la formula dell'emisenoverso. */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }

    /**
     * Abitudini ripetute: la stessa app aperta lo stesso giorno della settimana alla stessa ora in almeno
     * [SUGGEST_MIN_WEEKS] settimane diverse delle ultime [SUGGEST_WINDOW_WEEKS]. Esclude quelle già
     * coperte da un profilo e quelle che l'utente ha scartato.
     */
    fun suggest(
        records: List<LaunchRecord>,
        now: Long,
        zone: ZoneId,
        profiles: List<ContextProfile>,
        dismissed: Set<String>,
    ): List<RuleSuggestion> {
        val windowStart = now - SUGGEST_WINDOW_WEEKS * 7 * 24 * 3_600_000L
        val weeks = HashMap<Triple<String, Int, Int>, MutableSet<Int>>()
        for (r in records) {
            if (r.time < windowStart || r.time > now) continue
            val t = Instant.ofEpochMilli(r.time).atZone(zone)
            val key = Triple(r.appKey, t.dayOfWeek.value, t.hour)
            weeks.getOrPut(key) { HashSet() } += t.get(IsoFields.WEEK_BASED_YEAR) * 100 + t.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        }
        val covered = profiles.flatMap { it.apps }.toSet()
        return weeks.filter { it.value.size >= SUGGEST_MIN_WEEKS }
            .map { (k, w) -> RuleSuggestion(k.first, k.second, k.third, w.size) }
            .filter { it.id !in dismissed && it.appKey !in covered }
            .sortedWith(compareByDescending<RuleSuggestion> { it.weeks }.thenBy { it.dayOfWeek }.thenBy { it.hour })
    }

    /** Il profilo che nasce da un suggerimento accettato: quel giorno, quell'ora, quell'app. */
    fun fromSuggestion(s: RuleSuggestion, name: String, id: String): ContextProfile = ContextProfile(
        id = id,
        name = name,
        priority = 1,
        groups = listOf(TriggerGroup(listOf(Trigger(TriggerType.TIME, days = setOf(s.dayOfWeek), startMinute = s.hour * 60, endMinute = (s.hour + 1) * 60)))),
        apps = listOf(s.appKey),
    )

    /** Profilo pronto per le riunioni: durante un evento con un link di Teams. */
    fun meetingTemplate(id: String, name: String, apps: List<String>): ContextProfile = ContextProfile(
        id = id,
        name = name,
        priority = 10,
        groups = listOf(TriggerGroup(listOf(Trigger(TriggerType.CALENDAR, text = TEAMS_LINK)))),
        apps = apps,
    )

    const val TEAMS_LINK = "teams.microsoft.com"

    /** Pacchetti noti per il profilo riunione, nell'ordine in cui si propongono. */
    val MEETING_PACKAGES = listOf(
        "com.microsoft.teams",
        "com.microsoft.office.onenote",
        "com.google.android.apps.recorder",
        "com.sec.android.app.voicenote",
        "com.android.soundrecorder",
        "com.coloros.soundrecorder",
        "com.miui.soundrecorder",
    )

    fun dayName(day: Int): DayOfWeek = DayOfWeek.of(day.coerceIn(1, 7))
}
