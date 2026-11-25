package io.github.llwmht.sidepanel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LayoutStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun missingIsNull() = assertNull(LayoutStore(tmp.root).read())

    @Test fun presentReturnsContents() {
        File(tmp.root, "layout.json").writeText("""{"schema":1}""")
        assertEquals("""{"schema":1}""", LayoutStore(tmp.root).read())
    }

    @Test fun tooLargeIsNull() {
        File(tmp.root, "layout.json").writeBytes(ByteArray(1024 * 1024 + 1) { 'a'.code.toByte() })
        assertNull(LayoutStore(tmp.root).read())
    }

    @Test fun writeThenRead() {
        val store = LayoutStore(tmp.root)
        store.write("""{"schema":1,"v":1}""")
        store.write("""{"schema":1,"v":2}""")
        assertEquals("""{"schema":1,"v":2}""", store.read())
        assertFalse(File(tmp.root, "layout.json.tmp").exists())
    }

    @Test fun writeCreatesTheFolder() {
        val store = LayoutStore(File(tmp.root, "files"))
        store.write("{}")
        assertEquals("{}", store.read())
    }
}
