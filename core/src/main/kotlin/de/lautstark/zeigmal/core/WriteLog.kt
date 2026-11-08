package de.lautstark.zeigmal.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * One sticker written. Nothing reads this back to decide anything: the
 * writing mode does not skip, resume or tick by it. It is there to be looked
 * at, and for "how often" beside a card, where a card the family has twice
 * simply counts twice.
 */
@Serializable
data class WriteEntry(
    /** When, in milliseconds since 1970; null for what was taken over from before the log existed. */
    val at: Long?,
    /** What went on the sticker, and what "how often" counts by. */
    val slug: String,
    val label: String,
    /** SIGNdigital's id for the sign, when the card came from a Lernliste. */
    val signId: String? = null,
    /** The list it was written from, by its title; null when it came from a search. */
    val list: String? = null,
    /** The sticker's id as the reader saw it; null for what was taken over. */
    val tag: String? = null,
)

/** Where the written stickers are kept. Appending is all the writing mode does to it. */
interface WriteLog {
    fun append(entry: WriteEntry)

    fun all(): List<WriteEntry>
}

/** For tests, and for a model nobody gave a file. */
class MemoryWriteLog : WriteLog {
    private val entries = mutableListOf<WriteEntry>()

    override fun append(entry: WriteEntry) {
        entries += entry
    }

    override fun all(): List<WriteEntry> = entries.toList()
}

/**
 * One line of JSON per sticker, appended. About 150 bytes a line, so a
 * thousand stickers are 150 KB. A line that does not read (a write cut off by
 * a dead battery) is skipped, not fatal: the log is for looking at.
 */
class FileWriteLog(
    private val file: File,
) : WriteLog {
    override fun append(entry: WriteEntry) {
        try {
            file.parentFile?.mkdirs()
            file.appendText(Json.encodeToString(WriteEntry.serializer(), entry) + "\n", Charsets.UTF_8)
        } catch (e: IOException) {
            // A sticker written and not logged is still a sticker written.
        }
    }

    override fun all(): List<WriteEntry> =
        try {
            if (!file.exists()) {
                emptyList()
            } else {
                file.readLines(Charsets.UTF_8).mapNotNull { line ->
                    try {
                        Json.decodeFromString(WriteEntry.serializer(), line)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
            }
        } catch (e: IOException) {
            emptyList()
        }
}
