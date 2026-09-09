package de.lautstark.zeigmal.station

import de.lautstark.zeigmal.cardset.MediaEntry
import de.lautstark.zeigmal.cardset.TagId

/**
 * The whole child-facing behaviour, as a pure function: a card was seen, a card
 * went away, the video's first frame is on screen, the video ended — and what
 * the station shows now. No Android in here, so the rules are tested in
 * milliseconds and the screen only draws them.
 *
 * The card is on screen the instant the tag is seen — its symbol and its word —
 * and the video fades in over it when its first frame has actually rendered.
 * Nothing is ever black, and whatever the player needs to start (a decoder, a
 * disk, one day perhaps a network) is invisible behind the card.
 *
 * Removal is deliberately weak. Whether Android reports a card going away, and
 * how quickly, is what docs/experiments.md E1–E4 measure; a removed card never
 * stops a running video. What it does decide is what comes after the video:
 * the card again if it is still in the slot, idle if it is gone.
 */
sealed interface StationState {
    data object Idle : StationState

    data class Card(
        val entry: MediaEntry,
        val tag: TagId,
        /** Rises each time the same card starts again, so a screen can restart the video. */
        val run: Int,
        val phase: Phase,
        /** False once the reader reported the card gone; decides where ENDED leads. */
        val present: Boolean = true,
    ) : StationState

    data class Unknown(
        val tag: TagId,
    ) : StationState
}

enum class Phase {
    /** The card is on screen; the video is starting behind it. */
    LOADING,

    /** The video's first frame has rendered; it is in front now. */
    PLAYING,

    /** The video ended; the card is on screen again while it stays in the slot. */
    ENDED,
}

sealed interface StationEvent {
    data class CardSeen(
        val tag: TagId,
    ) : StationEvent

    data class CardGone(
        val tag: TagId,
    ) : StationEvent

    data object FirstFrame : StationEvent

    data object PlaybackEnded : StationEvent
}

class Station(
    private val resolve: (TagId) -> MediaEntry?,
) {
    fun next(
        state: StationState,
        event: StationEvent,
    ): StationState =
        when (event) {
            is StationEvent.CardSeen -> {
                val entry = resolve(event.tag)
                when {
                    entry == null -> {
                        StationState.Unknown(event.tag)
                    }

                    state is StationState.Card && state.entry.id == entry.id -> {
                        StationState.Card(entry, event.tag, state.run + 1, Phase.LOADING)
                    }

                    else -> {
                        StationState.Card(entry, event.tag, 1, Phase.LOADING)
                    }
                }
            }

            is StationEvent.CardGone -> {
                when {
                    state is StationState.Unknown && state.tag == event.tag -> {
                        StationState.Idle
                    }

                    state is StationState.Card && state.tag == event.tag -> {
                        if (state.phase == Phase.ENDED) StationState.Idle else state.copy(present = false)
                    }

                    else -> {
                        state
                    }
                }
            }

            StationEvent.FirstFrame -> {
                if (state is StationState.Card && state.phase == Phase.LOADING) state.copy(phase = Phase.PLAYING) else state
            }

            StationEvent.PlaybackEnded -> {
                when {
                    state !is StationState.Card -> state
                    state.present -> state.copy(phase = Phase.ENDED)
                    else -> StationState.Idle
                }
            }
        }
}
