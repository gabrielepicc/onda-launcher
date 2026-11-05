// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.drawer

import com.gabrielepiccione.onda.data.ContactHit
import com.gabrielepiccione.onda.data.OTHER_SECTION
import com.gabrielepiccione.onda.data.PlainSectionIndex
import com.gabrielepiccione.onda.data.SectionIndex
import org.junit.Assert.assertEquals
import org.junit.Test

class SectionsTest {

    private fun contact(id: Long, name: String) = ContactHit(id, "k$id", name, null)

    @Test
    fun contactsAddLettersInOrderWithHashFirst() {
        val sections = buildSections(emptyList(), contacts = listOf(contact(1, "Zoe"), contact(2, "Àlex"), contact(3, "3B")))
        assertEquals(listOf("#", "A", "Z"), sections.map { it.letter })
    }

    @Test
    fun theAlphabetDecidesLettersAndTheirOrder() {
        // Come in svedese: Å è una lettera a sé, dopo la Z
        val swedish = object : SectionIndex {
            override fun sectionOf(label: String) = label.take(1)
            override fun rank(section: String) = listOf("A", "Z", "Å").indexOf(section)
        }
        val sections = buildSections(emptyList(), contacts = listOf(contact(1, "Åsa"), contact(2, "Zoe"), contact(3, "Anna")), alphabet = swedish)
        assertEquals(listOf("A", "Z", "Å"), sections.map { it.letter })
    }

    @Test
    fun plainAlphabetKeepsSymbolsFirstAndIgnoresAccents() {
        assertEquals("A", PlainSectionIndex.sectionOf("Àlex"))
        assertEquals(OTHER_SECTION, PlainSectionIndex.sectionOf("3B"))
        assertEquals(OTHER_SECTION, PlainSectionIndex.sectionOf("   "))
        assertEquals(true, PlainSectionIndex.rank(OTHER_SECTION) < PlainSectionIndex.rank("A"))
    }

    @Test
    fun firstIndexCountsHeadersAndContacts() {
        val sections = buildSections(emptyList(), contacts = listOf(contact(1, "Anna"), contact(2, "Aldo"), contact(3, "Bea")))
        assertEquals(listOf(0, 3), sections.map { it.firstIndex })
    }

    @Test
    fun endOfListComesAfterLastSectionAndPrivateSpace() {
        val sections = buildSections(emptyList(), contacts = listOf(contact(1, "Anna"), contact(2, "Aldo"), contact(3, "Bea")))
        // A: intestazione + 2, B: intestazione + 1 → 5 righe; lo Spazio privato ne aggiunge 3
        assertEquals(5, endOfListIndex(sections))
        assertEquals(8, endOfListIndex(sections, privateRows = 3))
        assertEquals(0, endOfListIndex(emptyList()))
    }

    @Test
    fun noContactsNoSections() {
        assertEquals(emptyList<AppSection>(), buildSections(emptyList()))
    }
}
