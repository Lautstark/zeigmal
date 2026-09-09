package de.lautstark.zeigmal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.lautstark.zeigmal.cardset.KartensatzDirectory
import de.lautstark.zeigmal.cardset.Loaded
import de.lautstark.zeigmal.cardset.TagId
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

    fun onTag(event: TagEvent) {
        when (event) {
            is TagEvent.Seen -> {
                log("seen ${event.tag} ${event.technologies.joinToString(",")}")
                apply(StationEvent.CardSeen(event.tag))
            }

            is TagEvent.Gone -> {
                log("gone ${event.tag}")
                apply(StationEvent.CardGone(event.tag))
            }
        }
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

    private fun apply(event: StationEvent) {
        _state.update { s ->
            val next = station.next(s.station, event)
            if (next != s.station) log("→ ${describe(next)}")
            s.copy(station = next)
        }
    }

    private fun describe(state: StationState): String =
        when (state) {
            StationState.Idle -> "idle"
            is StationState.Playing -> "playing ${state.entry.id} (${state.entry.speech.name.lowercase()}) run ${state.run}"
            is StationState.Unknown -> "unknown ${state.tag}"
        }

    private fun log(text: String) {
        _state.update { it.copy(log = (it.log + LogLine(System.currentTimeMillis(), text)).takeLast(LOG_LINES)) }
    }

    private companion object {
        const val KARTENSATZ_DIR = "kartensatz"
        const val LOG_LINES = 200
    }
}

/** Kept for the diagnostics screen, which shows the last tag in the form a person can copy into cards.json. */
fun TagId.pretty(): String = toString()
