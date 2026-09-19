package de.lautstark.zeigmal.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** What the reader is doing: reporting cards, or writing the next sticker it sees. */
sealed interface TagMode {
    data object Read : TagMode

    data class Write(
        val record: CardRecord,
        /** Write even over a sticker that already carries a record. */
        val overwrite: Boolean = false,
    ) : TagMode
}

/** What the reader says about a write: that it began, or how it ended. */
sealed interface WriteEvent {
    val tag: TagId
}

/** The write has begun; the sticker has to stay where it is. */
data class WriteStarted(
    override val tag: TagId,
) : WriteEvent

/** What writing a sticker came to. */
sealed interface WriteOutcome : WriteEvent {
    data class Written(
        override val tag: TagId,
        val record: CardRecord,
    ) : WriteOutcome

    /** The sticker already carries a record; nothing was written. The adult decides. */
    data class AlreadyWritten(
        override val tag: TagId,
        val record: CardRecord,
    ) : WriteOutcome

    data class Failed(
        override val tag: TagId,
        val reason: String,
    ) : WriteOutcome
}

/**
 * The NFC hardware, as the rest of the code sees it. The Android implementation
 * is reader mode on the phone; the fake in the test fixtures is a list of
 * events. Everything above this line is testable without a sticker.
 */
interface TagSource {
    /** Raw reader events, before the presence filter. */
    val tags: Flow<TagEvent>

    /** What writing came to, while the mode is [TagMode.Write]. */
    val writes: Flow<WriteEvent>

    val mode: StateFlow<TagMode>

    fun setMode(mode: TagMode)

    val available: Boolean
    val enabled: Boolean
}

/** A key-value store for the little the app keeps: the login and the writing progress. */
interface KeyValueStore {
    fun get(key: String): String?

    fun put(
        key: String,
        value: String?,
    )
}

/** What the station tells the world about itself, one line at a time, for the log screen. */
fun interface Logger {
    fun log(line: String)
}
