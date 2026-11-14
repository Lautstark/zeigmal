package de.lautstark.zeigmal.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Base64

/** The search, and changes to the lists on the site: what the adult does to a list from the phone. */
@OptIn(ExperimentalCoroutinesApi::class)
class AdultModelListsTest {
    private val server = MockWebServer()
    private val store = InMemoryStore()
    private val logger = ListLogger()

    /** The site's one list, by sign id, as it stands; a PATCH that [refuse]s is answered but not kept. */
    private var kita = mutableListOf("s1", "s2")
    private var refuse = false
    private var deleted = false
    private var searches = 0
    private var patches = 0
    private val names = mapOf("s1" to "Jacke", "s2" to "Schuhe", "s3" to "Mama", "a1" to "keineAhnung")

    @Before
    fun start() {
        server.start()
        store.put(SignDigitalProvider.KEY_TOKEN, jwt("user-1"))
        store.put(SignDigitalProvider.KEY_EMAIL, "mail@example.org")
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse =
                    when {
                        request.url.encodedPath == "/api/lists" -> {
                            MockResponse(body = """{"total":1,"data":[${doc()}]}""")
                        }

                        request.url.encodedPath == "/api/lists/L1" && request.method == "PATCH" -> {
                            patches++
                            if (!refuse) {
                                val body = request.body!!.utf8()
                                kita =
                                    Regex("\"signs\":\\[([^]]*)]")
                                        .find(body)!!
                                        .groupValues[1]
                                        .split(',')
                                        .filter { it.isNotBlank() }
                                        .map { it.trim('"') }
                                        .toMutableList()
                            }
                            MockResponse(body = "{}")
                        }

                        request.url.encodedPath == "/api/lists/L1" -> {
                            if (deleted) MockResponse(code = 404) else MockResponse(body = doc())
                        }

                        request.url.encodedPath == "/api/file-nodes/N1" && request.method == "DELETE" -> {
                            deleted = true
                            MockResponse(body = "")
                        }

                        request.url.encodedPath == "/search/query" -> {
                            searches++
                            MockResponse(body = """{"total":1,"data":[{"_id":"a1","slug":"keineahnung","name":"keineAhnung"}]}""")
                        }

                        request.url.encodedPath == "/api/signs" -> {
                            MockResponse(body = """{"data":[{"slug":"x","edge":{"signBox":{"default":"p/c.png"}}}]}""")
                        }

                        else -> {
                            MockResponse(body = """["https://cdn/card?sig"]""")
                        }
                    }
            }
    }

    @After
    fun stop() = server.close()

    private fun doc(): String =
        """{"_id":"L1","name":"Kita","node":"N1","updatedAt":"2026-11-14","learnedSigns":[],"signs":[""" +
            kita.joinToString(",") { """{"_id":"$it","slug":"${names.getValue(it).lowercase()}","name":"${names.getValue(it)}"}""" } + "]}"

    private fun jwt(sub: String): String {
        val enc = Base64.getUrlEncoder().withoutPadding()
        return enc.encodeToString("{}".toByteArray()) + "." + enc.encodeToString("""{"sub":"$sub"}""".toByteArray()) + ".sig"
    }

    private fun TestScope.model(log: WriteLog = MemoryWriteLog()): AdultModel {
        val provider =
            SignDigitalProvider(store, baseUrl = server.url("/").toString().trimEnd('/'), io = UnconfinedTestDispatcher(testScheduler))
        return AdultModel(this, provider, store, logger, log).also {
            it.enterLists()
            advanceUntilIdle()
        }
    }

    private val hit = ListSign("a1", "keineahnung", "keineAhnung", null)

    @Test
    fun `the search asks the site once the typing pauses, and an empty field clears it`() =
        runTest {
            val m = model()
            m.search("ahn")
            m.search("ahnung")
            advanceTimeBy(AdultModel.SEARCH_PAUSE_MILLIS - 1)
            assertEquals(0, searches)
            advanceUntilIdle()
            assertEquals(
                listOf("keineahnung"),
                m.search.value.hits
                    .map { it.slug },
            )
            assertEquals("only the last text is asked", 1, searches)
            m.search("")
            assertEquals(Search(text = ""), m.search.value)
        }

    @Test
    fun `a hit is a stack of one, and after one sticker the reader stops until asked again`() =
        runTest {
            val log = MemoryWriteLog()
            val m = model(log)
            assertTrue(m.chooseHit(hit))
            advanceUntilIdle()
            val w = m.writing.value
            assertTrue(w.fromSearch)
            assertEquals(CardRecord("signdigital", "keineahnung", "keineAhnung"), w.record)
            assertEquals(TagMode.Write(w.record!!), m.tagMode(active = true))
            m.onWrite(WriteOutcome.Written(TagId("04aabbccddeeff"), w.record!!))
            advanceUntilIdle()
            assertTrue(m.writing.value.spent)
            assertEquals(TagMode.Read, m.tagMode(active = true))
            assertNull("a card from the search is from no list", log.all().single().list)
            assertEquals("a1", log.all().single().signId)
            m.again()
            assertEquals(TagMode.Write(w.record!!), m.tagMode(active = true))
        }

    @Test
    fun `a sign added to a list is at its end on the site and in the overview`() =
        runTest {
            val m = model()
            m.addToList("sd-L1", "a1")
            advanceUntilIdle()
            assertEquals(listOf("s1", "s2", "a1"), kita)
            assertEquals(
                listOf("Jacke", "Schuhe", "keineAhnung"),
                m.lists.value.entries
                    .single()
                    .set.words
                    .map { it.label },
            )
            assertEquals(setOf("sd-L1"), m.lists.value.holding("a1"))
            assertNull(m.lists.value.saveFailed)
            // Kept for the next start.
            assertTrue(store.get(AdultModel.KEY_LISTS_CACHE)!!.contains("keineahnung"))
        }

    @Test
    fun `adding a sign that is there already changes nothing`() =
        runTest {
            val m = model()
            m.addToList("sd-L1", "s2")
            advanceUntilIdle()
            assertEquals(listOf("s1", "s2"), kita)
            assertEquals(0, patches)
        }

    @Test
    fun `a sign taken out of the list being written leaves the writing mode on its place`() =
        runTest {
            kita = mutableListOf("s1", "s2", "s3")
            val m = model()
            m.chooseSet("sd-L1")
            m.goTo(2)
            m.removeFromList("sd-L1", "s2")
            advanceUntilIdle()
            assertEquals(listOf("s1", "s3"), kita)
            assertEquals(
                listOf("Jacke", "Mama"),
                m.writing.value.set.words
                    .map { it.label },
            )
            assertEquals(1, m.writing.value.index)
        }

    @Test
    fun `a change the site did not keep is said, and nothing of it is kept here`() =
        runTest {
            val m = model()
            refuse = true
            m.addToList("sd-L1", "a1")
            advanceUntilIdle()
            assertTrue(
                m.lists.value.saveFailed!!
                    .contains("anders"),
            )
            assertEquals(
                listOf("Jacke", "Schuhe"),
                m.lists.value.entries
                    .single()
                    .set.words
                    .map { it.label },
            )
            assertEquals(false, m.lists.value.saving)
        }

    @Test
    fun `a deleted list is gone from the site, the overview and the writing mode`() =
        runTest {
            val m = model()
            m.chooseSet("sd-L1")
            m.deleteList("sd-L1")
            advanceUntilIdle()
            assertTrue(deleted)
            assertEquals(emptyList<ListEntry>(), m.lists.value.entries)
            assertEquals(CardSet.NONE, m.writing.value.set)
        }
}
