package de.lautstark.zeigmal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.lautstark.zeigmal.core.AdultModel
import de.lautstark.zeigmal.core.KeyValueStore
import de.lautstark.zeigmal.core.Logger
import de.lautstark.zeigmal.core.Login
import de.lautstark.zeigmal.core.Pin
import de.lautstark.zeigmal.core.Provider
import de.lautstark.zeigmal.core.Settings
import de.lautstark.zeigmal.core.SignDigitalProvider
import de.lautstark.zeigmal.core.StationController
import de.lautstark.zeigmal.core.TagMode
import de.lautstark.zeigmal.core.TagSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The faces of the app. The corner held for two seconds leaves KID for PIN; Back returns to KID. */
enum class Mode { KID, PIN, LOGIN, WRITE, SETTINGS, PIN_CHANGE }

data class LogLine(
    val atMillis: Long,
    val text: String,
)

/**
 * Wiring, nothing more: the station controller for the child, the adult model
 * for login and writing, a log both write to, and the mode that says which
 * screen is up. The hardware is attached by whoever owns it (the activity) and
 * driven from here: events in, the mode it should be in out.
 */
class ZeigmalViewModel(
    store: KeyValueStore,
    providers: Map<String, Provider>,
) : ViewModel() {
    private val _log = MutableStateFlow<List<LogLine>>(emptyList())
    val log: StateFlow<List<LogLine>> = _log
    private val logger = Logger { line -> _log.update { (it + LogLine(System.currentTimeMillis(), line)).takeLast(LOG_LINES) } }

    val station = StationController(viewModelScope, providers, logger)
    val pin = Pin(store)
    val settings = Settings(store)

    /** True on the PIN screen when no PIN exists yet: the first entry sets it. */
    val pinIsNew: Boolean get() = !pin.isSet

    private val _pinRejected = MutableStateFlow(false)
    val pinRejected: StateFlow<Boolean> = _pinRejected
    val adult = AdultModel(viewModelScope, providers.getValue(SignDigitalProvider.ID) as SignDigitalProvider, store, logger)

    private val _mode = MutableStateFlow(Mode.KID)
    val mode: StateFlow<Mode> = _mode

    private val _showLog = MutableStateFlow(false)
    val showLog: StateFlow<Boolean> = _showLog

    private val _nfc = MutableStateFlow(true to true)

    /** available to enabled */
    val nfc: StateFlow<Pair<Boolean, Boolean>> = _nfc

    private var attached: List<Job> = emptyList()

    /** The hardware, for as long as the activity that owns it lives. */
    fun attach(source: TagSource) {
        detach()
        _nfc.value = source.available to source.enabled
        attached =
            listOf(
                viewModelScope.launch { source.tags.collect(station::onTag) },
                viewModelScope.launch { source.writes.collect(adult::onWrite) },
                viewModelScope.launch {
                    combine(_mode, adult.login, adult.writing) { mode, _, _ -> adult.tagMode(active = mode == Mode.WRITE) }
                        .collect(source::setMode)
                },
            )
    }

    fun detach() {
        attached.forEach { it.cancel() }
        attached = emptyList()
    }

    /** The corner was held: ask for the PIN. */
    fun askPin() {
        _pinRejected.value = false
        _mode.value = Mode.PIN
    }

    /** Four digits typed. The first ones ever become the PIN; on the change screen they replace it. */
    fun pinEntered(digits: String) {
        if (_mode.value == Mode.PIN_CHANGE) {
            pin.set(digits)
            logger.log("pin geändert")
            _mode.value = Mode.SETTINGS
            return
        }
        if (!pin.isSet) {
            pin.set(digits)
            logger.log("pin gesetzt")
            enterAdult()
            return
        }
        if (pin.verify(digits)) {
            _pinRejected.value = false
            enterAdult()
        } else {
            logger.log("pin falsch")
            _pinRejected.value = true
        }
    }

    /** Past the PIN (or the debug login): the login, or the writing mode when logged in. */
    fun enterAdult() {
        val next = if (adult.login.value is Login.In) Mode.WRITE else Mode.LOGIN
        _mode.value = next
        if (next == Mode.WRITE) adult.enterWriting()
    }

    fun leaveAdult() {
        _mode.value = Mode.KID
        _showLog.value = false
    }

    fun openSettings() {
        _mode.value = Mode.SETTINGS
    }

    fun closeSettings() {
        _mode.value = if (adult.login.value is Login.In) Mode.WRITE else Mode.LOGIN
        if (_mode.value == Mode.WRITE) adult.enterWriting()
    }

    fun changePin() {
        _pinRejected.value = false
        _mode.value = Mode.PIN_CHANGE
    }

    /** Sign out, then straight to the login: the "re-login" an adult asks for when the password changed. */
    fun relogin() {
        adult.logout()
        _mode.value = Mode.LOGIN
    }

    fun toggleLog() = _showLog.update { !it }

    // Once logged in, the login screen gives way to the writing mode; logged
    // out from the writing mode, it comes back.
    init {
        viewModelScope.launch {
            adult.login.collect { login ->
                if (login is Login.In && _mode.value == Mode.LOGIN) {
                    _mode.value = Mode.WRITE
                    adult.enterWriting()
                }
                if (login is Login.Out && _mode.value == Mode.WRITE) _mode.value = Mode.LOGIN
            }
        }
    }

    override fun onCleared() = detach()

    class Factory(
        private val store: KeyValueStore,
        private val providers: Map<String, Provider>,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ZeigmalViewModel(store, providers) as T
    }

    private companion object {
        const val LOG_LINES = 200
    }

    /** What the reader is asked to do right now; for tests and the log. */
    val currentTagMode: TagMode get() = adult.tagMode(active = _mode.value == Mode.WRITE)
}
