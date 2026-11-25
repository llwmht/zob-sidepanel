package io.github.llwmht.sidepanel

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL

class PanelServerTest {
    private val files = mapOf("panel/index.html" to "<p>hi</p>", "shared/bridge.js" to "export {}")
    private val asked = mutableListOf<String>()
    private val source = AssetSource { path -> asked += path; files[path]?.byteInputStream() }
    private lateinit var server: PanelServer

    @Before fun start() {
        server = PanelServer(source, 0)
        assertNull(server.startOrError())
    }

    @After fun stop() = server.stop()

    private fun get(path: String, method: String = "GET"): HttpURLConnection =
        (URL(server.baseUrl + path).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            requestMethod = method
        }

    /** Sends the path byte for byte: URL/HttpURLConnection would normalise "..". */
    private fun rawStatus(path: String): Int =
        Socket("127.0.0.1", server.listeningPort).use { s ->
            s.getOutputStream().write("GET $path HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n".toByteArray())
            s.getInputStream().bufferedReader().readLine().split(" ")[1].toInt()
        }

    @Test fun servesIndexForDirectory() {
        val c = get("/panel/")
        assertEquals(200, c.responseCode)
        assertTrue(c.contentType.startsWith("text/html"))
        assertEquals("<p>hi</p>", c.inputStream.bufferedReader().readText())
    }

    @Test fun servesJsWithModuleMime() {
        val c = get("/shared/bridge.js")
        assertEquals(200, c.responseCode)
        assertTrue(c.contentType.startsWith("text/javascript"))
    }

    @Test fun rootRedirectsToPanel() {
        val c = get("/")
        assertEquals(302, c.responseCode)
        assertEquals("/panel/", c.getHeaderField("Location"))
    }

    @Test fun missingFileIs404() = assertEquals(404, get("/panel/nope.js").responseCode)

    @Test fun traversalIs404() {
        listOf("/../secret", "/panel/../../secret", "/%2e%2e/secret", "/panel/%2e%2e%2fsecret", "//etc/passwd")
            .forEach { assertEquals(it, 404, rawStatus(it)) }
        // Every case must be rejected before the asset source is consulted at all.
        assertTrue(asked.toString(), asked.isEmpty())
    }

    @Test fun postIs405() = assertEquals(405, get("/panel/", "POST").responseCode)

    @Test fun secondServerOnSamePortReportsError() {
        val second = PanelServer(source, server.listeningPort)
        assertNotNull(second.startOrError())
    }

    @Test fun panelUrlMatchesOnlyExactOrigin() {
        assertTrue(isPanelUrl("http://127.0.0.1:8770/panel/", 8770))
        listOf(
            "http://127.0.0.1:8770.evil.com/", "http://127.0.0.1:8771/", "https://127.0.0.1:8770/",
            "http://localhost:8770/", "http://evil.com/?http://127.0.0.1:8770/", "intent://x", "not a url", null,
        ).forEach { assertFalse(it.toString(), isPanelUrl(it, 8770)) }
    }
}
