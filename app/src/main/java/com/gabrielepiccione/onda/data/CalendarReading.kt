// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/** Da dove si legge: l'elenco dei calendari, la tabella Instances (occorrenze già calcolate) o la tabella Events. */
enum class CalendarSource { CALENDARS, INSTANCES, EVENTS }

/** Cosa ha restituito una sorgente: righe lette ed eventuale errore (tipo e messaggio, mai dati personali). */
data class SourceReport(val source: CalendarSource, val rows: Int, val error: String? = null)

/** Il percorso che ha prodotto gli eventi. */
enum class ReadPath {
    /** Instances ha funzionato (più Events per i calendari non visibili, se ce ne sono). */
    INSTANCES,

    /** Instances ha dato errore o nessuna riga per i calendari visibili: si è ripiegato sulla tabella Events. */
    EVENTS_FALLBACK,

    /** Nessun calendario visibile: solo la tabella Events. */
    EVENTS_ONLY,

    /** Nessuna lettura riuscita. */
    NONE,
}

/** Gli eventi letti e il resoconto della lettura, per la diagnostica. */
data class CalendarRead(val events: List<CalendarEvent>, val sources: List<SourceReport>, val path: ReadPath)

/** Scelta del percorso di lettura, unione dei risultati e messaggi di errore: senza Android (testabile su JVM). */
object CalendarReading {
    private const val MAX_MESSAGE = 120

    /**
     * Tipo e messaggio dell'errore, accorciati: serve a capire quale colonna o quale provider ha rifiutato la query.
     * I numeri lunghi (id, orari) e gli indirizzi email si tolgono: nessun dato personale finisce nella diagnostica.
     */
    fun describeError(t: Throwable): String {
        val type = t.javaClass.simpleName.ifBlank { "Errore" }
        val message = t.message.orEmpty()
            .replace(Regex("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+"), "…")
            .replace(Regex("\\d{6,}"), "#")
            .replace('\n', ' ')
            .trim()
        return if (message.isEmpty()) type else (type + ": " + message).take(MAX_MESSAGE)
    }

    /**
     * Instances va sostituita con la tabella Events se lancia, o se non restituisce righe pur avendo calendari visibili
     * (un provider che non le calcola). Senza calendari visibili non c'è nulla da aspettarsi da Instances.
     */
    fun needsFallback(visibleCalendars: Int, instances: SourceReport?): Boolean =
        visibleCalendars > 0 && (instances == null || instances.error != null || instances.rows == 0)

    /** Il percorso dal resoconto: chi ha funzionato davvero. */
    fun path(visibleCalendars: Int, instances: SourceReport?, fellBack: Boolean): ReadPath = when {
        fellBack -> ReadPath.EVENTS_FALLBACK
        visibleCalendars == 0 -> ReadPath.EVENTS_ONLY
        instances != null && instances.error == null -> ReadPath.INSTANCES
        else -> ReadPath.NONE
    }

    /** Unisce due letture senza doppioni (stesso evento e stesso inizio); l'ordine resta quello di arrivo. */
    fun merge(first: List<CalendarEvent>, second: List<CalendarEvent>): List<CalendarEvent> {
        val seen = HashSet<Pair<Long, Long>>()
        return (first + second).filter { seen.add(it.eventId to it.begin) }
    }

    /** Dà il colore del calendario di provenienza agli eventi che non ce l'hanno (Instances non lo legge). */
    fun withColors(events: List<CalendarEvent>, colors: Map<Long, Int>): List<CalendarEvent> =
        events.map { if (it.color == 0 && it.calendarId in colors) it.copy(color = colors.getValue(it.calendarId)) else it }

    /** Righe lette da una sorgente, 0 se non l'ha fatta. */
    fun rows(sources: List<SourceReport>, source: CalendarSource): Int = sources.filter { it.source == source }.sumOf { it.rows }

    fun errors(sources: List<SourceReport>): List<SourceReport> = sources.filter { it.error != null }

    /** Il testo della diagnostica da copiare: solo id, flag, conteggi ed errori (nessun nome, account o titolo). */
    fun copyText(permission: Boolean, calendars: List<CalendarInfo>, excluded: Set<Long>, read: CalendarReadInfo?, allDayMode: AllDayMode): String = buildString {
        appendLine("Onda – diagnostica calendario")
        appendLine("permesso: " + if (permission) "sì" else "no")
        appendLine("eventi tutto il giorno: " + allDayMode.name)
        appendLine("calendari: ${calendars.size}")
        calendars.forEach { c ->
            appendLine("  #${c.id} visibile=${c.visible} sync=${c.synced} spento=${c.id in excluded} eventi14=${c.events14} tuttoilgiorno14=${c.allDay14}")
        }
        if (read != null) {
            appendLine("percorso: " + read.path.name)
            read.sources.forEach { appendLine("  ${it.source.name}: righe=${it.rows}" + (it.error?.let { e -> " errore=$e" } ?: "")) }
        }
    }.trim()
}

/** Come è andata l'ultima lettura (per la diagnostica). */
data class CalendarReadInfo(val path: ReadPath, val sources: List<SourceReport>)
