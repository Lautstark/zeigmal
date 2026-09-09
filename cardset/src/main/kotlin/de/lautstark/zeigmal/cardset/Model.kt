package de.lautstark.zeigmal.cardset

// The runtime model of a Kartensatz: what the player needs to turn a tag into a
// video, and nothing an authoring tool would need. docs/media-model.md is the
// prose; this file is what it says.

/** Who says the word. Written by the preparation, never inferred from the file. */
enum class SpeechMode {
    /** The video carries a usable spoken word. */
    VIDEO,

    /** The video is silent or its sound is not the word; play [MediaEntry.audio] too. */
    EXTERNAL,

    /** Nothing is spoken, on purpose. */
    NONE,
}

/** Where a video came from, so the attribution can be shown where the video is. */
data class MediaSource(
    val provider: String? = null,
    val licence: String? = null,
    val attribution: String? = null,
    val url: String? = null,
)

/** A prepared spoken word, with the facts stimmquelle's contract needs to say whether it is still right. */
data class Audio(
    /** Relative path inside the Kartensatz. */
    val file: String,
    val text: String? = null,
    /** A stimmquelle voice id in `backend:model` form, e.g. `piper:de_DE-thorsten-medium`. */
    val voice: String? = null,
    /** `keyFor(text, voice, options)`, truncated however the preparation chose. */
    val key: String? = null,
)

/** References to the family's symbol identifiers. Never pixels, never a METACOM path. */
data class SymbolRef(
    /** METACOM file name without extension, e.g. `Trinken`. Resolvable by bildquelle's `idForName`. */
    val metacom: String? = null,
    /** ARASAAC pictogram number. */
    val arasaac: Int? = null,
    /** Optional picture inside the Kartensatz, for the idle screen or diagnostics. */
    val file: String? = null,
)

data class MediaEntry(
    /** Lowercase slug; the join key the rest of the family calls `concept`. */
    val id: String,
    /** The household's word as printed on the card and spoken. */
    val label: String,
    /** Relative path of the sign video inside the Kartensatz. */
    val video: String,
    val videoSource: MediaSource?,
    val speech: SpeechMode,
    val audio: Audio?,
    val symbol: SymbolRef?,
)

data class CardSet(
    val name: String,
    val modified: String?,
    /**
     * Whether the set may leave the device. False for anything holding METACOM
     * names or licensed video. Stored so the constraint outlives the import; the
     * player has no export path today, and this is what a future one must read.
     */
    val redistributable: Boolean,
    /** Default voice for the set, informational. */
    val voice: String?,
    val entries: Map<String, MediaEntry>,
)

/**
 * A tag identifier as the hex digits of its UID, lowercase, no separators.
 * "Spelling does not matter; only the hex digits are compared" — the rule
 * wochenwerk arrived at first.
 */
@JvmInline
value class TagId(
    val hex: String,
) {
    override fun toString(): String = hex.chunked(2).joinToString(":")

    companion object {
        private const val MIN_DIGITS = 8
        private const val MAX_DIGITS = 20

        fun parse(raw: String): TagId? {
            val cleaned = raw.lowercase().filter { it != ':' && it != ' ' && it != '-' }
            if (cleaned.length !in MIN_DIGITS..MAX_DIGITS) return null
            if (cleaned.any { it !in "0123456789abcdef" }) return null
            return TagId(cleaned)
        }

        fun of(bytes: ByteArray): TagId = TagId(bytes.joinToString("") { "%02x".format(it) })
    }
}

/** One physical card. Several rows may name the same entry: a card laminated twice, a sticker replaced. */
data class CardMapping(
    val tag: TagId,
    val entry: String,
    val note: String? = null,
)

data class Warning(
    val code: WarningCode,
    val detail: String,
)

enum class WarningCode {
    ENTRY_INVALID,
    ENTRY_DUPLICATE,
    ENTRY_WITHOUT_AUDIO,
    CARD_INVALID,
    CARD_DUPLICATE,
    CARD_UNKNOWN_ENTRY,
    MEDIA_MISSING,
}

enum class RejectionCode {
    NOT_JSON,
    WRONG_FORMAT,
    VERSION_TOO_NEW,
    INVALID,
}

/**
 * Nothing in this module throws at the caller: every failure is a value, because
 * the caller is a screen somebody is looking at. Strict about the set, lenient
 * about entries — a bad entry costs one card its video, not the child the set.
 */
sealed interface Parsed<out T> {
    data class Accepted<T>(
        val value: T,
        val warnings: List<Warning>,
    ) : Parsed<T>

    data class Rejected(
        val code: RejectionCode,
        val detail: String,
    ) : Parsed<Nothing>
}
