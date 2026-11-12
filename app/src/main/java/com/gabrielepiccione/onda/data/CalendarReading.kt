// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/** Da dove si legge: l'elenco dei calendari, la tabella Instances (occorrenze già calcolate) o la tabella Events. */
enum class CalendarSource { CALENDARS, INSTANCES, EVENTS }

/** Cosa ha restituito una sorgente: righe lette ed eventuale errore (tipo e messaggio, mai dati personali). */
data class SourceReport(val source: CalendarSource, val rows: Int, val error: String? = null)

/** Quale sorgente ha dato gli eventi che si vedono. */
enum class ReadPath {
    /** Solo Instances. */
    INSTANCES,

    /** Solo la tabella Events (con le ricorrenze calcolate). */
    EVENTS,

    /** Tutte e due: gli eventi sono l'unione delle sorgenti, senza doppioni. */
    UNION,

    /** Nessuna sorgente ha dato righe. */
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

    /** Il percorso dalle righe che ogni sorgente ha restituito. */
    fun path(instancesRows: Int, eventsRows: Int): ReadPath = when {
        instancesRows > 0 && eventsRows > 0 -> ReadPath.UNION
        instancesRows > 0 -> ReadPath.INSTANCES
        eventsRows > 0 -> ReadPath.EVENTS
        else -> ReadPath.NONE
    }

    /**
     * Unisce Instances e gli eventi della tabella Events (ricorrenze già calcolate) senza doppioni, per EVENT_ID e inizio.
     * Se Instances ha già un evento (anche solo una occorrenza) vale Instances per tutte le sue occorrenze: la tabella
     * Events non sa delle eccezioni (occorrenze cancellate o spostate) e le farebbe rivivere. Gli eventi che Instances
     * non ha affatto vengono aggiunti dalla tabella Events.
     */
    fun merge(instances: List<CalendarEvent>, fromEvents: List<CalendarEvent>): List<CalendarEvent> {
        val known = instances.mapTo(HashSet()) { it.eventId }
        val seen = HashSet<Pair<Long, Long>>()
        return (instances + fromEvents.filter { it.eventId !in known }).filter { seen.add(it.eventId to it.begin) }
    }

    /** Dà il colore del calendario di provenienza agli eventi che non ce l'hanno (Instances non lo legge). */
    fun withColors(events: List<CalendarEvent>, colors: Map<Long, Int>): List<CalendarEvent> =
        events.map { if (it.color == 0 && it.calendarId in colors) it.copy(color = colors.getValue(it.calendarId)) else it }

    /** Righe lette da una sorgente, 0 se non l'ha fatta. */
    fun rows(sources: List<SourceReport>, source: CalendarSource): Int = sources.filter { it.source == source }.sumOf { it.rows }

    fun errors(sources: List<SourceReport>): List<SourceReport> = sources.filter { it.error != null }

    /**
     * Il testo della diagnostica da copiare: id, tipo di account, flag, conteggi ed errori, più produttore e versione
     * di Android. Nessun nome di calendario, titolo di evento o indirizzo email.
     */
    fun copyText(
        permission: Boolean,
        calendars: List<CalendarInfo>,
        excluded: Set<Long>,
        read: CalendarReadInfo?,
        allDayMode: AllDayMode,
        probe: CalendarProbe? = null,
        device: String = "",
    ): String = buildString {
        appendLine("Onda – diagnostica calendario")
        if (device.isNotBlank()) appendLine("dispositivo: $device")
        appendLine("permesso: " + if (permission) "sì" else "no")
        appendLine("eventi tutto il giorno: " + allDayMode.name)
        appendLine("calendari: ${calendars.size}, inclusi ${CalendarLogic.includedCount(calendars, excluded)} su ${calendars.size}")
        calendars.forEach { c ->
            appendLine(
                "  #${c.id} ${c.accountType.ifBlank { "?" }} visibile=${c.visible} sync=${c.synced} accesso=${CalendarLogic.accessName(c.access)}" +
                    " principale=${c.primary ?: "?"} incluso=${CalendarLogic.isIncluded(c.id, excluded)}",
            )
            appendLine("      Instances oggi=${c.instances.today} 14g=${c.instances.days14} tuttoilgiorno=${c.instances.allDay14}")
            appendLine("      Events    oggi=${c.events.today} 14g=${c.events.days14} tuttoilgiorno=${c.events.allDay14}")
        }
        if (read != null) {
            appendLine("percorso: " + read.path.name)
            read.sources.forEach { appendLine("  ${it.source.name}: righe=${it.rows}" + (it.error?.let { e -> " errore=$e" } ?: "")) }
        }
        if (probe != null) {
            appendLine("prova di lettura:")
            probe.steps.forEach { appendLine("  ${it.step.name}: righe=${it.rows}" + (it.error?.let { e -> " errore=$e" } ?: "")) }
            appendLine("  tabella Events: righe totali=${probe.eventsTotalRows}, evento futuro più vicino=${probe.nearestFuture ?: "—"}" + (probe.error?.let { e -> " errore=$e" } ?: ""))
        }
    }.trim()
}

/** I passi della prova di lettura. */
enum class ProbeStep { INSTANCES_TODAY, INSTANCES_14, EVENTS_14_NO_FILTER }

/** Un passo della prova: righe lette ed eventuale eccezione. */
data class ProbeResult(val step: ProbeStep, val rows: Int, val error: String? = null)

/** La prova di lettura: tre letture separate e la tabella Events in generale (totale righe e data del prossimo evento, senza titolo). */
data class CalendarProbe(val steps: List<ProbeResult>, val eventsTotalRows: Int, val nearestFuture: String?, val error: String? = null)

/** Come è andata l'ultima lettura (per la diagnostica). */
data class CalendarReadInfo(val path: ReadPath, val sources: List<SourceReport>)
