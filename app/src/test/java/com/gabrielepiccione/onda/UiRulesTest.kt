// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Controllo statico delle regole di layout (CLAUDE.md, "Proporzioni"): legge i sorgenti in ui/ senza
 * disegnare nulla, in pochi millisecondi.
 * - Nelle impostazioni, nei chip e nei pulsanti nessun testo si tronca: niente `maxLines = 1` né `TextOverflow.Ellipsis`.
 * - Un contenitore con del testo non ha un'altezza fissa: si usa `heightIn(min = ...)`.
 * Un caso voluto (righe delle app, titoli dei brani, anteprime) si segna con il commento `troncamento voluto`
 * o `altezza fissa voluta` sulla stessa riga o su quella sopra.
 */
class UiRulesTest {
    private val ui = File("src/main/java/com/gabrielepiccione/onda/ui")
    private val sources = ui.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    private val truncation = Regex("""maxLines\s*=\s*1\b|TextOverflow\.Ellipsis""")
    private val fixedHeight = Regex("""\.height\(\s*[0-9.]+\.dp\s*\)""")
    private val control = Regex("""Chip\(|Button\(|Chip\b|Button\b""")

    private fun allowed(lines: List<String>, i: Int, marker: String) =
        lines[i].contains(marker) || (i > 0 && lines[i - 1].contains(marker))

    @Test
    fun sourcesAreFound() {
        assertTrue("cartella ui non trovata: ${ui.absolutePath}", sources.size > 10)
    }

    @Test
    fun noTruncationInSettingsChipsAndButtons() {
        val problems = sources.flatMap { file ->
            val lines = file.readLines()
            val settings = file.path.replace('\\', '/').contains("/ui/settings/")
            lines.indices.filter { i ->
                truncation.containsMatchIn(lines[i]) && !allowed(lines, i, "troncamento voluto") &&
                    (settings || (maxOf(0, i - 8)..i).any { control.containsMatchIn(lines[it]) })
            }.map { "${file.name}:${it + 1}: ${lines[it].trim()}" }
        }
        assertTrue("Testi troncati (vedi CLAUDE.md, Proporzioni):\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun noFixedHeightOnContainersWithText() {
        val problems = sources.flatMap { file ->
            val lines = file.readLines()
            lines.indices.filter { i ->
                fixedHeight.containsMatchIn(lines[i]) && !allowed(lines, i, "altezza fissa voluta") &&
                    // Il contenitore ha del testo se nelle righe subito dopo compare un Text(
                    (i + 1..minOf(lines.lastIndex, i + 6)).any { lines[it].contains("Text(") }
            }.map { "${file.name}:${it + 1}: ${lines[it].trim()}" }
        }
        assertTrue("Altezze fisse su contenitori con testo (usa heightIn(min = …)):\n" + problems.joinToString("\n"), problems.isEmpty())
    }
}
