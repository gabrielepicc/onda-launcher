// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.home

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.ui.components.OndaChip
import com.gabrielepiccione.onda.ui.theme.blockTouches
import com.gabrielepiccione.onda.ui.settings.SettingEntry
import com.gabrielepiccione.onda.ui.settings.SettingsTargets
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors

/** Un punto del benvenuto: cosa fa Onda e, con "Mostrami", dove si trova. */
private class Highlight(val icon: ImageVector, @StringRes val text: Int, val target: SettingEntry?)

/**
 * Benvenuto al primo avvio: spiega in poche righe cosa sa fare Onda. Ogni punto ha "Mostrami", che apre
 * la schermata delle impostazioni giusta e fa lampeggiare la voce. I permessi si chiedono solo se l'utente li vuole.
 */
@Composable
internal fun OnboardingScreen(
    isDefaultLauncher: Boolean,
    notificationAccess: Boolean,
    onRequestDefault: () -> Unit,
    onNotificationAccess: () -> Unit,
    onShow: (SettingEntry) -> Unit,
    onDone: () -> Unit,
) {
    val colors = LocalOndaColors.current
    val highlights = listOf(
        Highlight(Icons.Filled.SortByAlpha, R.string.welcome_alphabet, SettingsTargets.alphabet),
        Highlight(Icons.Filled.TouchApp, R.string.welcome_swipe, SettingsTargets.gestures),
        Highlight(Icons.Filled.Widgets, R.string.welcome_longpress, SettingsTargets.home),
        Highlight(Icons.Filled.Search, R.string.welcome_search, SettingsTargets.search),
        Highlight(Icons.Filled.CreateNewFolder, R.string.welcome_folders, SettingsTargets.icons),
        Highlight(Icons.Filled.AutoAwesome, R.string.welcome_profiles, SettingsTargets.profiles),
        Highlight(Icons.Filled.HourglassEmpty, R.string.welcome_wellbeing, SettingsTargets.wellbeing),
        Highlight(Icons.Filled.Lock, R.string.welcome_privacy, SettingsTargets.backup),
    )
    BackHandler(onBack = onDone)

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .blockTouches()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.welcome_title), color = colors.onWallpaper, style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.welcome_intro), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.size(8.dp))
            highlights.forEach { h ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Icon(h.icon, null, tint = colors.accent, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(h.text), color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    h.target?.let { target ->
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { onShow(target) }) { Text(stringResource(R.string.welcome_show_me)) }
                    }
                }
            }
            Spacer(Modifier.size(8.dp))
            // I permessi, solo se servono
            if (!isDefaultLauncher) OndaChip(stringResource(R.string.hint_default), onRequestDefault, icon = Icons.Filled.Apps)
            if (!notificationAccess) OndaChip(stringResource(R.string.hint_notifications), onNotificationAccess, icon = Icons.Filled.Lock)
            if (isDefaultLauncher && notificationAccess) {
                Icon(Icons.Filled.Check, stringResource(R.string.onboarding_done_cd), tint = colors.accent)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.End) {
            Button(onClick = onDone) { Text(stringResource(R.string.onboarding_start)) }
        }
    }
}
