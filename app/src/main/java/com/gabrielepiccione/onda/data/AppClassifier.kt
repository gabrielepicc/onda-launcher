// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import android.content.Context
import android.content.pm.ApplicationInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Categorie usate per scegliere il glifo delle icone generate. */
enum class AppCategory {
    PHONE, MESSAGES, CHAT, EMAIL, CAMERA, GALLERY, MUSIC, VIDEO, MAPS, BROWSER, STORE, SHOPPING,
    BANK, PAYMENT, CALENDAR, CLOCK, CALCULATOR, NOTES, FILES, SETTINGS, WEATHER, NEWS, BOOKS,
    GAMES, SOCIAL, HEALTH, FOOD, TRAVEL, TRANSPORT, EDUCATION, WORK, CLOUD, SECURITY, TOOLS,
    TRANSLATE, CONTACTS, RECORDER, OTHER,
}

/**
 * Classifica un'app in una [AppCategory], in ordine di affidabilità:
 * 1. mappa curata pacchetto → categoria (assets/app_categories.json);
 * 2. parole chiave nel nome o nel pacchetto (italiano e inglese);
 * 3. categoria dichiarata dall'app al sistema (ApplicationInfo.category).
 * La parte decisionale è pura e testabile su JVM.
 */
object AppClassifier {

    /** Parole chiave in ordine di priorità (le più specifiche prima). */
    private val KEYWORDS: List<Pair<AppCategory, List<String>>> = listOf(
        AppCategory.CHAT to listOf("whatsapp", "telegram", "signal", "messenger", "chat", "discord", "viber"),
        AppCategory.MESSAGES to listOf("messag", "sms"),
        AppCategory.EMAIL to listOf("gmail", "mail", "posta", "outlook"),
        AppCategory.PHONE to listOf("dialer", "telefono", "phone"),
        AppCategory.CONTACTS to listOf("contact", "rubrica"),
        AppCategory.CAMERA to listOf("camera", "fotocamera", "scanner cam"),
        AppCategory.GALLERY to listOf("gallery", "galleria", "photos", "foto", "photo"),
        AppCategory.MUSIC to listOf("spotify", "music", "musica", "radio", "deezer", "soundcloud"),
        AppCategory.VIDEO to listOf("youtube", "netflix", "prime video", "disney", "twitch", "video", "raiplay", "mediaset"),
        AppCategory.MAPS to listOf("maps", "mappe", "waze", "navig"),
        AppCategory.BROWSER to listOf("chrome", "firefox", "browser", "brave", "opera", "internet"),
        AppCategory.STORE to listOf("play store", "vending", "store", "f-droid", "fdroid", "aurora"),
        AppCategory.SHOPPING to listOf("amazon", "ebay", "shop", "zalando", "vinted", "subito", "acquist", "temu", "shein"),
        AppCategory.BANK to listOf("bank", "banca", "banco", "intesa", "unicredit", "fineco", "revolut", "n26", "bper", "mediolanum", "hype"),
        AppCategory.PAYMENT to listOf("paypal", "satispay", "wallet", "postepay", "pay"),
        AppCategory.CALENDAR to listOf("calendar", "calendario", "agenda"),
        AppCategory.CLOCK to listOf("deskclock", "clock", "orologio", "sveglia", "alarm"),
        AppCategory.CALCULATOR to listOf("calcul"),
        AppCategory.NOTES to listOf("keep", "notes", "note", "memo", "notion", "obsidian"),
        AppCategory.FILES to listOf("files", "file manager", "documentsui", "explorer", "archivio"),
        AppCategory.SETTINGS to listOf("settings", "impostazioni"),
        AppCategory.WEATHER to listOf("weather", "meteo"),
        AppCategory.NEWS to listOf("news", "notizie", "giornale", "repubblica", "corriere", "ansa"),
        AppCategory.BOOKS to listOf("kindle", "books", "libri", "reader", "audible"),
        AppCategory.SOCIAL to listOf("instagram", "facebook", "tiktok", "twitter", "threads", "reddit", "linkedin", "snapchat", "pinterest", "social"),
        AppCategory.HEALTH to listOf("health", "salute", "fitness", "medic", "dottore", "strava", "fit"),
        AppCategory.FOOD to listOf("deliveroo", "glovo", "justeat", "mcdonald", "food", "ristor", "thefork"),
        AppCategory.TRAVEL to listOf("booking", "airbnb", "trenitalia", "italo", "ryanair", "travel", "viaggi", "trip", "flight"),
        AppCategory.TRANSPORT to listOf("uber", "taxi", "moovit", "transit", "bus", "freenow", "bolt"),
        AppCategory.EDUCATION to listOf("duolingo", "classroom", "moodle", "school", "learn", "edu"),
        AppCategory.WORK to listOf("teams", "slack", "office", "word", "excel", "powerpoint", "jira", "zoom", "meet"),
        AppCategory.CLOUD to listOf("drive", "dropbox", "onedrive", "cloud", "mega"),
        AppCategory.SECURITY to listOf("authenticator", "bitwarden", "password", "vpn", "security", "sicurezza"),
        AppCategory.TRANSLATE to listOf("translate", "tradutt", "deepl"),
        AppCategory.RECORDER to listOf("recorder", "registra"),
        AppCategory.GAMES to listOf("game", "gioco", "giochi"),
        AppCategory.TOOLS to listOf("torch", "torcia", "tool", "scanner", "qr", "compass", "bussola"),
    )

    /** Decisione pura (testabile): [known] è la mappa curata, [systemCategory] il valore di ApplicationInfo. */
    fun classify(packageName: String, label: String, systemCategory: Int, known: Map<String, AppCategory>): AppCategory {
        known[packageName]?.let { return it }
        val haystack = (label + " " + packageName).lowercase()
        KEYWORDS.firstOrNull { (_, words) -> words.any { haystack.contains(it) } }?.let { return it.first }
        return when (systemCategory) {
            ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMES
            ApplicationInfo.CATEGORY_AUDIO -> AppCategory.MUSIC
            ApplicationInfo.CATEGORY_VIDEO -> AppCategory.VIDEO
            ApplicationInfo.CATEGORY_IMAGE -> AppCategory.GALLERY
            ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
            ApplicationInfo.CATEGORY_NEWS -> AppCategory.NEWS
            ApplicationInfo.CATEGORY_MAPS -> AppCategory.MAPS
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.WORK
            ApplicationInfo.CATEGORY_ACCESSIBILITY -> AppCategory.TOOLS
            else -> AppCategory.OTHER
        }
    }

    /** Legge la mappa curata da assets (una volta sola). */
    fun loadKnown(context: Context): Map<String, AppCategory> = runCatching {
        val text = context.assets.open("app_categories.json").use { it.readBytes().decodeToString() }
        Json.parseToJsonElement(text).jsonObject.mapNotNull { (pkg, v) ->
            runCatching { pkg to AppCategory.valueOf(v.jsonPrimitive.content) }.getOrNull()
        }.toMap()
    }.getOrDefault(emptyMap())
}
