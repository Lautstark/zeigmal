package de.lautstark.zeigmal.cardset

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Reads `manifest.json`: the entries and their media. See docs/media-model.md. */
object Manifest {
    const val FORMAT = "zeigmal.kartensatz"
    const val VERSION = 1

    private val ID = Regex("^[a-z0-9][a-z0-9-]{0,63}$")

    fun parse(text: String): Parsed<CardSet> {
        val root =
            when (val r = Reader.root(text)) {
                is Parsed.Rejected -> return r
                is Parsed.Accepted -> r.value
            }
        Reader.header(root, FORMAT, VERSION)?.let { return it }

        val name = root.str("name") ?: return Parsed.Rejected(RejectionCode.INVALID, "name missing")
        val redistributable =
            root.bool("redistributable")
                ?: return Parsed.Rejected(RejectionCode.INVALID, "redistributable missing; say false unless you know")
        val entriesJson = root.array("entries") ?: return Parsed.Rejected(RejectionCode.INVALID, "entries missing")

        val warnings = mutableListOf<Warning>()
        val entries = LinkedHashMap<String, MediaEntry>()
        entriesJson.forEachIndexed { index, element ->
            val obj = element.asObject()
            if (obj == null) {
                warnings += Warning(WarningCode.ENTRY_INVALID, "entries[$index] is not an object")
                return@forEachIndexed
            }
            when (val e = entry(obj, "entries[$index]", warnings)) {
                null -> {
                    Unit
                }

                else -> {
                    if (entries.containsKey(e.id)) {
                        warnings += Warning(WarningCode.ENTRY_DUPLICATE, "entries[$index]: id ${e.id} appears twice, first one kept")
                    } else {
                        entries[e.id] = e
                    }
                }
            }
        }

        return Parsed.Accepted(
            CardSet(
                name = name,
                modified = root.str("modified"),
                redistributable = redistributable,
                voice = root.str("voice"),
                entries = entries,
            ),
            warnings,
        )
    }

    private fun entry(
        obj: JsonObject,
        where: String,
        warnings: MutableList<Warning>,
    ): MediaEntry? {
        val id = obj.str("id")
        if (id == null || !ID.matches(id)) {
            warnings += Warning(WarningCode.ENTRY_INVALID, "$where: id ${id ?: "missing"} is not a lowercase slug")
            return null
        }
        val label = obj.str("label")
        if (label.isNullOrBlank()) {
            warnings += Warning(WarningCode.ENTRY_INVALID, "$where ($id): label missing")
            return null
        }
        val video = media(obj["video"])
        if (video == null) {
            warnings += Warning(WarningCode.ENTRY_INVALID, "$where ($id): video missing or not a relative path")
            return null
        }
        val speechName = obj.str("speech")
        val speech =
            when (speechName) {
                "video" -> {
                    SpeechMode.VIDEO
                }

                "external" -> {
                    SpeechMode.EXTERNAL
                }

                "none" -> {
                    SpeechMode.NONE
                }

                else -> {
                    warnings +=
                        Warning(
                            WarningCode.ENTRY_INVALID,
                            "$where ($id): speech is ${speechName ?: "missing"}; must be video, external or none",
                        )
                    return null
                }
            }
        val audioObj = obj.obj("audio")
        val audioFile = media(obj["audio"])
        val audio =
            audioFile?.let {
                Audio(
                    file = it,
                    text = audioObj?.str("text"),
                    voice = audioObj?.str("voice"),
                    key = audioObj?.str("key"),
                )
            }
        var mode = speech
        if (mode == SpeechMode.EXTERNAL && audio == null) {
            warnings += Warning(WarningCode.ENTRY_WITHOUT_AUDIO, "$where ($id): speech is external but there is no audio; plays silent")
            mode = SpeechMode.NONE
        }
        val videoObj = obj.obj("video")
        val source =
            videoObj
                ?.let {
                    MediaSource(
                        provider = it.str("provider"),
                        licence = it.str("licence"),
                        attribution = it.str("attribution"),
                        url = it.str("url"),
                    )
                }?.takeIf { it != MediaSource() }
        val symbolObj = obj.obj("symbol")
        val symbol =
            symbolObj
                ?.let {
                    SymbolRef(
                        metacom = it.str("metacom"),
                        arasaac = it.int("arasaac"),
                        file = media(it["file"]),
                    )
                }?.takeIf { it != SymbolRef() }
        return MediaEntry(
            id = id,
            label = label,
            video = video,
            videoSource = source,
            speech = mode,
            audio = audio,
            symbol = symbol,
        )
    }

    /**
     * A media reference is a relative path, written either bare (`"videos/trinken.mp4"`)
     * or as an object with a `file` and facts about it. Absolute paths and `..` are
     * refused: the Kartensatz is a directory and nothing may point out of it.
     */
    private fun media(element: kotlinx.serialization.json.JsonElement?): String? {
        val path =
            when (element) {
                is JsonPrimitive -> element.takeIf { it.isString }?.contentOrNull
                is JsonObject -> element.str("file")
                else -> null
            } ?: return null
        return path.takeIf { RelativePath.isSafe(it) }
    }
}

internal object RelativePath {
    fun isSafe(path: String): Boolean {
        if (path.isBlank() || path.startsWith("/") || path.startsWith("\\")) return false
        if (path.contains("\\")) return false
        return path.split('/').none { it.isEmpty() || it == "." || it == ".." }
    }
}
