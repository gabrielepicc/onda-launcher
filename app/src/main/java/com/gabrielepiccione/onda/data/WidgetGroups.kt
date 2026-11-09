// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/**
 * Gruppi di widget in home (carosello o righe): raggruppare, separare, spostare un widget fuori dal gruppo.
 * Un gruppo occupa un solo blocco della home, quindi nel riordino si sposta come un'unica voce.
 * Logica pura, testabile su JVM; le pile salvate prima restano caroselli.
 */
object WidgetGroups {
    fun setLayout(s: Settings, stackId: String, layout: StackLayout): Settings =
        s.copy(widgetStacks = s.widgetStacks.map { if (it.id == stackId) it.copy(layout = layout) else it })

    /** I widget di [block] (un widget singolo o un gruppo), nell'ordine. */
    fun widgetsOf(s: Settings, block: String): List<Int> =
        HomeBlocks.widgetId(block)?.let { listOf(it) }
            ?: s.widgetStacks.firstOrNull { it.id == HomeBlocks.stackId(block) }?.widgets.orEmpty()

    /** Il blocco di widget subito dopo [block] nella home, se c'è. */
    fun nextWidgetBlock(s: Settings, block: String): String? {
        val blocks = HomeBlocks.resolve(s)
        val next = blocks.getOrNull(blocks.indexOf(block) + 1) ?: return null
        return next.takeIf { HomeBlocks.widgetId(it) != null || HomeBlocks.stackId(it) != null }
    }

    /**
     * Unisce [block] con il blocco di widget che lo segue: il risultato è un gruppo (carosello se [block] è un
     * widget singolo, altrimenti con la disposizione del primo gruppo) al posto di [block].
     */
    fun mergeWithNext(s: Settings, block: String, newStackId: String): Settings {
        val next = nextWidgetBlock(s, block) ?: return s
        val first = widgetsOf(s, block)
        val second = widgetsOf(s, next)
        if (first.isEmpty() || second.isEmpty()) return s
        val existing = s.widgetStacks.firstOrNull { it.id == HomeBlocks.stackId(block) }
        val merged = (existing ?: WidgetStack(newStackId, heightDp = HomeBlocks.widgetId(block)?.let { s.widgetHeights[it] }))
            .copy(widgets = first + second)
        val gone = setOfNotNull(HomeBlocks.stackId(block), HomeBlocks.stackId(next))
        return s.copy(
            widgetIds = s.widgetIds - first.toSet() - second.toSet(),
            widgetStacks = s.widgetStacks.filterNot { it.id in gone } + merged,
            homeLayout = HomeBlocks.resolve(s)
                .filterNot { it == next }
                .map { if (it == block) HomeBlocks.stack(merged.id) else it },
        )
    }

    /** Separa il gruppo: ogni widget torna un blocco a sé, nello stesso punto e nello stesso ordine. */
    fun split(s: Settings, stackId: String): Settings {
        val stack = s.widgetStacks.firstOrNull { it.id == stackId } ?: return s
        val block = HomeBlocks.stack(stackId)
        val layout = HomeBlocks.resolve(s).flatMap { if (it == block) stack.widgets.map(HomeBlocks::widget) else listOf(it) }
        return s.copy(
            widgetStacks = s.widgetStacks.filterNot { it.id == stackId },
            widgetIds = s.widgetIds + stack.widgets.filterNot { it in s.widgetIds },
            widgetHeights = stack.heightDp?.let { h -> s.widgetHeights + stack.widgets.associateWith { h } } ?: s.widgetHeights,
            homeLayout = layout,
        )
    }

    /** Porta un widget fuori dal suo gruppo, subito dopo il gruppo (o al suo posto se ne restava solo uno). */
    fun detach(s: Settings, widgetId: Int): Settings {
        val stack = s.widgetStacks.firstOrNull { widgetId in it.widgets } ?: return s
        if (stack.widgets.size <= 2) return split(s, stack.id)
        val block = HomeBlocks.stack(stack.id)
        val layout = HomeBlocks.resolve(s).flatMap { if (it == block) listOf(it, HomeBlocks.widget(widgetId)) else listOf(it) }
        return s.copy(
            widgetStacks = s.widgetStacks.map { if (it.id == stack.id) it.copy(widgets = it.widgets - widgetId) else it },
            widgetIds = s.widgetIds + widgetId,
            homeLayout = layout,
        )
    }

    /** Sposta un widget di un posto all'interno del suo gruppo. */
    fun moveInside(s: Settings, widgetId: Int, delta: Int): Settings = s.copy(
        widgetStacks = s.widgetStacks.map { st ->
            val i = st.widgets.indexOf(widgetId)
            val j = i + delta
            if (i < 0 || j !in st.widgets.indices) st
            else st.copy(widgets = st.widgets.toMutableList().apply { add(j, removeAt(i)) })
        },
    )
}
