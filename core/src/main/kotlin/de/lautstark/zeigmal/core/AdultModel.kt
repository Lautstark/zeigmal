package de.lautstark.zeigmal.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The login, as state the screen can draw. */
sealed interface Login {
    data object Out : Login

    data object Busy : Login

    data class Failed(
        val reason: String,
    ) : Login

    data class In(
        val email: String,
    ) : Login
}

/** Where in the box the writing mode is, and what the last sticker came to. */
data class Writing(
    val index: Int = 0,
    val cardImageUrl: String? = null,
    val lookupFailed: String? = null,
    val lastOutcome: WriteOutcome? = null,
    val written: Set<String> = emptySet(),
    val overwriteNext: Boolean = false,
) {
    val word: SignBoxWord get() = SignBox.box1[index.coerceIn(0, SignBox.box1.lastIndex)]
    val total: Int get() = SignBox.box1.size
    val record: CardRecord get() = CardRecord(SignDigitalProvider.ID, word.ref, word.label)
}

/**
 * The adult's side: the provider login and the writing mode, walking the box
 * card by card. Progress and the login are kept in the [KeyValueStore]; what
 * the reader should be doing follows from [writing] and is exposed as [tagMode]
 * for whoever owns the hardware.
 */
class AdultModel(
    private val scope: CoroutineScope,
    private val signDigital: SignDigitalProvider,
    private val store: KeyValueStore,
    private val logger: Logger = Logger { },
) {
    private val _login = MutableStateFlow<Login>(signDigital.email?.takeIf { signDigital.loggedIn }?.let { Login.In(it) } ?: Login.Out)
    val login: StateFlow<Login> = _login

    private val _writing =
        MutableStateFlow(
            Writing(
                index = store.get(KEY_WRITE_INDEX)?.toIntOrNull() ?: 0,
                written =
                    store
                        .get(KEY_WRITTEN)
                        ?.split(',')
                        ?.filter { it.isNotEmpty() }
                        ?.toSet() ?: emptySet(),
            ),
        )
    val writing: StateFlow<Writing> = _writing

    /** What the reader should do while the writing mode is up. */
    fun tagMode(active: Boolean): TagMode =
        if (active && _login.value is Login.In) {
            val w = _writing.value
            TagMode.Write(w.record, overwrite = w.overwriteNext)
        } else {
            TagMode.Read
        }

    fun login(
        email: String,
        password: String,
    ) {
        scope.launch {
            _login.value = Login.Busy
            try {
                signDigital.login(email.trim(), password)
                logger.log("signdigital: angemeldet als ${email.trim()}")
                _login.value = Login.In(email.trim())
                lookupCurrentCard()
            } catch (e: Exception) {
                logger.log("signdigital: Anmeldung fehlgeschlagen: ${e.message}")
                _login.value = Login.Failed(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun logout() {
        signDigital.logout()
        _login.value = Login.Out
        logger.log("signdigital: abgemeldet")
    }

    /** Called when the writing screen comes up, so the card picture is on its way. */
    fun enterWriting() {
        if (_writing.value.cardImageUrl == null) lookupCurrentCard()
    }

    fun goTo(index: Int) {
        val clamped = index.coerceIn(0, SignBox.box1.lastIndex)
        _writing.update { it.copy(index = clamped, cardImageUrl = null, lookupFailed = null, lastOutcome = null, overwriteNext = false) }
        store.put(KEY_WRITE_INDEX, clamped.toString())
        lookupCurrentCard()
    }

    fun skip() = goTo(_writing.value.index + 1)

    fun back() = goTo(_writing.value.index - 1)

    /** The adult chose to overwrite the sticker just seen: the next write goes through. */
    fun overwriteNext() = _writing.update { it.copy(lastOutcome = null, overwriteNext = true) }

    fun onWrite(outcome: WriteOutcome) {
        when (outcome) {
            is WriteOutcome.Written -> {
                logger.log("written ${outcome.tag}: ${outcome.record.provider}/${outcome.record.ref}")
                val written = _writing.value.written + outcome.record.ref
                store.put(KEY_WRITTEN, written.joinToString(","))
                val next = (_writing.value.index + 1).coerceAtMost(SignBox.box1.lastIndex)
                _writing.update {
                    it.copy(
                        written = written,
                        lastOutcome = outcome,
                        overwriteNext = false,
                        index = next,
                        cardImageUrl = null,
                        lookupFailed = null,
                    )
                }
                store.put(KEY_WRITE_INDEX, next.toString())
                lookupCurrentCard()
            }

            is WriteOutcome.AlreadyWritten -> {
                logger.log("sticker ${outcome.tag} already ${outcome.record.provider}/${outcome.record.ref}")
                _writing.update { it.copy(lastOutcome = outcome) }
            }

            is WriteOutcome.Failed -> {
                logger.log("write failed ${outcome.tag}: ${outcome.reason}")
                _writing.update { it.copy(lastOutcome = outcome, overwriteNext = false) }
            }
        }
    }

    private fun lookupCurrentCard() {
        val word = _writing.value.word
        scope.launch {
            try {
                val media = signDigital.resolve(word.ref)
                _writing.update { if (it.word == word) it.copy(cardImageUrl = media.cardImageUrl, lookupFailed = null) else it }
            } catch (e: Exception) {
                logger.log("card lookup ${word.ref}: ${e.message}")
                _writing.update { if (it.word == word) it.copy(lookupFailed = e.message ?: e.javaClass.simpleName) else it }
            }
        }
    }

    companion object {
        const val KEY_WRITE_INDEX = "write.index"
        const val KEY_WRITTEN = "write.written"
    }
}
