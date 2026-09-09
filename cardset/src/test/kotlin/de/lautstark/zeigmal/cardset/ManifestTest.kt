package de.lautstark.zeigmal.cardset

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestTest {
    private fun manifest(entries: String) =
        """
        {
          "format": "zeigmal.kartensatz",
          "version": 1,
          "name": "Test",
          "redistributable": false,
          "entries": [ $entries ]
        }
        """.trimIndent()

    private fun accepted(text: String): Parsed.Accepted<CardSet> = Manifest.parse(text) as Parsed.Accepted<CardSet>

    @Test
    fun `a minimal entry with the video speaking`() {
        val r = accepted(manifest("""{ "id": "trinken", "label": "Trinken", "video": "videos/trinken.mp4", "speech": "video" }"""))
        val e = r.value.entries.getValue("trinken")
        assertEquals("Trinken", e.label)
        assertEquals("videos/trinken.mp4", e.video)
        assertEquals(SpeechMode.VIDEO, e.speech)
        assertNull(e.audio)
        assertTrue(r.warnings.isEmpty())
    }

    @Test
    fun `external speech carries the audio and its provenance`() {
        val r =
            accepted(
                manifest(
                    """
                    { "id": "essen", "label": "Essen",
                      "video": { "file": "videos/essen.mp4", "provider": "signdigital", "attribution": "SIGNdigital" },
                      "speech": "external",
                      "audio": { "file": "audio/essen.mp3", "text": "Essen", "voice": "piper:de_DE-thorsten-medium", "key": "abc123" },
                      "symbol": { "metacom": "Essen", "arasaac": 6456 } }
                    """,
                ),
            )
        val e = r.value.entries.getValue("essen")
        assertEquals(SpeechMode.EXTERNAL, e.speech)
        assertEquals("audio/essen.mp3", e.audio?.file)
        assertEquals("piper:de_DE-thorsten-medium", e.audio?.voice)
        assertEquals("signdigital", e.videoSource?.provider)
        assertEquals("Essen", e.symbol?.metacom)
        assertEquals(6456, e.symbol?.arasaac)
    }

    @Test
    fun `external speech without audio is kept, silent, and warned about`() {
        val r = accepted(manifest("""{ "id": "essen", "label": "Essen", "video": "v.mp4", "speech": "external" }"""))
        assertEquals(
            SpeechMode.NONE,
            r.value.entries
                .getValue("essen")
                .speech,
        )
        assertEquals(listOf(WarningCode.ENTRY_WITHOUT_AUDIO), r.warnings.map { it.code })
    }

    @Test
    fun `speech is never inferred`() {
        val r = accepted(manifest("""{ "id": "essen", "label": "Essen", "video": "v.mp4" }"""))
        assertTrue(r.value.entries.isEmpty())
        assertEquals(listOf(WarningCode.ENTRY_INVALID), r.warnings.map { it.code })
    }

    @Test
    fun `a bad entry costs one card, not the set`() {
        val r =
            accepted(
                manifest(
                    """
                    { "id": "Trinken", "label": "x", "video": "v.mp4", "speech": "video" },
                    { "id": "ok", "label": "Ok", "video": "v.mp4", "speech": "video" },
                    { "id": "ok", "label": "Twice", "video": "v.mp4", "speech": "video" },
                    { "id": "out", "label": "Out", "video": "../v.mp4", "speech": "video" }
                    """,
                ),
            )
        assertEquals(setOf("ok"), r.value.entries.keys)
        assertEquals(
            "Ok",
            r.value.entries
                .getValue("ok")
                .label,
        )
        assertEquals(
            listOf(WarningCode.ENTRY_INVALID, WarningCode.ENTRY_DUPLICATE, WarningCode.ENTRY_INVALID),
            r.warnings.map { it.code },
        )
    }

    @Test
    fun `the set is refused whole when its header is wrong`() {
        assertEquals(RejectionCode.NOT_JSON, (Manifest.parse("{") as Parsed.Rejected).code)
        assertEquals(
            RejectionCode.WRONG_FORMAT,
            (Manifest.parse("""{"format":"bildhaft.collection","version":3}""") as Parsed.Rejected).code,
        )
        assertEquals(
            RejectionCode.VERSION_TOO_NEW,
            (
                Manifest.parse(
                    """{"format":"zeigmal.kartensatz","version":2,"name":"x","redistributable":false,"entries":[]}""",
                ) as Parsed.Rejected
            ).code,
        )
        assertEquals(
            RejectionCode.INVALID,
            (Manifest.parse("""{"format":"zeigmal.kartensatz","version":1,"name":"x","entries":[]}""") as Parsed.Rejected).code,
        )
    }
}
