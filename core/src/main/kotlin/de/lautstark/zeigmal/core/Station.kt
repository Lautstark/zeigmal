package de.lautstark.zeigmal.core

/**
 * The whole child-facing behaviour, as a pure function: a card was seen, a card
 * went away, the video's first frame is on screen, the video ended — and what
 * the station shows now. No Android in here, so the rules are tested in
 * milliseconds and the screen only draws them.
 *
 * The ring around the mark is on screen the instant the tag is seen; the video
 * takes over when its first frame has rendered; when it ends the card is
 * still there, so it plays again — up to [Station.MAX_ROUNDS] — with the ring
 * between rounds; and when the card is gone the station is idle once the
 * current round has finished.
 *
 * Presence arrives already debounced (see [Presence]); the station trusts it.
 */
sealed interface StationState {
    data object Idle : StationState

    data class Card(
        val record: CardRecord,
        val tag: TagId,
        val phase: Phase,
        /** 1 for the first playback of this card, counting up while it stays. */
        val round: Int = 1,
        /** False once the reader reported the card gone; decides what ENDED leads to. */
        val present: Boolean = true,
    ) : StationState

    /** A sticker without a record: the grey ring, until it is gone. */
    data class Unknown(
        val tag: TagId,
    ) : StationState
}

enum class Phase {
    /** The ring: the card is known, the video is on its way. */
    LOADING,

    /** The video's first frame has rendered; it is in front. */
    PLAYING,

    /** The rounds are used up; the ring stays until the card goes. */
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

    data object PlaybackEnded : StationEvent

    /** The video could not be fetched or played; the card stays, the ring stays. */
    data object PlaybackFailed : StationEvent
}

class Station(
    private val maxRounds: Int = MAX_ROUNDS,
) {
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

            StationEvent.PlaybackEnded -> {
                when {
                    state !is StationState.Card -> state
                    !state.present -> StationState.Idle
                    state.round < maxRounds -> state.copy(phase = Phase.LOADING, round = state.round + 1)
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
        /** How often a card that stays in the slot plays. A card left in overnight is not a loop. */
        const val MAX_ROUNDS = 10

        /** The ring between two rounds, so "again" reads as again and not as a stutter. */
        const val PAUSE_BETWEEN_ROUNDS_MILLIS = 1000L
    }
}
