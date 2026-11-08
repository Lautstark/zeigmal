package de.lautstark.zeigmal.core

import kotlinx.serialization.Serializable

/**
 * A word on a card, with the reference SIGNdigital uses for its sign. The
 * label is what the card says and what the sticker carries; two cards may
 * share a sign, never a label. A word from a Lernliste also has the site's
 * [id] for its sign, which goes into the write log beside the slug.
 */
@Serializable
data class SignBoxWord(
    val label: String,
    val ref: String,
    val id: String? = null,
)

/**
 * A stack of cards the writing mode walks, in the order the adult holds them.
 * The words and refs are ours or public; nothing of SIGN's material is in it.
 */
@Serializable
data class CardSet(
    val id: String,
    val title: String,
    val words: List<SignBoxWord>,
)

object SignBox {
    /** SIGNdigital's own slug: lowercase, umlauts to plain letters, ß to ss, the rest to hyphens. */
    fun refFor(label: String): String {
        var s = label.trim().lowercase()
        for ((from, to) in listOf("ä" to "a", "ö" to "o", "ü" to "u", "ß" to "ss")) s = s.replace(from, to)
        return s.replace(Regex("[^a-z0-9]+"), "-").trim('-')
    }

    /**
     * A printed card name such as `helfen/Hilfe` or `Abend(s)` names one sign;
     * its ref is the first plain word, which is how the site slugs it
     * (`abend-s` is the exception the site makes for the bracket, and it is
     * what [refFor] produces from the whole label).
     */
    fun refForCard(label: String): String = OVERRIDES[label] ?: refFor(label)

    /**
     * One card per line. `label = ref` names the sign outright, for a card
     * whose word has no sign of its own; a bare label gets [refForCard].
     */
    fun parse(lines: List<String>): List<SignBoxWord> =
        lines
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }
            .map { line ->
                val label = line.substringBefore('=').trim()
                val ref = line.substringAfter('=', "").trim()
                SignBoxWord(label, ref.ifEmpty { refForCard(label) })
            }

    private fun load(resource: String): List<SignBoxWord> {
        val stream = SignBox::class.java.getResourceAsStream(resource) ?: error("$resource missing")
        return parse(stream.bufferedReader(Charsets.UTF_8).readLines())
    }

    /** HHO's SIGNbox 1, in the order of their printed list (HHO publishes it as a PDF). */
    val box1: List<SignBoxWord> by lazy { load("/signbox1.txt") }

    /** The family's own Halloween cards. */
    val halloween: List<SignBoxWord> by lazy { load("/halloween.txt") }

    /** Every set the writing mode offers; the first is where it starts. */
    val sets: List<CardSet> by lazy {
        listOf(
            CardSet(BOX1, "SIGNbox 1", box1),
            CardSet(HALLOWEEN, "Halloween", halloween),
        )
    }

    fun set(id: String?): CardSet = sets.firstOrNull { it.id == id } ?: sets.first()

    const val BOX1 = "signbox1"
    const val HALLOWEEN = "halloween"

    /**
     * Where the printed name and the site's slug part ways. Found by probing
     * the site, 2026-09-13; the last five were renamed on the site and found
     * again 2026-10-31.
     */
    private val OVERRIDES =
        mapOf(
            "helfen/Hilfe" to "hilfe",
            "mögen/möchten" to "mogen",
            "gewesen/Partizip II" to "gewesen",
            "gucken/sehen" to "gucken-sehen-mg",
            "lieben/Liebe" to "lieben-liebe",
            "(Hände) waschen" to "waschen-hande",
            "wehtun (Schmerzen)" to "schmerzen",
            "selber" to "selbst",
        )
}
