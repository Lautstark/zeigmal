package de.lautstark.zeigmal.cardset

/**
 * Reads `cards.json`: which tag means which entry. Kept apart from the manifest
 * because it is a fact about one household's stickers, is written on the phone
 * when a card is scanned, and is the one file worth backing up on its own —
 * the media can be prepared again; three hundred scans cannot.
 */
object Cards {
    const val FORMAT = "zeigmal.karten"
    const val VERSION = 1

    fun parse(
        text: String,
        knownEntries: Set<String>? = null,
    ): Parsed<List<CardMapping>> {
        val root =
            when (val r = Reader.root(text)) {
                is Parsed.Rejected -> return r
                is Parsed.Accepted -> r.value
            }
        Reader.header(root, FORMAT, VERSION)?.let { return it }
        val rows = root.array("cards") ?: return Parsed.Rejected(RejectionCode.INVALID, "cards missing")

        val warnings = mutableListOf<Warning>()
        val seen = HashSet<TagId>()
        val mappings = ArrayList<CardMapping>()
        rows.forEachIndexed { index, element ->
            val obj = element.asObject()
            if (obj == null) {
                warnings += Warning(WarningCode.CARD_INVALID, "cards[$index] is not an object")
                return@forEachIndexed
            }
            val tag = obj.str("tag")?.let(TagId::parse)
            if (tag == null) {
                warnings += Warning(WarningCode.CARD_INVALID, "cards[$index]: tag ${obj.str("tag") ?: "missing"} is not hex digits")
                return@forEachIndexed
            }
            val entry = obj.str("entry")
            if (entry.isNullOrBlank()) {
                warnings += Warning(WarningCode.CARD_INVALID, "cards[$index] ($tag): entry missing")
                return@forEachIndexed
            }
            if (!seen.add(tag)) {
                warnings += Warning(WarningCode.CARD_DUPLICATE, "cards[$index]: tag $tag appears twice, first one kept")
                return@forEachIndexed
            }
            if (knownEntries != null && entry !in knownEntries) {
                warnings += Warning(WarningCode.CARD_UNKNOWN_ENTRY, "cards[$index] ($tag): no entry named $entry")
                return@forEachIndexed
            }
            mappings += CardMapping(tag, entry, obj.str("note"))
        }
        return Parsed.Accepted(mappings, warnings)
    }

    /** The other direction, for the backup the player writes. */
    fun write(mappings: List<CardMapping>): String {
        val rows =
            mappings.joinToString(",\n") { m ->
                buildString {
                    append("    { \"tag\": \"")
                        .append(m.tag.hex)
                        .append("\", \"entry\": \"")
                        .append(escape(m.entry))
                        .append('"')
                    m.note?.let { append(", \"note\": \"").append(escape(it)).append('"') }
                    append(" }")
                }
            }
        return "{\n  \"format\": \"$FORMAT\",\n  \"version\": $VERSION,\n  \"cards\": [\n$rows\n  ]\n}\n"
    }

    private fun escape(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"")
}
