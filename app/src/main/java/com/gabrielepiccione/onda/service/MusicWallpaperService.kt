// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.service

import android.content.res.Configuration
import android.graphics.Canvas
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
        private var alpha = WaveBlobs.IDLE_ALPHA
        private var time = 0f

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            app.media.onEach { media ->
                playing = media?.playing == true
                coverColors(media?.art).takeIf { it.isNotEmpty() }?.let { cover = it }
                restart()
            }.launchIn(scope)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            super.onSurfaceChanged(holder, format, w, h)
            width = w
            height = h
            restart()
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
                    val target = if (s.musicBackdrop == MusicBackdropMode.OFF) 0f else WaveBlobs.targetAlpha(playing, s.musicBackdropIntensity)
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
            val colors = WaveBlobs.readableColors(cover, dark)
            canvas.drawColor(if (dark) BASE_DARK else BASE_LIGHT)
            val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
            val w = width.toFloat()
            val h = height.toFloat()
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

    private companion object {
        const val FRAME_MS = 33L
        const val FADE_MS = 800f
        const val SETTLED = 0.01f
        const val BLOB_ALPHA = 0.8f
        const val BASE_DARK = 0xFF101215.toInt()
        const val BASE_LIGHT = 0xFFF4F5F7.toInt()
    }
}
