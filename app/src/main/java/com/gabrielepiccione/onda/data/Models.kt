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
    /** App dello Spazio privato (Android 15+): non è un'app del profilo di lavoro. */
    val isPrivate: Boolean = false,
    /** Momento della prima installazione (ms), per "Installate di recente". */
    val installedAt: Long = 0L,
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
    /** Icona scelta a mano: ha la precedenza sugli stili generati. */
    val hasCustomIcon: Boolean = false,
    val isPrivate: Boolean = false,
    val installedAt: Long = 0L,
) {
    val packageName: String get() = component.packageName
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

/** Azione associata a uno swipe verticale in home. */
@Serializable
enum class GestureAction(@StringRes val labelRes: Int) {
    SEARCH(R.string.gesture_search),
    NOTIFICATIONS(R.string.gesture_notifications),
    ALL_APPS(R.string.gesture_all_apps),
    LOCK_SCREEN(R.string.gesture_lock),
    SETTINGS(R.string.settings),
    OPEN_APP(R.string.gesture_open_app),
    NONE(R.string.gesture_none),
}

/** Animazione con cui si aprono le app dal launcher. */
@Serializable
enum class OpenAnimation(@StringRes val labelRes: Int) {
    SYSTEM(R.string.anim_system),
    FADE(R.string.anim_fade),
    SCALE(R.string.anim_scale),
    SLIDE(R.string.anim_slide),
}

/** Cartella in home: contiene app (per chiave) e widget (per id). */
@Immutable
@Serializable
data class Folder(
    val id: String,
    val name: String,
    val apps: List<String> = emptyList(),
    val widgets: List<Int> = emptyList(),
    /** Icona della cartella: "" = anteprima delle app, poi "app:<chiave>", "sym:<nome>" o "file:<percorso>" (vedi [FolderIcons]). */
    val icon: String = "",
    /** Altezza dei widget nella cartella (dp); null = la maggiore tra le altezze predefinite. */
    val widgetHeightDp: Int? = null,
)

/** Prefisso delle icone da immagine in [Settings.iconOverrides]; segue il percorso del file. */
const val ICON_FILE_PREFIX = "file:"

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

/** Pila di widget: occupa un solo blocco della home e si sfoglia in orizzontale. */
@Immutable
@Serializable
data class WidgetStack(
    val id: String,
    val widgets: List<Int> = emptyList(),
    /** Altezza scelta in dp; null = la maggiore tra le altezze predefinite dei widget. */
    val heightDp: Int? = null,
)

/** Operazioni sulle pile di widget, senza dipendenze Android (testabili su JVM). */
object WidgetStacks {

    /** Aggiunge un widget alla pila [block], oppure crea una pila se [block] è un widget singolo. */
    fun add(s: Settings, block: String, newId: Int, newStackId: String): Settings {
        val existing = HomeBlocks.stackId(block)
        if (existing != null) {
            return s.copy(widgetStacks = s.widgetStacks.map { if (it.id == existing) it.copy(widgets = it.widgets + newId) else it })
        }
        val first = HomeBlocks.widgetId(block) ?: return s
        return s.copy(
            widgetIds = s.widgetIds - first,
            widgetStacks = s.widgetStacks + WidgetStack(newStackId, listOf(first, newId), s.widgetHeights[first]),
            widgetHeights = s.widgetHeights - first,
            homeLayout = HomeBlocks.resolve(s).map { if (it == block) HomeBlocks.stack(newStackId) else it },
        )
    }

    /** Toglie un widget dalla sua pila: con un solo widget rimasto, la pila torna un widget singolo. */
    fun remove(s: Settings, widgetId: Int): Settings {
        val stack = s.widgetStacks.firstOrNull { widgetId in it.widgets } ?: return s
        val left = stack.widgets - widgetId
        val block = HomeBlocks.stack(stack.id)
        if (left.size >= 2) {
            return s.copy(widgetStacks = s.widgetStacks.map { if (it.id == stack.id) it.copy(widgets = left) else it })
        }
        val others = s.widgetStacks.filterNot { it.id == stack.id }
        val last = left.singleOrNull() ?: return s.copy(widgetStacks = others, homeLayout = s.homeLayout - block)
        return s.copy(
            widgetStacks = others,
            widgetIds = s.widgetIds + last,
            widgetHeights = stack.heightDp?.let { s.widgetHeights + (last to it) } ?: s.widgetHeights,
            homeLayout = s.homeLayout.map { if (it == block) HomeBlocks.widget(last) else it },
        )
    }

    /** Sostituisce un widget nella stessa posizione della sua pila. */
    fun replace(s: Settings, oldId: Int, newId: Int): Settings = s.copy(
        widgetStacks = s.widgetStacks.map { st -> st.copy(widgets = st.widgets.map { if (it == oldId) newId else it }) },
    )

    fun setHeight(s: Settings, stackId: String, dp: Int): Settings = s.copy(
        widgetStacks = s.widgetStacks.map { if (it.id == stackId) it.copy(heightDp = dp) else it },
    )
}

/** Blocchi della home, nell'ordine scelto. */
object HomeBlocks {
    const val CLOCK = "clock"
    const val MEDIA = "media"
    const val APPS = "apps"
    const val AGENDA = "agenda"
    const val NOTIFICATIONS = "notifications"
    const val WIDGET_PREFIX = "w:"
    fun widget(id: Int) = WIDGET_PREFIX + id
    fun widgetId(block: String): Int? = block.removePrefix(WIDGET_PREFIX).toIntOrNull()?.takeIf { block.startsWith(WIDGET_PREFIX) }
    const val STACK_PREFIX = "s:"
    fun stack(id: String) = STACK_PREFIX + id
    fun stackId(block: String): String? = block.takeIf { it.startsWith(STACK_PREFIX) }?.removePrefix(STACK_PREFIX)

    /** Ordine effettivo: quello salvato, ripulito, più gli elementi mancanti in coda (prima delle app). */
    fun resolve(settings: Settings): List<String> {
        val widgets = settings.widgetIds.map(::widget) + settings.widgetStacks.map { stack(it.id) }
        val optional = listOfNotNull(
            AGENDA.takeIf { settings.showAgenda },
            NOTIFICATIONS.takeIf { settings.showNotificationSummary },
        )
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
    /** Personalizzazione dei popup: chiave app -> app correlate, widget, scorciatoie nascoste. */
    val appPopups: Map<String, AppPopup> = emptyMap(),
    val launchCounts: Map<String, Int> = emptyMap(),
    /** Mostra sotto i preferiti le app usate spesso (vedi [HomeSuggestions]). */
    val suggestInHome: Boolean = false,
    /** App per cui si è scelto "Smetti di suggerire". */
    val suggestionsDismissed: Set<String> = emptySet(),
    val swipeDown: GestureAction = GestureAction.SEARCH,
    val swipeUp: GestureAction = GestureAction.SEARCH,
    val hideStatusBar: Boolean = false,
    val hideAlphabet: Boolean = false,
    /** Mostra i contatti nell'elenco alfabetico, insieme alle app. */
    val contactsInAlphabet: Boolean = false,
    val hideFavoriteNames: Boolean = false,
    val iconSize: Float = 1f,
    /** Ordine dei blocchi della home (vedi [HomeBlocks]). */
    val homeLayout: List<String> = emptyList(),
    /**
     * Punto dello schermo in cui inizia il primo blocco della home, come frazione dell'altezza
     * (0 = in alto). Negativo = automatico: i blocchi restano ancorati in basso.
     */
    val homeTopFraction: Float = -1f,
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
    /** Riepilogo delle notifiche come blocco della home. */
    val showNotificationSummary: Boolean = false,
    /** Blocca il layout: niente modifiche con la pressione lunga in home. */
    val lockLayout: Boolean = false,
    /** Motore per la ricerca web: SYSTEM, DUCKDUCKGO, STARTPAGE, GOOGLE, ECOSIA, BRAVE. */
    val searchEngine: String = "SYSTEM",
    /** Plug & Play: app mostrata in cima quando si collegano cuffie o casse (chiave app, vuoto = off). */
    val plugPlayApp: String = "",
    /** App che richiedono lo sblocco (impronta, volto o PIN) prima di aprirsi. */
    val protectedApps: Set<String> = emptySet(),
    /** Richiede lo sblocco anche per vedere l'elenco delle app nascoste. */
    val protectHidden: Boolean = false,
    /** Usage Breaker: app che mostrano una pausa prima di aprirsi. */
    val breakerApps: Set<String> = emptySet(),
    /** Usage Breaker a promemoria: app per cui avvisare durante l'uso (vedi [UsageReminders]). */
    val reminderApps: Set<String> = emptySet(),
    /** Minuti di uso continuo a cui arriva il primo promemoria. */
    val reminderStartMinutes: Int = 20,
    /** Durata della pausa in secondi. */
    val breakerSeconds: Int = 10,
    /** Giorno (AAAA-MM-GG) a cui si riferisce [breakerOpens]. */
    val breakerDay: String = "",
    /** Aperture di oggi delle app con Usage Breaker: chiave app -> numero. */
    val breakerOpens: Map<String, Int> = emptyMap(),
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
    /** Forma scelta per singola app: chiave app -> nome di [com.gabrielepiccione.onda.core.icons.IconShape]. */
    val iconShapeOverrides: Map<String, String> = emptyMap(),
    val themedIcons: Boolean = false,
    val showIcons: Boolean = true,
    val textScale: Float = 1f,
    val showClock: Boolean = true,
    val showCalendar: Boolean = true,
    /** Temperatura nell'orologio e previsioni nel foglio agenda, lette da Breezy Weather. */
    val showWeather: Boolean = false,
    val showMedia: Boolean = true,
    val wallpaperDim: Float = 0.2f,
    val leftHanded: Boolean = false,
    /** Doppio tocco su uno spazio vuoto della home o sull'alfabeto. */
    val doubleTapAction: GestureAction = GestureAction.NONE,
    /** App aperte dai gesti con azione [GestureAction.OPEN_APP] (chiave app). */
    val swipeUpApp: String = "",
    val doubleTapApp: String = "",
    val openAnimation: OpenAnimation = OpenAnimation.SYSTEM,
    val haptics: Boolean = true,
    val widgetIds: List<Int> = emptyList(),
    /** Scorciatoie aggiunte alla home dalle altre app; il loro posto è in [favorites]. */
    val pinnedShortcuts: List<PinnedShortcut> = emptyList(),
    /** Pile di widget in home; i loro widget non compaiono in [widgetIds]. */
    val widgetStacks: List<WidgetStack> = emptyList(),
    /** Nasconde il contenitore dello Spazio privato dall'elenco delle app. */
    val hidePrivateSpace: Boolean = false,
    /** Cartella scelta per il backup automatico (URI di documento); vuoto = disattivato. */
    val autoBackupUri: String = "",
    /** Giorno (AAAA-MM-GG) dell'ultimo backup automatico riuscito. */
    val autoBackupLastDay: String = "",
    val onboardingDone: Boolean = false,
    /** Benvenuto già visto; chi aggiorna da una versione precedente non lo rivede. */
    val welcomeDone: Boolean = false,
) {
    companion object {
        /**
         * 1 = formato JSON "libero" delle versioni 0.2/0.3; 2 = formato kotlinx.serialization (0.4+);
         * 3 = swipe giù sempre sulla tendina: lo swipe su, se era "notifiche", diventa "ricerca";
         * 4 = "doubleTapLock" sostituito da "doubleTapAction"; il benvenuto è già visto per chi aggiorna.
         */
        const val CURRENT_SCHEMA = 4
    }
}

/** Riepilogo delle notifiche attive: quante sono e di quali app, dalla più recente. */
data class NotificationDigest(val total: Int, val packages: List<String>) {
    companion object {
        fun of(byPackage: Map<String, List<NotificationItem>>): NotificationDigest {
            val active = byPackage.filterValues { it.isNotEmpty() }
            return NotificationDigest(
                total = active.values.sumOf { it.size },
                packages = active.entries.sortedByDescending { (_, list) -> list.maxOf { it.postTime } }.map { it.key },
            )
        }
    }
}

/** Contatore giornaliero di Usage Breaker: i conteggi ripartono da zero ogni giorno. */
object UsageBreaker {
    fun opensToday(s: Settings, key: String, today: String): Int =
        if (s.breakerDay == today) s.breakerOpens[key] ?: 0 else 0

    fun recordOpen(s: Settings, key: String, today: String): Settings {
        val opens = if (s.breakerDay == today) s.breakerOpens else emptyMap()
        return s.copy(breakerDay = today, breakerOpens = opens + (key to (opens[key] ?: 0) + 1))
    }

    fun toggle(s: Settings, key: String): Settings =
        s.copy(breakerApps = if (key in s.breakerApps) s.breakerApps - key else s.breakerApps + key)
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
