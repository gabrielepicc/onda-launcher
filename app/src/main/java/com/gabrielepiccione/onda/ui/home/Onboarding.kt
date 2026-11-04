// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import com.gabrielepiccione.onda.ui.theme.blockTouches
import kotlinx.coroutines.launch

/** Azioni dell'introduzione: ogni permesso viene chiesto solo se l'utente tocca il pulsante. */
interface OnboardingActions {
    fun defaultLauncher()
    fun notificationAccess()
    fun contacts()
    fun calendar()
    fun openStyles()
    fun done()
}

/** Introduzione al primo avvio: gesti, permessi facoltativi spiegati, scelta dello stile. */
@Composable
fun OnboardingScreen(status: HomeStatus, actions: OnboardingActions) {
    val colors = LocalOndaColors.current
    val pages = 3
    val pager = rememberPagerState { pages }
    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .blockTouches()
            .systemBarsPadding()
    ) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = actions::done) { Text(stringResource(R.string.intro_skip)) }
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .widthIn(max = 560.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                ) {
                    when (page) {
                        0 -> {
                            Title(R.string.intro_welcome_title)
                            Body(R.string.intro_welcome_body)
                            Bullet(R.string.intro_gesture_right)
                            Bullet(R.string.intro_gesture_left)
                            Bullet(R.string.intro_gesture_down)
                            Bullet(R.string.intro_gesture_alphabet)
                            Bullet(R.string.intro_gesture_long)
                        }
                        1 -> {
                            Title(R.string.intro_permissions_title)
                            Body(R.string.intro_permissions_body)
                            PermissionRow(R.string.default_launcher, R.string.intro_default_why, status.isDefaultLauncher, actions::defaultLauncher)
                            PermissionRow(R.string.notif_access, R.string.intro_notifications_why, status.notificationAccess, actions::notificationAccess)
                            PermissionRow(R.string.contacts, R.string.intro_contacts_why, status.contactsPermission, actions::contacts)
                            PermissionRow(R.string.calendar, R.string.intro_calendar_why, status.calendarPermission, actions::calendar)
                        }
                        else -> {
                            Title(R.string.intro_style_title)
                            Body(R.string.intro_style_body)
                            FilledTonalButton(onClick = actions::openStyles, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(stringResource(R.string.styles_open))
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    repeat(pages) { i ->
                        Box(
                            Modifier
                                .size(if (i == pager.currentPage) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (i == pager.currentPage) colors.accent else colors.onWallpaperMuted)
                        )
                    }
                }
                val last = pager.currentPage == pages - 1
                Button(
                    onClick = { if (last) actions.done() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(if (last) R.string.intro_start else R.string.intro_next))
                }
            }
        }
    }
}

@Composable
private fun Title(res: Int) {
    Text(stringResource(res), style = MaterialTheme.typography.headlineMedium, color = LocalOndaColors.current.onWallpaper)
}

@Composable
private fun Body(res: Int) {
    Text(stringResource(res), style = MaterialTheme.typography.bodyLarge, color = LocalOndaColors.current.onWallpaperMuted)
}

@Composable
private fun Bullet(res: Int) {
    Row {
        Text("•", color = LocalOndaColors.current.accent, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.size(10.dp))
        Text(stringResource(res), style = MaterialTheme.typography.bodyLarge, color = LocalOndaColors.current.onWallpaper)
    }
}

@Composable
private fun PermissionRow(title: Int, why: Int, granted: Boolean, onClick: () -> Unit) {
    val colors = LocalOndaColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = colors.onWallpaper)
            Text(stringResource(why), style = MaterialTheme.typography.bodySmall, color = colors.onWallpaperMuted)
        }
        Spacer(Modifier.size(12.dp))
        if (granted) {
            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.intro_granted), tint = colors.accent, modifier = Modifier.size(48.dp).padding(12.dp))
        } else {
            FilledTonalButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.intro_allow)) }
        }
    }
}
