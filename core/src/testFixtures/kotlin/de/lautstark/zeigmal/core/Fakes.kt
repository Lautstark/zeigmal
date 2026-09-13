package de.lautstark.zeigmal.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** A reader that reports whatever a test hands it, and remembers what it was asked to do. */
class FakeTagSource(
    override val available: Boolean = true,
    override val enabled: Boolean = true,
) : TagSource {
    private val _tags = MutableSharedFlow<TagEvent>(extraBufferCapacity = 64)
    private val _writes = MutableSharedFlow<WriteOutcome>(extraBufferCapacity = 64)
    private val _mode = MutableStateFlow<TagMode>(TagMode.Read)
    override val tags: SharedFlow<TagEvent> = _tags
    override val writes: SharedFlow<WriteOutcome> = _writes
    override val mode: StateFlow<TagMode> = _mode
    val modes = mutableListOf<TagMode>()

    override fun setMode(mode: TagMode) {
        _mode.value = mode
        modes += mode
    }

    fun seen(
        tag: TagId,
        record: CardRecord?,
    ) = check(_tags.tryEmit(TagEvent.Seen(tag, record, listOf("NfcA"))))

    fun gone(tag: TagId) = check(_tags.tryEmit(TagEvent.Gone(tag)))

    fun wrote(outcome: WriteOutcome) = check(_writes.tryEmit(outcome))
}

/** A provider with a fixed answer per ref, and a record of what was asked. */
class FakeProvider(
    override val id: String = "fake",
    private val media: Map<String, Media> = emptyMap(),
) : Provider {
    val asked = mutableListOf<String>()

    override suspend fun resolve(ref: String): Media {
        asked += ref
        return media[ref] ?: throw IllegalStateException("kein Zeichen mit der Kennung $ref")
    }
}

class InMemoryStore : KeyValueStore {
    val map = HashMap<String, String>()

    override fun get(key: String): String? = map[key]

    override fun put(
        key: String,
        value: String?,
    ) {
        if (value == null) map.remove(key) else map[key] = value
    }
}

class ListLogger : Logger {
    val lines = mutableListOf<String>()

    override fun log(line: String) {
        lines += line
    }
}
