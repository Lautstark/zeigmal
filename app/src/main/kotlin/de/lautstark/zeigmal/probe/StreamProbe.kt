package de.lautstark.zeigmal.probe

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Experiment S11: how long from "card seen" to the first frame, when the clip
 * is streamed rather than read from a file. This runs the exact chain the
 * site's own player runs, against a public sign, without a login and without
 * keeping a byte. It exists on this branch to produce one table in
 * docs/experiments.md and for nothing else.
 */
object StreamProbe {
    private const val BASE = "https://sign-digital.de"
    const val PUBLIC_SLUG = "schon"

    data class Link(
        val url: String,
        val lookupMs: Long,
        val linkMs: Long,
    )

    /** The sign's storage path, then a signed link for it: two round trips. */
    suspend fun signedLink(slug: String = PUBLIC_SLUG): Link =
        withContext(Dispatchers.IO) {
            val t0 = System.nanoTime()
            val sign = JSONObject(get("$BASE/api/signs?slug=$slug"))
            val path =
                sign
                    .getJSONArray("data")
                    .getJSONObject(0)
                    .getJSONObject("edge")
                    .getJSONObject("signVideo")
                    .let { it.optString("default").ifEmpty { it.getString("watermarked") } }
            val t1 = System.nanoTime()
            val body = JSONArray().put(JSONObject().put("file", path)).toString()
            val links = JSONArray(post("$BASE/cargo/presigned-url", body))
            val t2 = System.nanoTime()
            Link(links.getString(0), (t1 - t0) / 1_000_000, (t2 - t1) / 1_000_000)
        }

    private fun get(url: String): String =
        (URL(url).openConnection() as HttpURLConnection).run {
            setRequestProperty("Accept", "application/json")
            connectTimeout = 10_000
            readTimeout = 10_000
            inputStream.bufferedReader().use { it.readText() }
        }

    private fun post(
        url: String,
        json: String,
    ): String =
        (URL(url).openConnection() as HttpURLConnection).run {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 10_000
            readTimeout = 10_000
            outputStream.use { it.write(json.toByteArray()) }
            inputStream.bufferedReader().use { it.readText() }
        }
}
