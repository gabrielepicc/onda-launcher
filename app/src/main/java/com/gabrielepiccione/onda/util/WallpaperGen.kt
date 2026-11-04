// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/** Tipi di sfondo generato proceduralmente (nessuna immagine esterna, nessun problema di licenza). */
enum class WallpaperKind { MESH, WAVES, DUNES, BLOBS, GRADIENT, SOLID }

/**
 * Descrizione di uno sfondo: tipo, colori (ARGB) e seme per la casualità.
 * Lo stesso spec produce sempre lo stesso sfondo.
 */
data class WallpaperSpec(
    val kind: WallpaperKind,
    val colors: List<Long>,
    val seed: Int = 7,
    /** Aggiunge una leggera grana "carta". */
    val grain: Boolean = false,
)

object WallpaperGen {

    fun render(spec: WallpaperSpec, width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(max(1, width), max(1, height), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val c = spec.colors.map { it.toInt() }.ifEmpty { listOf(0xFF101418.toInt()) }
        val rnd = Random(spec.seed)
        val w = width.toFloat()
        val h = height.toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (spec.kind) {
            WallpaperKind.SOLID -> canvas.drawColor(c[0])

            WallpaperKind.GRADIENT -> {
                paint.shader = LinearGradient(0f, 0f, w * 0.3f, h, c.toIntArray().takeIf { it.size > 1 } ?: intArrayOf(c[0], c[0]), null, Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, w, h, paint)
            }

            WallpaperKind.MESH -> {
                // Base + macchie radiali morbide che si fondono: effetto "mesh gradient"
                canvas.drawColor(c[0])
                for (i in 1 until c.size) {
                    repeat(2) {
                        val cx = w * rnd.nextFloat()
                        val cy = h * rnd.nextFloat()
                        val r = max(w, h) * (0.45f + 0.35f * rnd.nextFloat())
                        paint.shader = RadialGradient(cx, cy, r, intArrayOf(c[i], c[i] and 0x00FFFFFF), null, Shader.TileMode.CLAMP)
                        canvas.drawRect(0f, 0f, w, h, paint)
                    }
                }
            }

            WallpaperKind.WAVES -> {
                canvas.drawColor(c[0])
                val layers = (c.size - 1).coerceAtLeast(1)
                for (i in 0 until layers) {
                    val color = c[(i + 1).coerceAtMost(c.lastIndex)]
                    val baseY = h * (0.45f + 0.5f * (i + 1) / (layers + 1))
                    val amp = h * (0.025f + 0.03f * rnd.nextFloat())
                    val freq = (1.2f + rnd.nextFloat() * 1.3f) * 2 * PI.toFloat() / w
                    val phase = rnd.nextFloat() * 2 * PI.toFloat()
                    val path = Path().apply {
                        moveTo(0f, h)
                        var x = 0f
                        while (x <= w) {
                            lineTo(x, baseY + amp * sin(freq * x + phase))
                            x += w / 120f
                        }
                        lineTo(w, h); close()
                    }
                    paint.shader = LinearGradient(0f, baseY - amp, 0f, h, color, darker(color, 0.75f), Shader.TileMode.CLAMP)
                    canvas.drawPath(path, paint)
                }
            }

            WallpaperKind.DUNES -> {
                paint.shader = LinearGradient(0f, 0f, 0f, h, c[0], c.getOrElse(1) { c[0] }, Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, w, h, paint)
                val layers = (c.size - 2).coerceAtLeast(1)
                for (i in 0 until layers) {
                    val color = c[(i + 2).coerceAtMost(c.lastIndex)]
                    val y0 = h * (0.5f + 0.45f * (i + 1) / (layers + 1))
                    val path = Path().apply {
                        moveTo(0f, y0 + h * 0.05f * rnd.nextFloat())
                        cubicTo(
                            w * 0.3f, y0 - h * (0.06f + 0.08f * rnd.nextFloat()),
                            w * 0.65f, y0 + h * (0.06f * rnd.nextFloat()),
                            w, y0 - h * 0.05f * rnd.nextFloat(),
                        )
                        lineTo(w, h); lineTo(0f, h); close()
                    }
                    paint.shader = LinearGradient(0f, y0 - h * 0.1f, 0f, h, color, darker(color, 0.8f), Shader.TileMode.CLAMP)
                    canvas.drawPath(path, paint)
                }
            }

            WallpaperKind.BLOBS -> {
                canvas.drawColor(c[0])
                for (i in 1 until c.size) {
                    val cx = w * (0.15f + 0.7f * rnd.nextFloat())
                    val cy = h * (0.15f + 0.7f * rnd.nextFloat())
                    val r = w * (0.35f + 0.25f * rnd.nextFloat())
                    paint.shader = RadialGradient(
                        cx, cy, r,
                        intArrayOf(c[i], c[i], c[i] and 0x00FFFFFF),
                        floatArrayOf(0f, 0.55f, 1f),
                        Shader.TileMode.CLAMP,
                    )
                    canvas.drawCircle(cx, cy, r, paint)
                }
            }
        }

        if (spec.grain) addGrain(bmp, spec.seed)
        return bmp
    }

    private fun darker(color: Int, f: Float): Int {
        val a = color ushr 24
        val r = ((color shr 16 and 0xFF) * f).toInt()
        val g = ((color shr 8 and 0xFF) * f).toInt()
        val b = ((color and 0xFF) * f).toInt()
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    /** Grana leggera: rumore monocromatico a bassa intensità. */
    private fun addGrain(bmp: Bitmap, seed: Int) {
        val rnd = Random(seed * 31 + 1)
        val w = bmp.width
        val row = IntArray(w)
        for (y in 0 until bmp.height) {
            bmp.getPixels(row, 0, w, 0, y, w, 1)
            for (x in 0 until w) {
                val n = rnd.nextInt(-10, 11)
                val p = row[x]
                val r = ((p shr 16 and 0xFF) + n).coerceIn(0, 255)
                val g = ((p shr 8 and 0xFF) + n).coerceIn(0, 255)
                val b = ((p and 0xFF) + n).coerceIn(0, 255)
                row[x] = (p and -0x1000000) or (r shl 16) or (g shl 8) or b
            }
            bmp.setPixels(row, 0, w, 0, y, w, 1)
        }
    }
}
