// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(ExperimentalMaterial3Api::class)

package com.gabrielepiccione.onda.ui.components

import kotlin.math.roundToInt
import com.gabrielepiccione.onda.data.WidgetSize
import com.gabrielepiccione.onda.data.WidgetSizing
import androidx.compose.foundation.layout.width
import com.gabrielepiccione.onda.ui.theme.*
import com.gabrielepiccione.onda.ui.home.*
import com.gabrielepiccione.onda.ui.drawer.*
import com.gabrielepiccione.onda.ui.search.*
import com.gabrielepiccione.onda.ui.settings.*
import com.gabrielepiccione.onda.ui.sheets.*

import androidx.compose.ui.res.stringResource
import com.gabrielepiccione.onda.R
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.gabrielepiccione.onda.data.WidgetManager

/** Widget ospitati in home, uno sotto l'altro. */
@Composable
fun WidgetsArea(widgetIds: List<Int>, widgets: WidgetManager, modifier: Modifier = Modifier) {
    if (widgetIds.isEmpty()) return
    Column(modifier) {
        widgetIds.forEach { id -> HostedWidget(id, widgets) }
    }
}

@Composable
fun HostedWidget(
    id: Int,
    widgets: WidgetManager,
    heightDp: Int? = null,
    highlighted: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    /** Larghezza scelta e "Automatica"; null = tutta la larghezza e altezza da [heightDp]. */
    size: WidgetSize? = null,
) {
    val info = remember(id) { widgets.infoFor(id) } ?: return
    val density = LocalDensity.current
    val colors = LocalOndaColors.current
    val preferredWidth = with(density) { info.minWidth.toDp() }.value.toInt()
    val preferredHeight = with(density) { info.minHeight.toDp() }.value.toInt().coerceIn(72, 320)
    val currentLongPress by androidx.compose.runtime.rememberUpdatedState(onLongPress)

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .then(
                if (highlighted) Modifier.border(2.dp, colors.accent, RoundedCornerShape(28.dp)).padding(2.dp)
                else Modifier
            )
            .clip(RoundedCornerShape(28.dp))
    ) {
        // Misure scelte (o automatiche), con una molla quando si regolano dal foglio di modifica
        val box = WidgetSizing.resolve(size, heightDp, preferredWidth, preferredHeight, maxWidth.value.toInt())
        val target = box.height.dp
        val animated by androidx.compose.animation.core.animateDpAsState(target, Motion.bouncy(), label = "widgetHeight")
        val widthDp = box.width?.dp ?: maxWidth
        val animatedWidth by androidx.compose.animation.core.animateDpAsState(widthDp, Motion.bouncy(), label = "widgetWidth")
        AndroidView(
            factory = { ctx ->
                (widgets.createView(ctx, id) ?: View(ctx)).also { v ->
                    (v as? com.gabrielepiccione.onda.data.OndaWidgetHostView)?.onLongPress = { currentLongPress?.invoke() }
                }
            },
            update = { view ->
                if (view is android.appwidget.AppWidgetHostView) {
                    val h = target.value
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        view.updateAppWidgetSize(Bundle(), listOf(SizeF(widthDp.value, h)))
                    } else {
                        @Suppress("DEPRECATION")
                        view.updateAppWidgetSize(null, widthDp.value.toInt(), h.toInt(), widthDp.value.toInt(), h.toInt())
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.Center)
                .width(animatedWidth)
                // altezza fissa voluta: è la misura del widget scelta dall'utente
                .height(animated),
        )
    }
}

/** Conferma prima di eliminare un widget: la sua configurazione andrebbe persa. */
@Composable
fun ConfirmDeleteWidgetDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.widget_delete_title)) },
        text = { Text(stringResource(R.string.widget_delete_body)) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) { Text(stringResource(R.string.remove)) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** Pila di widget: un blocco solo che si sfoglia in orizzontale, con i puntini della pagina. */
@Composable
fun WidgetStackCard(
    stack: com.gabrielepiccione.onda.data.WidgetStack,
    widgets: WidgetManager,
    highlighted: Boolean,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier,
    size: WidgetSize? = null,
) {
    if (stack.widgets.isEmpty()) return
    val colors = LocalOndaColors.current
    val density = LocalDensity.current
    val defaultHeight = remember(stack.widgets) {
        stack.widgets.mapNotNull { widgets.infoFor(it) }
            .maxOfOrNull { with(density) { it.minHeight.toDp().value.toInt() } }
            ?.coerceIn(72, 320) ?: 160
    }
    val height = stack.heightDp ?: defaultHeight
    val pager = rememberPagerState(pageCount = { stack.widgets.size })
    val position = stringResource(R.string.stack_page, pager.currentPage + 1, stack.widgets.size)
    // Il gesto orizzontale del carosello ha la precedenza: mentre si sfoglia, la pressione lunga non modifica nulla
    val guardedLongPress: (() -> Unit)? = onLongPress?.let { action ->
        { if (!pager.isScrollInProgress && pager.currentPageOffsetFraction == 0f) action() }
    }
    Column(modifier.semantics { stateDescription = position }) {
        HorizontalPager(
            state = pager,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            stack.widgets.getOrNull(page)?.let { id ->
                androidx.compose.runtime.key(id) {
                    HostedWidget(id, widgets, heightDp = height, highlighted = highlighted, onLongPress = guardedLongPress, size = size)
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        ) {
            repeat(stack.widgets.size) { i ->
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (i == pager.currentPage) colors.accent else colors.onWallpaperMuted.copy(alpha = 0.4f))
                )
            }
        }
    }
}

/**
 * Misure di un widget, uguali dappertutto: "Automatica" (misura preferita del widget), altezza e larghezza.
 * La larghezza al massimo vale "tutta la larghezza".
 */
@Composable
fun WidgetSizeControls(
    size: WidgetSize,
    height: Int,
    defaultHeight: Int,
    maxWidth: Int,
    onSize: (WidgetSize) -> Unit,
    onHeight: (Int?) -> Unit,
) {
    SwitchRow(stringResource(R.string.widget_size_auto), stringResource(R.string.widget_size_auto_sub), size.auto) { onSize(size.copy(auto = it)) }
    if (size.auto) {
        // Con «Automatico» le misure le decide il widget: gli slider restano nascosti, e si dice perché
        Text(
            stringResource(R.string.widget_size_auto_hint),
            color = LocalOndaColors.current.onWallpaperMuted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    } else {
        SliderRow(
            stringResource(R.string.edit_height),
            stringResource(R.string.value_dp, height),
            height.toFloat(),
            defaultHeight.toFloat(),
            WidgetSizing.MIN_HEIGHT.toFloat()..WidgetSizing.MAX_HEIGHT.toFloat(),
        ) { v -> onHeight(v.toInt().takeIf { it != defaultHeight }) }
        // Larghezza a tacche: ½, ¾, piena
        val step = WidgetSizing.widthStep(size.widthDp, maxWidth)
        val labels = listOf(R.string.widget_width_half, R.string.widget_width_three_quarters, R.string.widget_full_width)
        SliderRow(
            stringResource(R.string.edit_width),
            stringResource(labels[step]),
            step.toFloat(),
            WidgetSizing.WIDTH_FRACTIONS.lastIndex.toFloat(),
            0f..WidgetSizing.WIDTH_FRACTIONS.lastIndex.toFloat(),
            step = 1f,
        ) { v -> onSize(size.copy(widthDp = WidgetSizing.widthForStep(v.roundToInt(), maxWidth))) }
    }
}

val LocalWidgetSizes = androidx.compose.runtime.staticCompositionLocalOf<Map<String, WidgetSize>> { emptyMap() }
val LocalWidgetSizeSetter = androidx.compose.runtime.staticCompositionLocalOf<(String, WidgetSize) -> Unit> { { _, _ -> } }
