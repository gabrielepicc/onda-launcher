// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.core.icons

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Forme applicabili alle icone adattive. */
enum class IconShape { SYSTEM, CIRCLE, SQUIRCLE, ROUNDED_SQUARE, TEARDROP, CYLINDER, PETAL }

/** Stile di resa: icona originale oppure generata. */
enum class IconStyle { ORIGINAL, DOTS }

/**
 * Trasformazioni grafiche delle icone: forme, fallback degli icon pack
 * (iconback/iconmask/iconupon/scale) e stile generato "Puntini".
 */
object IconRenderer {

    /** Path della forma in un quadrato [size]×[size]. */
    fun shapePath(shape: IconShape, size: Float): Path {
        val p = Path()
        val r = size / 2f
        when (shape) {
            IconShape.SYSTEM, IconShape.CIRCLE -> p.addCircle(r, r, r, Path.Direction.CW)
            IconShape.ROUNDED_SQUARE -> p.addRoundRect(RectF(0f, 0f, size, size), size * 0.22f, size * 0.22f, Path.Direction.CW)
            IconShape.SQUIRCLE -> {
                // Superellisse |x|^n + |y|^n = 1 con n = 4
                val n = 4.0
                val steps = 96
                for (i in 0..steps) {
                    val t = 2 * Math.PI * i / steps
                    val c = Math.cos(t)
                    val s = Math.sin(t)
                    val x = r + r * Math.signum(c) * abs(c).pow(2 / n)
                    val y = r + r * Math.signum(s) * abs(s).pow(2 / n)
                    if (i == 0) p.moveTo(x.toFloat(), y.toFloat()) else p.lineTo(x.toFloat(), y.toFloat())
                }
                p.close()
            }
            IconShape.TEARDROP -> {
                // Tre angoli tondi e uno (in basso a destra) quasi vivo
                val big = size * 0.5f
                val small = size * 0.14f
                p.addRoundRect(
                    RectF(0f, 0f, size, size),
                    floatArrayOf(big, big, big, big, small, small, big, big),
                    Path.Direction.CW,
                )
            }
            IconShape.CYLINDER -> {
                val ry = size * 0.16f
                p.addRoundRect(RectF(0f, 0f, size, size), floatArrayOf(r, ry, r, ry, r, ry, r, ry), Path.Direction.CW)
            }
            IconShape.PETAL -> {
                val big = size * 0.5f
                val small = size * 0.12f
                p.addRoundRect(
                    RectF(0f, 0f, size, size),
                    floatArrayOf(big, big, small, small, big, big, small, small),
                    Path.Direction.CW,
                )
            }
        }
        return p
    }

    /**
     * Disegna un'icona nella forma richiesta. Le icone adattive usano i loro livelli;
     * quelle "vecchie" vengono rimpicciolite su uno sfondo del loro colore dominante.
     */
    fun render(drawable: Drawable, shape: IconShape, sizePx: Int): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        if (shape == IconShape.SYSTEM) {
            drawable.drawInto(canvas, Rect(0, 0, sizePx, sizePx))
            return bmp
        }
        val path = shapePath(shape, sizePx.toFloat())
        canvas.save()
        canvas.clipPath(path)
        if (drawable is AdaptiveIconDrawable) {
            // I livelli adattivi sono il 150% dell'area visibile
            val inset = sizePx / 4
            val bounds = Rect(-inset, -inset, sizePx + inset, sizePx + inset)
            drawable.background?.drawInto(canvas, bounds)
            drawable.foreground?.drawInto(canvas, bounds)
        } else {
            val src = drawable.toBitmapSafe(sizePx)
            canvas.drawColor(lighten(dominantColor(src), 0.85f))
            val pad = (sizePx * 0.18f).toInt()
            canvas.drawBitmap(src, null, Rect(pad, pad, sizePx - pad, sizePx - pad), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        canvas.restore()
        return bmp
    }

    /**
     * Fallback di un icon pack per le app che non hanno un'icona dedicata:
     * icona originale scalata su [back], ritagliata da [mask] e coperta da [upon].
     */
    fun packFallback(original: Drawable, back: Drawable?, mask: Drawable?, upon: Drawable?, scale: Float, sizePx: Int): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val full = Rect(0, 0, sizePx, sizePx)
        back?.drawInto(canvas, full)

        // Icona originale su un livello separato, così la maschera tocca solo lei
        val layer = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val lc = Canvas(layer)
        val s = (sizePx * scale.coerceIn(0.3f, 1f)).toInt()
        val off = (sizePx - s) / 2
        original.drawInto(lc, Rect(off, off, off + s, off + s))
        if (mask != null) {
            val m = mask.toBitmapSafe(sizePx)
            val p = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) }
            lc.drawBitmap(m, null, full, p)
        }
        canvas.drawBitmap(layer, 0f, 0f, null)
        upon?.drawInto(canvas, full)
        return bmp
    }

    /** Stile "Puntini": un cerchio pieno nel colore più vivace dell'icona originale. */
    fun dot(original: Drawable, sizePx: Int): Bitmap {
        val color = dominantColor(original.toBitmapSafe(48))
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val r = sizePx * 0.32f
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        return bmp
    }

    /** Colore "rappresentativo": media pesata sulla saturazione dei pixel opachi. */
    fun dominantColor(bmp: Bitmap): Int {
        val small = if (bmp.width > 48) Bitmap.createScaledBitmap(bmp, 48, 48, true) else bmp
        val px = IntArray(small.width * small.height)
        small.getPixels(px, 0, small.width, 0, 0, small.width, small.height)
        var r = 0.0; var g = 0.0; var b = 0.0; var wSum = 0.0
        val hsv = FloatArray(3)
        for (c in px) {
            if (Color.alpha(c) < 200) continue
            Color.colorToHSV(c, hsv)
            // I pixel quasi bianchi/neri contano poco, quelli saturi molto
            val w = 0.05 + hsv[1] * hsv[1] * (if (hsv[2] < 0.15f) 0.1f else 1f)
            r += Color.red(c) * w; g += Color.green(c) * w; b += Color.blue(c) * w; wSum += w
        }
        if (wSum == 0.0) return Color.GRAY
        return Color.rgb((r / wSum).toInt(), (g / wSum).toInt(), (b / wSum).toInt())
    }

    private fun lighten(color: Int, amount: Float): Int {
        val r = Color.red(color) + ((255 - Color.red(color)) * amount).toInt()
        val g = Color.green(color) + ((255 - Color.green(color)) * amount).toInt()
        val b = Color.blue(color) + ((255 - Color.blue(color)) * amount).toInt()
        return Color.rgb(min(255, r), min(255, g), min(255, b))
    }

    private fun Drawable.drawInto(canvas: Canvas, bounds: Rect) {
        val old = Rect(this.bounds)
        setBounds(bounds)
        draw(canvas)
        setBounds(old)
    }

    private fun Drawable.toBitmapSafe(size: Int): Bitmap {
        val s = max(1, size)
        val b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        drawInto(Canvas(b), Rect(0, 0, s, s))
        return b
    }
}
