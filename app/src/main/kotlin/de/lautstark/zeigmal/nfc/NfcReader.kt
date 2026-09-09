package de.lautstark.zeigmal.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import de.lautstark.zeigmal.cardset.TagId

sealed interface TagEvent {
    data class Seen(
        val tag: TagId,
        val technologies: List<String>,
    ) : TagEvent

    data class Gone(
        val tag: TagId,
    ) : TagEvent
}

/**
 * Reader mode, and nothing else. The foreground activity owns the NFC hardware
 * while it is on screen: no intent dispatch, no NDEF, no system sound, no other
 * app getting the tag first — which is what an appliance needs, and what a phone
 * that also has Google Pay on it would otherwise argue about.
 *
 * Removal comes from [NfcAdapter.ignore]: once a tag has been seen it is ignored
 * for as long as it stays in the field, and the listener fires when the presence
 * check says it left. Whether that is prompt and reliable on the Galaxy A51 is
 * experiment E2 in docs/experiments.md — this class reports it, the docs decide
 * whether to trust it.
 */
class NfcReader(
    private val activity: Activity,
    private val onEvent: (TagEvent) -> Unit,
) {
    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    private val main = Handler(Looper.getMainLooper())

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
        val technologies = tag.techList.map { it.substringAfterLast('.') }
        main.post { onEvent(TagEvent.Seen(id, technologies)) }
        nfc.ignore(tag, DEBOUNCE_MS, { main.post { onEvent(TagEvent.Gone(id)) } }, main)
    }

    private companion object {
        // Every tag technology, so the diagnostics screen can say what a sticker
        // is; SKIP_NDEF_CHECK because the UID is the key and reading NDEF costs a
        // round trip per card; NO_PLATFORM_SOUNDS because the video is the sound.
        const val FLAGS =
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

        // How often Android checks whether an ignored tag is still there. Lower is
        // a faster "gone" and more radio traffic; experiment E2 tunes it.
        const val PRESENCE_CHECK_MS = 250

        // How long a tag must be absent before it counts as gone. A single missed
        // poll must never arrive as a removal — wochenwerk's rule, kept here.
        const val DEBOUNCE_MS = 500
    }
}
