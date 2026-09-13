package de.lautstark.zeigmal.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinTest {
    private val store = InMemoryStore()
    private val pin = Pin(store)

    @Test
    fun `unset nothing verifies, set only the digits do`() {
        assertFalse(pin.isSet)
        assertFalse(pin.verify("1234"))
        pin.set("1234")
        assertTrue(pin.isSet)
        assertTrue(pin.verify("1234"))
        assertFalse(pin.verify("1235"))
        assertFalse(pin.verify(""))
    }

    @Test
    fun `the digits are not in the store`() {
        pin.set("4711")
        assertTrue(store.map.values.none { it.contains("4711") })
        assertEquals(2, store.map.size)
    }

    @Test
    fun `the same digits hash differently on two phones`() {
        pin.set("1234")
        val a = store.get(Pin.KEY_HASH)
        Pin(store).set("1234")
        assertNotEquals(a, store.get(Pin.KEY_HASH))
        assertTrue(pin.verify("1234"))
    }

    @Test
    fun `only four digits are a pin`() {
        try {
            pin.set("12")
            throw AssertionError()
        } catch (e: IllegalArgumentException) {
            assertFalse(pin.isSet)
        }
    }
}
