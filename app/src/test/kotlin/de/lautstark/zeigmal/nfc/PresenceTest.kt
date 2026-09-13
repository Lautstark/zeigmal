package de.lautstark.zeigmal.nfc

import de.lautstark.zeigmal.cardset.TagId
import org.junit.Assert.assertEquals
import org.junit.Test

class PresenceTest {
    private val a = TagId("04c5d4a88d2681")
    private val b = TagId("04c5d4a88d2682")
    private val events = mutableListOf<TagEvent>()
    private val timers = mutableListOf<Pair<Long, () -> Unit>>()
    private val presence =
        Presence(
            holdMillis = 1000,
            schedule = { delay, action ->
                val entry = delay to action
                timers += entry
                Presence.Cancel { timers.remove(entry) }
            },
            onEvent = { events += it },
        )

    private fun fire() {
        val due = timers.toList()
        timers.clear()
        due.forEach { it.second() }
    }

    private fun seen(t: TagId) = presence.seen(TagEvent.Seen(t, listOf("NfcA")))

    private fun gone(t: TagId) = presence.gone(TagEvent.Gone(t))

    @Test
    fun `a resting card that the reader keeps losing is one card`() {
        seen(a)
        repeat(5) {
            gone(a)
            seen(a)
        }
        assertEquals(listOf<TagEvent>(TagEvent.Seen(a, listOf("NfcA"))), events)
        assertEquals(0, timers.size)
    }

    @Test
    fun `gone arrives once the hold has passed without a new sighting`() {
        seen(a)
        gone(a)
        assertEquals(1, events.size)
        fire()
        assertEquals(listOf<TagEvent>(TagEvent.Seen(a, listOf("NfcA")), TagEvent.Gone(a)), events)
        seen(a)
        assertEquals(3, events.size)
    }

    @Test
    fun `another card ends the first at once`() {
        seen(a)
        gone(a)
        seen(b)
        assertEquals(listOf<TagEvent>(TagEvent.Seen(a, listOf("NfcA")), TagEvent.Gone(a), TagEvent.Seen(b, listOf("NfcA"))), events)
        fire()
        assertEquals(3, events.size)
    }

    @Test
    fun `a gone for a card that is not present is nothing`() {
        gone(a)
        seen(a)
        gone(b)
        fire()
        assertEquals(1, events.size)
    }
}
