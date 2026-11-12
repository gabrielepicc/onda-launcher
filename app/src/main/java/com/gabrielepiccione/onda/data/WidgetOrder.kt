// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/** Un gruppo di widget che si può riordinare: un carosello (o righe) della home, oppure i widget di una cartella. */
sealed interface WidgetGroupRef {
    data class Stack(val id: String) : WidgetGroupRef
    data class Folder(val id: String) : WidgetGroupRef
}

/**
 * Riordino dei widget dentro un gruppo e passaggio da un gruppo all'altro (home e cartelle), senza dipendenze Android
 * (testabile su JVM). Nessuna struttura salvata cambia: solo l'ordine delle liste esistenti.
 */
object WidgetOrder {
    fun widgetsOf(s: Settings, ref: WidgetGroupRef): List<Int> = when (ref) {
        is WidgetGroupRef.Stack -> s.widgetStacks.firstOrNull { it.id == ref.id }?.widgets.orEmpty()
        is WidgetGroupRef.Folder -> s.folders[ref.id]?.widgets.orEmpty()
    }

    private fun withWidgets(s: Settings, ref: WidgetGroupRef, widgets: List<Int>): Settings = when (ref) {
        is WidgetGroupRef.Stack -> s.copy(widgetStacks = s.widgetStacks.map { if (it.id == ref.id) it.copy(widgets = widgets) else it })
        is WidgetGroupRef.Folder -> s.folders[ref.id]?.let { f -> s.copy(folders = s.folders + (ref.id to f.copy(widgets = widgets))) } ?: s
    }

    /** Sposta la voce in posizione [from] alla posizione [to] (entrambe nel gruppo); fuori dai limiti non fa nulla. */
    fun reorder(list: List<Int>, from: Int, to: Int): List<Int> {
        if (from !in list.indices || to !in list.indices || from == to) return list
        return list.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun reorder(s: Settings, ref: WidgetGroupRef, from: Int, to: Int): Settings =
        withWidgets(s, ref, reorder(widgetsOf(s, ref), from, to))

    /** Sposta di [delta] posti (−1 = su, +1 = giù) il widget [widgetId]. */
    fun moveBy(s: Settings, ref: WidgetGroupRef, widgetId: Int, delta: Int): Settings {
        val list = widgetsOf(s, ref)
        val i = list.indexOf(widgetId)
        return if (i < 0) s else withWidgets(s, ref, reorder(list, i, i + delta))
    }

    /**
     * Posizione di arrivo trascinando una riga alta [rowPx] per [dragPx] pixel (positivo = verso il basso):
     * ogni riga attraversata cambia posizione; resta nei limiti dell'elenco di [size] voci.
     */
    fun targetIndex(from: Int, dragPx: Float, rowPx: Float, size: Int): Int {
        if (rowPx <= 0f || size <= 0) return from
        val steps = Math.round(dragPx / rowPx)
        return (from + steps).coerceIn(0, size - 1)
    }

    /** Toglie [widgetId] dal gruppo, senza cancellarlo dal telefono (un gruppo con un solo widget torna un widget singolo). */
    private fun detach(s: Settings, ref: WidgetGroupRef, widgetId: Int): Settings = when (ref) {
        is WidgetGroupRef.Stack -> WidgetStacks.remove(s, widgetId)
        is WidgetGroupRef.Folder -> withWidgets(s, ref, widgetsOf(s, ref) - widgetId)
    }

    /** I gruppi dove si può spostare un widget di [from]: gli altri caroselli della home e tutte le cartelle. */
    fun targets(s: Settings, from: WidgetGroupRef): List<WidgetGroupRef> =
        (s.widgetStacks.map { WidgetGroupRef.Stack(it.id) as WidgetGroupRef } + s.folders.keys.map { WidgetGroupRef.Folder(it) }).filter { it != from }

    /** Sposta [widgetId] da [from] a [to] (in fondo al gruppo di arrivo). Un gruppo che non esiste o uguale non cambia nulla. */
    fun moveToGroup(s: Settings, from: WidgetGroupRef, to: WidgetGroupRef, widgetId: Int): Settings {
        if (from == to || widgetId !in widgetsOf(s, from)) return s
        if (widgetsOf(s, to).let { widgetId in it } || !exists(s, to)) return s
        val detached = detach(s, from, widgetId)
        return withWidgets(detached, to, widgetsOf(detached, to) + widgetId)
    }

    private fun exists(s: Settings, ref: WidgetGroupRef): Boolean = when (ref) {
        is WidgetGroupRef.Stack -> s.widgetStacks.any { it.id == ref.id }
        is WidgetGroupRef.Folder -> ref.id in s.folders
    }
}
