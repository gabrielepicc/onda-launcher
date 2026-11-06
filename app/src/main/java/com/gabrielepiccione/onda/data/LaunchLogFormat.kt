// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import java.security.MessageDigest

/**
 * Formato di una riga del registro delle aperture, senza dipendenze Android (testabile su JVM):
 * `ora<TAB>chiave app<TAB>segnali<TAB>codice Wi-Fi`. Le righe vecchie, con solo ora e app, restano leggibili.
 * I segnali sono lettere: `h` cuffie, `c` in carica.
 */
object LaunchLogFormat {
    fun encode(r: LaunchRecord): String {
        val flags = (if (r.headphones) "h" else "") + (if (r.charging) "c" else "")
        return listOf(r.time.toString(), r.appKey, flags, r.wifiHash.orEmpty()).joinToString("\t").trimEnd('\t')
    }

    fun decode(line: String): LaunchRecord? {
        val parts = line.split('\t')
        if (parts.size < 2) return null
        val time = parts[0].toLongOrNull() ?: return null
        if (parts[1].isEmpty()) return null
        val flags = parts.getOrElse(2) { "" }
        return LaunchRecord(parts[1], time, 'h' in flags, 'c' in flags, parts.getOrNull(3)?.takeIf { it.isNotEmpty() })
    }

    /** Il Wi-Fi si ricorda solo come codice: non si può risalire al nome, e non si usa nessuna posizione. */
    fun wifiCode(ssid: String?): String? {
        val name = ssid?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return MessageDigest.getInstance("SHA-256").digest(name.toByteArray()).take(6).joinToString("") { "%02x".format(it) }
    }
}
