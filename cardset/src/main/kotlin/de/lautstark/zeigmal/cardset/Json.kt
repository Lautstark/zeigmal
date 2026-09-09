package de.lautstark.zeigmal.cardset

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/**
 * The manifest is walked by hand rather than bound to data classes, so that an
 * unknown field is ignored (a newer minor version may add one) and a wrong type
 * is a warning on one entry rather than an exception for the whole file.
 */
internal object Reader {
    fun root(text: String): Parsed<JsonObject> {
        val element =
            try {
                Json.parseToJsonElement(text)
            } catch (e: SerializationException) {
                return Parsed.Rejected(RejectionCode.NOT_JSON, e.message ?: "not JSON")
            } catch (e: IllegalArgumentException) {
                return Parsed.Rejected(RejectionCode.NOT_JSON, e.message ?: "not JSON")
            }
        val obj = element as? JsonObject ?: return Parsed.Rejected(RejectionCode.INVALID, "top level is not an object")
        return Parsed.Accepted(obj, emptyList())
    }

    /** Checks `format` and `version`, the two fields every Lautstark file starts with. */
    fun header(
        root: JsonObject,
        format: String,
        version: Int,
    ): Parsed.Rejected? {
        val actual = root.str("format")
        if (actual != format) {
            return Parsed.Rejected(RejectionCode.WRONG_FORMAT, "format is ${actual ?: "missing"}, expected $format")
        }
        val v = root.int("version") ?: return Parsed.Rejected(RejectionCode.INVALID, "version missing")
        if (v > version) {
            return Parsed.Rejected(RejectionCode.VERSION_TOO_NEW, "version $v, this player reads up to $version")
        }
        return null
    }
}

internal fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

internal fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull

internal fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull

internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

internal fun JsonObject.array(key: String): JsonArray? = this[key] as? JsonArray

internal fun JsonElement.asObject(): JsonObject? = this as? JsonObject
