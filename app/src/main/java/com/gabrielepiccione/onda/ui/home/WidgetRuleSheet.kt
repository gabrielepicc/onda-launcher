// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gabrielepiccione.onda.R
import com.gabrielepiccione.onda.data.AutomationTrigger
import com.gabrielepiccione.onda.data.WidgetCondition
import com.gabrielepiccione.onda.data.WidgetPlacement
import com.gabrielepiccione.onda.data.WidgetRule
import com.gabrielepiccione.onda.data.WidgetVisibility
import com.gabrielepiccione.onda.ui.components.ActionRow
import com.gabrielepiccione.onda.ui.components.ChoiceRow
import com.gabrielepiccione.onda.ui.components.GroupCard
import com.gabrielepiccione.onda.ui.components.OndaTextField
import com.gabrielepiccione.onda.ui.components.SheetScaffold
import com.gabrielepiccione.onda.ui.components.SliderRow
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors

/** Testo breve di una regola, per il menu del widget («Sempre», «Cuffie collegate · Di notte»). */
@Composable
fun widgetRuleSummary(rule: WidgetRule): String {
    val place = stringResource(rule.placement.labelRes)
    val whenText = if (rule.always || rule.conditions.isEmpty()) {
        stringResource(R.string.wrule_always)
    } else {
        rule.conditions.map { conditionText(it) }.joinToString(" · ")
    }
    return "$place · $whenText"
}

@Composable
private fun conditionText(c: WidgetCondition): String {
    val base = stringResource(WidgetVisibility.stateLabel(c.trigger))
    return when (c.trigger) {
        AutomationTrigger.TIME -> stringResource(R.string.wcond_time_range, hm(c.fromMinute), hm(c.toMinute))
        AutomationTrigger.DEVICE_ON, AutomationTrigger.DEVICE_OFF -> if (c.device.isBlank()) base else "$base: ${c.device}"
        else -> base
    }
}

private fun hm(minute: Int) = "%02d:%02d".format(minute / 60 % 24, minute % 60)

/**
 * «Mostra solo quando…»: dove sta il widget (sotto, accanto o al posto dell'orologio) e quando si vede.
 * Le condizioni sono quelle delle automazioni, lette come stati con un inizio e una fine.
 */
@Composable
fun WidgetRuleSheet(title: String, rule: WidgetRule, onChange: (WidgetRule) -> Unit, onDismiss: () -> Unit) {
    var adding by remember { mutableStateOf(false) }
    val colors = LocalOndaColors.current
    SheetScaffold(title = title, onDismiss = onDismiss) {
        GroupCard(stringResource(R.string.wplace_title)) {
            ChoiceRow(
                null,
                WidgetPlacement.entries.map { it to stringResource(it.labelRes) },
                rule.placement,
                descriptions = mapOf(
                    WidgetPlacement.BELOW_CLOCK to stringResource(R.string.wplace_below_sub),
                    WidgetPlacement.BESIDE_CLOCK to stringResource(R.string.wplace_beside_sub),
                    WidgetPlacement.REPLACE_CLOCK to stringResource(R.string.wplace_replace_sub),
                ),
            ) { onChange(rule.copy(placement = it)) }
            if (rule.placement != WidgetPlacement.BELOW_CLOCK) {
                Text(stringResource(R.string.wplace_one_at_a_time), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        GroupCard(stringResource(R.string.wrule_when)) {
            ChoiceRow(
                null,
                listOf(true to stringResource(R.string.wrule_always), false to stringResource(R.string.wrule_rules)),
                rule.always,
            ) { onChange(rule.copy(always = it)) }
            if (!rule.always) {
                if (rule.conditions.isEmpty()) {
                    Text(stringResource(R.string.wrule_none_yet), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(stringResource(R.string.wrule_all), color = colors.onWallpaperMuted, style = MaterialTheme.typography.bodyMedium)
                }
                rule.conditions.forEachIndexed { index, c ->
                    ConditionEditor(c, onChange = { n -> onChange(rule.copy(conditions = rule.conditions.mapIndexed { i, o -> if (i == index) n else o })) }) {
                        onChange(rule.copy(conditions = rule.conditions.filterIndexed { i, _ -> i != index }))
                    }
                }
                ActionRow(Icons.Filled.Add, stringResource(R.string.wrule_add)) { adding = true }
            }
        }
    }
    if (adding) {
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text(stringResource(R.string.wrule_add)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(AutomationTrigger.entries, key = { it.name }) { t ->
                        TextButton(
                            onClick = { adding = false; onChange(rule.copy(conditions = rule.conditions + WidgetCondition(t))) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) { Text(stringResource(WidgetVisibility.stateLabel(t)), modifier = Modifier.fillMaxWidth()) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { adding = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun ConditionEditor(c: WidgetCondition, onChange: (WidgetCondition) -> Unit, onRemove: () -> Unit) {
    val colors = LocalOndaColors.current
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(conditionText(c), color = colors.onWallpaper, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, stringResource(R.string.remove), tint = colors.onWallpaperMuted) }
        }
        when (c.trigger) {
            AutomationTrigger.TIME -> {
                SliderRow(stringResource(R.string.wcond_from), hm(c.fromMinute), (c.fromMinute / 60).toFloat(), 8f, 0f..23f, step = 1f) { v ->
                    onChange(c.copy(fromMinute = v.toInt() * 60))
                }
                SliderRow(stringResource(R.string.wcond_to), hm(c.toMinute), (c.toMinute / 60).toFloat(), 18f, 0f..23f, step = 1f) { v ->
                    onChange(c.copy(toMinute = v.toInt() * 60))
                }
            }
            AutomationTrigger.DEVICE_ON, AutomationTrigger.DEVICE_OFF -> OndaTextField(
                value = c.device,
                onValueChange = { onChange(c.copy(device = it)) },
                label = { Text(stringResource(R.string.auto_device)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            else -> Unit
        }
    }
}
