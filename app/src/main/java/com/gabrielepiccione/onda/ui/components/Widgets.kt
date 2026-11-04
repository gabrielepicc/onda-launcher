// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
@file:OptIn(ExperimentalMaterial3Api::class)

package com.gabrielepiccione.onda.ui.components

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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import com.gabrielepiccione.onda.data.normalizedForSearch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import com.gabrielepiccione.onda.data.WidgetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
) {
    val info = remember(id) { widgets.infoFor(id) } ?: return
    val density = LocalDensity.current
    val colors = LocalOndaColors.current
    val defaultHeight = with(density) { info.minHeight.toDp() }.coerceIn(72.dp, 320.dp)
    val target = heightDp?.dp ?: defaultHeight
    // L'altezza cambia con una molla quando la si regola dal foglio di modifica
    val animated by androidx.compose.animation.core.animateDpAsState(target, Motion.bouncy(), label = "widgetHeight")
    val currentLongPress by androidx.compose.runtime.rememberUpdatedState(onLongPress)

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .then(
                if (highlighted) Modifier.border(2.dp, colors.accent, RoundedCornerShape(24.dp)).padding(2.dp)
                else Modifier
            )
            .clip(RoundedCornerShape(22.dp))
    ) {
        val widthDp = maxWidth
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
                .fillMaxWidth()
                .height(animated),
        )
    }
}

/** Pila di widget: un blocco solo che si sfoglia in orizzontale, con i puntini della pagina. */
@Composable
fun WidgetStackCard(
    stack: com.gabrielepiccione.onda.data.WidgetStack,
    widgets: WidgetManager,
    highlighted: Boolean,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier,
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
    Column(modifier.semantics { stateDescription = position }) {
        HorizontalPager(
            state = pager,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            stack.widgets.getOrNull(page)?.let { id ->
                androidx.compose.runtime.key(id) {
                    HostedWidget(id, widgets, heightDp = height, highlighted = highlighted, onLongPress = onLongPress)
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

/** Elenco dei widget installati, eventualmente filtrato per una sola app. */
@Composable
fun WidgetPickerSheet(
    widgets: WidgetManager,
    packageFilter: String?,
    onPick: (WidgetManager.Provider) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalOndaColors.current
    var providers by remember(packageFilter) { mutableStateOf(emptyList<WidgetManager.Provider>()) }
    LaunchedEffect(packageFilter) {
        providers = withContext(Dispatchers.IO) { widgets.providers(packageFilter) }
    }
    var query by remember(packageFilter) { mutableStateOf("") }
    val grouped = remember(providers, query) {
        val q = query.normalizedForSearch().trim()
        providers
            .filter { q.isEmpty() || it.label.normalizedForSearch().contains(q) || it.appLabel.normalizedForSearch().contains(q) }
            .groupBy { it.appLabel }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.panel,
        contentColor = colors.onWallpaper,
    ) {
        Text(
            stringResource(R.string.add_widget),
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        if (providers.isEmpty()) {
            Text(
                if (packageFilter != null) stringResource(R.string.app_no_widgets) else stringResource(R.string.loading),
                color = colors.onWallpaperMuted,
                modifier = Modifier.padding(24.dp),
            )
        }
        // Con un'app sola (filtro) la ricerca e le intestazioni non servono
        if (packageFilter == null && providers.isNotEmpty()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.widget_search)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
            )
            if (grouped.isEmpty()) {
                Text(stringResource(R.string.widget_no_results), color = colors.onWallpaperMuted, modifier = Modifier.padding(24.dp))
            }
        }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
            grouped.forEach { (appLabel, list) ->
            if (packageFilter == null) {
                item(key = "h_$appLabel") {
                    Text(
                        appLabel,
                        color = colors.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 2.dp),
                    )
                }
            }
            items(list, key = { it.info.provider.flattenToString() + it.info.profile.hashCode() }) { p ->
                var preview by remember(p) { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(p) {
                    preview = withContext(Dispatchers.IO) {
                        runCatching {
                            val d = widgets.previewOf(p) ?: return@runCatching null
                            val w = d.intrinsicWidth.takeIf { it > 0 } ?: 300
                            val h = d.intrinsicHeight.takeIf { it > 0 } ?: 150
                            val scale = (600f / w).coerceAtMost(1f)
                            d.toBitmap((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1)).asImageBitmap()
                        }.getOrNull()
                    }
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPick(p) }
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(p.label, fontSize = 17.sp)
                    preview?.let {
                        Image(
                            it,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .heightIn(max = 160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.panelBorder),
                        )
                    }
                }
            }
            }
        }
    }
}
