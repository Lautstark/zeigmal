package de.lautstark.zeigmal.core

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WriteLogTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `entries are appended one per line and read back in order`() {
        val file = folder.root.resolve("log/schreibprotokoll.jsonl")
        val log = FileWriteLog(file)
        assertEquals(emptyList<WriteEntry>(), log.all())
        val a = WriteEntry(1L, "jacke", "Jacke", "s1", "Kita", "04aabbccddeeff")
        val b = WriteEntry(null, "abend-s", "Abend(s)", list = "SIGNbox 1")
        log.append(a)
        log.append(b)
        assertEquals(listOf(a, b), FileWriteLog(file).all())
        assertEquals(2, file.readLines().size)
    }

    @Test
    fun `a line that does not read is skipped, not fatal`() {
        val file = folder.newFile("schreibprotokoll.jsonl")
        file.writeText("{\"at\":1,\"slug\":\"a\",\"label\":\"A\"}\n{\"at\":2,\"slug\":\"b\",\"lab\n")
        assertEquals(listOf(WriteEntry(1L, "a", "A")), FileWriteLog(file).all())
    }
}
