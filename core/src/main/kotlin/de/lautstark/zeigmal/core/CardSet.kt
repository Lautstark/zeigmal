package de.lautstark.zeigmal.core

import kotlinx.serialization.Serializable

/**
 * A card in a list, with the reference SIGNdigital uses for its sign. The
 * label is what the card says and what the sticker carries. [id] is the
 * site's id for the sign, which goes into the write log beside the slug.
 */
@Serializable
data class ListCard(
    val label: String,
    val ref: String,
    val id: String? = null,
)

/**
 * A stack of cards the writing mode walks, in the order the adult holds them:
 * a Lernliste as last loaded from sign-digital.de. Lists are only groupings;
 * they are made and changed on the site, never here.
 */
@Serializable
data class CardSet(
    val id: String,
    val title: String,
    val words: List<ListCard>,
) {
    companion object {
        /** No list chosen yet: the writing mode has nothing to write. */
        val NONE = CardSet("", "", emptyList())
    }
}
