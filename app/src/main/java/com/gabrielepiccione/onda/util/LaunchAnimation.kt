// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

import android.app.ActivityOptions
import android.os.Bundle
import android.view.View
import com.gabrielepiccione.onda.R
import java.lang.ref.WeakReference

/** Animazioni di apertura delle app; l'origine è l'ultimo punto toccato sullo schermo. */
object LaunchAnimation {

    /** Valori di [com.gabrielepiccione.onda.data.Settings.openAnimation]. */
    const val SYSTEM = "SYSTEM"
    const val SCALE = "SCALE"
    const val REVEAL = "REVEAL"
    const val SLIDE = "SLIDE"
    const val FADE = "FADE"
    val ALL = listOf(SYSTEM, SCALE, REVEAL, SLIDE, FADE)

    private var root: WeakReference<View>? = null
    @Volatile private var lastX = -1
    @Volatile private var lastY = -1

    fun attach(view: View) {
        root = WeakReference(view)
    }

    fun onTouch(x: Float, y: Float) {
        lastX = x.toInt()
        lastY = y.toInt()
    }

    fun options(style: String): Bundle? {
        val view = root?.get() ?: return null
        val x = if (lastX >= 0) lastX else view.width / 2
        val y = if (lastY >= 0) lastY else view.height / 2
        val size = (48 * view.resources.displayMetrics.density).toInt()
        return runCatching {
            when (style) {
                SCALE -> ActivityOptions.makeScaleUpAnimation(view, x - size / 2, y - size / 2, size, size)
                REVEAL -> ActivityOptions.makeClipRevealAnimation(view, x - size / 2, y - size / 2, size, size)
                SLIDE -> ActivityOptions.makeCustomAnimation(view.context, R.anim.onda_slide_up, R.anim.onda_stay)
                FADE -> ActivityOptions.makeCustomAnimation(view.context, android.R.anim.fade_in, android.R.anim.fade_out)
                else -> null
            }?.toBundle()
        }.getOrNull()
    }
}
