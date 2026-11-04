// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

import android.content.pm.ApplicationInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class AppClassifierTest {
    private val known = mapOf("com.example.special" to AppCategory.BANK)
    private fun c(pkg: String, label: String, sys: Int = ApplicationInfo.CATEGORY_UNDEFINED) = AppClassifier.classify(pkg, label, sys, known)

    @Test fun knownMapWins() = assertEquals(AppCategory.BANK, c("com.example.special", "Giochi"))
    @Test fun whatsappIsChat() = assertEquals(AppCategory.CHAT, c("com.whatsapp", "WhatsApp"))
    @Test fun italianLabel() = assertEquals(AppCategory.WEATHER, c("it.ilmeteo", "Meteo"))
    @Test fun bankByName() = assertEquals(AppCategory.BANK, c("com.latuabancaperandroid", "Intesa Sanpaolo Mobile"))
    @Test fun systemCategoryFallback() = assertEquals(AppCategory.GAMES, c("com.studio.xyz", "Qwerty", ApplicationInfo.CATEGORY_GAME))
    @Test fun unknownIsOther() = assertEquals(AppCategory.OTHER, c("com.zzz.qqq", "Zzz"))
}
