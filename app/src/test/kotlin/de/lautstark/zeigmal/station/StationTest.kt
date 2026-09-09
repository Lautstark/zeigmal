package de.lautstark.zeigmal.station

import de.lautstark.zeigmal.cardset.MediaEntry
import de.lautstark.zeigmal.cardset.SpeechMode
import de.lautstark.zeigmal.cardset.TagId
import org.junit.Assert.assertEquals
import org.junit.Test

class StationTest {
    private val trinken = MediaEntry("trinken", "Trinken", "videos/trinken.mp4", null, SpeechMode.VIDEO, null, null)
    private val essen = MediaEntry("essen", "Essen", "videos/essen.mp4", null, SpeechMode.VIDEO, null, null)
    private val a = TagId("04a791b2c3d480")
    private val b = TagId("04a791b2c3d481")
    private val a2 = TagId("04a791b2c3d482")
    private val x = TagId("04a791b2c3d4ff")
    private val station = Station { tag -> mapOf(a to trinken, a2 to trinken, b to essen)[tag] }

    private fun StationState.then(vararg events: StationEvent): StationState = events.fold(this) { s, e -> station.next(s, e) }

    @Test
    fun `the card is on screen at once, the video when its first frame is`() {
        val seen = StationState.Idle.then(StationEvent.CardSeen(a))
        assertEquals(StationState.Card(trinken, a, 1, Phase.LOADING), seen)
        assertEquals(StationState.Card(trinken, a, 1, Phase.PLAYING), seen.then(StationEvent.FirstFrame))
    }

    @Test
    fun `after the video the card stays while the card stays, and idle follows the card`() {
        val playing = StationState.Idle.then(StationEvent.CardSeen(a), StationEvent.FirstFrame)
        val ended = playing.then(StationEvent.PlaybackEnded)
        assertEquals(StationState.Card(trinken, a, 1, Phase.ENDED), ended)
        assertEquals(StationState.Idle, ended.then(StationEvent.CardGone(a)))
        // Removed mid-video: the video finishes, then idle.
        val goneWhilePlaying = playing.then(StationEvent.CardGone(a))
        assertEquals(StationState.Card(trinken, a, 1, Phase.PLAYING, present = false), goneWhilePlaying)
        assertEquals(StationState.Idle, goneWhilePlaying.then(StationEvent.PlaybackEnded))
    }

    @Test
    fun `another card replaces at once, the same card again restarts`() {
        val playing = StationState.Idle.then(StationEvent.CardSeen(a), StationEvent.FirstFrame)
        assertEquals(StationState.Card(essen, b, 1, Phase.LOADING), playing.then(StationEvent.CardSeen(b)))
        assertEquals(StationState.Card(trinken, a, 2, Phase.LOADING), playing.then(StationEvent.CardSeen(a)))
        // A second physical card for the same word counts as the same word.
        assertEquals(StationState.Card(trinken, a2, 2, Phase.LOADING), playing.then(StationEvent.CardSeen(a2)))
    }

    @Test
    fun `an unknown card is neutral and goes away with the card`() {
        val s = StationState.Idle.then(StationEvent.CardSeen(x))
        assertEquals(StationState.Unknown(x), s)
        assertEquals(StationState.Unknown(x), s.then(StationEvent.CardGone(a)))
        assertEquals(StationState.Idle, s.then(StationEvent.CardGone(x)))
    }

    @Test
    fun `stray events change nothing`() {
        assertEquals(
            StationState.Idle,
            StationState.Idle.then(StationEvent.FirstFrame, StationEvent.PlaybackEnded, StationEvent.CardGone(a)),
        )
        val ended = StationState.Idle.then(StationEvent.CardSeen(a), StationEvent.FirstFrame, StationEvent.PlaybackEnded)
        assertEquals(ended, ended.then(StationEvent.FirstFrame))
    }
}
