package de.lautstark.zeigmal.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * SIGNdigital as a [Provider]: a card's ref is the sign's slug there, and the
 * clip is played from a link the site signs when asked. Nothing is written to
 * disk — their terms allow watching through a subscription and do not allow
 * keeping the file, and this class is the line between the two.
 *
 * It speaks to the three endpoints the site's own player speaks to (seen
 * 2026-09-09): `POST /api/authentication` with the local strategy for a token,
 * `GET /api/signs?slug=…` for the sign (the only lookup the server allows), and
 * `POST /cargo/presigned-url` for links that expire. The login and the token
 * live in the [KeyValueStore] the app hands in, and nowhere else.
 */
class SignDigitalProvider(
    private val credentials: KeyValueStore,
    private val baseUrl: String = "https://sign-digital.de",
    /** Where the blocking calls run; a test passes its own so virtual time sees them. */
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val client: OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build(),
) : Provider {
    override val id: String = ID

    class Refused(
        message: String,
    ) : IOException(message)

    class NotFound(
        message: String,
    ) : IOException(message)

    val email: String? get() = credentials.get(KEY_EMAIL)
    val loggedIn: Boolean get() = credentials.get(KEY_TOKEN) != null

    suspend fun login(
        email: String,
        password: String,
    ) = withContext(io) {
        val body =
            buildJsonObject {
                put("strategy", JsonPrimitive("local"))
                put("email", JsonPrimitive(email))
                put("password", JsonPrimitive(password))
            }
        val result = request("POST", "$baseUrl/api/authentication", body.toString(), token = null).jsonObject
        val token = result["accessToken"]?.jsonPrimitive?.content ?: throw IOException("Antwort ohne Token")
        credentials.put(KEY_EMAIL, email)
        credentials.put(KEY_PASSWORD, password)
        credentials.put(KEY_TOKEN, token)
    }

    fun logout() {
        credentials.put(KEY_EMAIL, null)
        credentials.put(KEY_PASSWORD, null)
        credentials.put(KEY_TOKEN, null)
    }

    override suspend fun resolve(ref: String): Media = authed { token -> media(ref, token) }

    /**
     * The Lernlisten of the account the app is logged in with, newest first.
     * The owner is the token's
     * `sub`, which is the user id the site's own list page asks with (checked
     * 2026-11-08), so a login from before this existed needs nothing new.
     */
    suspend fun lists(): List<Lernliste> =
        authed { token ->
            val owner = ownerOf(token)
            val lists = mutableListOf<Lernliste>()
            do {
                val url =
                    "$baseUrl/api/lists"
                        .toHttpUrl()
                        .newBuilder()
                        .addQueryParameter("owner", owner)
                        .addQueryParameter("ownerType", "users")
                        .addQueryParameter("\$limit", PAGE.toString())
                        .addQueryParameter("\$skip", lists.size.toString())
                        .addQueryParameter("\$sort[updatedAt]", "-1")
                        .build()
                        .toString()
                val json = request("GET", url, null, token).jsonObject
                val page = Lernliste.page(json)
                lists += page
                val total = json["total"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            } while (page.isNotEmpty() && lists.size < total)
            lists
        }

    /**
     * Signs whose name holds [text], as the site's own search finds them. It
     * wants the login since 2026-10-31.
     */
    suspend fun search(text: String): List<ListSign> =
        authed { token ->
            val url =
                "$baseUrl/search/query"
                    .toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("text", text)
                    .addQueryParameter("categories", "alle")
                    .addQueryParameter("\$skip", "0")
                    .addQueryParameter("\$limit", SEARCH_LIMIT.toString())
                    .build()
                    .toString()
            Lernliste.signs(request("GET", url, null, token).jsonObject)
        }

    /**
     * Changes which signs a list holds, on the site. The site takes a list back
     * whole (measured 2026-11-08), so it is read fresh right before, [change] is
     * applied to the sign ids read then, and it is read once more after: what
     * comes back is the list as the site now has it, or this throws. [change]
     * must not mind running twice, since a token that ran out runs it again.
     */
    suspend fun changeSigns(
        listId: String,
        change: (List<String>) -> List<String>,
    ): Lernliste =
        authed { token ->
            val doc = listDoc(listId, token)
            val before = ids(doc["signs"])
            val after = change(before)
            if (after != before) {
                val body =
                    JsonObject(
                        doc +
                            mapOf(
                                "signs" to JsonArray(after.map(::JsonPrimitive)),
                                "learnedSigns" to JsonArray(ids(doc["learnedSigns"]).map(::JsonPrimitive)),
                            ),
                    )
                request("PATCH", listUrl(listId), body.toString(), token)
            }
            val fresh = listDoc(listId, token)
            if (ids(fresh["signs"]) != after) throw IOException("Die Liste ist nach dem Speichern anders als gedacht")
            Lernliste.one(fresh) ?: throw IOException("Liste ohne Kennung")
        }

    /**
     * Deletes a list on the site. The site deletes the list's entry in its
     * folders, and the list goes with it (measured 2026-11-08: the list is 404
     * after); so does this, and checks it is gone.
     */
    suspend fun deleteList(listId: String) =
        authed { token ->
            val node = (listDoc(listId, token)["node"] as? JsonPrimitive)?.content ?: throw IOException("Liste ohne Ordner-Eintrag")
            request("DELETE", "$baseUrl/api/file-nodes/${node.urlSegment()}", null, token)
            try {
                listDoc(listId, token)
                throw IOException("Die Liste ist nach dem Löschen noch da")
            } catch (e: NotFound) {
                // Gone, as it should be.
            }
        }

    private fun listUrl(listId: String) = "$baseUrl/api/lists/${listId.urlSegment()}"

    private fun listDoc(
        listId: String,
        token: String,
    ): JsonObject = request("GET", listUrl(listId), null, token).jsonObject

    /** A list's signs come back filled in and go out as ids; either reads as the ids. */
    private fun ids(signs: kotlinx.serialization.json.JsonElement?): List<String> =
        (signs as? JsonArray).orEmpty().mapNotNull { sign ->
            when (sign) {
                is JsonPrimitive -> sign.content
                is JsonObject -> (sign["_id"] as? JsonPrimitive)?.content
                else -> null
            }
        }

    /** An id from the site goes into a path: anything but letters and digits would make it another path. */
    private fun String.urlSegment(): String = also { require(all(Char::isLetterOrDigit)) { "keine Kennung: $it" } }

    /** Runs [call] with the saved token; a token that has run out logs in again, once, with what was saved. */
    private suspend fun <T> authed(call: (String) -> T): T =
        withContext(io) {
            try {
                call(token())
            } catch (e: Refused) {
                val email = credentials.get(KEY_EMAIL)
                val password = credentials.get(KEY_PASSWORD)
                if (email == null || password == null) throw e
                credentials.put(KEY_TOKEN, null)
                login(email, password)
                call(token())
            }
        }

    private fun token(): String = credentials.get(KEY_TOKEN) ?: throw Refused("nicht angemeldet")

    private fun media(
        ref: String,
        token: String,
    ): Media {
        // The ref comes off a sticker anyone can write, so it is a query
        // parameter, encoded, and never text pasted into the URL: a "&" or "#"
        // in it would otherwise ask the server a different question.
        val signs =
            "$baseUrl/api/signs"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("slug", ref)
                .build()
                .toString()
        val page = request("GET", signs, null, token).jsonObject
        val sign = page["data"]?.jsonArray?.firstOrNull()?.jsonObject ?: throw NotFound("kein Zeichen mit der Kennung $ref")
        val edge = sign["edge"]?.jsonObject ?: JsonObject(emptyMap())
        val videoPath = edge["signVideo"]?.jsonObject?.let { pick(it, VIDEO_VARIANTS) }
        val cardPath = edge["signBox"]?.jsonObject?.let { pick(it, IMAGE_VARIANTS) }
        val paths = listOfNotNull(videoPath, cardPath)
        if (paths.isEmpty()) return Media(null, null)
        val body = buildJsonArray { paths.forEach { add(buildJsonObject { put("file", JsonPrimitive(it)) }) } }
        val links = request("POST", "$baseUrl/cargo/presigned-url", body.toString(), token).jsonArray.map { it.jsonPrimitive.content }
        val byPath = paths.zip(links).toMap()
        return Media(videoUrl = videoPath?.let(byPath::get), cardImageUrl = cardPath?.let(byPath::get))
    }

    private fun pick(
        variants: JsonObject,
        order: List<String>,
    ): String? = order.firstNotNullOfOrNull { key -> variants[key]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() } }

    private fun request(
        method: String,
        url: String,
        body: String?,
        token: String?,
    ): kotlinx.serialization.json.JsonElement {
        val builder = Request.Builder().url(url).header("Accept", "application/json")
        if (token != null) builder.header("Authorization", token)
        when (method) {
            "POST" -> builder.post((body ?: "").toRequestBody(JSON))
            "PATCH" -> builder.patch((body ?: "").toRequestBody(JSON))
            "DELETE" -> builder.delete()
            else -> builder.get()
        }
        client.newCall(builder.build()).execute().use { response ->
            val text = response.body.string()
            when {
                response.code == 401 || response.code == 403 -> throw Refused("${response.code} für $method ${url.substringBefore('?')}")
                response.code == 404 -> throw NotFound("404 für $method ${url.substringBefore('?')}")
                !response.isSuccessful -> throw IOException("HTTP ${response.code} für $method ${url.substringBefore('?')}")
            }
            // A DELETE may answer with nothing.
            return if (text.isBlank()) kotlinx.serialization.json.JsonNull else Json.parseToJsonElement(text)
        }
    }

    companion object {
        const val ID = "signdigital"
        private const val PAGE = 50
        private const val SEARCH_LIMIT = 30

        /** The user id in a site token: the `sub` of its JWT payload. */
        fun ownerOf(token: String): String {
            val payload = token.split('.').getOrNull(1) ?: throw Refused("Token ohne Inhalt")
            // Bad base64, a payload that is not JSON, or JSON that is not an
            // object all throw IllegalArgumentException (SerializationException is one).
            val sub =
                try {
                    val text = String(Base64.getUrlDecoder().decode(payload.trimEnd('=')), Charsets.UTF_8)
                    Json
                        .parseToJsonElement(text)
                        .jsonObject["sub"]
                        ?.jsonPrimitive
                        ?.content
                } catch (e: IllegalArgumentException) {
                    throw Refused("Token nicht lesbar")
                }
            return sub?.takeIf { it.isNotEmpty() } ?: throw Refused("Token ohne Nutzer")
        }

        private val JSON = "application/json; charset=utf-8".toMediaType()

        // Seen in a subscriber's JSON on 2026-09-09: "default" is what their own
        // player plays; anonymous users only get "watermarked".
        private val VIDEO_VARIANTS = listOf("default", "medium", "original", "small", "watermarked")
        private val IMAGE_VARIANTS = listOf("default", "original", "small", "tiny")

        const val KEY_EMAIL = "signdigital.email"
        const val KEY_PASSWORD = "signdigital.password"
        const val KEY_TOKEN = "signdigital.token"
    }
}
