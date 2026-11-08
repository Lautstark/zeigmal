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

/** One stack in the overview, with how many of its cards are on a sticker. */
data class ListEntry(
    val set: CardSet,
    val written: Int,
) {
    val total: Int get() = set.words.size
}

/**
 * The overview the adult mode opens on: the account's Lernlisten, newest
 * first, then the sets still in the app. [lists] is the last good load,
 * kept on the phone so the overview stands without a network.
 */
data class Lists(
    val entries: List<ListEntry> = emptyList(),
    val loading: Boolean = false,
    /** Why the last load failed; the entries are then the ones loaded before. */
    val failed: String? = null,
    /** How many of [entries] are Lernlisten; the rest are the sets in the app. */
    val fromSite: Int = 0,
)

/**
 * Where in the set the writing mode is, and what the sticker on the antenna is
 * up to. [written] holds each word's [SignBoxWord.key], not refs: two cards of
 * a set may share a sign.
 */
data class Writing(
    val set: CardSet = SignBox.set(null),
    val index: Int = 0,
    val cardImageUrl: String? = null,
    val lookupFailed: String? = null,
    val status: WriteStatus = WriteStatus.Waiting,
    val written: Set<String> = emptySet(),
    val overwriteNext: Boolean = false,
) {
    val word: SignBoxWord get() = set.words[index.coerceIn(0, set.words.lastIndex)]
    val total: Int get() = set.words.size
    val record: CardRecord get() = CardRecord(SignDigitalProvider.ID, word.ref, word.label)

    fun isWritten(word: SignBoxWord): Boolean = word.key in written

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

    /** The Lernlisten as last loaded; kept so the overview and a started list stand without a network. */
    private var siteSets: List<CardSet> = cachedSiteSets()

    private val _writing = MutableStateFlow(progress(set(store.get(KEY_WRITE_SET))))
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

    /** Every stack the adult can write: the Lernlisten first, then the sets in the app. */
    private fun allSets(): List<CardSet> = siteSets + SignBox.sets

    /**
     * The set with this id, wherever it comes from. An unknown id, or a list
     * emptied on the site, is the first set in the app: the writing mode always
     * has a card to stand on.
     */
    private fun set(id: String?): CardSet = allSets().firstOrNull { it.id == id && it.words.isNotEmpty() } ?: SignBox.set(null)

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
        val entries = allSets().filter { it.words.isNotEmpty() }.map { ListEntry(it, writtenIn(it).size) }
        _lists.update { it.copy(entries = entries, fromSite = siteSets.count { s -> s.words.isNotEmpty() }) }
    }

    private fun cachedSiteSets(): List<CardSet> =
        try {
            store.get(KEY_LISTS_CACHE)?.let { Json.decodeFromString(ListSerializer(CardSet.serializer()), it) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

    /**
     * Another stack of cards: each set keeps what is done, and opens on the
     * first card not yet written from where it stopped, so a stack picked up
     * again goes on with the next card in the pile.
     */
    fun chooseSet(id: String) {
        if (_writing.value.busy) return
        val set = set(id)
        doneHold?.cancel()
        store.put(KEY_WRITE_SET, set.id)
        val w = progress(set)
        _writing.value = w.copy(index = firstOpen(w))
        store.put(indexKey(set), _writing.value.index.toString())
        lookupCurrentCard()
    }

    private fun firstOpen(w: Writing): Int {
        val words = w.set.words
        val from = w.index.coerceIn(0, (words.size - 1).coerceAtLeast(0))
        return (from until words.size).firstOrNull { !w.isWritten(words[it]) }
            ?: (0 until from).firstOrNull { !w.isWritten(words[it]) }
            ?: from
    }

    /**
     * A set's progress as the store keeps it. SIGNbox 1 keeps its first keys,
     * which held refs before sets had two cards on one sign; a stored ref reads
     * as the label of its card.
     */
    private fun progress(set: CardSet): Writing =
        Writing(set = set, index = store.get(indexKey(set))?.toIntOrNull() ?: 0, written = writtenIn(set))

    private fun writtenIn(set: CardSet): Set<String> =
        store
            .get(writtenKey(set))
            ?.split(WRITTEN_SEPARATOR)
            ?.filter { it.isNotEmpty() }
            ?.map { e -> if (set.words.any { it.key == e }) e else set.words.firstOrNull { it.ref == e }?.key ?: e }
            ?.toSet() ?: emptySet()

    private fun indexKey(set: CardSet) = if (set.id == SignBox.BOX1) KEY_WRITE_INDEX else "write.${set.id}.index"

    private fun writtenKey(set: CardSet) = if (set.id == SignBox.BOX1) KEY_WRITTEN else "write.${set.id}.written"

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
        store.put(indexKey(_writing.value.set), clamped.toString())
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
                // Kept by the word's key; a record that is no word of this set (it
                // cannot be, but the label is what it would have been) by its label.
                val key =
                    set.words.firstOrNull { CardRecord(SignDigitalProvider.ID, it.ref, it.label) == outcome.record }?.key
                        ?: outcome.record.label
                val written = _writing.value.written + key
                store.put(writtenKey(set), written.joinToString(WRITTEN_SEPARATOR))
                // The box moves on now, so the reader writes the right card to a
                // sticker that comes early; the screen stays on this one a moment.
                // Only a write of the card the box is on moves it: an outcome for
                // a card the box has already left is a sticker written, not a step.
                val advance = outcome.record == _writing.value.record
                val here = _writing.value.index
                val next = if (advance) (here + 1).coerceAtMost(set.words.lastIndex) else here
                _writing.update { it.copy(written = written, status = WriteStatus.Done(outcome), overwriteNext = false, index = next) }
                store.put(indexKey(set), next.toString())
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
        const val KEY_WRITE_SET = "write.set"
        const val KEY_LISTS_CACHE = "lernlisten.cache"

        /**
         * Between the keys of what is written. A comma, as it always was: a
         * site id is hex and a label in the app has none; a Lernliste's word is
         * kept by its id, so its name may hold any comma it likes.
         */
        private const val WRITTEN_SEPARATOR = ","

        /** "Schreibt …" stays at least this long, or it is a flicker nobody reads. */
        const val MIN_BUSY_MILLIS = 400L

        /** "Geschrieben" stays this long before the next card comes up. */
        const val DONE_MILLIS = 1500L

        /** For this long after a write, the same sticker reporting itself written is still the same sticker. */
        const val SAME_STICKER_MILLIS = 3000L
    }
}
