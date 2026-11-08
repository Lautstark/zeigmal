package de.lautstark.zeigmal.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Base64

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
    fun `every sticker written goes into the log, with when, what, where and which sticker`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, "tok")
            val log = MemoryWriteLog()
            val m = AdultModel(this, provider(), store, logger, log, clock = { 1_000L })
            m.goTo(5)
            val word = m.writing.value.word
            m.onWrite(WriteOutcome.Written(TagId("04AABBCCDDEEFF"), m.writing.value.record))
            advanceUntilIdle()
            assertEquals(listOf(WriteEntry(1_000L, word.ref, word.label, null, "SIGNbox 1", "04AABBCCDDEEFF")), log.all())
            // Nothing else is kept: a fresh model starts at the first card and only counts.
            val again = AdultModel(this, provider(), store, logger, log)
            assertEquals(0, again.writing.value.index)
            assertEquals(1, again.writing.value.count(word))
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
    fun `a written sticker moves the box on and is counted`() =
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
            assertEquals(mapOf("abend-s" to 1), m.writing.value.counts)
            assertEquals("https://cdn/aber?sig", m.writing.value.cardImageUrl)
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
            assertEquals(emptyMap<String, Int>(), m.writing.value.counts)
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

    @Test
    fun `an overwritten sticker rediscovered during the busy hold is still one word`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, "tok")
            store.put(SignDigitalProvider.KEY_EMAIL, "mail@example.org")
            val m = AdultModel(this, provider(), store, logger)
            m.goTo(5)
            advanceUntilIdle()
            val tag = TagId("04aabbccddeeff")
            m.onWrite(WriteOutcome.AlreadyWritten(tag, CardRecord("signdigital", "essen", "essen")))
            m.overwriteNext()
            val record = m.writing.value.record
            // The first discovery writes; "Schreibt" holds the outcome for 400 ms.
            m.onWrite(WriteStarted(tag))
            advanceTimeBy(100)
            m.onWrite(WriteOutcome.Written(tag, record))
            // The A51 sees the resting sticker again ~290 ms after the first
            // discovery, inside that hold: the reader is still in overwrite mode
            // for the same card and writes it a second time.
            advanceTimeBy(190)
            assertEquals(TagMode.Write(record, overwrite = true), m.tagMode(active = true))
            m.onWrite(WriteStarted(tag))
            advanceTimeBy(100)
            m.onWrite(WriteOutcome.Written(tag, record))
            advanceUntilIdle()
            assertEquals("one sticker, one word", 6, m.writing.value.index)
            assertEquals(WriteStatus.Waiting, m.writing.value.status)
            assertEquals(false, (m.tagMode(active = true) as TagMode.Write).overwrite)
        }

    @Test
    fun `a write for a card the box has already left does not move it again`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, "tok")
            val m = AdultModel(this, provider(), store, logger)
            m.goTo(5)
            val stale = m.writing.value.record
            m.skip()
            m.onWrite(WriteOutcome.Written(TagId("04c4d4a88d2681"), stale))
            advanceUntilIdle()
            assertEquals(6, m.writing.value.index)
            assertEquals(1, m.writing.value.counts[stale.ref])
        }

    @Test
    fun `a stack opens on its first card, and nothing is kept of where it stopped`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, "tok")
            val m = AdultModel(this, provider(), store, logger)
            m.goTo(5)
            m.chooseSet(SignBox.HALLOWEEN)
            assertEquals(0, m.writing.value.index)
            assertEquals("Halloween", m.writing.value.word.label)
            m.goTo(2)
            assertEquals(CardRecord("signdigital", "oder-variante", "oder"), m.writing.value.record)
            m.chooseSet(SignBox.BOX1)
            assertEquals(0, m.writing.value.index)
            assertEquals(
                SignBox.BOX1,
                AdultModel(this, provider(), store, logger)
                    .writing.value.set.id,
            )
            advanceUntilIdle()
        }

    @Test
    fun `a sign on two cards counts twice`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, "tok")
            val log = MemoryWriteLog()
            val m = AdultModel(this, provider(), store, logger, log)
            m.chooseSet(SignBox.HALLOWEEN)
            m.goTo(SignBox.halloween.indexOfFirst { it.label == "Kostüm" })
            m.onWrite(WriteOutcome.Written(TagId("04aabbccddeeff"), m.writing.value.record))
            advanceUntilIdle()
            m.goTo(SignBox.halloween.indexOfFirst { it.label == "verkleiden" })
            m.onWrite(WriteOutcome.Written(TagId("04aabbccddee00"), m.writing.value.record))
            advanceUntilIdle()
            assertEquals(2, m.writing.value.counts["kostum"])
            assertEquals(listOf("Kostüm", "verkleiden"), log.all().map { it.label })
        }

    @Test
    fun `the old ticks become log entries without a time, once, and the old keys go`() =
        runTest {
            store.put(AdultModel.OLD_KEY_WRITTEN, "abend-s,aber")
            store.put("write.halloween.written", "Kostüm,gibtsnicht")
            store.put(AdultModel.OLD_KEY_WRITE_INDEX, "7")
            store.put(AdultModel.OLD_KEY_WRITE_SET, SignBox.HALLOWEEN)
            val log = MemoryWriteLog()
            val m = AdultModel(this, provider(), store, logger, log)
            assertEquals(
                listOf(
                    WriteEntry(null, "abend-s", "Abend(s)", list = "SIGNbox 1"),
                    WriteEntry(null, "aber", "aber", list = "SIGNbox 1"),
                    WriteEntry(null, "kostum", "Kostüm", list = "Halloween"),
                ),
                log.all(),
            )
            assertEquals(mapOf("abend-s" to 1, "aber" to 1, "kostum" to 1), m.writing.value.counts)
            for (key in listOf(
                AdultModel.OLD_KEY_WRITTEN,
                "write.halloween.written",
                AdultModel.OLD_KEY_WRITE_INDEX,
                AdultModel.OLD_KEY_WRITE_SET,
            )) {
                assertEquals(key, null, store.get(key))
            }
            AdultModel(this, provider(), store, logger, log)
            assertEquals(3, log.all().size)
        }

    // --- Lernlisten ---

    private fun jwt(sub: String): String {
        val enc = Base64.getUrlEncoder().withoutPadding()
        return enc.encodeToString("{}".toByteArray()) + "." + enc.encodeToString("""{"sub":"$sub"}""".toByteArray()) + ".sig"
    }

    /** The site, by path: one Lernliste "Kita" of three signs, or a failure for the lists when [listsDown]. */
    private fun site(listsDown: () -> Boolean = { false }) {
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse =
                    when (request.url.encodedPath) {
                        "/api/lists" -> {
                            if (listsDown()) {
                                MockResponse(code = 502)
                            } else {
                                MockResponse(
                                    body =
                                        """{"total":1,"data":[{"_id":"L1","name":"Kita","updatedAt":"2026-11-08","signs":[""" +
                                            """{"_id":"s1","slug":"jacke","name":"Jacke"},""" +
                                            """{"_id":"s2","slug":"schuh","name":"Schuhe"},""" +
                                            """{"_id":"s3","slug":"mama","name":"Mama"}]}]}""",
                                )
                            }
                        }

                        "/api/signs" -> {
                            MockResponse(body = sign("p/card.png"))
                        }

                        else -> {
                            MockResponse(body = """["https://cdn/card?sig"]""")
                        }
                    }
            }
    }

    @Test
    fun `the overview shows the Lernlisten first, then the sets in the app`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, jwt("user-1"))
            site()
            val m = AdultModel(this, provider(), store, logger)
            m.enterLists()
            advanceUntilIdle()
            val lists = m.lists.value
            assertEquals(listOf("Kita", "SIGNbox 1", "Halloween"), lists.entries.map { it.set.title })
            assertEquals(1, lists.fromSite)
            assertEquals(3, lists.entries.first().total)
            assertEquals(null, lists.failed)
        }

    @Test
    fun `a Lernliste is written as the site names its signs, and logged with the sign's id`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, jwt("user-1"))
            site()
            val log = MemoryWriteLog()
            val m = AdultModel(this, provider(), store, logger, log, clock = { 5L })
            m.enterLists()
            advanceUntilIdle()
            m.chooseSet("sd-L1")
            assertEquals(CardRecord("signdigital", "jacke", "Jacke"), m.writing.value.record)
            m.onWrite(WriteOutcome.Written(TagId("04aabbccddeeff"), m.writing.value.record))
            advanceUntilIdle()
            assertEquals(listOf(WriteEntry(5L, "jacke", "Jacke", "s1", "Kita", "04aabbccddeeff")), log.all())
            assertEquals("Schuhe", m.writing.value.word.label)
        }

    @Test
    fun `a sign written from one list counts in every list that holds it`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, jwt("user-1"))
            site()
            val log = MemoryWriteLog()
            log.append(WriteEntry(null, "mama", "Mama", list = "SIGNbox 1"))
            val m = AdultModel(this, provider(), store, logger, log)
            m.enterLists()
            advanceUntilIdle()
            m.chooseSet("sd-L1")
            assertEquals("Jacke", m.writing.value.word.label)
            assertEquals(
                1,
                m.writing.value.count(
                    m.writing.value.set.words
                        .last(),
                ),
            )
            advanceUntilIdle()
        }

    @Test
    fun `the lists last loaded stand without the network, and the failure is said`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, jwt("user-1"))
            var down = false
            site { down }
            AdultModel(this, provider(), store, logger).apply {
                enterLists()
                advanceUntilIdle()
            }
            down = true
            val offline = AdultModel(this, provider(), store, logger)
            offline.enterLists()
            advanceUntilIdle()
            assertEquals(
                "Kita",
                offline.lists.value.entries
                    .first()
                    .set.title,
            )
            assertTrue(offline.lists.value.failed != null)
            offline.chooseSet("sd-L1")
            assertEquals("Jacke", offline.writing.value.word.label)
            advanceUntilIdle()
        }

    @Test
    fun `a list that is gone falls back to the box`() =
        runTest {
            store.put(SignDigitalProvider.KEY_TOKEN, jwt("user-1"))
            site()
            val m = AdultModel(this, provider(), store, logger)
            m.chooseSet("sd-gone")
            assertEquals(SignBox.BOX1, m.writing.value.set.id)
            advanceUntilIdle()
        }
}
