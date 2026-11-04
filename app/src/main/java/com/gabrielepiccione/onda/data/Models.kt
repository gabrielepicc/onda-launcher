// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import android.app.PendingIntent
import android.content.ComponentName
import android.os.UserHandle
import androidx.compose.ui.graphics.ImageBitmap
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.gabrielepiccione.onda.R
import kotlinx.serialization.Serializable
import java.text.Normalizer

/** App installata così come la restituisce LauncherApps, prima delle personalizzazioni. */
data class RawApp(
    val key: String,
    val component: ComponentName,
    val user: UserHandle,
    val label: String,
    val icon: ImageBitmap,
    /** Maschera monocromatica per le icone a tema (Android 13+), se l'app la fornisce. */
    val mono: ImageBitmap?,
    val isWorkProfile: Boolean,
    val category: AppCategory = AppCategory.OTHER,
    /** Colore dominante dell'icona originale (ARGB), per gli stili generati. */
    val color: Int = 0xFF808080.toInt(),
)

/** App pronta per la UI, con nome personalizzato applicato. */
data class AppInfo(
    val key: String,
    val component: ComponentName,
    val user: UserHandle,
    val label: String,
    val originalLabel: String,
    val icon: ImageBitmap,
    val mono: ImageBitmap?,
    val isWorkProfile: Boolean,
    val category: AppCategory = AppCategory.OTHER,
    val color: Int = 0xFF808080.toInt(),
    val isFavorite: Boolean,
    val isHidden: Boolean,
    /** Chiave dell'app aperta scorrendo la riga verso destra, se impostata. */
    val swipeTarget: String?,
) {
    val packageName: String get() = component.packageName
    val section: Char get() = sectionOf(label)
}

/** Riga di una conversazione (WhatsApp, Telegram, SMS…). */
data class MessageLine(val sender: String, val text: String)

data class NotificationItem(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val contentIntent: PendingIntent?,
    val autoCancel: Boolean,
    val clearable: Boolean,
    /** Ultimi messaggi se la notifica è una conversazione (MessagingStyle). */
    val messages: List<MessageLine> = emptyList(),
    /** Azione con campo di testo libero: risposta diretta dalla home. */
    val replyAction: android.app.Notification.Action? = null,
    /** Altre azioni senza input (es. "Segna come letto"), al massimo tre. */
    val actions: List<android.app.Notification.Action> = emptyList(),
    /** Foto del contatto o icona grande della notifica. */
    val largeIcon: ImageBitmap? = null,
    /** Nome della conversazione (gruppi) se diverso dal titolo. */
    val conversationTitle: String? = null,
) {
    /** A chi si risponde: titolo della conversazione o mittente. */
    val replyTo: String get() = conversationTitle ?: title
    val canReply: Boolean get() = replyAction != null
}

data class ContactHit(
    val id: Long,
    val lookupKey: String,
    val name: String,
    val photoUri: String?,
)

data class CalendarEvent(
    val title: String,
    val begin: Long,
    val allDay: Boolean,
    val eventId: Long,
    val end: Long = begin,
    /** Colore del calendario di provenienza (ARGB), 0 se sconosciuto. */
    val color: Int = 0,
)

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Azione associata a un gesto in home (swipe su, doppio tocco). */
@Serializable
enum class GestureAction(@StringRes val labelRes: Int, val needsAccessibility: Boolean = false) {
    SEARCH(R.string.gesture_search),
    NOTIFICATIONS(R.string.gesture_notifications),
    ALL_APPS(R.string.gesture_all_apps),
    LOCK_SCREEN(R.string.gesture_lock, needsAccessibility = true),
    QUICK_SETTINGS(R.string.gesture_quick_settings),
    RECENTS(R.string.gesture_recents, needsAccessibility = true),
    SCREENSHOT(R.string.gesture_screenshot, needsAccessibility = true),
    ONDA_SETTINGS(R.string.gesture_onda_settings),
    OPEN_APP(R.string.gesture_open_app),
    NONE(R.string.gesture_none),
}

/** Cartella in home: contiene app (per chiave) e widget (per id). */
@Immutable
@Serializable
data class Folder(
    val id: String,
    val name: String,
    val apps: List<String> = emptyList(),
    val widgets: List<Int> = emptyList(),
)

/** Pila di widget in home: un solo widget visibile alla volta, si cambia scorrendo di lato. */
@Immutable
@Serializable
data class WidgetStack(
    val id: String,
    val widgets: List<Int> = emptyList(),
    /** Altezza comune in dp; 0 = la maggiore tra le altezze minime dei widget. */
    val heightDp: Int = 0,
)

/** Prefisso delle voci "cartella" nell'elenco dei preferiti. */
const val FOLDER_PREFIX = "folder:"

/** Stili dell'orologio in home (disegnati da Onda). */
@Serializable
enum class ClockStyle(@StringRes val labelRes: Int) {
    LARGE_THIN(R.string.clock_large_thin),
    STACKED(R.string.clock_stacked),
    BOLD(R.string.clock_bold),
    MONO(R.string.clock_mono),
    OUTLINE(R.string.clock_outline),
    GRADIENT(R.string.clock_gradient),
    WORDS(R.string.clock_words),
    ANALOG(R.string.clock_analog),
}

@Serializable
enum class HAlign(@StringRes val labelRes: Int) { START(R.string.align_start), CENTER(R.string.align_center), END(R.string.align_end) }

/** Blocchi della home, nell'ordine scelto. */
object HomeBlocks {
    const val CLOCK = "clock"
    const val MEDIA = "media"
    const val APPS = "apps"
    const val AGENDA = "agenda"
    const val SUMMARY = "summary"
    const val WIDGET_PREFIX = "w:"
    const val STACK_PREFIX = "s:"
    fun widget(id: Int) = WIDGET_PREFIX + id
    fun widgetId(block: String): Int? = block.removePrefix(WIDGET_PREFIX).toIntOrNull()?.takeIf { block.startsWith(WIDGET_PREFIX) }
    fun stack(id: String) = STACK_PREFIX + id
    fun stackId(block: String): String? = block.removePrefix(STACK_PREFIX).takeIf { block.startsWith(STACK_PREFIX) && it.isNotEmpty() }

    /** Ordine effettivo: quello salvato, ripulito, più gli elementi mancanti in coda (prima delle app). */
    fun resolve(settings: Settings): List<String> {
        val widgets = settings.widgetIds.map(::widget) +
            settings.widgetStacks.values.filter { it.widgets.isNotEmpty() }.map { stack(it.id) }
        val optional = listOfNotNull(AGENDA.takeIf { settings.showAgenda }, SUMMARY.takeIf { settings.showSummary })
        val valid = setOf(CLOCK, MEDIA, APPS) + optional + widgets
        val saved = settings.homeLayout.filter { it in valid }.distinct()
        val missing = (listOf(CLOCK, MEDIA) + optional + widgets + APPS).filter { it !in saved }
        if (saved.isEmpty()) return listOf(CLOCK, MEDIA) + optional + widgets + APPS
        val out = saved.toMutableList()
        missing.forEach { m ->
            val appsAt = out.indexOf(APPS)
            if (m == APPS || appsAt < 0) out.add(m) else out.add(appsAt, m)
        }
        return out
    }
}

@Serializable
enum class FontChoice(@StringRes val labelRes: Int) {
    SYSTEM(R.string.font_system),
    SANS(R.string.font_sans),
    SERIF(R.string.font_serif),
    MONO(R.string.font_mono),
    ROUNDED(R.string.font_cursive),
}

/**
 * Tutte le preferenze dell'utente. Ogni campo ha un default, così i file salvati con versioni
 * precedenti restano leggibili. Se un campo cambia significato, incrementare [CURRENT_SCHEMA]
 * e aggiungere il passo in [SettingsCodec.migrate].
 */
@Immutable
@Serializable
data class Settings(
    val schemaVersion: Int = CURRENT_SCHEMA,
    val favorites: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val renames: Map<String, String> = emptyMap(),
    val swipeActions: Map<String, String> = emptyMap(),
    val folders: Map<String, Folder> = emptyMap(),
    /** Widget mostrato nel pannello dell'app (swipe sulla riga): chiave app -> id widget. */
    val appWidgets: Map<String, Int> = emptyMap(),
    val launchCounts: Map<String, Int> = emptyMap(),
    val swipeDown: GestureAction = GestureAction.SEARCH,
    val swipeUp: GestureAction = GestureAction.SEARCH,
    /** App aperta dallo swipe su quando l'azione è [GestureAction.OPEN_APP]. */
    val swipeUpApp: String = "",
    /** Doppio tocco su uno spazio vuoto della home o sull'alfabeto. */
    val doubleTap: GestureAction = GestureAction.NONE,
    val doubleTapApp: String = "",
    val hideStatusBar: Boolean = false,
    val hideAlphabet: Boolean = false,
    val hideFavoriteNames: Boolean = false,
    val iconSize: Float = 1f,
    /** Ordine dei blocchi della home (vedi [HomeBlocks]). */
    val homeLayout: List<String> = emptyList(),
    /** Pile di widget in home (blocchi "s:<id>"); i loro widget non sono in [widgetIds]. */
    val widgetStacks: Map<String, WidgetStack> = emptyMap(),
    /** Altezza personalizzata dei widget in dp: id widget -> dp. */
    val widgetHeights: Map<Int, Int> = emptyMap(),
    val clockStyle: ClockStyle = ClockStyle.LARGE_THIN,
    val clockScale: Float = 1f,
    val clockAlign: HAlign = HAlign.START,
    /** Player: compatto su una riga (true) o esteso con barra di avanzamento. */
    val mediaCompact: Boolean = true,
    val mediaScale: Float = 1f,
    /** Colore del testo sopra lo sfondo (ARGB); 0 = automatico dal tema. */
    val textColor: Long = 0,
    /** Allineamento delle righe dei preferiti. */
    val listAlign: HAlign = HAlign.START,
    /** Peso del carattere dei nomi delle app (100–900). */
    val listFontWeight: Int = 400,
    /** Spaziatura tra le lettere dei nomi delle app, in em. */
    val listLetterSpacing: Float = 0f,
    /** Ombra sotto i testi, utile con sfondi chiari o molto dettagliati. */
    val textShadow: Boolean = false,
    /** Mostra la percentuale di batteria accanto alla data. */
    val showBattery: Boolean = true,
    /** Font importati (percorso nella memoria privata dell'app), per lista e orologio; vuoto = carattere scelto. */
    val customFontList: String = "",
    val customFontClock: String = "",
    /** Agenda del calendario in home (blocco) e numero di giorni mostrati. */
    val showAgenda: Boolean = false,
    val agendaDays: Int = 3,
    /**
     * Posizione verticale del primo blocco della home (orologio o widget), come frazione dell'altezza
     * utile dello schermo (0 = in alto, 0.75 = molto in basso); tutto il resto lo segue.
     * Negativo = automatico: contenuto appoggiato in basso, vicino al pollice.
     */
    val homeOffset: Float = -1f,
    /** Animazione di apertura delle app (vedi [com.gabrielepiccione.onda.util.LaunchAnimation]). */
    val openAnimation: String = "SYSTEM",
    /** Blocca il layout: niente modifiche con la pressione lunga in home. */
    val lockLayout: Boolean = false,
    /** Motore per la ricerca web: SYSTEM, DUCKDUCKGO, STARTPAGE, GOOGLE, ECOSIA, BRAVE. */
    val searchEngine: String = "SYSTEM",
    /** Plug & Play: app mostrata in cima quando si collegano cuffie o casse (chiave app, vuoto = off). */
    val plugPlayApp: String = "",
    /** App che richiedono lo sblocco (impronta, volto o PIN) prima di aprirsi. */
    val protectedApps: Set<String> = emptySet(),
    /** Blocco "riepilogo notifiche" in home (notifiche raggruppate per app). */
    val showSummary: Boolean = false,
    /** App (pacchetti) le cui notifiche vengono rinviate agli orari del riepilogo. */
    val digestApps: Set<String> = emptySet(),
    /** Orari del riepilogo in minuti dall'inizio del giorno. */
    val digestTimes: List<Int> = listOf(9 * 60, 13 * 60 + 30, 19 * 60),
    /** Contatti preferiti in cima all'elenco app, raggiungibili con "@" sull'alfabeto. */
    val alphabetContacts: Boolean = false,
    /** Pausa consapevole: app che prima di aprirsi mostrano qualche secondo di respiro. */
    val breakApps: Set<String> = emptySet(),
    /** Durata della pausa in secondi. */
    val breakSeconds: Int = 5,
    /** Aperture giornaliere oltre le quali la pausa raddoppia (0 = nessun limite). */
    val breakDailyLimit: Int = 0,
    /** Aperture di oggi per app (chiave app -> volte), azzerate al cambio di giorno. */
    val dailyOpens: Map<String, Int> = emptyMap(),
    /** Giorno a cui si riferisce [dailyOpens] (epoch day locale). */
    val dailyOpensDay: Long = 0,
    /** Richiede lo sblocco anche per vedere l'elenco delle app nascoste. */
    val protectHidden: Boolean = false,
    /** Temi salvati dall'utente nel Theme Studio. */
    val savedThemes: List<LookTheme> = emptyList(),
    /** Ultimo preset di stile applicato (solo informativo). */
    val stylePreset: String = "",
    val mutedNotifications: Set<String> = emptySet(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val accent: Long = 0xFF7FE3D1,
    val font: FontChoice = FontChoice.SYSTEM,
    val iconPack: String = "",
    /** Forma delle icone adattive (nome di [com.gabrielepiccione.onda.core.icons.IconShape]). */
    val iconShape: String = "SYSTEM",
    /** Stile delle icone (nome di [com.gabrielepiccione.onda.core.icons.IconStyle]). */
    val iconStyle: String = "ORIGINAL",
    /** Icona scelta a mano per singola app: chiave app -> "pacchetto_pack|nome_drawable". */
    val iconOverrides: Map<String, String> = emptyMap(),
    val themedIcons: Boolean = false,
    val showIcons: Boolean = true,
    val textScale: Float = 1f,
    val showClock: Boolean = true,
    val showCalendar: Boolean = true,
    val showMedia: Boolean = true,
    val wallpaperDim: Float = 0.2f,
    val leftHanded: Boolean = false,
    val haptics: Boolean = true,
    val widgetIds: List<Int> = emptyList(),
    val onboardingDone: Boolean = false,
) {
    companion object {
        /**
         * 1 = formato JSON "libero" delle versioni 0.2/0.3; 2 = formato kotlinx.serialization (0.4+);
         * 3 = swipe giù sempre sulla tendina: lo swipe su, se era "notifiche", diventa "ricerca".
         * 4 = il doppio tocco diventa un gesto configurabile: "doubleTapLock" -> "doubleTap" = LOCK_SCREEN.
         */
        const val CURRENT_SCHEMA = 4
    }
}

/** Lettera di sezione: maiuscola senza accenti, '#' per numeri e simboli. */
fun sectionOf(label: String): Char {
    val first = label.trim().firstOrNull() ?: return '#'
    val plain = Normalizer.normalize(first.toString(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .uppercase()
        .firstOrNull() ?: return '#'
    return if (plain in 'A'..'Z') plain else '#'
}

/** Normalizzazione per la ricerca: minuscolo, senza accenti. */
fun String.normalizedForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()
