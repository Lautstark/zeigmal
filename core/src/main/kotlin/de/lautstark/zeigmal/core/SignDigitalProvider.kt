package de.lautstark.zeigmal.core

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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
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
 * live in the [CredentialStore] the app hands in, and nowhere else.
 */
class SignDigitalProvider(
    private val credentials: CredentialStore,
    private val baseUrl: String = "https://sign-digital.de",
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
    ) = withContext(Dispatchers.IO) {
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

    override suspend fun resolve(ref: String): Media =
        withContext(Dispatchers.IO) {
            try {
                media(ref, token())
            } catch (e: Refused) {
                // A token that has run out: log in again, once, with what was saved.
                val email = credentials.get(KEY_EMAIL)
                val password = credentials.get(KEY_PASSWORD)
                if (email == null || password == null) throw e
                credentials.put(KEY_TOKEN, null)
                login(email, password)
                media(ref, token())
            }
        }

    private fun token(): String = credentials.get(KEY_TOKEN) ?: throw Refused("nicht angemeldet")

    private fun media(
        ref: String,
        token: String,
    ): Media {
        val page = request("GET", "$baseUrl/api/signs?slug=$ref", null, token).jsonObject
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
            else -> builder.get()
        }
        client.newCall(builder.build()).execute().use { response ->
            val text = response.body.string()
            when {
                response.code == 401 || response.code == 403 -> throw Refused("${response.code} für $method ${url.substringBefore('?')}")
                !response.isSuccessful -> throw IOException("HTTP ${response.code} für $method ${url.substringBefore('?')}")
            }
            return Json.parseToJsonElement(text)
        }
    }

    companion object {
        const val ID = "signdigital"
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
