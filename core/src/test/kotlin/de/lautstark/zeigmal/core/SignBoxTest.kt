package de.lautstark.zeigmal.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignBoxTest {
    @Test
    fun `the box has its 224 words, in the printed order`() {
        assertEquals(224, SignBox.box1.size)
        assertEquals("Abend(s)", SignBox.box1.first().label)
        assertEquals("zusammen", SignBox.box1.last().label)
        assertEquals(
            224,
            SignBox.box1
                .map { it.label }
                .toSet()
                .size,
        )
    }

    @Test
    fun `refs follow the site's slug rule`() {
        assertEquals("schon", SignBox.refFor("schön"))
        assertEquals("draussen", SignBox.refFor("draußen"))
        assertEquals("abend-s", SignBox.refFor("Abend(s)"))
        assertEquals("hilfe", SignBox.refForCard("helfen/Hilfe"))
        assertEquals("nicht-verstanden", SignBox.refForCard("nicht verstanden"))
        assertTrue(SignBox.box1.all { it.ref.matches(Regex("[a-z0-9-]+")) })
    }
}
