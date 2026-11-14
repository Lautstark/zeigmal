package de.lautstark.zeigmal.core

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * A sign as a Lernliste holds it. [id] is SIGNdigital's own and survives a
 * renamed slug, so what has been written is kept by it; [slug] is what goes on
 * the sticker; [name] is the site's name for the sign, which is the card's word
 * here: Lernlisten have no word of their own per entry.
 */
data class ListSign(
    val id: String,
    val slug: String,
    val name: String,
    /** The card picture's path on the site, to be signed before it is shown. Null when the sign has none. */
    val cardPath: String?,
)

/**
 * One of the family's Lernlisten on sign-digital.de, as the site keeps it. The
 * site holds it; zeigmal reads it and changes it there (ADR 0013).
 */
data class Lernliste(
    val id: String,
    val name: String,
    /** The site's own timestamp, ISO 8601; the newest list comes first. */
    val updatedAt: String,
    val signs: List<ListSign>,
) {
    /**
     * The list as a stack for the writing mode. The site's name for each sign is
     * the card's word; its slug goes on the sticker.
     */
    fun toCardSet(): CardSet =
        CardSet(
            id = SET_PREFIX + id,
            title = name.ifBlank { "Lernliste" },
            words = signs.map { ListCard(label = it.name.oneLine().ifEmpty { it.slug }, ref = it.slug, id = it.id) },
        )

    companion object {
        /** A card set made from a Lernliste has this before the list's id, so it never meets a set in the app. */
        const val SET_PREFIX = "sd-"

        /**
         * One page of `GET /api/lists`, as measured 2026-11-08 with a throwaway
         * list: `data` holds the lists, each with its `signs` already filled in,
         * in the list's order. A sign without a slug cannot be written, so it is
         * left out rather than failing the list; so is a list without an id.
         */
        fun page(json: JsonObject): List<Lernliste> = json["data"]?.jsonArray.orEmpty().mapNotNull { one(it as? JsonObject) }

        /** One list as `GET /api/lists/<id>` returns it, signs filled in; null without an id. */
        fun one(list: JsonObject?): Lernliste? {
            if (list == null) return null
            val id = list.string("_id") ?: return null
            return Lernliste(
                id = id,
                name = list.string("name") ?: "",
                updatedAt = list.string("updatedAt") ?: "",
                signs = list["signs"]?.jsonArray.orEmpty().mapNotNull { sign(it as? JsonObject) },
            )
        }

        /** The hits of `GET /search/query`, which come in the same shape as a list's signs. */
        fun signs(json: JsonObject): List<ListSign> = json["data"]?.jsonArray.orEmpty().mapNotNull { sign(it as? JsonObject) }

        private fun sign(json: JsonObject?): ListSign? {
            if (json == null) return null
            val id = json.string("_id") ?: return null
            // A slug goes on the sticker as it is; one with a space in it is not one the site makes.
            val slug = json.string("slug")?.takeIf { it.none(Char::isWhitespace) } ?: return null
            val card =
                json["edge"]
                    ?.jsonObject
                    ?.get("signBox")
                    ?.jsonObject
                    ?.string("default")
            return ListSign(id = id, slug = slug, name = json.string("name") ?: slug, cardPath = card)
        }

        // A sticker's fields are single lines (CardRecord): a name the site
        // wrapped is one line again, with single spaces.
        private fun String.oneLine(): String = replace(Regex("\\s+"), " ").trim()

        private fun JsonObject.string(key: String): String? =
            (this[key] as? JsonPrimitive)
                ?.takeIf {
                    it.isString
                }?.content
                ?.takeIf { it.isNotEmpty() }
    }
}
