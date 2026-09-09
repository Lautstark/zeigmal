package de.lautstark.zeigmal.cardset

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardsTest {
    @Test
    fun `tag ids compare by hex digits only`() {
        assertEquals(TagId("04a791b2c3d480"), TagId.parse("04:A7:91:B2:C3:D4:80"))
        assertEquals(TagId("04a791b2c3d480"), TagId.parse("04 a7 91 b2 c3 d4 80"))
        assertEquals(
            TagId("04a791b2c3d480"),
            TagId.of(byteArrayOf(0x04, 0xa7.toByte(), 0x91.toByte(), 0xb2.toByte(), 0xc3.toByte(), 0xd4.toByte(), 0x80.toByte())),
        )
        assertNull(TagId.parse("trinken"))
        assertNull(TagId.parse("04a7"))
        assertEquals("04:a7:91:b2:c3:d4:80", TagId("04a791b2c3d480").toString())
    }

    @Test
    fun `two cards may mean one entry, one tag may not mean two`() {
        val r =
            Cards.parse(
                """
                { "format": "zeigmal.karten", "version": 1, "cards": [
                  { "tag": "04:A7:91:B2:C3:D4:80", "entry": "trinken" },
                  { "tag": "04a791b2c3d481", "entry": "trinken", "note": "zweite Karte" },
                  { "tag": "04a791b2c3d480", "entry": "essen" },
                  { "tag": "nope", "entry": "essen" },
                  { "tag": "04a791b2c3d482", "entry": "unbekannt" }
                ] }
                """,
                knownEntries = setOf("trinken", "essen"),
            ) as Parsed.Accepted
        assertEquals(listOf("trinken", "trinken"), r.value.map { it.entry })
        assertEquals("zweite Karte", r.value[1].note)
        assertEquals(
            listOf(WarningCode.CARD_DUPLICATE, WarningCode.CARD_INVALID, WarningCode.CARD_UNKNOWN_ENTRY),
            r.warnings.map { it.code },
        )
    }

    @Test
    fun `what is written can be read back`() {
        val cards = listOf(CardMapping(TagId("04a791b2c3d480"), "trinken"), CardMapping(TagId("04a791b2c3d481"), "essen", "Küche \"neu\""))
        val r = Cards.parse(Cards.write(cards)) as Parsed.Accepted
        assertEquals(cards, r.value)
    }
}
