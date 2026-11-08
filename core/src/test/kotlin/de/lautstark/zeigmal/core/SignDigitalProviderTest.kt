package de.lautstark.zeigmal.core

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.Base64

class SignDigitalProviderTest {
    private val server = MockWebServer()
    private val store = InMemoryStore()
    private lateinit var provider: SignDigitalProvider

    @Before
    fun start() {
        server.start()
        provider = SignDigitalProvider(store, baseUrl = server.url("/").toString().trimEnd('/'))
    }

    @After
    fun stop() {
        server.close()
    }

    private fun sign(
        video: String?,
        card: String?,
    ): String {
        val edge =
            buildString {
                append("{")
                if (video != null) append("\"signVideo\":{\"default\":\"$video\"}")
                if (video != null && card != null) append(",")
                if (card != null) append("\"signBox\":{\"default\":\"$card\"}")
                append("}")
            }
        return """{"total":1,"data":[{"name":"trinken","slug":"trinken","edge":$edge}]}"""
    }

    @Test
    fun `login keeps the token and the credentials in the store`() =
        runTest {
            server.enqueue(MockResponse(body = """{"accessToken":"tok-1"}"""))
            provider.login("mail@example.org", "pw")
            val request = server.takeRequest()
            assertEquals("/api/authentication", request.url.encodedPath)
            assertTrue(request.body?.utf8()?.contains("\"strategy\":\"local\"") == true)
            assertEquals("tok-1", store.get(SignDigitalProvider.KEY_TOKEN))
            assertEquals("mail@example.org", provider.email)
            assertTrue(provider.loggedIn)
        }

    @Test
    fun `resolve asks for the sign and then for links, with the token`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, "tok-1")
            server.enqueue(MockResponse(body = sign("p/video.mp4", "p/card.png")))
            server.enqueue(MockResponse(body = """["https://cdn/video?sig","https://cdn/card?sig"]"""))
            val media = provider.resolve("trinken")
            assertEquals("https://cdn/video?sig", media.videoUrl)
            assertEquals("https://cdn/card?sig", media.cardImageUrl)
            val first = server.takeRequest()
            assertEquals("/api/signs", first.url.encodedPath)
            assertEquals("trinken", first.url.queryParameter("slug"))
            assertEquals("tok-1", first.headers["Authorization"])
            val second = server.takeRequest()
            assertEquals("/cargo/presigned-url", second.url.encodedPath)
            assertEquals("""[{"file":"p/video.mp4"},{"file":"p/card.png"}]""", second.body?.utf8())
        }

    @Test
    fun `an expired token logs in again once and retries`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, "old")
            store.put(SignDigitalProvider.KEY_EMAIL, "mail@example.org")
            store.put(SignDigitalProvider.KEY_PASSWORD, "pw")
            server.enqueue(MockResponse(code = 401))
            server.enqueue(MockResponse(body = """{"accessToken":"new"}"""))
            server.enqueue(MockResponse(body = sign("p/video.mp4", null)))
            server.enqueue(MockResponse(body = """["https://cdn/video?sig"]"""))
            val media = provider.resolve("trinken")
            assertEquals("https://cdn/video?sig", media.videoUrl)
            assertNull(media.cardImageUrl)
            assertEquals("new", store.get(SignDigitalProvider.KEY_TOKEN))
        }

    @Test
    fun `no sign for the ref is said so, not thrown as a mystery`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, "tok-1")
            server.enqueue(MockResponse(body = """{"total":0,"data":[]}"""))
            try {
                provider.resolve("gibtsnicht")
                fail()
            } catch (e: SignDigitalProvider.NotFound) {
                assertTrue(e.message!!.contains("gibtsnicht"))
            }
        }

    @Test
    fun `not logged in is refused before any request`() =
        runTest {
            try {
                provider.resolve("trinken")
                fail()
            } catch (e: SignDigitalProvider.Refused) {
                assertEquals(0, server.requestCount)
            }
        }

    @Test
    fun `a ref off a sticker is one encoded query parameter, whatever it holds`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, "tok-1")
            server.enqueue(MockResponse(body = """{"total":0,"data":[]}"""))
            try {
                provider.resolve("a&slug=b#c")
                fail()
            } catch (e: SignDigitalProvider.NotFound) {
                // nothing found is the right answer; the question is what was asked
            }
            val request = server.takeRequest()
            assertEquals(listOf("a&slug=b#c"), request.url.queryParameterValues("slug"))
            assertEquals("slug=a%26slug%3Db%23c", request.url.encodedQuery)
        }

    /** A token shaped like the site's: a JWT whose payload names the user in `sub`. */
    private fun jwt(sub: String): String {
        val enc = Base64.getUrlEncoder().withoutPadding()
        val header = enc.encodeToString("""{"alg":"HS256","typ":"access"}""".toByteArray())
        val payload = enc.encodeToString("""{"iat":1,"exp":2,"sub":"$sub","jti":"x"}""".toByteArray())
        return "$header.$payload.sig"
    }

    private fun listPage(
        total: Int,
        vararg names: String,
    ): String =
        names.joinToString(",", prefix = """{"total":$total,"data":[""", postfix = "]}") { name ->
            """{"_id":"id-$name","name":"$name","updatedAt":"2026-11-08T00:00:00Z","signs":[{"_id":"s-$name","slug":"$name","name":"$name"}]}"""
        }

    @Test
    fun `the owner is the token's sub`() {
        assertEquals("6aa052b857086b62332eb0d4", SignDigitalProvider.ownerOf(jwt("6aa052b857086b62332eb0d4")))
    }

    @Test
    fun `a token that is not a JWT is refused, not crashed on`() {
        for (token in listOf("tok-1", "a.!!!.c", "a.${Base64.getUrlEncoder().encodeToString("{}".toByteArray())}.c")) {
            try {
                SignDigitalProvider.ownerOf(token)
                fail(token)
            } catch (e: SignDigitalProvider.Refused) {
                // the app then asks for a new login
            }
        }
    }

    @Test
    fun `lists asks for the owner's lists, newest first, with the token`() =
        runTest {
            val token = jwt("user-1")
            store.put(SignDigitalProvider.KEY_TOKEN, token)
            server.enqueue(MockResponse(body = listPage(2, "Kita", "Essen")))
            val lists = provider.lists()
            assertEquals(listOf("Kita", "Essen"), lists.map { it.name })
            val request = server.takeRequest()
            assertEquals("/api/lists", request.url.encodedPath)
            assertEquals("user-1", request.url.queryParameter("owner"))
            assertEquals("users", request.url.queryParameter("ownerType"))
            assertEquals("-1", request.url.queryParameter("\$sort[updatedAt]"))
            assertEquals("0", request.url.queryParameter("\$skip"))
            assertEquals(token, request.headers["Authorization"])
            assertEquals("GET", request.method)
        }

    @Test
    fun `lists reads on until it has them all`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, jwt("user-1"))
            server.enqueue(MockResponse(body = listPage(3, "a", "b")))
            server.enqueue(MockResponse(body = listPage(3, "c")))
            assertEquals(listOf("a", "b", "c"), provider.lists().map { it.name })
            assertEquals("0", server.takeRequest().url.queryParameter("\$skip"))
            assertEquals("2", server.takeRequest().url.queryParameter("\$skip"))
        }

    @Test
    fun `lists stops at an empty page even when total says more`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, jwt("user-1"))
            server.enqueue(MockResponse(body = listPage(5, "a")))
            server.enqueue(MockResponse(body = """{"total":5,"data":[]}"""))
            assertEquals(listOf("a"), provider.lists().map { it.name })
            assertEquals(2, server.requestCount)
        }

    @Test
    fun `lists with an expired token logs in again once`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, jwt("user-1"))
            store.put(SignDigitalProvider.KEY_EMAIL, "mail@example.org")
            store.put(SignDigitalProvider.KEY_PASSWORD, "pw")
            server.enqueue(MockResponse(code = 401))
            server.enqueue(MockResponse(body = """{"accessToken":"${jwt("user-1")}"}"""))
            server.enqueue(MockResponse(body = listPage(1, "Kita")))
            assertEquals(listOf("Kita"), provider.lists().map { it.name })
        }
}
