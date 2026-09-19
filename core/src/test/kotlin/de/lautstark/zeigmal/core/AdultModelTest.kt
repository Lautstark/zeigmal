package de.lautstark.zeigmal.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
    fun `a write is shown long enough to be seen, then written for a moment, then the next card`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, "tok")
            val m = AdultModel(this, provider(), store, logger)
            val tag = TagId("04c5d4a88d2681")
            m.onWrite(WriteStarted(tag))
            assertEquals(WriteStatus.Busy(tag), m.writing.value.status)
            assertEquals(true, m.writing.value.busy)
            // The hardware is done in 100 ms; the screen is not.
            val written = WriteOutcome.Written(tag, m.writing.value.record)
            m.onWrite(written)
            advanceTimeBy(100)
            assertEquals(WriteStatus.Busy(tag), m.writing.value.status)
            advanceTimeBy(AdultModel.MIN_BUSY_MILLIS)
            assertEquals(WriteStatus.Done(written), m.writing.value.status)
            // The box has moved on for the reader, the screen still shows what was written.
            assertEquals(1, m.writing.value.index)
            assertEquals("Abend(s)", m.writing.value.shownLabel)
            assertEquals("aber", m.writing.value.word.label)
            server.enqueue(MockResponse(body = sign("p/aber.png")))
            server.enqueue(MockResponse(body = """["https://cdn/aber?sig"]"""))
            advanceTimeBy(AdultModel.DONE_MILLIS + 1)
            advanceUntilIdle()
            assertEquals(WriteStatus.Waiting, m.writing.value.status)
            assertEquals("aber", m.writing.value.shownLabel)
            assertEquals("https://cdn/aber?sig", m.writing.value.cardImageUrl)
        }

    @Test
    fun `the sticker just written, still lying there, is no stranger`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, "tok")
            val m = AdultModel(this, provider(), store, logger)
            val tag = TagId("04c5d4a88d2681")
            val record = m.writing.value.record
            m.onWrite(WriteOutcome.Written(tag, record))
            // Three times a second, for as long as it lies there.
            repeat(5) {
                advanceTimeBy(300)
                m.onWrite(WriteOutcome.AlreadyWritten(tag, record))
            }
            assertEquals(1, m.writing.value.index)
            assertTrue(m.writing.value.status !is WriteStatus.Already)
            // A stranger is a stranger at once.
            m.onWrite(WriteOutcome.AlreadyWritten(TagId("04ffffffffffff"), CardRecord("signdigital", "essen", "essen")))
            assertTrue(m.writing.value.status is WriteStatus.Already)
            m.overwriteNext()
            // And the same sticker, put back after a while, is a question again.
            advanceTimeBy(AdultModel.SAME_STICKER_MILLIS + 1)
            m.onWrite(WriteOutcome.AlreadyWritten(tag, record))
            assertTrue(m.writing.value.status is WriteStatus.Already)
        }

    @Test
    fun `a failed write stays on the card and Nochmal waits again`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, "tok")
            store.put(SignDigitalProvider.KEY_EMAIL, "mail@example.org")
            val m = AdultModel(this, provider(), store, logger)
            val tag = TagId("04c5d4a88d2681")
            m.onWrite(WriteStarted(tag))
            m.onWrite(WriteOutcome.Failed(tag, "Tag was lost"))
            advanceTimeBy(AdultModel.MIN_BUSY_MILLIS + 1)
            assertEquals(WriteStatus.Failed(WriteOutcome.Failed(tag, "Tag was lost")), m.writing.value.status)
            assertEquals(0, m.writing.value.index)
            assertEquals(emptySet<String>(), m.writing.value.written)
            m.retry()
            assertEquals(WriteStatus.Waiting, m.writing.value.status)
            assertEquals(TagMode.Write(m.writing.value.record), m.tagMode(active = true))
        }

    @Test
    fun `a sticker that is already written waits for the adult, and overwrite is one write`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, "tok")
            store.put(SignDigitalProvider.KEY_EMAIL, "mail@example.org")
            val m = AdultModel(this, provider(), store, logger)
            val existing = CardRecord("signdigital", "essen", "essen")
            m.onWrite(WriteOutcome.AlreadyWritten(TagId("04c4d4a88d2681"), existing))
            assertTrue(m.writing.value.status is WriteStatus.Already)
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
