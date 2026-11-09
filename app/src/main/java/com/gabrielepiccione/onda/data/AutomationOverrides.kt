// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import androidx.annotation.StringRes
import com.gabrielepiccione.onda.R
import kotlinx.serialization.Serializable

/** Impostazioni di Onda che un'automazione può cambiare, con i valori proposti. */
@Serializable
enum class LauncherSetting(@StringRes val labelRes: Int) {
    ICON_SIZE(R.string.ls_icon_size),
    TEXT_SIZE(R.string.ls_text_size),
    APP_LABELS(R.string.ls_app_labels),
    PROFILE(R.string.ls_profile),
    GRAYSCALE(R.string.ls_grayscale),
    THEME(R.string.ls_theme),
    SHOW_CLOCK(R.string.ls_show_clock),
    SHOW_AGENDA(R.string.ls_show_agenda),
    SHOW_SUMMARY(R.string.ls_show_summary),
    SHOW_MEDIA(R.string.ls_show_media),
    BLOCK_SPACING(R.string.ls_block_spacing),
    MUSIC_BACKDROP(R.string.ls_music_backdrop),
    HAPTICS(R.string.ls_haptics),
    /** Mostra o nasconde un widget («Mostra widget» / «Nascondi widget»): non compare fra le impostazioni da scegliere. */
    WIDGET(R.string.gesture_widget_show),
}

/** Un valore proposto per un'impostazione: etichetta e testo salvato. */
data class LauncherPreset(@StringRes val labelRes: Int, val value: String)

/** Cosa fa un'azione «Impostazione di Onda»: quale impostazione, che valore, e se ripristinarla al termine. */
data class LauncherChange(val setting: LauncherSetting, val value: String, val restore: Boolean) {
    /** Formato dell'argomento dell'azione: "ICON_SIZE=1.25;1" (1 = ripristina al termine). */
    fun encode(): String = setting.name + "=" + value + ";" + (if (restore) "1" else "0")

    companion object {
        fun decode(arg: String): LauncherChange? {
            val setting = LauncherSetting.entries.firstOrNull { it.name == arg.substringBefore('=') } ?: return null
            val rest = arg.substringAfter('=', "")
            return LauncherChange(setting, rest.substringBeforeLast(';'), rest.substringAfterLast(';', "1") == "1")
        }
    }
}

/**
 * Una sostituzione attiva: l'automazione [automationId] ha messo [setting] a [value]. Le impostazioni salvate non
 * cambiano: quelle in uso sono le salvate con sopra le sostituzioni. [baseline] è il valore salvato quando è partita:
 * se l'utente lo cambia a mano, la sua scelta vince e la sostituzione cade.
 */
@Serializable
data class ActiveOverride(
    val automationId: String,
    val trigger: AutomationTrigger,
    val device: String = "",
    val setting: LauncherSetting,
    val value: String,
    val baseline: String,
    val startedAt: Long,
    /** Cade quando la condizione finisce (scollego, esco di casa…). */
    val restore: Boolean,
    /** Scadenza (ms), 0 = nessuna; per gli orari con «ripristina» è la mezzanotte. */
    val until: Long = 0L,
)

/** Lo strato delle sostituzioni delle automazioni, senza dipendenze Android (testabile su JVM). */
object AutomationOverrides {
    private const val ON = "1"
    private const val OFF = "0"

    fun presets(setting: LauncherSetting): List<LauncherPreset> = when (setting) {
        LauncherSetting.ICON_SIZE -> listOf(
            LauncherPreset(R.string.ls_small, "0.85"), LauncherPreset(R.string.ls_medium, "1.0"),
            LauncherPreset(R.string.ls_large, "1.25"), LauncherPreset(R.string.ls_extra_large, "1.45"),
        )
        LauncherSetting.TEXT_SIZE -> listOf(
            LauncherPreset(R.string.ls_small, "0.9"), LauncherPreset(R.string.ls_medium, "1.0"),
            LauncherPreset(R.string.ls_large, "1.15"), LauncherPreset(R.string.ls_extra_large, "1.3"),
        )
        LauncherSetting.THEME -> listOf(
            LauncherPreset(R.string.ls_theme_light, ThemeMode.LIGHT.name), LauncherPreset(R.string.ls_theme_dark, ThemeMode.DARK.name),
            LauncherPreset(R.string.ls_theme_system, ThemeMode.SYSTEM.name),
        )
        LauncherSetting.BLOCK_SPACING -> listOf(
            LauncherPreset(R.string.ls_compact, "0"), LauncherPreset(R.string.ls_medium, "8"),
            LauncherPreset(R.string.ls_wide, "16"), LauncherPreset(R.string.ls_extra_wide, "24"),
        )
        // Il profilo prende i valori dai profili dell'utente
        LauncherSetting.PROFILE -> listOf(LauncherPreset(R.string.ls_profile_auto, ""))
        else -> listOf(LauncherPreset(R.string.ls_on, ON), LauncherPreset(R.string.ls_off, OFF))
    }

    private fun flag(b: Boolean) = if (b) ON else OFF

    /** Il valore attuale (salvato) di un'impostazione, come testo. */
    fun read(s: Settings, setting: LauncherSetting): String = when (setting) {
        LauncherSetting.ICON_SIZE -> s.iconSize.toString()
        LauncherSetting.TEXT_SIZE -> s.textScale.toString()
        LauncherSetting.APP_LABELS -> flag(!s.hideFavoriteNames)
        LauncherSetting.PROFILE -> s.profileOverride.profileId
        LauncherSetting.GRAYSCALE -> flag(s.grayscaleMode == GrayscaleMode.ALWAYS)
        LauncherSetting.THEME -> s.themeMode.name
        LauncherSetting.SHOW_CLOCK -> flag(s.showClock)
        LauncherSetting.SHOW_AGENDA -> flag(s.showAgenda)
        LauncherSetting.SHOW_SUMMARY -> flag(s.showNotificationSummary)
        LauncherSetting.SHOW_MEDIA -> flag(s.showMedia)
        LauncherSetting.BLOCK_SPACING -> s.homeBlockSpacing.toString()
        LauncherSetting.MUSIC_BACKDROP -> flag(s.musicBackdrop != MusicBackdropMode.OFF)
        LauncherSetting.HAPTICS -> flag(s.haptics)
        // Nessun valore salvato da confrontare: la sostituzione cade solo a fine condizione o se l'automazione sparisce
        LauncherSetting.WIDGET -> ""
    }

    /** Le impostazioni con [setting] a [value]; un valore non valido lascia tutto com'è. */
    fun apply(s: Settings, setting: LauncherSetting, value: String): Settings {
        val on = value == ON
        return when (setting) {
            LauncherSetting.ICON_SIZE -> value.toFloatOrNull()?.let { s.copy(iconSize = it.coerceIn(0.6f, 1.8f)) } ?: s
            LauncherSetting.TEXT_SIZE -> value.toFloatOrNull()?.let { s.copy(textScale = it.coerceIn(0.7f, 1.6f)) } ?: s
            LauncherSetting.APP_LABELS -> s.copy(hideFavoriteNames = !on)
            LauncherSetting.PROFILE -> s.copy(profileOverride = if (value.isBlank()) ProfileOverride() else ProfileOverride(value, Long.MAX_VALUE))
            LauncherSetting.GRAYSCALE -> s.copy(grayscaleMode = if (on) GrayscaleMode.ALWAYS else GrayscaleMode.OFF)
            LauncherSetting.THEME -> ThemeMode.entries.firstOrNull { it.name == value }?.let { s.copy(themeMode = it) } ?: s
            LauncherSetting.SHOW_CLOCK -> s.copy(showClock = on)
            LauncherSetting.SHOW_AGENDA -> s.copy(showAgenda = on)
            LauncherSetting.SHOW_SUMMARY -> s.copy(showNotificationSummary = on)
            LauncherSetting.SHOW_MEDIA -> s.copy(showMedia = on)
            LauncherSetting.BLOCK_SPACING -> value.toIntOrNull()?.let { s.copy(homeBlockSpacing = it.coerceIn(0, 32)) } ?: s
            LauncherSetting.MUSIC_BACKDROP -> s.copy(
                musicBackdrop = if (!on) MusicBackdropMode.OFF else if (s.musicBackdrop == MusicBackdropMode.OFF) MusicBackdropMode.MOTION else s.musicBackdrop,
            )
            LauncherSetting.HAPTICS -> s.copy(haptics = on)
            LauncherSetting.WIDGET -> WidgetVisibility.apply(s, value)
        }
    }

    /** Le impostazioni in uso: le salvate con sopra le sostituzioni attive; con più regole sulla stessa vince l'ultima. */
    fun effective(saved: Settings): Settings =
        saved.automationOverrides.sortedBy { it.startedAt }.fold(saved) { acc, o -> apply(acc, o.setting, o.value) }

    /** Fa partire una sostituzione per l'automazione [a]; se ne aveva già una sulla stessa impostazione la rinnova. */
    fun start(saved: Settings, a: Automation, change: LauncherChange, now: Long, until: Long = 0L): Settings {
        val o = ActiveOverride(a.id, a.trigger, a.device, change.setting, change.value, read(saved, change.setting), now, change.restore, until)
        return saved.copy(automationOverrides = saved.automationOverrides.filterNot { it.automationId == a.id && it.setting == change.setting && sameTarget(it.value, change) } + o)
    }

    /** Per i widget ogni widget è un bersaglio a sé; le altre impostazioni ne hanno uno solo. */
    private fun sameTarget(value: String, change: LauncherChange): Boolean =
        change.setting != LauncherSetting.WIDGET || WidgetVisibility.widgetOf(value) == WidgetVisibility.widgetOf(change.value)

    /**
     * La condizione del trigger vale ancora? null per gli orari e il sole, che non hanno una fine. Si legge dallo stato,
     * così funziona anche dopo che il processo è stato chiuso o il telefono riavviato.
     */
    fun conditionHolds(trigger: AutomationTrigger, device: String, env: EnvSnapshot): Boolean? = when (trigger) {
        AutomationTrigger.HOME_ARRIVE -> env.atHome
        AutomationTrigger.HOME_LEAVE -> !env.atHome
        AutomationTrigger.HEADPHONES_ON -> env.headphones
        AutomationTrigger.HEADPHONES_OFF -> !env.headphones
        AutomationTrigger.CHARGING_ON -> env.charging
        AutomationTrigger.CHARGING_OFF -> !env.charging
        AutomationTrigger.MUSIC_ON -> env.musicPlaying
        AutomationTrigger.MUSIC_OFF -> !env.musicPlaying
        AutomationTrigger.DEVICE_ON -> connected(device, env)
        AutomationTrigger.DEVICE_OFF -> !connected(device, env)
        AutomationTrigger.TIME, AutomationTrigger.SUNSET, AutomationTrigger.SUNRISE -> null
    }

    private fun connected(device: String, env: EnvSnapshot) =
        if (device.isBlank()) env.devices.isNotEmpty() else env.devices.any { it.equals(device, ignoreCase = true) }

    /** «Ripristina al termine» è acceso di default per le coppie (collego/scollego, arrivo/esco, carica). */
    fun defaultRestore(trigger: AutomationTrigger): Boolean = !trigger.isTimed

    /**
     * Toglie le sostituzioni che non valgono più: condizione finita (con «ripristina»), scadute, scelta manuale
     * dell'utente sulla stessa impostazione, automazione eliminata o spenta. [env] null = stato non noto (non si
     * guarda la condizione). Restituisce lo stesso oggetto se non cambia nulla.
     */
    fun prune(saved: Settings, env: EnvSnapshot?, now: Long): Settings {
        val list = saved.automationOverrides
        if (list.isEmpty()) return saved
        val kept = list.filter { o ->
            val automation = saved.automations.firstOrNull { it.id == o.automationId }
            val alive = automation != null && automation.enabled
            val manual = read(saved, o.setting) != o.baseline
            val expired = o.until in 1..now
            val ended = o.restore && env != null && conditionHolds(o.trigger, o.device, env) == false
            alive && !manual && !expired && !ended
        }
        return if (kept.size == list.size) saved else saved.copy(automationOverrides = kept)
    }
}
