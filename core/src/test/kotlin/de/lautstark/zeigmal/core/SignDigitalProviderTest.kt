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
}
