// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda

import com.gabrielepiccione.onda.data.PlayerLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Titolo e artista del brano hanno le misure di [PlayerLayout] in ogni variante del player. */
class PlayerTextTest {
    private val ui = File("src/main/java/com/gabrielepiccione/onda/ui")
    private val sources = ui.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    @Test
    fun sizes() {
        assertEquals(19f, PlayerLayout.TITLE_SP)
        assertEquals(15f, PlayerLayout.ARTIST_SP)
    }

    @Test
    fun everyPlayerTextUsesTheConstants() {
        val problems = mutableListOf<String>()
        var uses = 0
        sources.forEach { file ->
            val lines = file.readLines()
            lines.forEachIndexed { i, line ->
                val t = line.trim()
                if (t == "media.title," || t == "media.artist,") {
                    // Lo stile sta nelle righe subito dopo il testo
                    val style = lines.drop(i + 1).take(3).firstOrNull { it.contains("style =") }.orEmpty()
                    val wanted = if (t.startsWith("media.title")) "PlayerLayout.TITLE_SP" else "PlayerLayout.ARTIST_SP"
                    if (!style.contains(wanted) || style.contains("titleLarge") || style.contains("bodyLarge")) {
                        problems += "${file.name}:${i + 1}"
                    } else {
                        uses++
                    }
                }
            }
        }
        assertTrue("misure del player non uniformi: $problems", problems.isEmpty())
        assertTrue("nessuna variante del player trovata", uses >= 4)
    }
}
