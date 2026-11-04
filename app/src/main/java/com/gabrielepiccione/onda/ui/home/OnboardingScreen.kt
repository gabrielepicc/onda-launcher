// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.home

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import com.gabrielepiccione.onda.ui.theme.blockTouches
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    @StringRes val title: Int,
    @StringRes val body: Int,
    /** Pulsante facoltativo: testo, testo quando è già fatto. */
    @StringRes val action: Int? = null,
)

/** Benvenuto al primo avvio: spiega i gesti e chiede i permessi solo se l'utente li vuole. */
@Composable
fun OnboardingScreen(
    isDefaultLauncher: Boolean,
    notificationAccess: Boolean,
    onRequestDefault: () -> Unit,
    onNotificationAccess: () -> Unit,
    onDone: () -> Unit,
) {
    val colors = LocalOndaColors.current
    val scope = rememberCoroutineScope()
    val pages = listOf(
        OnboardingPage(Icons.Filled.Home, R.string.onboarding_home_title, R.string.onboarding_home_body, R.string.hint_default),
        OnboardingPage(Icons.Filled.SortByAlpha, R.string.onboarding_alphabet_title, R.string.onboarding_alphabet_body),
        OnboardingPage(Icons.Filled.TouchApp, R.string.onboarding_gestures_title, R.string.onboarding_gestures_body),
        OnboardingPage(Icons.Filled.Notifications, R.string.onboarding_notifications_title, R.string.onboarding_notifications_body, R.string.hint_notifications),
    )
    val pager = rememberPagerState(pageCount = { pages.size })
    val last = pager.currentPage == pages.lastIndex
    BackHandler(enabled = pager.currentPage > 0) {
        scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .blockTouches()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { index ->
            val page = pages[index]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(88.dp).clip(CircleShape).background(colors.accentContainer),
                ) {
                    Icon(page.icon, null, tint = colors.onAccentContainer, modifier = Modifier.size(44.dp))
                }
                Text(
                    stringResource(page.title),
                    color = colors.onWallpaper,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 24.dp),
                )
                Text(
                    stringResource(page.body),
                    color = colors.onWallpaperMuted,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
                page.action?.let { action ->
                    val done = if (action == R.string.hint_default) isDefaultLauncher else notificationAccess
                    Spacer(Modifier.size(20.dp))
                    if (done) {
                        Icon(Icons.Filled.Check, stringResource(R.string.onboarding_done_cd), tint = colors.accent)
                    } else {
                        FilledTonalButton(onClick = if (action == R.string.hint_default) onRequestDefault else onNotificationAccess) {
                            Text(stringResource(action))
                        }
                    }
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (last) Spacer(Modifier.weight(1f)) else {
                TextButton(onClick = onDone, modifier = Modifier.weight(1f, fill = false)) { Text(stringResource(R.string.onboarding_skip)) }
                Spacer(Modifier.weight(1f))
            }
            Button(onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) {
                Text(stringResource(if (last) R.string.onboarding_start else R.string.onboarding_next))
            }
        }
    }
}
