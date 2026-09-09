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

    @Test
    fun `a known card plays, another replaces it, the end is idle`() {
        var s: StationState = StationState.Idle
        s = station.next(s, StationEvent.CardSeen(a))
        assertEquals(StationState.Playing(trinken, a, 1), s)
        s = station.next(s, StationEvent.CardSeen(b))
        assertEquals(StationState.Playing(essen, b, 1), s)
        s = station.next(s, StationEvent.PlaybackEnded)
        assertEquals(StationState.Idle, s)
    }

    @Test
    fun `the same card again starts the video again`() {
        val first = station.next(StationState.Idle, StationEvent.CardSeen(a))
        val again = station.next(first, StationEvent.CardSeen(a))
        assertEquals(StationState.Playing(trinken, a, 2), again)
        // A second physical card for the same word counts as the same word.
        assertEquals(StationState.Playing(trinken, a2, 3), station.next(again, StationEvent.CardSeen(a2)))
    }

    @Test
    fun `an unknown card is neutral and goes away with the card`() {
        val s = station.next(StationState.Idle, StationEvent.CardSeen(x))
        assertEquals(StationState.Unknown(x), s)
        assertEquals(StationState.Unknown(x), station.next(s, StationEvent.CardGone(a)))
        assertEquals(StationState.Idle, station.next(s, StationEvent.CardGone(x)))
    }

    @Test
    fun `removing a card does not stop its video`() {
        val playing = station.next(StationState.Idle, StationEvent.CardSeen(a))
        assertEquals(playing, station.next(playing, StationEvent.CardGone(a)))
    }
}
