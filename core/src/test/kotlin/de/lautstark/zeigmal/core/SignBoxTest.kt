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

    @Test
    fun `slugs the site renamed are followed`() {
        assertEquals("gucken-sehen-mg", SignBox.refForCard("gucken/sehen"))
        assertEquals("selbst", SignBox.refForCard("selber"))
    }

    @Test
    fun `a line may name its sign outright`() {
        assertEquals(
            listOf(SignBoxWord("Show", "theater"), SignBoxWord("Abend(s)", "abend-s")),
            SignBox.parse(listOf("# comment", "Show = theater", "", "Abend(s)")),
        )
    }

    @Test
    fun `the Halloween set has its 24 cards, labels unique, every ref a slug`() {
        val words = SignBox.halloween
        assertEquals(24, words.size)
        assertEquals("Halloween", words.first().label)
        assertEquals("ansehen", words.last().label)
        assertEquals(24, words.map { it.label }.toSet().size)
        assertTrue(words.all { it.ref.matches(Regex("[a-z0-9-]+")) })
        assertEquals("oder-variante", words.first { it.label == "oder" }.ref)
    }

    @Test
    fun `the writing mode starts on SIGNbox 1 and falls back to it`() {
        assertEquals(SignBox.BOX1, SignBox.set(null).id)
        assertEquals(SignBox.BOX1, SignBox.set("gone").id)
        assertEquals(SignBox.HALLOWEEN, SignBox.set(SignBox.HALLOWEEN).id)
    }
}
