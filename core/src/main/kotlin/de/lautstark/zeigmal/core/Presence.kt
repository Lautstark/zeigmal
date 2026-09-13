package de.lautstark.zeigmal.core

/**
 * Turns the reader's flapping into a card's presence.
 *
 * Measured on the Galaxy A51 on 2026-09-13: a resting NTAG213 is reported seen,
 * gone 210 ms later, seen again 80 ms after that, three times a second for as
 * long as it lies there. Android's presence check fails on this phone before
 * the card has moved. So a card is present from its first `seen` until it has
 * been gone for [holdMillis] without being seen again, and only then is it
 * gone; a `seen` inside that window is the same card still lying there and is
 * not an event. A single missed read must never arrive as a removal.
 *
 * No Android in here so the timing can be tested with a fake clock; the
 * scheduler is whatever posts a delayed runnable on the main thread.
 */
class Presence(
    private val holdMillis: Long,
    private val schedule: (delayMillis: Long, action: () -> Unit) -> Cancel,
    private val onEvent: (TagEvent) -> Unit,
) {
    fun interface Cancel {
        fun cancel()
    }

    private var present: TagId? = null
    private var pendingGone: Cancel? = null

    fun seen(event: TagEvent.Seen) {
        if (event.tag == present) {
            // Still there; the reader only blinked.
            pendingGone?.cancel()
            pendingGone = null
            return
        }
        // Another card: the first one is gone now, whatever the reader thinks.
        present?.let { previous ->
            pendingGone?.cancel()
            pendingGone = null
            onEvent(TagEvent.Gone(previous))
        }
        present = event.tag
        onEvent(event)
    }

    fun gone(event: TagEvent.Gone) {
        if (event.tag != present || pendingGone != null) return
        pendingGone =
            schedule(holdMillis) {
                pendingGone = null
                if (present == event.tag) {
                    present = null
                    onEvent(event)
                }
            }
    }

    companion object {
        /** Far above the A51's 80 ms blink, still immediate for "card out → ready". */
        const val DEFAULT_HOLD_MILLIS = 1000L
    }
}
