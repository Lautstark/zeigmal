package de.lautstark.zeigmal.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StationControllerTest {
    private val trinken = CardRecord("fake", "trinken", "trinken")
    private val a = TagId("04c5d4a88d2681")
    private val logger = ListLogger()

    private fun controller(scope: kotlinx.coroutines.CoroutineScope) =
        StationController(
            scope = scope,
            providers = mapOf("fake" to FakeProvider(media = mapOf("trinken" to Media("https://cdn/trinken", null)))),
            logger = logger,
            holdMillis = 1000,
        )

    @Test
    fun `a blinking card is one card, and its video comes from the provider`() =
        runTest {
            val c = controller(this)
            c.onTag(TagEvent.Seen(a, trinken))
            repeat(5) {
                c.onTag(TagEvent.Gone(a))
                advanceTimeBy(200)
                c.onTag(TagEvent.Seen(a, trinken))
            }
            assertEquals(StationState.Card(trinken, a, Phase.LOADING), c.state.value)
            assertEquals("https://cdn/trinken", c.videoUrl(trinken))
            c.onFirstFrame()
            assertEquals(Phase.PLAYING, (c.state.value as StationState.Card).phase)
            assertEquals(1, logger.lines.count { it.startsWith("card in") })
        }

    @Test
    fun `gone a second after the last blink, idle after the round`() =
        runTest {
            val c = controller(this)
            c.onTag(TagEvent.Seen(a, trinken))
            c.onFirstFrame()
            c.onTag(TagEvent.Gone(a))
            advanceTimeBy(999)
            assertTrue((c.state.value as StationState.Card).present)
            advanceTimeBy(2)
            assertTrue(!(c.state.value as StationState.Card).present)
            c.onPlaybackEnded()
            assertEquals(StationState.Idle, c.state.value)
        }

    @Test
    fun `an unknown provider on a sticker is a reason, not a crash`() =
        runTest {
            val c = controller(this)
            try {
                c.videoUrl(CardRecord("elsewhere", "x", "x"))
                fail()
            } catch (e: IllegalArgumentException) {
                assertTrue(e.message!!.contains("elsewhere"))
            }
        }
}
