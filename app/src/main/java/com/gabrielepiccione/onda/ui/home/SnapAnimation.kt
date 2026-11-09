// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember

/**
 * Come animateFloatAsState, ma con [snap] a true porta subito il valore a [target], anche a metà animazione:
 * serve quando il launcher esce di scena, perché un'animazione ferma a metà riprenderebbe al ritorno.
 */
@Composable
fun animateOrSnap(target: Float, snap: Boolean, spec: AnimationSpec<Float>): State<Float> {
    val value = remember { Animatable(target) }
    LaunchedEffect(target, snap) {
        if (snap) value.snapTo(target) else value.animateTo(target, spec)
    }
    return value.asState()
}
