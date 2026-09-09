package de.lautstark.zeigmal.cardset

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class KartensatzDirectoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun set(vararg files: Pair<String, String>): File {
        val dir = tmp.newFolder("kartensatz")
        for ((name, content) in files) {
            File(dir, name).apply { parentFile.mkdirs() }.writeText(content)
        }
        return dir
    }

    @Test
    fun `no manifest is missing, not broken`() {
        assertTrue(KartensatzDirectory.load(tmp.newFolder("empty")) is Loaded.Missing)
    }

    @Test
    fun `a tag resolves to the entry whose video is there`() {
        val dir =
            set(
                "manifest.json" to
                    """
                    { "format": "zeigmal.kartensatz", "version": 1, "name": "Test", "redistributable": false, "entries": [
                      { "id": "trinken", "label": "Trinken", "video": "videos/trinken.mp4", "speech": "external", "audio": "audio/trinken.mp3" },
                      { "id": "essen", "label": "Essen", "video": "videos/essen.mp4", "speech": "video" },
                      { "id": "schlafen", "label": "Schlafen", "video": "videos/schlafen.mp4", "speech": "external", "audio": "audio/schlafen.mp3" }
                    ] }
                    """,
                "cards.json" to
                    """
                    { "format": "zeigmal.karten", "version": 1, "cards": [
                      { "tag": "04a791b2c3d480", "entry": "trinken" },
                      { "tag": "04a791b2c3d481", "entry": "essen" },
                      { "tag": "04a791b2c3d482", "entry": "schlafen" }
                    ] }
                    """,
                "videos/trinken.mp4" to "x",
                "audio/trinken.mp3" to "x",
                "videos/schlafen.mp4" to "x",
            )
        val loaded = KartensatzDirectory.load(dir) as Loaded.Ready
        assertEquals("trinken", loaded.resolve(TagId("04a791b2c3d480"))?.id)
        // essen's video is missing: the card is unknown rather than a card that starts nothing.
        assertNull(loaded.resolve(TagId("04a791b2c3d481")))
        // schlafen's audio is missing: the video plays, silently.
        assertEquals(SpeechMode.NONE, loaded.resolve(TagId("04a791b2c3d482"))?.speech)
        assertEquals(
            listOf(WarningCode.MEDIA_MISSING, WarningCode.MEDIA_MISSING, WarningCode.CARD_UNKNOWN_ENTRY),
            loaded.warnings.map { it.code },
        )
        assertEquals(File(dir, "videos/trinken.mp4"), loaded.file("videos/trinken.mp4"))
    }

    @Test
    fun `a broken cards file names itself`() {
        val dir =
            set(
                "manifest.json" to
                    """{ "format": "zeigmal.kartensatz", "version": 1, "name": "T", "redistributable": false, "entries": [] }""",
                "cards.json" to "not json",
            )
        val r = KartensatzDirectory.load(dir) as Loaded.Rejected
        assertEquals("cards.json", r.file)
        assertEquals(RejectionCode.NOT_JSON, r.code)
    }
}
