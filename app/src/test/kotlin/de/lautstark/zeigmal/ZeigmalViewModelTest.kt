package de.lautstark.zeigmal

import de.lautstark.zeigmal.core.CardRecord
import de.lautstark.zeigmal.core.FakeProvider
import de.lautstark.zeigmal.core.FakeTagSource
import de.lautstark.zeigmal.core.InMemoryStore
import de.lautstark.zeigmal.core.Media
import de.lautstark.zeigmal.core.Phase
import de.lautstark.zeigmal.core.Provider
import de.lautstark.zeigmal.core.SignDigitalProvider
import de.lautstark.zeigmal.core.StationState
import de.lautstark.zeigmal.core.TagId
import de.lautstark.zeigmal.core.TagMode
import de.lautstark.zeigmal.core.WriteOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The wiring: hardware events reach the station, the mode reaches the hardware. */
@OptIn(ExperimentalCoroutinesApi::class)
class ZeigmalViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = InMemoryStore()
    private val trinken = CardRecord("fake", "trinken", "trinken")
    private val a = TagId("04c5d4a88d2681")

    @Before
    fun main() = Dispatchers.setMain(dispatcher)

    @After
    fun reset() = Dispatchers.resetMain()

    private fun model(): ZeigmalViewModel {
        val providers: Map<String, Provider> =
            mapOf(
                SignDigitalProvider.ID to SignDigitalProvider(store, baseUrl = "http://localhost:1"),
                "fake" to FakeProvider(media = mapOf("trinken" to Media("https://cdn/trinken", null))),
            )
        return ZeigmalViewModel(store, providers)
    }

    @Test
    fun `a tag from the hardware becomes a card on the station`() =
        runTest(dispatcher) {
            val source = FakeTagSource()
            val m = model()
            m.attach(source)
            // The collectors start on the next turn of the loop, as they do in the app before any tag can arrive.
            advanceUntilIdle()
            source.seen(a, trinken)
            advanceUntilIdle()
            assertEquals(StationState.Card(trinken, a, Phase.LOADING), m.station.state.value)
            assertEquals("https://cdn/trinken", m.station.videoUrl(trinken))
            m.detach()
        }

    @Test
    fun `the first pin sets, the next has to match`() =
        runTest(dispatcher) {
            val m = model()
            m.askPin()
            assertEquals(Mode.PIN, m.mode.value)
            m.pinEntered("1234")
            assertEquals(Mode.LOGIN, m.mode.value)
            m.leaveAdult()
            m.askPin()
            m.pinEntered("0000")
            assertEquals(Mode.PIN, m.mode.value)
            assertTrue(m.pinRejected.value)
            m.pinEntered("1234")
            assertEquals(Mode.LOGIN, m.mode.value)
        }

    @Test
    fun `the hardware is told to write only in the writing mode, logged in`() =
        runTest(dispatcher) {
            val source = FakeTagSource()
            val m = model()
            m.attach(source)
            advanceUntilIdle()
            assertEquals(TagMode.Read, source.mode.value)
            m.enterAdult()
            advanceUntilIdle()
            assertEquals(Mode.LOGIN, m.mode.value)
            assertEquals(TagMode.Read, source.mode.value)
            m.detach()

            store.put(SignDigitalProvider.KEY_TOKEN, "tok")
            store.put(SignDigitalProvider.KEY_EMAIL, "mail@example.org")
            val loggedIn = model()
            val source2 = FakeTagSource()
            loggedIn.attach(source2)
            loggedIn.enterAdult()
            advanceUntilIdle()
            assertEquals(Mode.WRITE, loggedIn.mode.value)
            assertTrue(source2.mode.value is TagMode.Write)
            assertEquals("abend-s", (source2.mode.value as TagMode.Write).record.ref)
            source2.wrote(WriteOutcome.Written(a, (source2.mode.value as TagMode.Write).record))
            advanceUntilIdle()
            assertEquals(1, loggedIn.adult.writing.value.index)
            assertEquals("aber", (source2.mode.value as TagMode.Write).record.label)
            loggedIn.leaveAdult()
            advanceUntilIdle()
            assertEquals(TagMode.Read, source2.mode.value)
            loggedIn.detach()
        }
}
