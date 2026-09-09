package de.lautstark.zeigmal.cardset

import java.io.File

/**
 * A Kartensatz on disk: a directory holding `manifest.json`, `cards.json` and the
 * media the manifest names. The player points this at one directory and gets
 * back everything it needs to answer "which video does this tag start".
 */
sealed interface Loaded {
    /** No manifest at that path. The idle screen says so; nothing else changes. */
    data class Missing(
        val dir: File,
    ) : Loaded

    data class Rejected(
        val dir: File,
        val file: String,
        val code: RejectionCode,
        val detail: String,
    ) : Loaded

    data class Ready(
        val dir: File,
        val set: CardSet,
        val cards: List<CardMapping>,
        val warnings: List<Warning>,
    ) : Loaded {
        private val byTag: Map<TagId, MediaEntry> =
            cards.mapNotNull { m -> set.entries[m.entry]?.let { m.tag to it } }.toMap()

        fun resolve(tag: TagId): MediaEntry? = byTag[tag]

        fun file(relative: String): File = File(dir, relative)
    }
}

object KartensatzDirectory {
    const val MANIFEST = "manifest.json"
    const val CARDS = "cards.json"

    fun load(dir: File): Loaded {
        val manifestFile = File(dir, MANIFEST)
        if (!manifestFile.isFile) return Loaded.Missing(dir)

        val set =
            when (val p = Manifest.parse(manifestFile.readText())) {
                is Parsed.Rejected -> return Loaded.Rejected(dir, MANIFEST, p.code, p.detail)
                is Parsed.Accepted -> p
            }
        val warnings = set.warnings.toMutableList()

        // Entries whose video is not there are dropped: a card that starts nothing is
        // worse than a card the player does not know, because the second one at least
        // gets the "unknown" feedback. A missing audio file only costs the word.
        val entries = LinkedHashMap<String, MediaEntry>()
        for ((id, entry) in set.value.entries) {
            if (!File(dir, entry.video).isFile) {
                warnings += Warning(WarningCode.MEDIA_MISSING, "$id: video ${entry.video} not found")
                continue
            }
            var kept = entry
            if (entry.audio != null && !File(dir, entry.audio.file).isFile) {
                warnings += Warning(WarningCode.MEDIA_MISSING, "$id: audio ${entry.audio.file} not found, plays silent")
                kept = entry.copy(audio = null, speech = if (entry.speech == SpeechMode.EXTERNAL) SpeechMode.NONE else entry.speech)
            }
            entries[id] = kept
        }
        val cardSet = set.value.copy(entries = entries)

        val cardsFile = File(dir, CARDS)
        val cards =
            if (!cardsFile.isFile) {
                emptyList()
            } else {
                when (val p = Cards.parse(cardsFile.readText(), entries.keys)) {
                    is Parsed.Rejected -> {
                        return Loaded.Rejected(dir, CARDS, p.code, p.detail)
                    }

                    is Parsed.Accepted -> {
                        warnings += p.warnings
                        p.value
                    }
                }
            }
        return Loaded.Ready(dir, cardSet, cards, warnings)
    }
}
