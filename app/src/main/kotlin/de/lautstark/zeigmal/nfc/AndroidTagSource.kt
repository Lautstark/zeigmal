package de.lautstark.zeigmal.nfc

import android.app.Activity
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import de.lautstark.zeigmal.core.CardRecord
import de.lautstark.zeigmal.core.TagEvent
import de.lautstark.zeigmal.core.TagId
import de.lautstark.zeigmal.core.TagMode
import de.lautstark.zeigmal.core.TagSource
import de.lautstark.zeigmal.core.WriteEvent
import de.lautstark.zeigmal.core.WriteOutcome
import de.lautstark.zeigmal.core.WriteStarted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException

/**
 * The phone's NFC hardware as a [TagSource]: reader mode while the activity is
 * in front, our NDEF record read on every discovery, and — in [TagMode.Write] —
 * the record written to the sticker instead.
 *
 * Reader mode means the foreground activity owns the hardware: no intent
 * dispatch, no system sound, no other app getting the tag first. Removal comes
 * from [NfcAdapter.ignore]: the tag is ignored while it stays in the field and
 * the listener fires when the presence check says it left — which on the
 * Galaxy A51 is every 200 ms while the card lies still, so the core's
 * `Presence` holds it. Nothing here decides anything; it reports.
 */
class AndroidTagSource(
    private val activity: Activity,
) : TagSource {
    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    private val _tags = MutableSharedFlow<TagEvent>(extraBufferCapacity = 64)
    private val _writes = MutableSharedFlow<WriteEvent>(extraBufferCapacity = 64)
    private val _mode = MutableStateFlow<TagMode>(TagMode.Read)

    override val tags: SharedFlow<TagEvent> = _tags
    override val writes: SharedFlow<WriteEvent> = _writes
    override val mode: StateFlow<TagMode> = _mode
    override val available: Boolean get() = adapter != null
    override val enabled: Boolean get() = adapter?.isEnabled == true

    override fun setMode(mode: TagMode) {
        _mode.value = mode
    }

    fun start() {
        val nfc = adapter ?: return
        val extras = Bundle().apply { putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, PRESENCE_CHECK_MS) }
        nfc.enableReaderMode(activity, { tag -> discovered(nfc, tag) }, FLAGS, extras)
    }

    fun stop() {
        adapter?.disableReaderMode(activity)
    }

    /** On the NFC binder thread. */
    private fun discovered(
        nfc: NfcAdapter,
        tag: Tag,
    ) {
        val id = TagId.of(tag.id)
        when (val mode = _mode.value) {
            is TagMode.Write -> {
                // A sticker that already carries a record is reported without a
                // "writing": the one just written lies there and is seen again
                // and again, and the core tells those apart from a stranger.
                if (!mode.overwrite) {
                    read(tag)?.let {
                        _writes.tryEmit(WriteOutcome.AlreadyWritten(id, it))
                        return
                    }
                }
                _writes.tryEmit(WriteStarted(id))
                _writes.tryEmit(write(tag, id, mode))
            }

            TagMode.Read -> {
                val technologies = tag.techList.map { it.substringAfterLast('.') }
                _tags.tryEmit(TagEvent.Seen(id, read(tag), technologies))
                nfc.ignore(tag, DEBOUNCE_MS, { _tags.tryEmit(TagEvent.Gone(id)) }, null)
            }
        }
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
        mode: TagMode.Write,
    ): WriteOutcome {
        val message = NdefMessage(NdefRecord.createExternal(CardRecord.NDEF_DOMAIN, CardRecord.NDEF_TYPE, mode.record.encode()))
        val ndef = Ndef.get(tag)
        try {
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) return WriteOutcome.Failed(id, "Aufkleber ist schreibgeschützt")
                if (ndef.maxSize < message.byteArrayLength) return WriteOutcome.Failed(id, "Aufkleber zu klein (${ndef.maxSize} Bytes)")
                ndef.writeNdefMessage(message)
                return WriteOutcome.Written(id, mode.record)
            }
            val formatable = NdefFormatable.get(tag) ?: return WriteOutcome.Failed(id, "Aufkleber kann kein NDEF")
            formatable.connect()
            formatable.format(message)
            return WriteOutcome.Written(id, mode.record)
        } catch (e: Exception) {
            return WriteOutcome.Failed(id, e.message ?: e.javaClass.simpleName)
        } finally {
            runCatching { ndef?.close() }
        }
    }

    private companion object {
        const val EXTERNAL_TYPE = "${CardRecord.NDEF_DOMAIN}:${CardRecord.NDEF_TYPE}"

        // Every technology, so the log can say what a sticker is; no platform
        // sounds because the video is the sound. The platform's NDEF check is
        // NOT skipped: with FLAG_READER_SKIP_NDEF_CHECK a tag arrives without
        // the Ndef technology at all, and neither reading our record nor
        // writing one is possible through it (seen 2026-09-13, "Aufkleber
        // kann kein NDEF").
        const val FLAGS =
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
        const val PRESENCE_CHECK_MS = 250
        const val DEBOUNCE_MS = 500
    }
}
