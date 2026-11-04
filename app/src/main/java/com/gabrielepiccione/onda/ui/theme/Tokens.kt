// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.theme

import com.gabrielepiccione.onda.ui.components.*
import com.gabrielepiccione.onda.ui.home.*
import com.gabrielepiccione.onda.ui.drawer.*
import com.gabrielepiccione.onda.ui.search.*
import com.gabrielepiccione.onda.ui.settings.*
import com.gabrielepiccione.onda.ui.sheets.*

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spaziature standard (griglia da 4dp). */
@Immutable
data class OndaSpacing(
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 12.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    /** Area minima di tocco (linee guida di accessibilità). */
    val touchTarget: Dp = 48.dp,
    /** Larghezza della colonna dell'alfabeto visibile. */
    val scrollerVisible: Dp = 48.dp,
    /** Larghezza della zona invisibile sul bordo opposto. */
    val scrollerHidden: Dp = 28.dp,
)

/** Forme Material 3 usate dal launcher. */
@Immutable
data class OndaShapes(
    val small: RoundedCornerShape = RoundedCornerShape(12.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(20.dp),
    val large: RoundedCornerShape = RoundedCornerShape(28.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(50),
)

/** Insieme dei token di design; i temi futuri (Fase 3) li sostituiranno come dati. */
@Immutable
data class OndaTokens(
    val spacing: OndaSpacing = OndaSpacing(),
    val shapes: OndaShapes = OndaShapes(),
)

val LocalOndaTokens = staticCompositionLocalOf { OndaTokens() }
