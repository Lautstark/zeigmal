package de.lautstark.zeigmal.core

import org.junit.Assert.assertEquals
import org.junit.Test

class StationTest {
    private val trinken = CardRecord("signdigital", "trinken", "trinken")
    private val essen = CardRecord("signdigital", "essen", "essen")
    private val a = TagId("04a791b2c3d480")
    private val b = TagId("04a791b2c3d481")
    private val x = TagId("04a791b2c3d4ff")
    private val station = Station(maxRounds = 3)

    private fun StationState.then(vararg events: StationEvent): StationState = events.fold(this) { s, e -> station.next(s, e) }

    private fun seen(
        tag: TagId,
        record: CardRecord?,
    ) = StationEvent.CardSeen(tag, record)

    @Test
    fun `the ring at once, the video when its first frame is`() {
        val s = StationState.Idle.then(seen(a, trinken))
        assertEquals(StationState.Card(trinken, a, Phase.LOADING), s)
        assertEquals(StationState.Card(trinken, a, Phase.PLAYING), s.then(StationEvent.FirstFrame))
    }

    @Test
    fun `a card that stays plays again, up to the cap, then rests on the ring`() {
        var s = StationState.Idle.then(seen(a, trinken), StationEvent.FirstFrame)
        s = s.then(StationEvent.PlaybackEnded)
        assertEquals(StationState.Card(trinken, a, Phase.LOADING, round = 2), s)
        s = s.then(StationEvent.FirstFrame, StationEvent.PlaybackEnded, StationEvent.FirstFrame, StationEvent.PlaybackEnded)
        assertEquals(StationState.Card(trinken, a, Phase.DONE, round = 3), s)
        assertEquals(s, s.then(StationEvent.PlaybackEnded))
        assertEquals(StationState.Idle, s.then(StationEvent.CardGone(a)))
    }

    @Test
    fun `a card removed mid-video finishes the round and then rests`() {
        val playing = StationState.Idle.then(seen(a, trinken), StationEvent.FirstFrame)
        val gone = playing.then(StationEvent.CardGone(a))
        assertEquals(StationState.Card(trinken, a, Phase.PLAYING, present = false), gone)
        assertEquals(StationState.Idle, gone.then(StationEvent.PlaybackEnded))
    }

    @Test
    fun `another card replaces at once`() {
        val playing = StationState.Idle.then(seen(a, trinken), StationEvent.FirstFrame)
        assertEquals(StationState.Card(essen, b, Phase.LOADING), playing.then(seen(b, essen)))
    }

    @Test
    fun `an unknown sticker is the grey ring until it goes`() {
        val s = StationState.Idle.then(seen(x, null))
        assertEquals(StationState.Unknown(x), s)
        assertEquals(StationState.Unknown(x), s.then(StationEvent.CardGone(a)))
        assertEquals(StationState.Idle, s.then(StationEvent.CardGone(x)))
    }

    @Test
    fun `a video that cannot be fetched leaves the card on the ring`() {
        val s = StationState.Idle.then(seen(a, trinken), StationEvent.PlaybackFailed)
        assertEquals(StationState.Card(trinken, a, Phase.DONE), s)
        assertEquals(StationState.Idle, s.then(StationEvent.CardGone(a)))
    }

    @Test
    fun `stray events change nothing`() {
        assertEquals(
            StationState.Idle,
            StationState.Idle.then(StationEvent.FirstFrame, StationEvent.PlaybackEnded, StationEvent.CardGone(a)),
        )
    }
}
