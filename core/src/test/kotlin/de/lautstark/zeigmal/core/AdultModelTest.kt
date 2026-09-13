package de.lautstark.zeigmal.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdultModelTest {
    private val server = MockWebServer()
    private val store = InMemoryStore()
    private val logger = ListLogger()

    @Before
    fun start() {
        server.start()
    }

    private fun kotlinx.coroutines.test.TestScope.provider(): SignDigitalProvider =
        SignDigitalProvider(store, baseUrl = server.url("/").toString().trimEnd('/'), io = UnconfinedTestDispatcher(testScheduler))

    @After
    fun stop() = server.close()

    private fun sign(card: String) = """{"total":1,"data":[{"name":"x","slug":"x","edge":{"signBox":{"default":"$card"}}}]}"""

    @Test
    fun `login is state, and a failure says why`() =
        runTest {
            val m = AdultModel(this, provider(), store, logger)
            assertEquals(Login.Out, m.login.value)
            server.enqueue(MockResponse(code = 401))
            m.login("mail@example.org", "wrong")
            advanceUntilIdle()
            assertTrue(m.login.value is Login.Failed)
            server.enqueue(MockResponse(body = """{"accessToken":"tok"}"""))
            server.enqueue(MockResponse(body = sign("p/abend.png")))
            server.enqueue(MockResponse(body = """["https://cdn/abend?sig"]"""))
            m.login("mail@example.org", "right")
            advanceUntilIdle()
            assertEquals(Login.In("mail@example.org"), m.login.value)
            assertEquals("https://cdn/abend?sig", m.writing.value.cardImageUrl)
        }

    @Test
    fun `where the writing stopped, and which cards are done, survive a restart`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, "tok")
            val m = AdultModel(this, provider(), store, logger)
            m.goTo(5)
            m.onWrite(WriteOutcome.Written(TagId("04AABBCCDDEEFF"), m.writing.value.record))
            advanceUntilIdle()
            val again = AdultModel(this, provider(), store, logger)
            assertEquals(6, again.writing.value.index)
            assertEquals(setOf(SignBox.box1[5].ref), again.writing.value.written)
        }

    @Test
    fun `the reader writes the current card only while logged in and writing`() =
        runTest {
            val m = AdultModel(this, provider(), store, logger)
            assertEquals(TagMode.Read, m.tagMode(active = true))
            store.put(SignDigitalProvider.KEY_TOKEN, "tok")
            store.put(SignDigitalProvider.KEY_EMAIL, "mail@example.org")
            val logged = AdultModel(this, provider(), store, logger)
            assertEquals(TagMode.Write(CardRecord("signdigital", "abend-s", "Abend(s)")), logged.tagMode(active = true))
            assertEquals(TagMode.Read, logged.tagMode(active = false))
        }

    @Test
    fun `a written sticker moves the box on and is remembered`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, "tok")
            store.put(SignDigitalProvider.KEY_EMAIL, "mail@example.org")
            val m = AdultModel(this, provider(), store, logger)
            server.enqueue(MockResponse(body = sign("p/aber.png")))
            server.enqueue(MockResponse(body = """["https://cdn/aber?sig"]"""))
            m.onWrite(WriteOutcome.Written(TagId("04c5d4a88d2681"), m.writing.value.record))
            advanceUntilIdle()
            assertEquals(1, m.writing.value.index)
            assertEquals("aber", m.writing.value.word.label)
            assertEquals(setOf("abend-s"), m.writing.value.written)
            assertEquals("1", store.get(AdultModel.KEY_WRITE_INDEX))
            assertEquals("abend-s", store.get(AdultModel.KEY_WRITTEN))
            assertEquals("https://cdn/aber?sig", m.writing.value.cardImageUrl)
            // A fresh model picks up where this one left off.
            val again = AdultModel(this, provider(), store, logger)
            assertEquals(1, again.writing.value.index)
            assertEquals(setOf("abend-s"), again.writing.value.written)
        }

    @Test
    fun `a sticker that is already written waits for the adult, and overwrite is one write`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, "tok")
            store.put(SignDigitalProvider.KEY_EMAIL, "mail@example.org")
            val m = AdultModel(this, provider(), store, logger)
            val existing = CardRecord("signdigital", "essen", "essen")
            m.onWrite(WriteOutcome.AlreadyWritten(TagId("04c4d4a88d2681"), existing))
            assertTrue(m.writing.value.lastOutcome is WriteOutcome.AlreadyWritten)
            assertEquals(0, m.writing.value.index)
            m.overwriteNext()
            assertEquals(true, (m.tagMode(active = true) as TagMode.Write).overwrite)
            server.enqueue(MockResponse(body = sign("p/aber.png")))
            server.enqueue(MockResponse(body = """["https://cdn/aber?sig"]"""))
            m.onWrite(WriteOutcome.Written(TagId("04c4d4a88d2681"), m.writing.value.record))
            advanceUntilIdle()
            assertEquals(false, (m.tagMode(active = true) as TagMode.Write).overwrite)
            assertEquals(1, m.writing.value.index)
        }
}
