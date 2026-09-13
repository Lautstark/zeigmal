package de.lautstark.zeigmal.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The child's side, wired up: reader events in through the presence filter,
 * the station's rules, a provider for the link, and the screen's callbacks
 * back in. Everything the child mode does, with no Android in it.
 *
 * One instance per app; the screen reads [state] and calls the three playback
 * callbacks. The presence hold and the pause between rounds are coroutine
 * delays on [scope], so a test drives them with virtual time.
 */
class StationController(
    private val scope: CoroutineScope,
    private val providers: Map<String, Provider>,
    private val logger: Logger = Logger { },
    private val station: Station = Station(),
    holdMillis: Long = Presence.DEFAULT_HOLD_MILLIS,
) {
    private val _state = MutableStateFlow<StationState>(StationState.Idle)
    val state: StateFlow<StationState> = _state

    private val presence =
        Presence(
            holdMillis = holdMillis,
            schedule = { delayMillis, action ->
                val job: Job =
                    scope.launch {
                        delay(delayMillis)
                        action()
                    }
                Presence.Cancel { job.cancel() }
            },
            onEvent = ::onCard,
        )

    /** Raw from the reader. Logged as it comes, then filtered by [Presence]. */
    fun onTag(event: TagEvent) {
        when (event) {
            is TagEvent.Seen -> {
                logger.log(
                    "reader: seen ${event.tag} ${event.record?.let {
                        "${it.provider}/${it.ref}"
                    } ?: "kein Eintrag"} ${event.technologies.joinToString(
                        ",",
                    )}",
                )
                presence.seen(event)
            }

            is TagEvent.Gone -> {
                logger.log("reader: gone ${event.tag}")
                presence.gone(event)
            }
        }
    }

    private fun onCard(event: TagEvent) {
        when (event) {
            is TagEvent.Seen -> {
                logger.log("card in ${event.tag}")
                apply(StationEvent.CardSeen(event.tag, event.record))
            }

            is TagEvent.Gone -> {
                logger.log("card out ${event.tag}")
                apply(StationEvent.CardGone(event.tag))
            }
        }
    }

    fun onFirstFrame() = apply(StationEvent.FirstFrame)

    fun onPlaybackEnded() = apply(StationEvent.PlaybackEnded)

    fun onPlaybackFailed(reason: String) {
        logger.log("video failed: $reason")
        apply(StationEvent.PlaybackFailed)
    }

    /** The clip for a card, from whichever provider the card names. Throws with a reason. */
    suspend fun videoUrl(record: CardRecord): String =
        media(record).videoUrl ?: throw IllegalStateException("${record.provider}/${record.ref} hat kein Video")

    /** The card's own picture, for after the last round. Null when the provider has none. */
    suspend fun cardImageUrl(record: CardRecord): String? = media(record).cardImageUrl

    private suspend fun media(record: CardRecord): Media {
        val provider = providers[record.provider] ?: throw IllegalArgumentException("unbekannte Quelle ${record.provider}")
        val t0 = System.nanoTime()
        val media = provider.resolve(record.ref)
        logger.log("${record.provider}/${record.ref}: link after ${(System.nanoTime() - t0) / 1_000_000} ms")
        return media
    }

    private fun apply(event: StationEvent) {
        _state.update { current ->
            val next = station.next(current, event)
            if (next != current) logger.log("→ ${describe(next)}")
            next
        }
    }

    private fun describe(state: StationState): String =
        when (state) {
            StationState.Idle -> {
                "idle"
            }

            is StationState.Card -> {
                "${state.phase.name.lowercase()} ${state.record.ref} round ${state.round}" +
                    if (state.present) "" else " (card gone)"
            }

            is StationState.Unknown -> {
                "unknown ${state.tag}"
            }
        }
}
