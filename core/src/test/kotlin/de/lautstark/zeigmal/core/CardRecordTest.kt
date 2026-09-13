package de.lautstark.zeigmal.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardRecordTest {
    @Test
    fun `a record survives the trip onto a sticker and back`() {
        val record = CardRecord("signdigital", "trinken", "trinken")
        val bytes = record.encode()
        assertTrue("small enough for an NTAG213", bytes.size < 100)
        assertEquals("zeigmal/1\nprovider=signdigital\nref=trinken\nlabel=trinken\n", String(bytes))
        assertEquals(record, CardRecord.decode(bytes))
    }

    @Test
    fun `umlauts and brackets in the word are fine`() {
        val record = CardRecord("signdigital", "abend-s", "Abend(s) · draußen")
        assertEquals(record, CardRecord.decode(record.encode()))
    }

    @Test
    fun `anything that is not ours is unknown, not an error`() {
        assertNull(CardRecord.decode("hello".toByteArray()))
        assertNull(CardRecord.decode("zeigmal/1\nprovider=signdigital\n".toByteArray()))
        assertNull(CardRecord.decode(byteArrayOf(0xff.toByte(), 0xfe.toByte())))
        assertNull(CardRecord.decode(ByteArray(0)))
    }

    @Test
    fun `unknown lines are ignored so a later version may add some`() {
        val bytes = "zeigmal/1\nprovider=signdigital\nref=essen\nvoice=1\nlabel=essen\n".toByteArray()
        assertEquals(CardRecord("signdigital", "essen", "essen"), CardRecord.decode(bytes))
    }

    @Test
    fun `a missing label falls back to the ref`() {
        assertEquals("essen", CardRecord.decode("zeigmal/1\nprovider=signdigital\nref=essen\n".toByteArray())?.label)
    }
}
