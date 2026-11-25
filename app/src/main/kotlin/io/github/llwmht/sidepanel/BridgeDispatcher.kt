package io.github.llwmht.sidepanel

import org.json.JSONObject

/**
 * Turns one bridge request into one reply. Wire format: see docs/bridge.md.
 * Never throws: bad input and failing handlers become error replies.
 */
class BridgeDispatcher(private val handlers: Map<String, (JSONObject) -> Any?>) {

    fun handle(message: String?): String {
        val req = try { JSONObject(message ?: "") } catch (e: Exception) { null }
        val id = req?.opt("id")
        val method = req?.optString("method", "")
        if (req == null || id == null || id == JSONObject.NULL || method.isNullOrEmpty()) {
            return reply(JSONObject.NULL, "error", "invalid message")
        }
        val handler = handlers[method] ?: return reply(id, "error", "unknown method: $method")
        return try {
            val result = handler(req.optJSONObject("params") ?: JSONObject())
            // Unit/null are mapped here: JSONObject.wrap differs between Android and the JVM test library.
            reply(id, "result", if (result == null || result == Unit) JSONObject.NULL else JSONObject.wrap(result))
        } catch (e: Exception) {
            reply(id, "error", e.message ?: e.javaClass.simpleName)
        }
    }

    private fun reply(id: Any, key: String, value: Any?): String =
        JSONObject().put("id", id).put(key, value ?: JSONObject.NULL).toString()
}

/** -1 keeps the system brightness; other values are clamped to 0..255 and scaled to 0..1. */
fun clampBrightness(level: Int): Float = if (level == -1) -1f else level.coerceIn(0, 255) / 255f
