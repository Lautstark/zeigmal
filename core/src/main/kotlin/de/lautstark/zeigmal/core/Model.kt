package de.lautstark.zeigmal.core

/**
 * A tag identifier as the hex digits of its UID, lowercase, no separators.
 * Only the digits compare; how a person spells it does not matter.
 */
@JvmInline
value class TagId(
    val hex: String,
) {
    override fun toString(): String = hex.chunked(2).joinToString(":")

    companion object {
        fun of(bytes: ByteArray): TagId = TagId(bytes.joinToString("") { "%02x".format(it) })
    }
}

/**
 * What a card is, written on its own sticker. The phone reads this and needs
 * nothing else: which provider, which sign there, and the word for the log.
 *
 * On the wire it is one small text: a version line, then one field per line,
 * `key=value`, UTF-8. Under a hundred bytes for any SIGNbox word, and readable
 * by a person with any NFC app.
 */
data class CardRecord(
    val provider: String,
    val ref: String,
    val label: String,
) {
    init {
        require(provider.isNotBlank() && provider == provider.trim()) { "provider" }
        require(ref.isNotBlank() && ref == ref.trim()) { "ref" }
        require(label.isNotBlank() && '\n' !in label) { "label" }
    }

    fun encode(): ByteArray = "$HEADER\nprovider=$provider\nref=$ref\nlabel=$label\n".toByteArray(Charsets.UTF_8)

    companion object {
        const val HEADER = "zeigmal/1"

        /** The NDEF external type this record travels under: `lautstark.de:zeigmal`. */
        const val NDEF_DOMAIN = "lautstark.de"
        const val NDEF_TYPE = "zeigmal"

        /** Null for anything that is not one of ours; a sticker from elsewhere is simply unknown. */
        fun decode(bytes: ByteArray): CardRecord? {
            val text = runCatching { String(bytes, Charsets.UTF_8) }.getOrNull() ?: return null
            val lines = text.split('\n').map { it.trimEnd('\r') }
            if (lines.firstOrNull() != HEADER) return null
            val fields =
                lines
                    .drop(1)
                    .filter { '=' in it }
                    .associate { it.substringBefore('=') to it.substringAfter('=') }
            val provider = fields["provider"]?.trim().orEmpty()
            val ref = fields["ref"]?.trim().orEmpty()
            val label = fields["label"]?.trim().orEmpty()
            if (provider.isEmpty() || ref.isEmpty()) return null
            return CardRecord(provider, ref, label.ifEmpty { ref })
        }
    }
}

/** What the reader reports, before the presence filter. */
sealed interface TagEvent {
    data class Seen(
        val tag: TagId,
        /** Null when the sticker carries no zeigmal record. */
        val record: CardRecord?,
        val technologies: List<String> = emptyList(),
    ) : TagEvent

    data class Gone(
        val tag: TagId,
    ) : TagEvent
}

/** What a provider hands back for a reference. Links, never bytes. */
data class Media(
    /** The clip to play, or null when the provider has none for this ref. */
    val videoUrl: String?,
    /** A picture of the card itself, for the adult writing it. */
    val cardImageUrl: String?,
)

/**
 * A source of sign videos. The station never knows what is behind a card
 * beyond this: a provider id on the sticker and a ref the provider understands.
 * SignDict, a folder of own recordings, or a provider that only plays a sound
 * are each one more implementation, and none of them changes the station.
 */
interface Provider {
    val id: String

    suspend fun resolve(ref: String): Media
}

/** Where a provider's login lives. The app backs it with private preferences; tests with a map. */
interface CredentialStore {
    fun get(key: String): String?

    fun put(
        key: String,
        value: String?,
    )
}

class InMemoryCredentialStore : CredentialStore {
    private val map = HashMap<String, String>()

    override fun get(key: String): String? = map[key]

    override fun put(
        key: String,
        value: String?,
    ) {
        if (value == null) map.remove(key) else map[key] = value
    }
}
