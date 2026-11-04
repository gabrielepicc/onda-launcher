// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Conversione JSON <-> [Settings], senza dipendenze Android (testabile su JVM).
 *
 * Legge tre formati:
 *  - schema 2+ (corrente): serializzazione diretta di [Settings];
 *  - schema 1 (Onda 0.2/0.3, riconoscibile dalla chiave "version"): nomi di campo diversi e
 *    cartelle salvate come array; viene convertito al volo;
 *  - qualunque campo sconosciuto viene ignorato, qualunque campo mancante prende il default.
 */
object SettingsCodec {

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
        isLenient = true
        prettyPrint = false
    }

    private val pretty = Json(json) { prettyPrint = true }

    fun encode(settings: Settings, prettyPrint: Boolean = false): String =
        (if (prettyPrint) pretty else json).encodeToString(Settings.serializer(), settings)

    /** @throws IllegalArgumentException se il testo non è un JSON di impostazioni valido. */
    fun decode(text: String): Settings {
        val root = runCatching { json.parseToJsonElement(text).jsonObject }
            .getOrElse { throw IllegalArgumentException("JSON non valido", it) }
        val migrated = migrate(root)
        return json.decodeFromJsonElement(Settings.serializer(), migrated)
            .copy(schemaVersion = Settings.CURRENT_SCHEMA)
    }

    /** Porta un oggetto JSON di qualunque versione allo schema corrente. */
    fun migrate(root: JsonObject): JsonObject {
        var obj = root
        val version = obj["schemaVersion"]?.jsonPrimitive?.intOrNull
            ?: if ("version" in obj) 1 else Settings.CURRENT_SCHEMA
        if (version < 2) obj = fromSchema1(obj)
        if (version < 3) obj = fromSchema2(obj)
        if (version < 4) obj = fromSchema3(obj)
        // Passi futuri: if (version < 5) obj = fromSchema4(obj) ...
        return obj
    }

    /** Schema 3 -> 4: il doppio tocco per bloccare diventa un'azione; chi aggiorna non rivede il benvenuto. */
    private fun fromSchema3(old: JsonObject): JsonObject = buildJsonObject {
        for ((key, value) in old) {
            if (key != "doubleTapLock") put(key, value)
        }
        if (old["doubleTapLock"]?.jsonPrimitive?.booleanOrNull == true) put("doubleTapAction", JsonPrimitive("LOCK_SCREEN"))
        put("welcomeDone", JsonPrimitive(true))
        put("schemaVersion", JsonPrimitive(4))
    }

    /** Schema 1 (0.2/0.3): "muted" -> mutedNotifications, "widgets" -> widgetIds, cartelle da array a mappa. */
    private fun fromSchema1(old: JsonObject): JsonObject = buildJsonObject {
        val renamed = mapOf("muted" to "mutedNotifications", "widgets" to "widgetIds")
        for ((key, value) in old) {
            when (key) {
                "version" -> Unit
                "folders" -> put("folders", foldersArrayToMap(value))
                else -> put(renamed[key] ?: key, value)
            }
        }
        put("schemaVersion", JsonPrimitive(2))
    }

    /** Schema 2 -> 3: lo swipe giù è fisso sulla tendina, quindi lo swipe su non la duplica. */
    private fun fromSchema2(old: JsonObject): JsonObject = buildJsonObject {
        for ((key, value) in old) {
            if (key == "swipeUp" && value.jsonPrimitive.content == "NOTIFICATIONS") put(key, JsonPrimitive("SEARCH"))
            else put(key, value)
        }
        put("schemaVersion", JsonPrimitive(3))
    }

    private fun foldersArrayToMap(value: JsonElement): JsonElement {
        val array = value as? JsonArray ?: return value
        return buildJsonObject {
            array.forEach { item ->
                val f = item as? JsonObject ?: return@forEach
                val id = f["id"]?.jsonPrimitive?.content ?: return@forEach
                put(id, f)
            }
        }
    }

    /** Versione dello schema dichiarata in un JSON, utile per i messaggi all'utente. */
    fun schemaOf(text: String): Int? = runCatching {
        val o = json.parseToJsonElement(text).jsonObject
        o["schemaVersion"]?.jsonPrimitive?.int ?: if ("version" in o) 1 else null
    }.getOrNull()
}
