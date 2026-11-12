// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/**
 * Testo e forme dell'ora per gli stili dell'orologio, senza dipendenze Android (testabile su JVM):
 * cifre con o senza secondi, numeri romani, segmenti dei sette segmenti e matrici a punti 5×7.
 */
object ClockText {
    /** «14:07» o, con i secondi, «14:07:09». */
    fun digits(hour: Int, minute: Int, second: Int, seconds: Boolean): String {
        val base = "%02d:%02d".format(hour.mod(24), minute.mod(60))
        return if (seconds) base + ":%02d".format(second.mod(60)) else base
    }

    private val ROMAN = listOf(1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC", 50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")

    /** Numero romano (1–3999); lo zero, che i romani non scrivevano, diventa «—». */
    fun roman(n: Int): String {
        if (n <= 0 || n > 3999) return "—"
        var rest = n
        val sb = StringBuilder()
        for ((value, symbol) in ROMAN) while (rest >= value) { sb.append(symbol); rest -= value }
        return sb.toString()
    }

    /** «XIV : VII»: ore e minuti in numeri romani. */
    fun romanTime(hour: Int, minute: Int): String = roman(hour.mod(24)) + " : " + roman(minute.mod(60))

    /** I sette segmenti nell'ordine a, b, c, d, e, f, g (alto, alto-dx, basso-dx, basso, basso-sx, alto-sx, centro). */
    private val SEGMENTS = mapOf(
        '0' to "1111110", '1' to "0110000", '2' to "1101101", '3' to "1111001", '4' to "0110011",
        '5' to "1011011", '6' to "1011111", '7' to "1110000", '8' to "1111111", '9' to "1111011",
    )

    /** Segmenti accesi per la cifra [digit]; tutti spenti per un carattere che non è una cifra. */
    fun segments(digit: Char): List<Boolean> = (SEGMENTS[digit] ?: "0000000").map { it == '1' }

    const val DOT_COLUMNS = 5
    const val DOT_ROWS = 7

    private val GLYPHS = mapOf(
        '0' to listOf("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
        '1' to listOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
        '2' to listOf("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
        '3' to listOf("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
        '4' to listOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
        '5' to listOf("11111", "10000", "11110", "00001", "00001", "10001", "01110"),
        '6' to listOf("00110", "01000", "10000", "11110", "10001", "10001", "01110"),
        '7' to listOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
        '8' to listOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
        '9' to listOf("01110", "10001", "10001", "01111", "00001", "00010", "01100"),
        ':' to listOf("00000", "00100", "00100", "00000", "00100", "00100", "00000"),
    )

    /** Le 7 righe di 5 punti (1 = acceso) di una cifra o dei due punti. */
    fun dotGlyph(c: Char): List<String> = GLYPHS[c] ?: List(DOT_ROWS) { "0".repeat(DOT_COLUMNS) }
}
