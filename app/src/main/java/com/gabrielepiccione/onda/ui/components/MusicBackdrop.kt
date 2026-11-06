// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.components

import android.media.audiofx.Visualizer
import androidx.compose.runtime.DisposableEffect
import com.gabrielepiccione.onda.data.BeatTracker
import android.graphics.RuntimeShader
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.palette.graphics.Palette
import com.gabrielepiccione.onda.data.MoodParams
import com.gabrielepiccione.onda.data.MusicBackdropMode
import com.gabrielepiccione.onda.data.MusicMood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

/** Colori estratti da una copertina con androidx.palette, dal più rappresentativo. */
suspend fun coverColors(art: ImageBitmap?): List<Int> = withContext(Dispatchers.Default) {
    if (art == null) return@withContext emptyList()
    runCatching {
        val p = Palette.from(art.asAndroidBitmap()).maximumColorCount(16).generate()
        listOfNotNull(p.vibrantSwatch, p.darkVibrantSwatch, p.mutedSwatch, p.lightVibrantSwatch, p.dominantSwatch, p.darkMutedSwatch)
            .map { it.rgb }
            .distinct()
            .take(5)
    }.getOrDefault(emptyList())
}

/**
 * Strato animato dietro la home mentre suona la musica: onde morbide con i colori della copertina, che si
 * muovono secondo il "carattere" del brano ([MusicMood]). Disegno originale di Onda.
 * - Massimo 30 fotogrammi al secondo; si ferma con il launcher non visibile, con il risparmio
 *   energetico e con "Rimuovi animazioni" (allora i colori restano fermi).
 * - Android 13+: shader AGSL; versioni precedenti: sfumature radiali che si spostano.
 * - Quando la musica si ferma, rallenta e poi svanisce.
 */
@Composable
fun MusicBackdrop(
    art: ImageBitmap?,
    playing: Boolean,
    mode: MusicBackdropMode,
    intensity: Float,
    dark: Boolean,
    modifier: Modifier = Modifier,
    /** Permesso per l'analizzatore audio: senza, "a ritmo" si comporta come "colori e movimento". */
    beatAllowed: Boolean = false,
) {
    if (mode == MusicBackdropMode.OFF) return
    val context = LocalContext.current
    var palette by remember { mutableStateOf<List<Int>>(emptyList()) }
    LaunchedEffect(art) { coverColors(art).takeIf { it.isNotEmpty() }?.let { palette = it } }
    val mood = remember(palette) { MusicMood.from(palette) }
    val safe = remember(palette, dark) {
        val base = palette.ifEmpty { listOf(0xFF3D5AFE.toInt(), 0xFF00BFA5.toInt(), 0xFF7C4DFF.toInt()) }
        List(4) { i -> MusicMood.readable(base[i % base.size], dark) }
    }
    // Passaggio morbido di circa 1,5 secondi tra i colori di un brano e quelli del successivo
    val c0 by animateColorAsState(Color(safe[0]), tween(1500), label = "mb0")
    val c1 by animateColorAsState(Color(safe[1]), tween(1500), label = "mb1")
    val c2 by animateColorAsState(Color(safe[2]), tween(1500), label = "mb2")
    val c3 by animateColorAsState(Color(safe[3]), tween(1500), label = "mb3")
    val alpha by animateFloatAsState(
        if (playing) intensity.coerceIn(0.1f, 1f) else 0f,
        tween(if (playing) 800 else (MusicMood.SLOWDOWN_MS + MusicMood.FADE_MS).toInt(), delayMillis = if (playing) 0 else MusicMood.SLOWDOWN_MS.toInt() / 2),
        label = "mbAlpha",
    )
    if (alpha <= 0.01f && !playing) return

    val lifecycle = LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val visible = lifecycle.value.isAtLeast(Lifecycle.State.RESUMED)
    val power = remember { context.getSystemService(PowerManager::class.java) }
    val noAnimations = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val moving = mode != MusicBackdropMode.COLORS && !noAnimations
    val speed by animateFloatAsState(if (playing && moving) mood.speed else 0f, tween(MusicMood.SLOWDOWN_MS.toInt()), label = "mbSpeed")
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(visible, moving) {
        if (!visible || !moving) return@LaunchedEffect
        var last = 0L
        var saver = power?.isPowerSaveMode == true
        var checked = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (now - checked > 5_000_000_000L) {
                    saver = power?.isPowerSaveMode == true
                    checked = now
                }
                if (!saver && now - last >= FRAME_NANOS) {
                    if (last != 0L) time += (now - last) / 1_000_000_000f * speed
                    last = now
                }
            }
            if (saver) delay(1_000)
        }
    }

    // A ritmo di musica: pulsazione sui bassi, letta solo mentre suona e si vede
    var beat by remember { mutableFloatStateOf(0f) }
    val listening = mode == MusicBackdropMode.BEAT && beatAllowed && playing && visible && moving
    DisposableEffect(listening) {
        val visualizer = if (listening) startBeat { beat = it } else null
        onDispose {
            runCatching { visualizer?.enabled = false; visualizer?.release() }
            beat = 0f
        }
    }

    val shader = remember { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) runCatching { waveShader() }.getOrNull() else null }
    Canvas(modifier) {
        if (shader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            updateShader(shader, size.width, size.height, time, mood.copy(pulse = mood.pulse + beat * BEAT_PULSE), listOf(c0, c1, c2, c3), beat)
            drawRect(ShaderBrush(shader), alpha = alpha)
        } else {
            // Versione semplice: tre macchie di colore che girano lente su un fondo sfumato
            drawRect(Brush.verticalGradient(listOf(c0, c3)), alpha = alpha)
            listOf(c1, c2, c3).forEachIndexed { i, c ->
                val a = time * (0.6f + 0.25f * i) + i * 2.1f
                val center = Offset(size.width * (0.5f + 0.35f * cos(a)), size.height * (0.5f + 0.3f * sin(a * 0.8f)))
                val r = size.maxDimension * (0.55f + 0.08f * beat)
                drawCircle(Brush.radialGradient(listOf(c, Color.Transparent), center, r), r, center, alpha = alpha * 0.8f)
            }
        }
    }
}

private const val FRAME_NANOS = 33_333_333L
private const val BEAT_PULSE = 0.25f

/**
 * Analizzatore audio sull'uscita generale (sessione 0): solo lo spettro, circa 20 volte al secondo.
 * Restituisce null se Android non lo concede (permesso mancante o telefono che non lo supporta).
 */
private fun startBeat(onBeat: (Float) -> Unit): Visualizer? = runCatching {
    val tracker = BeatTracker()
    Visualizer(0).apply {
        captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(1024)
        setDataCaptureListener(
            object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) = Unit
                override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                    if (fft != null) onBeat(tracker.update(BeatTracker.bassEnergy(fft, samplingRate)))
                }
            },
            (Visualizer.getMaxCaptureRate() / 2).coerceAtMost(20_000),
            false,
            true,
        )
        enabled = true
    }
}.getOrNull()

/** Onde di colore che si piegano e si fondono: tre funzioni seno intrecciate, mosse dal tempo. */
private const val WAVES_AGSL = """
uniform float2 size;
uniform float time;
uniform float turbulence;
uniform float pulse;
uniform float beat;
layout(color) uniform half4 c0;
layout(color) uniform half4 c1;
layout(color) uniform half4 c2;
layout(color) uniform half4 c3;

half4 main(float2 p) {
    float2 uv = p / size;
    float t = time;
    float bend = 1.0 + turbulence * 2.0;
    float w1 = 0.5 + 0.5 * sin(uv.x * 3.1 + t * 0.7 + sin(uv.y * 2.3 + t * 0.4) * bend);
    float w2 = 0.5 + 0.5 * sin(uv.y * 3.7 - t * 0.5 + sin(uv.x * 2.9 - t * 0.3) * bend);
    float w3 = 0.5 + 0.5 * sin((uv.x + uv.y) * 2.2 + t * 0.9 + sin(t * 0.6) * turbulence);
    half4 a = mix(c0, c1, smoothstep(0.1, 0.9, w1));
    half4 b = mix(c2, c3, smoothstep(0.1, 0.9, w2));
    half4 c = mix(a, b, smoothstep(0.2, 0.8, w3));
    float glow = 1.0 + pulse * sin(t * 6.2831) + beat * 0.18;
    return half4(c.rgb * glow, 1.0);
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun waveShader(): RuntimeShader = RuntimeShader(WAVES_AGSL)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun updateShader(shader: RuntimeShader, w: Float, h: Float, time: Float, mood: MoodParams, colors: List<Color>, beat: Float) {
    shader.setFloatUniform("size", w, h)
    shader.setFloatUniform("time", time)
    shader.setFloatUniform("turbulence", mood.turbulence)
    shader.setFloatUniform("pulse", mood.pulse)
    shader.setFloatUniform("beat", beat)
    colors.forEachIndexed { i, c -> shader.setColorUniform("c$i", c.toArgb()) }
}
