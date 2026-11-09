// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import androidx.annotation.StringRes
import com.gabrielepiccione.onda.R
import kotlinx.serialization.Serializable

/** Dove sta un widget della home. */
@Serializable
enum class WidgetPlacement(@StringRes val labelRes: Int) {
    /** Widget classico, nell'ordine dei blocchi. */
    BELOW_CLOCK(R.string.wplace_below),
    /** Nella riga dell'orologio, dal lato libero (come le luci di casa). */
    BESIDE_CLOCK(R.string.wplace_beside),
    /** Al posto dell'orologio finché è visibile. */
    REPLACE_CLOCK(R.string.wplace_replace),
}

/**
 * Una condizione di STATO, presa dal catalogo dei trigger delle automazioni: vale dall'evento d'inizio a quello di
 * fine (cuffie collegate → scollegate, a casa → fuori…). [TIME] vale tra [fromMinute] e [toMinute]; [SUNRISE] è il
 * giorno (alba → tramonto) e [SUNSET] la notte.
 */
@Serializable
data class WidgetCondition(
    val trigger: AutomationTrigger,
    /** Per i dispositivi: nome del Bluetooth/audio, vuoto = qualunque. */
    val device: String = "",
    val fromMinute: Int = 8 * 60,
    val toMinute: Int = 18 * 60,
)

/** Regola di un widget della home: collocazione e quando si vede. Senza regola: sotto l'orologio, sempre. */
@Serializable
data class WidgetRule(
    val placement: WidgetPlacement = WidgetPlacement.BELOW_CLOCK,
    /** true = sempre; false = solo quando valgono tutte le [conditions]. */
    val always: Boolean = true,
    val conditions: List<WidgetCondition> = emptyList(),
    /** Solo nello strato delle sostituzioni («Nascondi widget»): mai salvato a mano. */
    val hidden: Boolean = false,
)

/** Cosa si vede: i widget sotto l'orologio, quello accanto e quello al suo posto (al più uno per collocazione). */
data class VisibleWidgets(val below: Set<Int>, val beside: Int?, val replace: Int?)

/** Widget che compaiono con regole, senza dipendenze Android (testabile su JVM). */
object WidgetVisibility {
    private const val MINUTES_PER_DAY = 24 * 60

    /** Valore dell'impostazione di Onda per le automazioni «Mostra/Nascondi widget». */
    private const val SHOW = "show"
    private const val HIDE = "hide"

    fun ruleOf(s: Settings, id: Int): WidgetRule = s.widgetRules[id] ?: WidgetRule()

    /** La condizione vale adesso? Riusa le coppie inizio/fine delle automazioni; senza stato noto non vale. */
    fun holds(c: WidgetCondition, env: EnvSnapshot?): Boolean {
        if (env == null) return false
        return when (c.trigger) {
            AutomationTrigger.TIME -> inWindow(env.minuteOfDay, c.fromMinute, c.toMinute)
            AutomationTrigger.SUNRISE -> sun(env)?.let { (rise, set) -> inWindow(env.minuteOfDay, rise, set) } ?: false
            AutomationTrigger.SUNSET -> sun(env)?.let { (rise, set) -> inWindow(env.minuteOfDay, set, rise) } ?: false
            else -> AutomationOverrides.conditionHolds(c.trigger, c.device, env) ?: false
        }
    }

    private fun sun(env: EnvSnapshot): Pair<Int, Int>? {
        val rise = env.sunriseMinute ?: return null
        val set = env.sunsetMinute ?: return null
        return rise to set
    }

    /** [minute] è tra [from] (incluso) e [to] (escluso), anche a cavallo della mezzanotte. */
    fun inWindow(minute: Int, from: Int, to: Int): Boolean {
        val m = minute.mod(MINUTES_PER_DAY)
        return when {
            from == to -> true
            from < to -> m in from until to
            else -> m >= from || m < to
        }
    }

    fun isVisible(rule: WidgetRule, env: EnvSnapshot?): Boolean = when {
        rule.hidden -> false
        rule.always -> true
        rule.conditions.isEmpty() -> false
        else -> rule.conditions.all { holds(it, env) }
    }

    /**
     * I widget visibili, nell'ordine della home ([ordered]): per «accanto» e «al posto» vince il primo visibile,
     * gli altri della stessa collocazione restano nascosti finché il primo non sparisce.
     */
    fun resolve(ordered: List<Int>, rules: Map<Int, WidgetRule>, env: EnvSnapshot?): VisibleWidgets {
        val visible = ordered.filter { isVisible(rules[it] ?: WidgetRule(), env) }
        fun placementOf(id: Int) = (rules[id] ?: WidgetRule()).placement
        return VisibleWidgets(
            below = visible.filter { placementOf(it) == WidgetPlacement.BELOW_CLOCK }.toSet(),
            beside = visible.firstOrNull { placementOf(it) == WidgetPlacement.BESIDE_CLOCK },
            replace = visible.firstOrNull { placementOf(it) == WidgetPlacement.REPLACE_CLOCK },
        )
    }

    /** Servono le fotografie dello stato del telefono: c'è almeno una regola con condizioni. */
    fun needsEnv(s: Settings): Boolean = s.widgetRules.values.any { !it.always && it.conditions.isNotEmpty() }

    // --- Azioni delle automazioni «Mostra widget» / «Nascondi widget» ---

    /** Argomento dell'azione: "12;BESIDE_CLOCK;1" (1 = ripristina al termine). */
    fun showArg(id: Int, placement: WidgetPlacement, restore: Boolean) = "$id;${placement.name};${if (restore) 1 else 0}"

    fun hideArg(id: Int, restore: Boolean) = "$id;;${if (restore) 1 else 0}"

    data class WidgetAction(val id: Int, val placement: WidgetPlacement?, val restore: Boolean)

    fun parseArg(arg: String): WidgetAction? {
        val p = arg.split(';')
        val id = p.getOrNull(0)?.toIntOrNull() ?: return null
        val placement = WidgetPlacement.entries.firstOrNull { it.name == p.getOrNull(1) }
        return WidgetAction(id, placement, p.getOrNull(2) != "0")
    }

    /** La sostituzione corrispondente, nello strato delle automazioni (impostazione [LauncherSetting.WIDGET]). */
    fun change(action: GestureAction, arg: String): LauncherChange? {
        val a = parseArg(arg) ?: return null
        val value = when (action) {
            GestureAction.WIDGET_SHOW -> "$SHOW:${a.id}:${(a.placement ?: WidgetPlacement.BELOW_CLOCK).name}"
            GestureAction.WIDGET_HIDE -> "$HIDE:${a.id}"
            else -> return null
        }
        return LauncherChange(LauncherSetting.WIDGET, value, a.restore)
    }

    /** Widget toccato da un valore della sostituzione, per non confondere due widget della stessa automazione. */
    fun widgetOf(value: String): Int? = value.split(':').getOrNull(1)?.toIntOrNull()

    /** Applica una sostituzione alle impostazioni in uso (mai a quelle salvate). */
    fun apply(s: Settings, value: String): Settings {
        val p = value.split(':')
        val id = p.getOrNull(1)?.toIntOrNull() ?: return s
        val rule = ruleOf(s, id)
        val next = when (p[0]) {
            SHOW -> rule.copy(
                always = true,
                hidden = false,
                placement = WidgetPlacement.entries.firstOrNull { it.name == p.getOrNull(2) } ?: rule.placement,
            )
            HIDE -> rule.copy(hidden = true)
            else -> return s
        }
        return s.copy(widgetRules = s.widgetRules + (id to next))
    }

    /** Testo della condizione di stato per l'editor («Cuffie collegate», «Di notte»…). */
    @StringRes
    fun stateLabel(trigger: AutomationTrigger): Int = when (trigger) {
        AutomationTrigger.HOME_ARRIVE -> R.string.wcond_home
        AutomationTrigger.HOME_LEAVE -> R.string.wcond_away
        AutomationTrigger.HEADPHONES_ON -> R.string.wcond_headphones
        AutomationTrigger.HEADPHONES_OFF -> R.string.wcond_no_headphones
        AutomationTrigger.CHARGING_ON -> R.string.wcond_charging
        AutomationTrigger.CHARGING_OFF -> R.string.wcond_not_charging
        AutomationTrigger.DEVICE_ON -> R.string.wcond_device
        AutomationTrigger.DEVICE_OFF -> R.string.wcond_no_device
        AutomationTrigger.TIME -> R.string.wcond_time
        AutomationTrigger.SUNRISE -> R.string.wcond_day
        AutomationTrigger.SUNSET -> R.string.wcond_night
        AutomationTrigger.MUSIC_ON -> R.string.wcond_music
        AutomationTrigger.MUSIC_OFF -> R.string.wcond_no_music
    }
}
