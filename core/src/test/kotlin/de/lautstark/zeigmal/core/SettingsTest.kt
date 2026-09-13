package de.lautstark.zeigmal.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsTest {
    @Test
    fun `loops default, persist, and stay in range`() {
        val store = InMemoryStore()
        val s = Settings(store)
        assertEquals(Station.MAX_ROUNDS, s.values.value.maxLoops)
        s.setMaxLoops(5)
        assertEquals(5, Settings(store).values.value.maxLoops)
        s.setMaxLoops(0)
        assertEquals(1, s.values.value.maxLoops)
        s.setMaxLoops(500)
        assertEquals(99, s.values.value.maxLoops)
    }
}
