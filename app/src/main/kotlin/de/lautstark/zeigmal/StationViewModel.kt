package de.lautstark.zeigmal

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.lautstark.zeigmal.cardset.KartensatzDirectory
import de.lautstark.zeigmal.cardset.Loaded
import de.lautstark.zeigmal.cardset.TagId
import de.lautstark.zeigmal.nfc.Presence
import de.lautstark.zeigmal.nfc.TagEvent
import de.lautstark.zeigmal.station.Station
import de.lautstark.zeigmal.station.StationEvent
import de.lautstark.zeigmal.station.StationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** One line of the diagnostics log: what the reader saw, when. */
data class LogLine(
    val atMillis: Long,
    val text: String,
)

data class UiState(
    val loaded: Loaded? = null,
    val station: StationState = StationState.Idle,
    val log: List<LogLine> = emptyList(),
    val diagnostics: Boolean = false,
    val nfcAvailable: Boolean = true,
    val nfcEnabled: Boolean = true,
)

class StationViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    /**
     * The Kartensatz lives in the app's external files directory: reachable over
     * a USB cable and `adb push` without any permission, gone with the app, and
     * never part of a cloud backup. docs/media-import.md.
     */
    val directory: File =
        File(application.getExternalFilesDir(null) ?: application.filesDir, KARTENSATZ_DIR)

    private var station = Station { null }

    private val main = Handler(Looper.getMainLooper())
    private val presence =
        Presence(
            holdMillis = HOLD_MILLIS,
            schedule = { delay, action ->
                val runnable = Runnable(action)
                main.postDelayed(runnable, delay)
                Presence.Cancel { main.removeCallbacks(runnable) }
            },
            onEvent = ::onCard,
        )

    fun reload() {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { KartensatzDirectory.load(directory) }
            station = Station { tag -> (loaded as? Loaded.Ready)?.resolve(tag) }
            _state.update { it.copy(loaded = loaded, station = StationState.Idle) }
            log(
                when (loaded) {
                    is Loaded.Missing -> {
                        "no Kartensatz at ${loaded.dir}"
                    }

                    is Loaded.Rejected -> {
                        "${loaded.file} refused: ${loaded.code} ${loaded.detail}"
                    }

                    is Loaded.Ready -> {
                        "Kartensatz '${loaded.set.name}': ${loaded.set.entries.size} entries, " +
                            "${loaded.cards.size} cards, ${loaded.warnings.size} warnings"
                    }
                },
            )
        }
    }

    /** Raw from the reader: logged as it comes, then filtered by [Presence]. */
    fun onTag(event: TagEvent) {
        when (event) {
            is TagEvent.Seen -> {
                log("reader: seen ${event.tag} ${event.technologies.joinToString(",")}")
                presence.seen(event)
            }

            is TagEvent.Gone -> {
                log("reader: gone ${event.tag}")
                presence.gone(event)
            }
        }
    }

    /** After the debounce: what the station acts on. */
    private fun onCard(event: TagEvent) {
        when (event) {
            is TagEvent.Seen -> {
                log("card in ${event.tag}")
                apply(StationEvent.CardSeen(event.tag))
            }

            is TagEvent.Gone -> {
                log("card out ${event.tag}")
                apply(StationEvent.CardGone(event.tag))
            }
        }
    }

    fun onFirstFrame() {
        apply(StationEvent.FirstFrame)
    }

    fun onPlaybackEnded() {
        apply(StationEvent.PlaybackEnded)
    }

    fun nfcStatus(
        available: Boolean,
        enabled: Boolean,
    ) {
        _state.update { it.copy(nfcAvailable = available, nfcEnabled = enabled) }
    }

    fun toggleDiagnostics() {
        _state.update { it.copy(diagnostics = !it.diagnostics) }
    }

    // Everything here runs on the main thread, so reading the state, deciding
    // the next one and writing it back is one uninterrupted step. The log line
    // is written outside the update on purpose: `update` retries until its
    // compare-and-set wins, and a log written from inside it changes the state
    // it is comparing against, so it never wins. That was an ANR on the first
    // tag the Galaxy A51 ever saw (2026-09-13).
    private fun apply(event: StationEvent) {
        val current = _state.value.station
        val next = station.next(current, event)
        if (next != current) log("→ ${describe(next)}")
        _state.update { it.copy(station = next) }
    }

    private fun describe(state: StationState): String =
        when (state) {
            StationState.Idle -> {
                "idle"
            }

            is StationState.Card -> {
                "${state.phase.name.lowercase()} ${state.entry.id} (${state.entry.speech.name.lowercase()}) run ${state.run}" +
                    if (state.present) "" else " card gone"
            }

            is StationState.Unknown -> {
                "unknown ${state.tag}"
            }
        }

    private fun log(text: String) {
        _state.update { it.copy(log = (it.log + LogLine(System.currentTimeMillis(), text)).takeLast(LOG_LINES)) }
    }

    private companion object {
        const val KARTENSATZ_DIR = "kartensatz"
        const val LOG_LINES = 200

        // How long a card may be unseen before it counts as gone. The A51 loses a
        // resting card for ~80 ms three times a second; a second is far above
        // that and still feels immediate for "card out → ready".
        const val HOLD_MILLIS = 1000L
    }
}

/** Kept for the diagnostics screen, which shows the last tag in the form a person can copy into cards.json. */
fun TagId.pretty(): String = toString()
