// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.drawer

import com.gabrielepiccione.onda.data.ContactHit
import org.junit.Assert.assertEquals
import org.junit.Test

class SectionsTest {

    private fun contact(id: Long, name: String) = ContactHit(id, "k$id", name, null)

    @Test
    fun contactsAddLettersInOrderWithHashFirst() {
        val sections = buildSections(emptyList(), contacts = listOf(contact(1, "Zoe"), contact(2, "Àlex"), contact(3, "3B")))
        assertEquals(listOf('#', 'A', 'Z'), sections.map { it.letter })
    }

    @Test
    fun firstIndexCountsHeadersAndContacts() {
        val sections = buildSections(emptyList(), contacts = listOf(contact(1, "Anna"), contact(2, "Aldo"), contact(3, "Bea")))
        assertEquals(listOf(0, 3), sections.map { it.firstIndex })
    }

    @Test
    fun noContactsNoSections() {
        assertEquals(emptyList<AppSection>(), buildSections(emptyList()))
    }
}
