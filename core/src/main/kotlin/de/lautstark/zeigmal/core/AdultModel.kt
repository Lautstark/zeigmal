package de.lautstark.zeigmal.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

/** What the sticker on the antenna is up to, as the screen draws it. */
sealed interface WriteStatus {
    /** Nothing lies there, or nothing the reader has said anything about. */
    data object Waiting : WriteStatus

    /** The reader is writing; the sticker has to stay. */
    data class Busy(
        val tag: TagId,
    ) : WriteStatus

    /** Written. Shown for a moment before the box moves on; the index already has. */
    data class Done(
        val outcome: WriteOutcome.Written,
    ) : WriteStatus

    /** The sticker already carries another card; the adult decides. */
    data class Already(
        val outcome: WriteOutcome.AlreadyWritten,
    ) : WriteStatus

    /** The reader could not write; the adult tries again or moves on. */
    data class Failed(
        val outcome: WriteOutcome.Failed,
    ) : WriteStatus
}

/** Where in the box the writing mode is, and what the sticker on the antenna is up to. */
data class Writing(
    val index: Int = 0,
    val cardImageUrl: String? = null,
    val lookupFailed: String? = null,
    val status: WriteStatus = WriteStatus.Waiting,
    val written: Set<String> = emptySet(),
    val overwriteNext: Boolean = false,
) {
    val word: SignBoxWord get() = SignBox.box1[index.coerceIn(0, SignBox.box1.lastIndex)]
    val total: Int get() = SignBox.box1.size
    val record: CardRecord get() = CardRecord(SignDigitalProvider.ID, word.ref, word.label)

    /** The word on screen: while "written" is shown it is the one just written, not the next. */
    val shownLabel: String get() = (status as? WriteStatus.Done)?.outcome?.record?.label ?: word.label

    /** Whether the buttons should sit still: nothing is to be pressed while the reader writes. */
    val busy: Boolean get() = status is WriteStatus.Busy
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

    /* The reader's timing is too fast to see and the box's too fast to check;
       these three jobs slow the screen down to the adult's pace. */
    private var busyHold: Job? = null
    private var doneHold: Job? = null
    private var sameSticker: Job? = null
    private var lastWritten: WriteOutcome.Written? = null

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
        doneHold?.cancel()
        val clamped = index.coerceIn(0, SignBox.box1.lastIndex)
        _writing.update {
            it.copy(
                index = clamped,
                cardImageUrl = null,
                lookupFailed = null,
                status = WriteStatus.Waiting,
                overwriteNext = false,
            )
        }
        store.put(KEY_WRITE_INDEX, clamped.toString())
        lookupCurrentCard()
    }

    fun skip() = goTo(_writing.value.index + 1)

    /** The adult chose to overwrite the sticker just seen: the next write goes through. */
    fun overwriteNext() = _writing.update { it.copy(status = WriteStatus.Waiting, overwriteNext = true) }

    /**
     * After a failure: back to waiting. That is all it takes — the reader sees
     * a sticker that lies still again and again, so the next attempt follows on
     * its own, without lifting it.
     */
    fun retry() = _writing.update { if (it.status is WriteStatus.Failed) it.copy(status = WriteStatus.Waiting) else it }

    fun onWrite(event: WriteEvent) {
        when (event) {
            is WriteStarted -> {
                // A quick adult has the next sticker on before "written" has faded.
                if (doneHold?.isActive == true) endDone()
                logger.log("writing ${event.tag}")
                _writing.update { it.copy(status = WriteStatus.Busy(event.tag)) }
                busyHold = scope.launch { delay(MIN_BUSY_MILLIS) }
            }

            is WriteOutcome -> {
                // Written in 100 ms is written unseen; "Schreibt" stays up long enough to be read.
                val hold = busyHold?.takeIf { it.isActive }
                if (hold == null) {
                    apply(event)
                } else {
                    scope.launch {
                        hold.join()
                        apply(event)
                    }
                }
            }
        }
    }

    private fun apply(outcome: WriteOutcome) {
        busyHold = null
        when (outcome) {
            is WriteOutcome.Written -> {
                logger.log("written ${outcome.tag}: ${outcome.record.provider}/${outcome.record.ref}")
                val written = _writing.value.written + outcome.record.ref
                store.put(KEY_WRITTEN, written.joinToString(","))
                // The box moves on now, so the reader writes the right card to a
                // sticker that comes early; the screen stays on this one a moment.
                val next = (_writing.value.index + 1).coerceAtMost(SignBox.box1.lastIndex)
                _writing.update { it.copy(written = written, status = WriteStatus.Done(outcome), overwriteNext = false, index = next) }
                store.put(KEY_WRITE_INDEX, next.toString())
                rememberSticker(outcome)
                doneHold =
                    scope.launch {
                        delay(DONE_MILLIS)
                        endDone()
                    }
            }

            is WriteOutcome.AlreadyWritten -> {
                val last = lastWritten
                if (last != null && last.tag == outcome.tag && last.record == outcome.record) {
                    // The sticker just written, still lying there: the reader sees it
                    // three times a second, and none of those is news.
                    rememberSticker(last)
                    return
                }
                logger.log("sticker ${outcome.tag} already ${outcome.record.provider}/${outcome.record.ref}")
                _writing.update { it.copy(status = WriteStatus.Already(outcome)) }
            }

            is WriteOutcome.Failed -> {
                logger.log("write failed ${outcome.tag}: ${outcome.reason}")
                _writing.update { it.copy(status = WriteStatus.Failed(outcome), overwriteNext = false) }
            }
        }
    }

    /** "Written" has been seen: the next word, and its card on its way. */
    private fun endDone() {
        doneHold?.cancel()
        doneHold = null
        _writing.update {
            if (it.status is WriteStatus.Done) it.copy(status = WriteStatus.Waiting, cardImageUrl = null, lookupFailed = null) else it
        }
        lookupCurrentCard()
    }

    private fun rememberSticker(written: WriteOutcome.Written) {
        lastWritten = written
        sameSticker?.cancel()
        sameSticker =
            scope.launch {
                delay(SAME_STICKER_MILLIS)
                lastWritten = null
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

        /** "Schreibt …" stays at least this long, or it is a flicker nobody reads. */
        const val MIN_BUSY_MILLIS = 400L

        /** "Geschrieben" stays this long before the next card comes up. */
        const val DONE_MILLIS = 1500L

        /** For this long after a write, the same sticker reporting itself written is still the same sticker. */
        const val SAME_STICKER_MILLIS = 3000L
    }
}
