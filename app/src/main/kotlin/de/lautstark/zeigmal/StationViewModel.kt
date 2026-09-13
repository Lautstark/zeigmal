package de.lautstark.zeigmal

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.lautstark.zeigmal.core.CardRecord
import de.lautstark.zeigmal.core.CredentialStore
import de.lautstark.zeigmal.core.Media
import de.lautstark.zeigmal.core.Presence
import de.lautstark.zeigmal.core.Provider
import de.lautstark.zeigmal.core.SignBox
import de.lautstark.zeigmal.core.SignBoxWord
import de.lautstark.zeigmal.core.SignDigitalProvider
import de.lautstark.zeigmal.core.Station
import de.lautstark.zeigmal.core.StationEvent
import de.lautstark.zeigmal.core.StationState
import de.lautstark.zeigmal.core.TagEvent
import de.lautstark.zeigmal.nfc.WriteOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The three faces of the app. A long press leaves KID; Back returns to it. */
enum class Mode { KID, LOGIN, WRITE }

data class LogLine(
    val atMillis: Long,
    val text: String,
)

/** The writing mode's state: where in the box we are, and what the last sticker came to. */
data class Writing(
    val index: Int = 0,
    val cardImageUrl: String? = null,
    val lookupFailed: String? = null,
    val lastOutcome: WriteOutcome? = null,
    val written: Set<String> = emptySet(),
) {
    val word: SignBoxWord get() = SignBox.box1[index.coerceIn(0, SignBox.box1.lastIndex)]
    val total: Int get() = SignBox.box1.size
}

data class UiState(
    val mode: Mode = Mode.KID,
    val station: StationState = StationState.Idle,
    val nfcAvailable: Boolean = true,
    val nfcEnabled: Boolean = true,
    val account: String? = null,
    val busy: Boolean = false,
    val writing: Writing = Writing(),
    val log: List<LogLine> = emptyList(),
    val showLog: Boolean = false,
)

class StationViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val prefs = application.getSharedPreferences("zeigmal", Context.MODE_PRIVATE)
    private val credentials =
        object : CredentialStore {
            override fun get(key: String): String? = prefs.getString(key, null)

            override fun put(
                key: String,
                value: String?,
            ) = prefs.edit { if (value == null) remove(key) else putString(key, value) }
        }
    private val signDigital = SignDigitalProvider(credentials)
    private val providers: Map<String, Provider> = listOf<Provider>(signDigital).associateBy { it.id }

    private val station = Station()
    private val main = Handler(Looper.getMainLooper())
    private val presence =
        Presence(
            holdMillis = Presence.DEFAULT_HOLD_MILLIS,
            schedule = { delay, action ->
                val runnable = Runnable(action)
                main.postDelayed(runnable, delay)
                Presence.Cancel { main.removeCallbacks(runnable) }
            },
            onEvent = ::onCard,
        )

    init {
        _state.update {
            it.copy(
                account = signDigital.email.takeIf { _ -> signDigital.loggedIn },
                writing =
                    Writing(
                        index = prefs.getInt(KEY_WRITE_INDEX, 0),
                        written = prefs.getStringSet(KEY_WRITTEN, emptySet()).orEmpty(),
                    ),
            )
        }
    }

    // ---- the reader

    fun nfcStatus(
        available: Boolean,
        enabled: Boolean,
    ) = _state.update { it.copy(nfcAvailable = available, nfcEnabled = enabled) }

    /** Raw from the reader: logged as it comes, then filtered by [Presence]. */
    fun onTag(event: TagEvent) {
        when (event) {
            is TagEvent.Seen -> {
                log(
                    "reader: seen ${event.tag} ${event.record?.let {
                        "${it.provider}/${it.ref}"
                    } ?: "kein Eintrag"} ${event.technologies.joinToString(
                        ",",
                    )}",
                )
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
                apply(StationEvent.CardSeen(event.tag, event.record))
            }

            is TagEvent.Gone -> {
                log("card out ${event.tag}")
                apply(StationEvent.CardGone(event.tag))
            }
        }
    }

    fun onFirstFrame() = apply(StationEvent.FirstFrame)

    fun onPlaybackEnded() = apply(StationEvent.PlaybackEnded)

    fun onPlaybackFailed(reason: String) {
        log("video failed: $reason")
        apply(StationEvent.PlaybackFailed)
    }

    private fun apply(event: StationEvent) {
        val current = _state.value.station
        val next = station.next(current, event)
        if (next != current) log("→ ${describe(next)}")
        _state.update { it.copy(station = next) }
    }

    /** The clip for a card, from whichever provider the card names. Throws with a reason. */
    suspend fun mediaFor(record: CardRecord): Media {
        val provider = providers[record.provider] ?: throw IllegalArgumentException("unbekannte Quelle ${record.provider}")
        val t0 = System.nanoTime()
        val media = provider.resolve(record.ref)
        log("${record.provider}/${record.ref}: link after ${(System.nanoTime() - t0) / 1_000_000} ms")
        return media
    }

    // ---- modes

    fun enterAdult() {
        _state.update { it.copy(mode = if (it.account == null) Mode.LOGIN else Mode.WRITE) }
        if (_state.value.mode == Mode.WRITE) lookupCurrentCard()
    }

    fun leaveAdult() = _state.update { it.copy(mode = Mode.KID, showLog = false) }

    fun toggleLog() = _state.update { it.copy(showLog = !it.showLog) }

    fun login(
        email: String,
        password: String,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                signDigital.login(email.trim(), password)
                log("signdigital: angemeldet als ${email.trim()}")
                _state.update { it.copy(account = email.trim(), mode = Mode.WRITE) }
                lookupCurrentCard()
            } catch (e: Exception) {
                log("signdigital: Anmeldung fehlgeschlagen: ${e.message}")
            }
            _state.update { it.copy(busy = false) }
        }
    }

    fun logout() {
        signDigital.logout()
        _state.update { it.copy(account = null, mode = Mode.LOGIN) }
        log("signdigital: abgemeldet")
    }

    // ---- writing cards

    /** What the reader should write to the next sticker it sees, or null when not in that mode. */
    val pendingRecord: CardRecord?
        get() =
            _state.value.takeIf { it.mode == Mode.WRITE }?.writing?.word?.let {
                CardRecord(SignDigitalProvider.ID, it.ref, it.label)
            }

    fun goTo(index: Int) {
        _state.update {
            it.copy(
                writing =
                    it.writing.copy(
                        index = index.coerceIn(0, SignBox.box1.lastIndex),
                        cardImageUrl = null,
                        lookupFailed = null,
                        lastOutcome = null,
                    ),
            )
        }
        prefs.edit { putInt(KEY_WRITE_INDEX, _state.value.writing.index) }
        lookupCurrentCard()
    }

    fun skip() = goTo(_state.value.writing.index + 1)

    fun back() = goTo(_state.value.writing.index - 1)

    fun onWrite(outcome: WriteOutcome) {
        when (outcome) {
            is WriteOutcome.Written -> {
                log("written ${outcome.tag}: ${outcome.record.provider}/${outcome.record.ref}")
                val written = _state.value.writing.written + outcome.record.ref
                prefs.edit { putStringSet(KEY_WRITTEN, written) }
                _state.update { it.copy(writing = it.writing.copy(written = written, lastOutcome = outcome)) }
                // On to the next card; the confirmation line carries the one just done.
                val next = _state.value.writing.index + 1
                if (next < SignBox.box1.size) {
                    _state.update { it.copy(writing = it.writing.copy(index = next, cardImageUrl = null, lookupFailed = null)) }
                    prefs.edit { putInt(KEY_WRITE_INDEX, next) }
                    lookupCurrentCard()
                }
            }

            is WriteOutcome.AlreadyWritten -> {
                log("sticker ${outcome.tag} already ${outcome.record.provider}/${outcome.record.ref}")
                _state.update { it.copy(writing = it.writing.copy(lastOutcome = outcome)) }
            }

            is WriteOutcome.Failed -> {
                log("write failed ${outcome.tag}: ${outcome.reason}")
                _state.update { it.copy(writing = it.writing.copy(lastOutcome = outcome)) }
            }
        }
    }

    /** The adult chose to overwrite the sticker just seen. The reader is told once. */
    var overwriteNext: Boolean = false
        private set

    fun overwriteNext() {
        overwriteNext = true
        _state.update { it.copy(writing = it.writing.copy(lastOutcome = null)) }
    }

    fun overwriteDone() {
        overwriteNext = false
    }

    private fun lookupCurrentCard() {
        val word = _state.value.writing.word
        viewModelScope.launch {
            try {
                val media = signDigital.resolve(word.ref)
                _state.update { s ->
                    if (s.writing.word ==
                        word
                    ) {
                        s.copy(writing = s.writing.copy(cardImageUrl = media.cardImageUrl, lookupFailed = null))
                    } else {
                        s
                    }
                }
            } catch (e: Exception) {
                log("card lookup ${word.ref}: ${e.message}")
                _state.update { s ->
                    if (s.writing.word == word) s.copy(writing = s.writing.copy(lookupFailed = e.message ?: "?")) else s
                }
            }
        }
    }

    // ---- log

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

    private fun log(text: String) =
        _state.update { it.copy(log = (it.log + LogLine(System.currentTimeMillis(), text)).takeLast(LOG_LINES)) }

    private companion object {
        const val LOG_LINES = 200
        const val KEY_WRITE_INDEX = "write.index"
        const val KEY_WRITTEN = "write.written"
    }
}
