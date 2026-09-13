package de.lautstark.zeigmal.core

/**
 * The whole child-facing behaviour, as a pure function: a card was seen, a card
 * went away, the video's first frame is on screen, the video ended — and what
 * the station shows now. No Android in here, so the rules are tested in
 * milliseconds and the screen only draws them.
 *
 * The ring around the mark is on screen the instant the tag is seen; the video
 * takes over when its first frame has rendered and loops in the player, with
 * no reload and no ring between rounds, up to [Station.MAX_ROUNDS] times;
 * after that the card's own picture stays on screen until the card goes; and
 * when the card is gone the station is idle once the current round has
 * finished. A card taken away and put back starts from the first round.
 *
 * Presence arrives already debounced (see [Presence]); the station trusts it.
 */
sealed interface StationState {
    data object Idle : StationState

    data class Card(
        val record: CardRecord,
        val tag: TagId,
        val phase: Phase,
        /** Which loop of the video is running, from 1. Starts over with every card in. */
        val loop: Int = 1,
        /** False once the reader reported the card gone; decides what the end leads to. */
        val present: Boolean = true,
    ) : StationState {
        /** Whether the player should loop again after this one. */
        fun wantsAnotherLoop(maxLoops: Int): Boolean = present && loop < maxLoops
    }

    /** A sticker without a record: the grey ring, until it is gone. */
    data class Unknown(
        val tag: TagId,
    ) : StationState
}

enum class Phase {
    /** The ring: the card is known, the video is on its way. */
    LOADING,

    /** The video's first frame has rendered; it is in front, looping. */
    PLAYING,

    /** The rounds are used up, or nothing could be fetched; the card's picture stays until the card goes. */
    DONE,
}

sealed interface StationEvent {
    data class CardSeen(
        val tag: TagId,
        val record: CardRecord?,
    ) : StationEvent

    data class CardGone(
        val tag: TagId,
    ) : StationEvent

    data object FirstFrame : StationEvent

    /** One loop of the video ended and the player started the next. */
    data object Looped : StationEvent

    /** The last loop has ended. */
    data object PlaybackEnded : StationEvent

    /** The video could not be fetched or played; the card stays, the ring stays. */
    data object PlaybackFailed : StationEvent
}

class Station {
    fun next(
        state: StationState,
        event: StationEvent,
    ): StationState =
        when (event) {
            is StationEvent.CardSeen -> {
                when {
                    event.record == null -> StationState.Unknown(event.tag)
                    else -> StationState.Card(event.record, event.tag, Phase.LOADING)
                }
            }

            is StationEvent.CardGone -> {
                when {
                    state is StationState.Unknown && state.tag == event.tag -> {
                        StationState.Idle
                    }

                    state is StationState.Card && state.tag == event.tag -> {
                        if (state.phase == Phase.DONE) StationState.Idle else state.copy(present = false)
                    }

                    else -> {
                        state
                    }
                }
            }

            StationEvent.FirstFrame -> {
                if (state is StationState.Card && state.phase == Phase.LOADING) state.copy(phase = Phase.PLAYING) else state
            }

            StationEvent.Looped -> {
                if (state is StationState.Card && state.phase == Phase.PLAYING) state.copy(loop = state.loop + 1) else state
            }

            StationEvent.PlaybackEnded -> {
                when {
                    state !is StationState.Card -> state
                    !state.present -> StationState.Idle
                    else -> state.copy(phase = Phase.DONE)
                }
            }

            StationEvent.PlaybackFailed -> {
                when {
                    state !is StationState.Card -> state
                    !state.present -> StationState.Idle
                    else -> state.copy(phase = Phase.DONE)
                }
            }
        }

    companion object {
        /** How often a card that stays in the slot plays before its picture takes over. */
        const val MAX_ROUNDS = 20
    }
}
