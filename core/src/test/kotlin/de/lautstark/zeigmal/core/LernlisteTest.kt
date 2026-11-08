package de.lautstark.zeigmal.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LernlisteTest {
    private fun fixture(): String = LernlisteTest::class.java.getResource("/lernlisten-page.json")!!.readText()

    @Test
    fun `a page of the site's lists reads as lists of signs, in their order`() {
        val lists = Lernliste.page(Json.parseToJsonElement(fixture()).jsonObject)
        assertEquals(listOf("zeigmal-test", "Halloween"), lists.map { it.name })
        val test = lists.first()
        assertEquals("6af0ba83ba88783bc0f188cc", test.id)
        assertEquals("2026-11-08T18:08:41.621Z", test.updatedAt)
        assertEquals(listOf("trinken", "apfel"), test.signs.map { it.slug })
        assertEquals(listOf("trinken", "Apfel"), test.signs.map { it.name })
        assertEquals("60e7481e63f62cd51523cea2", test.signs.first().id)
    }

    @Test
    fun `the card picture is the signBox path, and a sign may have none`() {
        val signs = Lernliste.page(Json.parseToJsonElement(fixture()).jsonObject).first().signs
        assertEquals("production/files/public/signs/a/products/b/trinken_sb.png", signs[0].cardPath)
        assertNull(signs[1].cardPath)
    }

    @Test
    fun `a sign without a slug cannot be written and is left out`() {
        val signs = Lernliste.page(Json.parseToJsonElement(fixture()).jsonObject).first().signs
        assertTrue(signs.none { it.name == "ohne Slug" })
    }

    @Test
    fun `the card's word is the site's name for the sign`() {
        val halloween = Lernliste.page(Json.parseToJsonElement(fixture()).jsonObject)[1]
        assertEquals("sauer (Geschmack)", halloween.signs.single().name)
        assertNull(halloween.signs.single().cardPath)
    }

    @Test
    fun `an empty or odd page is no lists, not a crash`() {
        assertEquals(emptyList<Lernliste>(), Lernliste.page(Json.parseToJsonElement("""{"total":0,"data":[]}""").jsonObject))
        assertEquals(emptyList<Lernliste>(), Lernliste.page(Json.parseToJsonElement("""{"data":[1,{"name":"no id"}]}""").jsonObject))
        assertEquals(emptyList<Lernliste>(), Lernliste.page(Json.parseToJsonElement("""{}""").jsonObject))
    }
}
