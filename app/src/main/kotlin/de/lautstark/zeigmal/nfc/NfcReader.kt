package de.lautstark.zeigmal.nfc

import android.app.Activity
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import de.lautstark.zeigmal.core.CardRecord
import de.lautstark.zeigmal.core.TagEvent
import de.lautstark.zeigmal.core.TagId
import java.io.IOException

/** What writing a sticker came to. */
sealed interface WriteOutcome {
    data class Written(
        val tag: TagId,
        val record: CardRecord,
    ) : WriteOutcome

    /** The sticker already carries a record; nothing was written. The adult decides. */
    data class AlreadyWritten(
        val tag: TagId,
        val record: CardRecord,
    ) : WriteOutcome

    data class Failed(
        val tag: TagId,
        val reason: String,
    ) : WriteOutcome
}

/**
 * Reader mode, and nothing else. The foreground activity owns the NFC hardware
 * while it is on screen: no intent dispatch, no system sound, no other app
 * getting the tag first — which is what an appliance needs.
 *
 * Reading: every discovered tag is read for our NDEF record, and reported as
 * seen with or without one. Removal comes from [NfcAdapter.ignore]: the tag is
 * ignored while it stays in the field and the listener fires when the presence
 * check says it left — which on the Galaxy A51 happens every 200 ms while the
 * card lies still, so [de.lautstark.zeigmal.core.Presence] holds it.
 *
 * Writing: while [pendingWrite] is set, a discovered tag is written instead of
 * reported, unless it already carries a record and [overwrite] is false.
 */
class NfcReader(
    private val activity: Activity,
    private val onTag: (TagEvent) -> Unit,
    private val onWrite: (WriteOutcome) -> Unit,
) {
    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    private val main = Handler(Looper.getMainLooper())

    @Volatile var pendingWrite: CardRecord? = null

    @Volatile var overwrite: Boolean = false

    val available: Boolean get() = adapter != null
    val enabled: Boolean get() = adapter?.isEnabled == true

    fun start() {
        val nfc = adapter ?: return
        val extras = Bundle().apply { putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, PRESENCE_CHECK_MS) }
        nfc.enableReaderMode(activity, { tag -> discovered(nfc, tag) }, FLAGS, extras)
    }

    fun stop() {
        adapter?.disableReaderMode(activity)
    }

    private fun discovered(
        nfc: NfcAdapter,
        tag: Tag,
    ) {
        val id = TagId.of(tag.id)
        val toWrite = pendingWrite
        if (toWrite != null) {
            val outcome = write(tag, id, toWrite)
            main.post { onWrite(outcome) }
            if (outcome is WriteOutcome.Written) pendingWrite = null
            return
        }
        val technologies = tag.techList.map { it.substringAfterLast('.') }
        val record = read(tag)
        main.post { onTag(TagEvent.Seen(id, record, technologies)) }
        nfc.ignore(tag, DEBOUNCE_MS, { main.post { onTag(TagEvent.Gone(id)) } }, main)
    }

    /** Our record from the tag, or null: no NDEF, no record of ours, or a blink mid-read. */
    private fun read(tag: Tag): CardRecord? {
        val ndef = Ndef.get(tag) ?: return null
        return try {
            ndef.connect()
            val message = ndef.cachedNdefMessage ?: ndef.ndefMessage ?: return null
            message.records
                .firstOrNull { it.tnf == NdefRecord.TNF_EXTERNAL_TYPE && String(it.type, Charsets.US_ASCII) == EXTERNAL_TYPE }
                ?.payload
                ?.let(CardRecord::decode)
        } catch (e: IOException) {
            null
        } finally {
            runCatching { ndef.close() }
        }
    }

    private fun write(
        tag: Tag,
        id: TagId,
        record: CardRecord,
    ): WriteOutcome {
        val message = NdefMessage(NdefRecord.createExternal(CardRecord.NDEF_DOMAIN, CardRecord.NDEF_TYPE, record.encode()))
        val ndef = Ndef.get(tag)
        try {
            if (ndef != null) {
                ndef.connect()
                if (!overwrite) {
                    val existing = read(tag)
                    if (existing != null) return WriteOutcome.AlreadyWritten(id, existing)
                    if (!ndef.isConnected) ndef.connect()
                }
                if (!ndef.isWritable) return WriteOutcome.Failed(id, "Aufkleber ist schreibgeschützt")
                if (ndef.maxSize < message.byteArrayLength) return WriteOutcome.Failed(id, "Aufkleber zu klein (${ndef.maxSize} Bytes)")
                ndef.writeNdefMessage(message)
                return WriteOutcome.Written(id, record)
            }
            val formatable = NdefFormatable.get(tag) ?: return WriteOutcome.Failed(id, "Aufkleber kann kein NDEF")
            formatable.connect()
            formatable.format(message)
            return WriteOutcome.Written(id, record)
        } catch (e: Exception) {
            return WriteOutcome.Failed(id, e.message ?: e.javaClass.simpleName)
        } finally {
            runCatching { ndef?.close() }
        }
    }

    private companion object {
        const val EXTERNAL_TYPE = "${CardRecord.NDEF_DOMAIN}:${CardRecord.NDEF_TYPE}"

        // Every technology, so the log can say what a sticker is; no platform
        // sounds because the video is the sound. NDEF is read by hand above, so
        // the platform's own NDEF check is skipped.
        const val FLAGS =
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
        const val PRESENCE_CHECK_MS = 250
        const val DEBOUNCE_MS = 500
    }
}
