package de.lautstark.zeigmal.station

import de.lautstark.zeigmal.cardset.MediaEntry
import de.lautstark.zeigmal.cardset.TagId

/**
 * The whole child-facing behaviour, as a pure function: a card was seen, a card
 * went away, a video ended — and what the station shows now. No Android in here,
 * so the rules are tested in milliseconds and the screen only draws them.
 *
 * Removal is deliberately weak. Whether Android reports a card going away, and
 * how quickly, is what docs/experiments.md E1–E4 measure; until then a removed
 * card changes nothing about a running video. A new card replaces it at once.
 */
sealed interface StationState {
    data object Idle : StationState

    /** [run] rises each time the same card starts again, so a screen can restart the video. */
    data class Playing(
        val entry: MediaEntry,
        val tag: TagId,
        val run: Int,
    ) : StationState

    data class Unknown(
        val tag: TagId,
    ) : StationState
}

sealed interface StationEvent {
    data class CardSeen(
        val tag: TagId,
    ) : StationEvent

    data class CardGone(
        val tag: TagId,
    ) : StationEvent

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

                    state is StationState.Playing && state.entry.id == entry.id -> {
                        StationState.Playing(entry, event.tag, state.run + 1)
                    }

                    else -> {
                        StationState.Playing(entry, event.tag, 1)
                    }
                }
            }

            is StationEvent.CardGone -> {
                if (state is StationState.Unknown && state.tag == event.tag) StationState.Idle else state
            }

            StationEvent.PlaybackEnded -> {
                if (state is StationState.Playing) StationState.Idle else state
            }
        }
}
