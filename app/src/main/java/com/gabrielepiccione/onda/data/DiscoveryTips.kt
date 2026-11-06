// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import kotlinx.serialization.Serializable

/** Suggerimenti "Lo sapevi?", nell'ordine in cui si propongono. */
enum class Tip {
    FAVORITE, SWIPE_NOTIFICATIONS, ALPHABET, PLAYER, HEADPHONES, GRAYSCALE, PROFILES,
}

/**
 * Cosa si è già visto e fatto. Tutto ha un default, quindi i file salvati prima restano validi.
 * [shownTip] e [shownDay]: il suggerimento di oggi (uno al giorno al massimo, finché non lo si chiude).
 */
@Serializable
data class TipState(
    val enabled: Boolean = true,
    /** "Non mostrare più". */
    val dismissed: Set<String> = emptySet(),
    /** "Non ora": non riappare oggi, ma può tornare un altro giorno. */
    val snoozedDay: String = "",
    val snoozed: Set<String> = emptySet(),
    val shownDay: String = "",
    val shownTip: String = "",
    /** Quante volte ognuno è stato mostrato in giorni diversi: dopo [DiscoveryTips.MAX_SHOWS] non torna più. */
    val shownCount: Map<String, Int> = emptyMap(),
    val firstDay: String = "",
    /** Cosa ha già usato l'utente, così non si suggerisce ciò che sa già fare. */
    val swiped: Boolean = false,
    val alphabetUsed: Boolean = false,
    val listOpens: Int = 0,
)

/** Quello che il telefono sa adesso, per decidere. */
data class TipSignals(
    val today: String,
    val minuteOfDay: Int,
    val daysSinceFirst: Int,
    /** Un'app che non è tra i preferiti è stata aperta spesso. */
    val oftenOpenedNotFavorite: Boolean,
    val hasNotifications: Boolean,
    val playerSeen: Boolean,
    val headphonesSeen: Boolean,
    val grayscaleOn: Boolean,
    val hasProfiles: Boolean,
    /** Un gesto o un pannello è in corso: non si mostra nulla. */
    val busy: Boolean = false,
)

/** Regole dei suggerimenti, senza dipendenze Android (testabili su JVM). */
object DiscoveryTips {
    const val MAX_SHOWS = 3
    const val OFTEN_OPENED = 5
    const val LIST_OPENS_BEFORE_ALPHABET = 3
    const val EVENING_FROM = 21 * 60
    const val DAYS_BEFORE_PROFILES = 7

    /** Il suggerimento da mostrare adesso, o null. Al massimo uno al giorno, mai durante un gesto. */
    fun next(state: TipState, s: TipSignals): Tip? {
        if (!state.enabled || s.busy) return null
        // Quello di oggi resta finché non lo si chiude
        if (state.shownDay == s.today && state.shownTip.isNotEmpty()) {
            val tip = Tip.entries.firstOrNull { it.name == state.shownTip } ?: return null
            return tip.takeIf { it.name !in state.dismissed && !(state.snoozedDay == s.today && it.name in state.snoozed) }
        }
        return Tip.entries.firstOrNull { tip ->
            tip.name !in state.dismissed &&
                (state.shownCount[tip.name] ?: 0) < MAX_SHOWS &&
                !(state.snoozedDay == s.today && tip.name in state.snoozed) &&
                applies(tip, state, s)
        }
    }

    fun applies(tip: Tip, state: TipState, s: TipSignals): Boolean = when (tip) {
        Tip.FAVORITE -> s.oftenOpenedNotFavorite
        Tip.SWIPE_NOTIFICATIONS -> s.hasNotifications && !state.swiped
        Tip.ALPHABET -> state.listOpens >= LIST_OPENS_BEFORE_ALPHABET && !state.alphabetUsed
        Tip.PLAYER -> s.playerSeen
        Tip.HEADPHONES -> s.headphonesSeen
        Tip.GRAYSCALE -> s.minuteOfDay >= EVENING_FROM && !s.grayscaleOn
        Tip.PROFILES -> s.daysSinceFirst >= DAYS_BEFORE_PROFILES && !s.hasProfiles
    }

    /** Segna [tip] come mostrato oggi, contandolo una sola volta per giorno. */
    fun shown(state: TipState, tip: Tip, today: String): TipState =
        if (state.shownDay == today && state.shownTip == tip.name) {
            state
        } else {
            state.copy(
                shownDay = today,
                shownTip = tip.name,
                shownCount = state.shownCount + (tip.name to (state.shownCount[tip.name] ?: 0) + 1),
            )
        }

    fun notNow(state: TipState, tip: Tip, today: String): TipState =
        state.copy(snoozedDay = today, snoozed = (if (state.snoozedDay == today) state.snoozed else emptySet()) + tip.name)

    fun never(state: TipState, tip: Tip): TipState = state.copy(dismissed = state.dismissed + tip.name)

    /** Dalla voce "Suggerimenti" delle impostazioni: si riattivano e tornano quelli scartati. */
    fun reset(state: TipState): TipState = state.copy(enabled = true, dismissed = emptySet(), snoozed = emptySet(), shownTip = "", shownCount = emptyMap())

    /** Giorni tra due date "AAAA-MM-GG", 0 se una manca. */
    fun daysBetween(first: String, today: String): Int = runCatching {
        java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.parse(first), java.time.LocalDate.parse(today)).toInt()
    }.getOrDefault(0)
}
