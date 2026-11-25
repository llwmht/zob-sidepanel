package io.github.llwmht.sidepanel

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BridgeDispatcherTest {
    private val d = BridgeDispatcher(
        mapOf(
            "echo" to { p -> p.optString("v") },
            "unit" to { _ -> Unit },
            "boom" to { _ -> error("nope") },
        ),
    )

    private fun reply(msg: String?) = JSONObject(d.handle(msg))

    @Test fun returnsResult() {
        val r = reply("""{"id":1,"method":"echo","params":{"v":"x"}}""")
        assertEquals(1, r.getInt("id")); assertEquals("x", r.getString("result"))
    }

    @Test fun unitBecomesNull() {
        val r = reply("""{"id":2,"method":"unit"}""")
        assertTrue(r.has("result")); assertTrue(r.isNull("result"))
    }

    @Test fun unknownMethod() {
        val r = reply("""{"id":3,"method":"zzz"}""")
        assertEquals(3, r.getInt("id")); assertEquals("unknown method: zzz", r.getString("error"))
    }

    @Test fun handlerThrows() {
        val r = reply("""{"id":4,"method":"boom"}""")
        assertEquals(4, r.getInt("id")); assertEquals("nope", r.getString("error"))
    }

    @Test fun notJson() {
        val r = reply("{{")
        assertTrue(r.isNull("id")); assertEquals("invalid message", r.getString("error"))
    }

    @Test fun nullMessage() {
        val r = reply(null)
        assertTrue(r.isNull("id")); assertEquals("invalid message", r.getString("error"))
    }

    @Test fun missingId() {
        val r = reply("""{"method":"echo"}""")
        assertTrue(r.isNull("id")); assertEquals("invalid message", r.getString("error"))
    }

    @Test fun missingParamsIsEmptyObject() {
        val r = reply("""{"id":5,"method":"echo"}""")
        assertEquals(5, r.getInt("id")); assertEquals("", r.getString("result"))
    }

    @Test fun brightness() {
        assertEquals(-1f, clampBrightness(-1))
        assertEquals(0f, clampBrightness(-5))
        assertEquals(1f, clampBrightness(999))
        assertEquals(128 / 255f, clampBrightness(128))
    }
}
