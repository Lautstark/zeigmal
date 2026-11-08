package de.lautstark.zeigmal.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

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

/** One stack in the overview. */
data class ListEntry(
    val set: CardSet,
) {
    val total: Int get() = set.words.size
}

/**
 * The overview the adult mode opens on: the account's Lernlisten, newest
 * first. They are the only lists there are; [entries] is the last good load,
 * kept on the phone so the overview stands without a network.
 */
data class Lists(
    val entries: List<ListEntry> = emptyList(),
    val loading: Boolean = false,
    /** Why the last load failed; the entries are then the ones loaded before. */
    val failed: String? = null,
)

/**
 * Where in the set the writing mode is, and what the sticker on the antenna is
 * up to. [counts] is how often each slug has gone on a sticker, from the
 * [WriteLog]: shown beside a card, never used to skip or resume. The adult
 * holds the stack and taps the card on top of it.
 */
data class Writing(
    val set: CardSet = SignBox.set(null),
    val index: Int = 0,
    val cardImageUrl: String? = null,
    val lookupFailed: String? = null,
    val status: WriteStatus = WriteStatus.Waiting,
    val counts: Map<String, Int> = emptyMap(),
    val overwriteNext: Boolean = false,
) {
    val word: SignBoxWord get() = set.words[index.coerceIn(0, set.words.lastIndex)]
    val total: Int get() = set.words.size
    val record: CardRecord get() = CardRecord(SignDigitalProvider.ID, word.ref, word.label)

    /** How often this card's sign has gone on a sticker, from any list. */
    fun count(word: SignBoxWord): Int = counts[word.ref] ?: 0

    /** The word on screen: while "written" is shown it is the one just written, not the next. */
    val shownLabel: String get() = (status as? WriteStatus.Done)?.outcome?.record?.label ?: word.label

    /** Whether the buttons should sit still: nothing is to be pressed while the reader writes. */
    val busy: Boolean get() = status is WriteStatus.Busy
}

/**
 * The adult's side: the provider login and the writing mode, walking a stack
 * card by card. The login and the lists last loaded are kept in the
 * [KeyValueStore], every sticker written in the [WriteLog]; what the reader
 * should be doing follows from [writing] and is exposed as [tagMode] for
 * whoever owns the hardware.
 */
class AdultModel(
    private val scope: CoroutineScope,
    private val signDigital: SignDigitalProvider,
    private val store: KeyValueStore,
    private val logger: Logger = Logger { },
    private val writeLog: WriteLog = MemoryWriteLog(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _login = MutableStateFlow<Login>(signDigital.email?.takeIf { signDigital.loggedIn }?.let { Login.In(it) } ?: Login.Out)
    val login: StateFlow<Login> = _login

    /** The Lernlisten as last loaded; kept so the overview and a started list stand without a network. */
    private var siteSets: List<CardSet> = cachedSiteSets()

    init {
        takeOverOldTicks()
    }

    private val _writing = MutableStateFlow(Writing(counts = counts()))
    val writing: StateFlow<Writing> = _writing

    private val _lists = MutableStateFlow(Lists())
    val lists: StateFlow<Lists> = _lists

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

    /**
     * The Lernliste with this id. An unknown id, or a list emptied on the site,
     * is SIGNbox 1 as it was in the app: the writing mode always has a card to
     * stand on, and the overview never offers that one.
     */
    private fun set(id: String?): CardSet = siteSets.firstOrNull { it.id == id && it.words.isNotEmpty() } ?: SignBox.set(null)

    /** The overview comes up: what is known now, and a fresh load from the site. */
    fun enterLists() {
        publishLists()
        loadLists()
    }

    /** Reads the account's Lernlisten. A failure keeps the ones loaded before and says why. */
    fun loadLists() {
        if (_lists.value.loading) return
        _lists.update { it.copy(loading = true) }
        scope.launch {
            try {
                val loaded = signDigital.lists().map(Lernliste::toCardSet)
                siteSets = loaded
                store.put(KEY_LISTS_CACHE, Json.encodeToString(ListSerializer(CardSet.serializer()), loaded))
                logger.log("lernlisten: ${loaded.size} geladen")
                // The list being written may have changed on the site: take the new one, keep the place.
                val current = _writing.value.set
                val fresh = loaded.firstOrNull { it.id == current.id }
                if (fresh != null && fresh != current && fresh.words.isNotEmpty() && !_writing.value.busy) {
                    _writing.update { it.copy(set = fresh, index = it.index.coerceAtMost(fresh.words.lastIndex)) }
                }
                _lists.update { it.copy(loading = false, failed = null) }
            } catch (e: Exception) {
                logger.log("lernlisten: ${e.message}")
                _lists.update { it.copy(loading = false, failed = e.message ?: e.javaClass.simpleName) }
            }
            publishLists()
        }
    }

    private fun publishLists() {
        _lists.update { it.copy(entries = siteSets.filter { s -> s.words.isNotEmpty() }.map(::ListEntry)) }
    }

    private fun cachedSiteSets(): List<CardSet> =
        try {
            store.get(KEY_LISTS_CACHE)?.let { Json.decodeFromString(ListSerializer(CardSet.serializer()), it) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

    /** Another stack of cards, from its first card: the adult taps the one on top of the pile. */
    fun chooseSet(id: String) {
        if (_writing.value.busy) return
        doneHold?.cancel()
        _writing.value = Writing(set = set(id), counts = counts())
        lookupCurrentCard()
    }

    private fun counts(): Map<String, Int> = writeLog.all().groupingBy { it.slug }.eachCount()

    /**
     * Until 2026-11 the app kept ticks per set and a place in each. They become
     * log entries without a time, one per ticked card, so "how often" starts
     * right; then the old keys go, so this has nothing to do the next time.
     */
    private fun takeOverOldTicks() {
        for (old in SignBox.sets) {
            val key = if (old.id == SignBox.BOX1) OLD_KEY_WRITTEN else "write.${old.id}.written"
            val ticks = store.get(key) ?: continue
            for (tick in ticks.split(',').filter { it.isNotEmpty() }) {
                // SIGNbox 1 once kept refs here; either reads as its card.
                val word = old.words.firstOrNull { it.label == tick } ?: old.words.firstOrNull { it.ref == tick } ?: continue
                writeLog.append(WriteEntry(at = null, slug = word.ref, label = word.label, list = old.title))
            }
            store.put(key, null)
            logger.log("übernommen: Häkchen aus ${old.title}")
        }
        store.put(OLD_KEY_WRITE_INDEX, null)
        store.put(OLD_KEY_WRITE_SET, null)
        for (old in SignBox.sets) store.put("write.${old.id}.index", null)
    }

    fun goTo(index: Int) {
        doneHold?.cancel()
        val clamped = index.coerceIn(0, _writing.value.set.words.lastIndex)
        _writing.update {
            it.copy(
                index = clamped,
                cardImageUrl = null,
                lookupFailed = null,
                status = WriteStatus.Waiting,
                overwriteNext = false,
            )
        }
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
                val last = lastWritten
                if (last != null && last.tag == outcome.tag && last.record == outcome.record) {
                    // The same sticker written with the same card again. After an
                    // overwrite the reader keeps the overwrite mode until this
                    // outcome is applied, which is only after the busy hold — and
                    // the A51 rediscovers a resting sticker every ~290 ms, inside
                    // that hold. So the sticker is written twice, and counting the
                    // second as a card skipped the next word in the box. One
                    // sticker is one word, however often the reader wrote it.
                    logger.log("written ${outcome.tag} again, same card: not a new one")
                    rememberSticker(last)
                    _writing.update { if (it.status is WriteStatus.Busy) it.copy(status = WriteStatus.Waiting) else it }
                    return
                }
                logger.log("written ${outcome.tag}: ${outcome.record.provider}/${outcome.record.ref}")
                val set = _writing.value.set
                val word = set.words.firstOrNull { CardRecord(SignDigitalProvider.ID, it.ref, it.label) == outcome.record }
                writeLog.append(
                    WriteEntry(
                        at = clock(),
                        slug = outcome.record.ref,
                        label = outcome.record.label,
                        signId = word?.id,
                        list = set.title,
                        tag = outcome.tag.hex,
                    ),
                )
                val counts = _writing.value.counts.let { it + (outcome.record.ref to (it[outcome.record.ref] ?: 0) + 1) }
                // The box moves on now, so the reader writes the right card to a
                // sticker that comes early; the screen stays on this one a moment.
                // Only a write of the card the box is on moves it: an outcome for
                // a card the box has already left is a sticker written, not a step.
                val advance = outcome.record == _writing.value.record
                val here = _writing.value.index
                val next = if (advance) (here + 1).coerceAtMost(set.words.lastIndex) else here
                _writing.update { it.copy(counts = counts, status = WriteStatus.Done(outcome), overwriteNext = false, index = next) }
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
        const val KEY_LISTS_CACHE = "lernlisten.cache"

        /** Where the app kept ticks and places before the write log; read once, then removed. */
        const val OLD_KEY_WRITE_INDEX = "write.index"
        const val OLD_KEY_WRITTEN = "write.written"
        const val OLD_KEY_WRITE_SET = "write.set"

        /** "Schreibt …" stays at least this long, or it is a flicker nobody reads. */
        const val MIN_BUSY_MILLIS = 400L

        /** "Geschrieben" stays this long before the next card comes up. */
        const val DONE_MILLIS = 1500L

        /** For this long after a write, the same sticker reporting itself written is still the same sticker. */
        const val SAME_STICKER_MILLIS = 3000L
    }
}
