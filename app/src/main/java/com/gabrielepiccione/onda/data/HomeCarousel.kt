// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.data

/** Una scheda del carosello della home. */
enum class CardPage(val block: String) {
    MEDIA(HomeBlocks.MEDIA),
    AGENDA(HomeBlocks.AGENDA),
    NOTIFICATIONS(HomeBlocks.NOTIFICATIONS),
}

/**
 * Player, agenda e riepilogo delle notifiche in un solo blocco «Schede» a scorrimento orizzontale.
 * Regole pure (testabili su JVM): quali schede, in che ordine, dove compare il carosello e quando andare al player.
 */
object HomeCarousel {
    /** I blocchi della home che finiscono nel carosello. */
    val CARD_BLOCKS: Set<String> = CardPage.entries.map { it.block }.toSet()

    /**
     * Le schede da mostrare, nell'ordine dei blocchi della home, solo quelle con contenuto:
     * il player solo se c'è un brano, il riepilogo solo con notifiche, l'agenda se il blocco è attivo.
     */
    fun pages(blocks: List<String>, hasMedia: Boolean, hasAgenda: Boolean, hasNotifications: Boolean): List<CardPage> =
        blocks.mapNotNull { b -> CardPage.entries.firstOrNull { it.block == b } }.filter {
            when (it) {
                CardPage.MEDIA -> hasMedia
                CardPage.AGENDA -> hasAgenda
                CardPage.NOTIFICATIONS -> hasNotifications
            }
        }

    /** Il carosello prende il posto del primo dei tre blocchi nell'ordine della home; gli altri due spariscono. */
    fun anchorBlock(blocks: List<String>): String? = blocks.firstOrNull { it in CARD_BLOCKS }

    /**
     * Quando parte la musica il carosello va al player, ma non mentre l'utente sta trascinando le schede.
     * Restituisce la pagina a cui andare, o null.
     */
    fun musicTarget(pages: List<CardPage>, wasPlaying: Boolean, playing: Boolean, userDragging: Boolean): Int? {
        if (!playing || wasPlaying || userDragging) return null
        return pages.indexOf(CardPage.MEDIA).takeIf { it >= 0 }
    }

    /** I puntini compaiono solo con più di una scheda. */
    fun showDots(count: Int): Boolean = count > 1

    /** Al più quattro icone sovrapposte nel riepilogo. */
    const val MAX_STACKED_ICONS = 4

    fun stackedIcons(packages: List<String>): List<String> = packages.distinct().take(MAX_STACKED_ICONS)
}
