// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

import android.icu.text.AlphabeticIndex
import android.icu.text.Transliterator
import android.icu.util.ULocale
import android.os.Build
import android.os.LocaleList
import com.gabrielepiccione.onda.data.OTHER_SECTION
import com.gabrielepiccione.onda.data.PlainSectionIndex
import com.gabrielepiccione.onda.data.SectionIndex

/**
 * Alfabeto secondo le lingue del telefono, nel loro ordine: lettere extra (Å Ä Ö, Ñ…) e altri
 * alfabeti. I nomi in un alfabeto che le lingue del telefono non coprono vengono traslitterati
 * (Android 10+) e messi sotto la lettera corrispondente, invece che sotto "#".
 */
class IcuSectionIndex private constructor(locales: List<ULocale>) : SectionIndex {

    private val index = AlphabeticIndex<Any>(locales.first())
        .setMaxLabelCount(MAX_LABELS)
        .addLabels(*locales.drop(1).toTypedArray())
        .buildImmutableIndex()

    private val ranks: Map<String, Int> = (0 until index.bucketCount).associate { index.getBucket(it).label to it }

    private val latin: Transliterator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { Transliterator.getInstance("Any-Latin; Latin-ASCII") }.getOrNull()
        } else {
            null
        }

    // Le stesse etichette tornano a ogni aggiornamento dell'elenco: la traslitterazione costa
    private val cache = HashMap<String, String>()

    override fun sectionOf(label: String): String = cache.getOrPut(label) {
        val name = label.trim()
        if (name.isEmpty()) return@getOrPut OTHER_SECTION
        normalBucket(name)
            ?: latin?.let { t -> runCatching { normalBucket(t.transliterate(name)) }.getOrNull() }
            ?: OTHER_SECTION
    }

    /** Lettera del nome; null se finisce nei gruppi "prima, in mezzo o dopo" l'alfabeto. */
    private fun normalBucket(name: String): String? {
        val bucket = index.getBucket(index.getBucketIndex(name))
        return bucket.label.takeIf { bucket.labelType == AlphabeticIndex.Bucket.LabelType.NORMAL }
    }

    override fun rank(section: String): Int =
        if (section == OTHER_SECTION) -1 else ranks[section] ?: Int.MAX_VALUE

    companion object {
        private const val MAX_LABELS = 300

        /** Se ICU non è utilizzabile, l'elenco resta comunque ordinato con l'alfabeto A–Z. */
        fun of(locales: LocaleList): SectionIndex = runCatching<SectionIndex> {
            val list = (0 until locales.size()).map { ULocale.forLocale(locales[it]) }
            IcuSectionIndex(list.ifEmpty { listOf(ULocale.getDefault()) })
        }.getOrDefault(PlainSectionIndex)
    }
}
