// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import androidx.annotation.StringRes
import com.gabrielepiccione.onda.R
import kotlinx.serialization.Serializable

/** Che cosa fa partire un'automazione. */
@Serializable
enum class AutomationTrigger(@StringRes val labelRes: Int) {
    HOME_ARRIVE(R.string.auto_trigger_home_arrive),
    HOME_LEAVE(R.string.auto_trigger_home_leave),
    HEADPHONES_ON(R.string.auto_trigger_headphones_on),
    HEADPHONES_OFF(R.string.auto_trigger_headphones_off),
    CHARGING_ON(R.string.auto_trigger_charging_on),
    CHARGING_OFF(R.string.auto_trigger_charging_off),
    DEVICE_ON(R.string.auto_trigger_device_on),
    DEVICE_OFF(R.string.auto_trigger_device_off),
    TIME(R.string.auto_trigger_time),
    SUNSET(R.string.auto_trigger_sunset),
    SUNRISE(R.string.auto_trigger_sunrise),
    ;

    /** Parte a un orario del giorno (fisso o legato al sole). */
    val isTimed: Boolean get() = this == TIME || this == SUNSET || this == SUNRISE
}

/**
 * Una regola «se… allora»: quando succede [trigger] si eseguono le [actions] del catalogo (formato di
 * [Actions.encode]), si passa al profilo [profile] per un'ora e si mostra il promemoria [reminder].
 */
@Serializable
data class Automation(
    val id: String,
    val name: String,
    val trigger: AutomationTrigger,
    val enabled: Boolean = true,
    /** Per DEVICE_ON e DEVICE_OFF: il nome del dispositivo audio o Bluetooth; vuoto = qualunque. */
    val device: String = "",
    /** Per TIME: minuti dalla mezzanotte. */
    val minute: Int = 22 * 60,
    /** Giorni (1 = lunedì … 7 = domenica) per i trigger a orario; vuoto = tutti. */
    val days: Set<Int> = emptySet(),
    val actions: List<String> = emptyList(),
    /** Profilo dei preferiti contestuali da attivare per un'ora; vuoto = nessuno. */
    val profile: String = "",
    val reminder: String = "",
) {
    /** Fa qualcosa: almeno un'azione, un profilo o un promemoria. */
    val isUsable: Boolean get() = actions.isNotEmpty() || profile.isNotBlank() || reminder.isNotBlank()
}

/** Com'è il telefono in un momento: da due fotografie consecutive si capisce cosa è appena successo. */
data class EnvSnapshot(
    val atHome: Boolean,
    val headphones: Boolean,
    val charging: Boolean,
    val devices: Set<String>,
    val minuteOfDay: Int,
    /** 1 = lunedì … 7 = domenica. */
    val dayOfWeek: Int,
    val sunriseMinute: Int? = null,
    val sunsetMinute: Int? = null,
)

/** Modello di partenza: l'utente lo completa nell'editor (scena, app, orario…). */
class AutomationTemplate(
    @StringRes val nameRes: Int,
    @StringRes val subRes: Int,
    val trigger: AutomationTrigger,
    val minute: Int = 22 * 60,
    val days: Set<Int> = emptySet(),
    val actions: List<GestureAction> = emptyList(),
    @StringRes val reminderRes: Int? = null,
    /** Azioni con argomento già pronte (es. impostazioni di Onda), nel formato di [Actions.encode]. */
    val encoded: List<String> = emptyList(),
)

/** Motore delle automazioni e modelli, senza dipendenze Android (testabile su JVM). */
object Automations {
    const val ONE_HOUR_MS = 3_600_000L
    private val WEEKDAYS = setOf(1, 2, 3, 4, 5)

    /** Le automazioni da eseguire passando da [prev] a [now]. Al primo controllo ([prev] nullo) non parte nulla. */
    fun fired(list: List<Automation>, prev: EnvSnapshot?, now: EnvSnapshot): List<Automation> {
        if (prev == null) return emptyList()
        return list.filter { it.enabled && it.isUsable && matches(it, prev, now) }
    }

    private fun matches(a: Automation, prev: EnvSnapshot, now: EnvSnapshot): Boolean = when (a.trigger) {
        AutomationTrigger.HOME_ARRIVE -> !prev.atHome && now.atHome
        AutomationTrigger.HOME_LEAVE -> prev.atHome && !now.atHome
        AutomationTrigger.HEADPHONES_ON -> !prev.headphones && now.headphones
        AutomationTrigger.HEADPHONES_OFF -> prev.headphones && !now.headphones
        AutomationTrigger.CHARGING_ON -> !prev.charging && now.charging
        AutomationTrigger.CHARGING_OFF -> prev.charging && !now.charging
        AutomationTrigger.DEVICE_ON -> appeared(a.device, prev.devices, now.devices)
        AutomationTrigger.DEVICE_OFF -> appeared(a.device, now.devices, prev.devices)
        AutomationTrigger.TIME, AutomationTrigger.SUNSET, AutomationTrigger.SUNRISE -> {
            val target = triggerMinute(a, now)
            target != null && dayAllowed(a, now.dayOfWeek) && crossed(prev.minuteOfDay, now.minuteOfDay, target)
        }
    }

    /** Un dispositivo (quello scelto, o uno qualunque se il nome è vuoto) è in [to] e non era in [from]. */
    private fun appeared(device: String, from: Set<String>, to: Set<String>): Boolean {
        val gained = to.filter { n -> from.none { it.equals(n, ignoreCase = true) } }
        return if (device.isBlank()) gained.isNotEmpty() else gained.any { it.equals(device, ignoreCase = true) }
    }

    private fun dayAllowed(a: Automation, dayOfWeek: Int) = a.days.isEmpty() || dayOfWeek in a.days

    /** L'orario del giorno a cui parte un'automazione a orario (null se il sole non è noto). */
    fun triggerMinute(a: Automation, env: EnvSnapshot): Int? = when (a.trigger) {
        AutomationTrigger.TIME -> a.minute.coerceIn(0, MINUTES_PER_DAY - 1)
        AutomationTrigger.SUNSET -> env.sunsetMinute
        AutomationTrigger.SUNRISE -> env.sunriseMinute
        else -> null
    }

    /** [target] cade tra [prevMinute] (escluso) e [nowMinute] (incluso), anche passando la mezzanotte. */
    fun crossed(prevMinute: Int, nowMinute: Int, target: Int): Boolean =
        if (nowMinute >= prevMinute) target in (prevMinute + 1)..nowMinute else target > prevMinute || target <= nowMinute

    /** Il testo del dispositivo scelto, "" per qualunque. */
    fun deviceLabel(a: Automation): String = a.device.trim()

    fun upsert(list: List<Automation>, a: Automation): List<Automation> =
        if (list.any { it.id == a.id }) list.map { if (it.id == a.id) a else it } else list + a

    fun remove(list: List<Automation>, id: String): List<Automation> = list.filterNot { it.id == id }

    fun fromTemplate(t: AutomationTemplate, id: String, name: String, reminder: String): Automation = Automation(
        id = id,
        name = name,
        trigger = t.trigger,
        minute = t.minute,
        days = t.days,
        actions = t.actions.map { Actions.encode(ActionChoice(it)) } + t.encoded,
        reminder = reminder,
    )

    /** Esempi pronti: arrivo e uscita da casa, notte, mattina, auto, cuffie, ricarica, sole. */
    val TEMPLATES: List<AutomationTemplate> = listOf(
        AutomationTemplate(R.string.auto_tpl_arrive, R.string.auto_tpl_arrive_sub, AutomationTrigger.HOME_ARRIVE, actions = listOf(GestureAction.SOUND_ON)),
        AutomationTemplate(R.string.auto_tpl_leave, R.string.auto_tpl_leave_sub, AutomationTrigger.HOME_LEAVE, reminderRes = R.string.auto_tpl_leave_text),
        AutomationTemplate(R.string.auto_tpl_goodnight, R.string.auto_tpl_goodnight_sub, AutomationTrigger.TIME, minute = 23 * 60, actions = listOf(GestureAction.SILENT_ON)),
        AutomationTemplate(R.string.auto_tpl_morning, R.string.auto_tpl_morning_sub, AutomationTrigger.TIME, minute = 7 * 60, days = WEEKDAYS, actions = listOf(GestureAction.SOUND_ON)),
        AutomationTemplate(
            R.string.auto_tpl_car,
            R.string.auto_tpl_car_sub,
            AutomationTrigger.DEVICE_ON,
            // Modalità auto: icone e testo grandi, che tornano come prima quando l'auto si scollega
            encoded = listOf(
                launcherAction(LauncherChange(LauncherSetting.ICON_SIZE, "1.45", restore = true)),
                launcherAction(LauncherChange(LauncherSetting.TEXT_SIZE, "1.3", restore = true)),
            ),
        ),
        AutomationTemplate(R.string.auto_tpl_headphones, R.string.auto_tpl_headphones_sub, AutomationTrigger.HEADPHONES_ON),
        AutomationTemplate(R.string.auto_tpl_charge_full, R.string.auto_tpl_charge_full_sub, AutomationTrigger.CHARGING_OFF, reminderRes = R.string.auto_tpl_charge_full_text),
        AutomationTemplate(R.string.auto_tpl_sunset, R.string.auto_tpl_sunset_sub, AutomationTrigger.SUNSET),
        AutomationTemplate(R.string.auto_tpl_sunrise, R.string.auto_tpl_sunrise_sub, AutomationTrigger.SUNRISE, actions = listOf(GestureAction.SOUND_ON)),
        AutomationTemplate(R.string.auto_tpl_arrive_note, R.string.auto_tpl_arrive_note_sub, AutomationTrigger.HOME_ARRIVE, reminderRes = R.string.auto_tpl_arrive_note_text),
    )

    private const val MINUTES_PER_DAY = 24 * 60

    private fun launcherAction(change: LauncherChange): String = Actions.encode(ActionChoice(GestureAction.LAUNCHER_SET, change.encode()))
}
