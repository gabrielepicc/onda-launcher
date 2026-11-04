// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.core.icons

/**
 * Contenuto di un file `appfilter.xml` (formato ADW/Nova usato dalla maggior parte degli icon pack).
 *
 * Elementi supportati:
 * - `<item component="ComponentInfo{pkg/cls}" drawable="nome"/>`  icona dedicata
 * - `<calendar component="..." prefix="cal_"/>`                 icona calendario dinamica (prefix + giorno 1..31)
 * - `<iconback img1="a" img2="b"/>`                             sfondi per le app non coperte
 * - `<iconmask img1="m"/>` / `<iconupon img1="u"/>`              maschera e livello superiore
 * - `<scale factor="0.8"/>`                                      scala dell'icona originale nel fallback
 */
data class AppFilter(
    /** "pkg/cls" -> nome drawable. */
    val components: Map<String, String> = emptyMap(),
    /** "pkg" -> nome drawable (prima voce trovata per il pacchetto, usata come ripiego). */
    val packages: Map<String, String> = emptyMap(),
    /** "pkg/cls" -> prefisso delle icone calendario. */
    val calendars: Map<String, String> = emptyMap(),
    val iconBacks: List<String> = emptyList(),
    val iconMask: String? = null,
    val iconUpon: String? = null,
    val scale: Float = 1f,
) {
    val isEmpty: Boolean
        get() = components.isEmpty() && calendars.isEmpty() && iconBacks.isEmpty()

    /** Ha un fallback (sfondo/maschera) per le app non coperte. */
    val hasFallback: Boolean
        get() = iconBacks.isNotEmpty() || iconMask != null || iconUpon != null

    /**
     * Nome del drawable per un componente.
     * @param dayOfMonth giorno corrente, usato per le icone calendario dinamiche
     */
    fun drawableFor(packageName: String, className: String, dayOfMonth: Int): String? {
        val key = "$packageName/$className"
        calendars[key]?.let { return it + dayOfMonth }
        return components[key] ?: packages[packageName]
    }

    /** Sfondo scelto in modo stabile per un pacchetto (stessa app = stesso sfondo). */
    fun iconBackFor(packageName: String): String? =
        if (iconBacks.isEmpty()) null else iconBacks[Math.floorMod(packageName.hashCode(), iconBacks.size)]
}

/** Un tag XML con i suoi attributi, indipendente dal parser usato. */
data class XmlTag(val name: String, val attributes: Map<String, String>)

object AppFilterParser {

    /** Costruisce un [AppFilter] da una sequenza di tag (da XmlPullParser, regex o altro). */
    fun parse(tags: Sequence<XmlTag>): AppFilter {
        val components = HashMap<String, String>()
        val packages = HashMap<String, String>()
        val calendars = HashMap<String, String>()
        val backs = ArrayList<String>()
        var mask: String? = null
        var upon: String? = null
        var scale = 1f

        for (tag in tags) {
            when (tag.name.lowercase()) {
                "item" -> {
                    val comp = parseComponent(tag.attributes["component"]) ?: continue
                    val drawable = tag.attributes["drawable"]?.trim()?.takeIf { it.isNotEmpty() } ?: continue
                    components.putIfAbsent(comp, drawable)
                    packages.putIfAbsent(comp.substringBefore('/'), drawable)
                }
                "calendar" -> {
                    val comp = parseComponent(tag.attributes["component"]) ?: continue
                    val prefix = tag.attributes["prefix"]?.trim()?.takeIf { it.isNotEmpty() } ?: continue
                    calendars[comp] = prefix
                }
                "iconback" -> tag.attributes.entries
                    .filter { it.key.startsWith("img") }
                    .sortedBy { it.key.removePrefix("img").toIntOrNull() ?: Int.MAX_VALUE }
                    .mapTo(backs) { it.value }
                "iconmask" -> mask = tag.attributes["img1"] ?: tag.attributes.values.firstOrNull()
                "iconupon" -> upon = tag.attributes["img1"] ?: tag.attributes.values.firstOrNull()
                "scale" -> scale = tag.attributes["factor"]?.toFloatOrNull()?.takeIf { it > 0f && it <= 2f } ?: scale
            }
        }
        return AppFilter(components, packages, calendars, backs, mask, upon, scale)
    }

    /** "ComponentInfo{pkg/cls}" -> "pkg/cls" (classe relativa ".X" espansa con il pacchetto). */
    fun parseComponent(raw: String?): String? {
        val inner = raw?.trim()?.substringAfter("ComponentInfo{", "")?.substringBefore("}")?.trim() ?: return null
        val slash = inner.indexOf('/')
        if (slash <= 0 || slash == inner.lastIndex) return null
        val pkg = inner.substring(0, slash)
        val cls = inner.substring(slash + 1).let { if (it.startsWith(".")) pkg + it else it }
        return "$pkg/$cls"
    }

    private val TAG = Regex("<\\s*([A-Za-z]+)([^>]*?)/?>")
    private val ATTR = Regex("([A-Za-z0-9_:]+)\\s*=\\s*\"([^\"]*)\"")

    /** Parser testuale per `appfilter.xml` in chiaro (cartella assets o file di test). */
    fun parseText(xml: String): AppFilter {
        val withoutComments = xml.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
        val tags = TAG.findAll(withoutComments).map { m ->
            val attrs = ATTR.findAll(m.groupValues[2]).associate { a -> a.groupValues[1] to unescape(a.groupValues[2]) }
            XmlTag(m.groupValues[1], attrs)
        }
        return parse(tags)
    }

    private fun unescape(s: String) =
        s.replace("&amp;", "&").replace("&quot;", "\"").replace("&lt;", "<").replace("&gt;", ">").replace("&apos;", "'")
}
