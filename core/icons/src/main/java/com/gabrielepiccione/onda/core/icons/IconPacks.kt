// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.core.icons

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser
import java.util.Calendar

/** Ricerca e caricamento degli icon pack installati. */
object IconPacks {

    data class Pack(val packageName: String, val label: String)

    /** Intent con cui gli icon pack si dichiarano ai launcher. */
    val THEME_ACTIONS = listOf(
        "org.adw.launcher.THEMES",
        "com.gau.go.launcherex.theme",
        "com.novalauncher.THEME",
        "com.anddoes.launcher.THEME",
        "com.teslacoilsw.launcher.THEME",
    )

    fun installed(context: Context): List<Pack> {
        val pm = context.packageManager
        return THEME_ACTIONS
            .flatMap { action -> runCatching { pm.queryIntentActivities(Intent(action), 0) }.getOrDefault(emptyList()) }
            .map { it.activityInfo.packageName }
            .distinct()
            .map { pkg ->
                val label = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
                Pack(pkg, label)
            }
            .sortedBy { it.label.lowercase() }
    }

    /** Pack caricato: risorse del pacchetto + appfilter già interpretato. */
    class Loaded(
        private val res: Resources,
        val packageName: String,
        val filter: AppFilter,
    ) {
        @SuppressLint("DiscouragedApi")
        fun drawable(name: String?, densityDpi: Int): Drawable? {
            if (name.isNullOrBlank()) return null
            val id = res.getIdentifier(name, "drawable", packageName).takeIf { it != 0 } ?: return null
            return runCatching { res.getDrawableForDensity(id, densityDpi, null) }.getOrNull()
        }

        /** Tutti i nomi di drawable noti del pack (da appfilter e, se c'è, da drawable.xml). */
        val allDrawables: List<String> by lazy {
            val names = LinkedHashSet<String>()
            names += filter.components.values
            @SuppressLint("DiscouragedApi")
            val id = res.getIdentifier("drawable", "xml", packageName)
            if (id != 0) {
                runCatching {
                    val p = res.getXml(id)
                    var e = p.eventType
                    while (e != XmlPullParser.END_DOCUMENT) {
                        if (e == XmlPullParser.START_TAG && p.name == "item") p.getAttributeValue(null, "drawable")?.let { names += it }
                        e = p.next()
                    }
                }
            }
            names.sorted()
        }

        /** Elementi del fallback (sfondo scelto in modo stabile per pacchetto). */
        fun fallbackParts(packageName: String, densityDpi: Int): Triple<Drawable?, Drawable?, Drawable?>? {
            if (!filter.hasFallback) return null
            return Triple(
                drawable(filter.iconBackFor(packageName), densityDpi),
                drawable(filter.iconMask, densityDpi),
                drawable(filter.iconUpon, densityDpi),
            )
        }

        /** Icona dedicata del pack (calendario dinamico incluso), oppure null. */
        fun iconFor(component: ComponentName, densityDpi: Int): Drawable? {
            val day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            return drawable(filter.drawableFor(component.packageName, component.className, day), densityDpi)
        }
    }

    @SuppressLint("DiscouragedApi")
    fun load(context: Context, packageName: String): Loaded? {
        if (packageName.isBlank()) return null
        val res = runCatching { context.packageManager.getResourcesForApplication(packageName) }.getOrNull() ?: return null

        val id = res.getIdentifier("appfilter", "xml", packageName)
        val filter = if (id != 0) {
            runCatching { AppFilterParser.parse(res.getXml(id).tags()) }.getOrNull()
        } else {
            runCatching {
                res.assets.open("appfilter.xml").use { AppFilterParser.parseText(it.readBytes().decodeToString()) }
            }.getOrNull()
        }
        if (filter == null || filter.isEmpty) return null
        return Loaded(res, packageName, filter)
    }

    /** Converte un XmlPullParser in una sequenza di tag di apertura. */
    private fun XmlPullParser.tags(): Sequence<XmlTag> = sequence {
        var event = eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                val attrs = (0 until attributeCount).associate { getAttributeName(it) to getAttributeValue(it) }
                yield(XmlTag(name, attrs))
            }
            event = next()
        }
    }
}

/** Cache dei pack caricati (il parsing di appfilter.xml è costoso). */
class IconPackCache(private val context: Context) {
    private val loaded = HashMap<String, IconPacks.Loaded?>()

    @Synchronized
    fun get(packageName: String): IconPacks.Loaded? {
        if (packageName.isBlank()) return null
        return loaded.getOrPut(packageName) { IconPacks.load(context, packageName) }
    }

    @Synchronized
    fun invalidate(packageName: String? = null) {
        if (packageName == null) loaded.clear() else loaded.remove(packageName)
    }
}
