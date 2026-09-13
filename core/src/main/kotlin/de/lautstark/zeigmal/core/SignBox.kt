package de.lautstark.zeigmal.core

/**
 * The words of HHO's SIGNbox 1, in the order of their printed list, each with
 * the reference SIGNdigital uses for it. The list is public (HHO publishes it
 * as a PDF); nothing of SIGN's material is in it. It is what the writing mode
 * walks through, card by card.
 */
data class SignBoxWord(
    val label: String,
    val ref: String,
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

    val box1: List<SignBoxWord> by lazy {
        val stream = SignBox::class.java.getResourceAsStream("/signbox1.txt") ?: error("signbox1.txt missing")
        stream
            .bufferedReader(Charsets.UTF_8)
            .readLines()
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }
            .map { SignBoxWord(it, refForCard(it)) }
    }

    /** Where the printed name and the site's slug part ways. Found by probing the site, 2026-09-13. */
    private val OVERRIDES =
        mapOf(
            "helfen/Hilfe" to "hilfe",
            "gucken/sehen" to "gucken",
            "lieben/Liebe" to "lieben",
            "mögen/möchten" to "mogen",
            "(Hände) waschen" to "waschen",
            "wehtun (Schmerzen)" to "wehtun",
            "gewesen/Partizip II" to "gewesen",
        )
}
