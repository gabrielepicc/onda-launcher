// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.service

import android.content.res.Configuration
import android.graphics.Canvas
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import com.gabrielepiccione.onda.data.ThemePalette
import com.gabrielepiccione.onda.data.MusicWallpaper
import android.os.Build
import android.graphics.RectF
import android.graphics.Rect
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.gabrielepiccione.onda.OndaApp
import com.gabrielepiccione.onda.data.MusicBackdropMode
import com.gabrielepiccione.onda.data.MusicMood
import com.gabrielepiccione.onda.data.WaveBlobs
import com.gabrielepiccione.onda.ui.components.coverColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

/**
 * Sfondo animato di sistema (anche nella schermata di blocco) con i colori della copertina e il carattere del brano:
 * lo stesso disegno della home ([WaveBlobs]). Solo colori e movimento: nessun microfono, nessun permesso.
 * Batteria: 30 fotogrammi al secondo solo mentre suona la musica e lo sfondo si vede; altrimenti un solo disegno fermo.
 */
class MusicWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = MusicEngine()

    private inner class MusicEngine : Engine() {
        private val app = application as OndaApp
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        private val power = getSystemService(PowerManager::class.java)
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var job: Job? = null
        private var visible = false
        private var width = 0
        private var height = 0

        private var cover: List<Int> = emptyList()
        private var playing = false
        // Opacità dei colori della musica sopra l'immagine di base: entra ed esce con una dissolvenza
        private var alpha = 0f
        private var time = 0f
        private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private var basePath: String? = null
        private var base: Bitmap? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            app.media.media.onEach { media ->
                playing = media?.playing == true
                coverColors(media?.art).takeIf { it.isNotEmpty() }?.let { cover = it }
                restart()
            }.launchIn(scope)
            // L'immagine di base cambia dalle impostazioni: si ricarica e si ridisegna
            app.settings.settings.map { it.musicWallpaperBase }.distinctUntilChanged().onEach { path ->
                loadBase(path)
                restart()
            }.launchIn(scope)
        }

        private suspend fun loadBase(path: String) {
            basePath = path
            // Prima che la superficie abbia una misura vale quella dello schermo
            val w = if (width > 0) width else resources.displayMetrics.widthPixels
            val h = if (height > 0) height else resources.displayMetrics.heightPixels
            base = if (path.isBlank()) null else withContext(Dispatchers.IO) {
                runCatching {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(path, bounds)
                    val options = BitmapFactory.Options().apply { inSampleSize = MusicWallpaper.sampleSize(bounds.outWidth, bounds.outHeight, w, h) }
                    BitmapFactory.decodeFile(path, options)
                }.getOrNull()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            super.onSurfaceChanged(holder, format, w, h)
            val grew = w > width || h > height
            width = w
            height = h
            // Schermo più grande di quello per cui era stata ridotta l'immagine: la si ricarica nitida
            if (grew && basePath != null) scope.launch { loadBase(basePath.orEmpty()); restart() } else restart()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            restart()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            job?.cancel()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            scope.cancel()
            super.onDestroy()
        }

        /** Riparte il ciclo di disegno: gira solo se serve, poi si ferma su un fotogramma. */
        private fun restart() {
            job?.cancel()
            if (!visible || width == 0) return
            job = scope.launch {
                var last = 0L
                while (isActive) {
                    val s = app.settings.current
                    val now = SystemClock.elapsedRealtime()
                    val dt = if (last == 0L) 0L else now - last
                    last = now
                    val animations = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
                    val moving = playing && animations && power?.isPowerSaveMode != true &&
                        (s.musicBackdrop == MusicBackdropMode.MOTION || s.musicBackdrop == MusicBackdropMode.BEAT)
                    if (moving) time += dt / 1000f * MusicMood.from(cover).speed
                    val target = MusicWallpaper.plan(base != null, playing, s.musicBackdrop, s.musicBackdropIntensity).musicAlpha
                    alpha += (target - alpha) * min(1f, dt / FADE_MS)
                    draw()
                    if (!moving && abs(target - alpha) < SETTLED) break
                    delay(FRAME_MS)
                }
            }
        }

        private fun draw() {
            val holder = surfaceHolder
            val canvas = runCatching { holder.lockCanvas() }.getOrNull() ?: return
            try {
                paint(canvas)
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }

        private fun paint(canvas: Canvas) {
            val dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val w = width.toFloat()
            val h = height.toFloat()
            // Sotto: l'immagine scelta dall'utente (ritagliata come lo sfondo normale) o, se manca, i colori del tema
            val image = base
            if (image != null) {
                val crop = MusicWallpaper.centerCrop(image.width, image.height, width, height)
                canvas.drawBitmap(image, Rect(crop[0], crop[1], crop[2], crop[3]), RectF(0f, 0f, w, h), bitmapPaint)
            } else {
                val (top, bottom) = themeGradient(dark)
                paint.shader = LinearGradient(0f, 0f, 0f, h, top, bottom, Shader.TileMode.CLAMP)
                paint.alpha = 255
                canvas.drawRect(0f, 0f, w, h, paint)
            }
            // Sopra: i colori dell'album, solo con la musica, in dissolvenza
            val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
            if (a == 0) {
                paint.shader = null
                return
            }
            val colors = WaveBlobs.readableColors(cover, dark)
            paint.shader = LinearGradient(0f, 0f, 0f, h, colors[0], colors[3], Shader.TileMode.CLAMP)
            paint.alpha = a
            canvas.drawRect(0f, 0f, w, h, paint)
            for (i in 0 until WaveBlobs.COUNT) {
                val blob = WaveBlobs.blob(i, time)
                val cx = w * blob.cx
                val cy = h * blob.cy
                val r = maxOf(w, h) * blob.radius
                val c = colors[i + 1]
                paint.shader = RadialGradient(cx, cy, r, c, c and 0x00FFFFFF, Shader.TileMode.CLAMP)
                paint.alpha = (a * BLOB_ALPHA).toInt()
                canvas.drawCircle(cx, cy, r, paint)
            }
            paint.shader = null
        }
    }

    /** Gradiente dai colori Material You (di sistema da Android 12, altrimenti quelli scelti in Onda). */
    private fun themeGradient(dark: Boolean): Pair<Int, Int> {
        val s = (application as OndaApp).settings.current
        val (primary, accent) = ThemePalette.resolveSeeds(
            staticAccent = s.accent,
            dynamic = s.dynamicColor,
            chosenAccent = s.themeAccent,
            systemPrimary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) getColor(android.R.color.system_accent1_500) else null,
            systemAccent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) getColor(android.R.color.system_accent3_500) else null,
        )
        return MusicWallpaper.fallbackColors(primary, accent, dark)
    }

    private companion object {
        const val FRAME_MS = 33L
        const val FADE_MS = 800f
        const val SETTLED = 0.01f
        const val BLOB_ALPHA = 0.8f
    }
}
