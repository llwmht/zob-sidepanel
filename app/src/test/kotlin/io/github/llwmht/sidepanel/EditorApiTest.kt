package io.github.llwmht.sidepanel

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.Random

class EditorApiTest {
    @get:Rule val tmp = TemporaryFolder()

    private val tokens = object : TokenStore {
        var saved = emptySet<String>()
        override fun load() = saved
        override fun save(tokens: Set<String>) { saved = tokens }
    }
    private val pairing = Pairing({ n -> ByteArray(n).also { Random(1).nextBytes(it) } }, tokens)
    private var reloads = 0
    private val store by lazy { LayoutStore(tmp.root) }
    private val api by lazy {
        EditorApi(store, pairing, catalog = { """[{"type":"clock"}]""" }, device = { """{"width":800,"height":1280}""" }, reloadPanel = { reloads++ })
    }
    private val good = """{"schema":1,"pages":[]}"""

    @Test fun pairWithRightCodeSetsCookie() {
        val r = api.handle("POST", "/api/pair", """{"code":"${pairing.newCode()}"}""")
        assertEquals(200, r.status)
        assertTrue(r.setCookie!!.startsWith("sp_session="))
        assertTrue(r.setCookie!!.contains("HttpOnly") && r.setCookie!!.contains("SameSite=Strict"))
    }

    @Test fun pairWithWrongCodeIs401() {
        pairing.newCode()
        assertEquals(401, api.handle("POST", "/api/pair", """{"code":"x"}""").status)
        assertEquals(400, api.handle("POST", "/api/pair", "{{").status)
    }

    @Test fun saveValidLayoutWritesAndReloads() {
        assertEquals(204, api.handle("PUT", "/api/layout", good).status)
        assertEquals(good, store.read())
        assertEquals(1, reloads)
    }

    @Test fun saveRejectsBadBodiesAndKeepsTheFile() {
        store.write(good)
        for (bad in listOf("{{", "[]", """{"schema":2}""", "", null)) {
            val r = api.handle("PUT", "/api/layout", bad)
            assertEquals(bad.toString(), 400, r.status)
            assertNotNull(JSONObject(r.json!!).getString("error"))
        }
        assertEquals(good, store.read())
        assertEquals(0, reloads)
    }

    @Test fun getLayoutReturnsTextOrNull() {
        assertTrue(JSONObject(api.handle("GET", "/api/layout", null).json!!).isNull("layout"))
        store.write(good)
        assertEquals(good, JSONObject(api.handle("GET", "/api/layout", null).json!!).getString("layout"))
    }

    @Test fun previewIsADraftUntilSavedOrDiscarded() {
        store.write(good)
        val draft = """{"schema":1,"pages":[],"draft":true}"""
        assertEquals(204, api.handle("PUT", "/api/preview", draft).status)
        assertEquals(draft, api.currentLayout())
        assertEquals(good, store.read())
        assertEquals(204, api.handle("DELETE", "/api/preview", null).status)
        assertEquals(good, api.currentLayout())
        api.handle("PUT", "/api/preview", draft)
        api.handle("PUT", "/api/layout", good)
        assertNull(api.draft)
        assertEquals(4, reloads) // preview, discard, preview, save

    }

    @Test fun catalogueDeviceAndUnknownRoutes() {
        assertEquals("""[{"type":"clock"}]""", api.handle("GET", "/api/widgets", null).json)
        assertEquals(800, JSONObject(api.handle("GET", "/api/device", null).json!!).getInt("width"))
        assertEquals(404, api.handle("GET", "/api/nope", null).status)
        assertEquals(405, api.handle("DELETE", "/api/layout", null).status)
    }
}
