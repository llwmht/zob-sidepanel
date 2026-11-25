package io.github.llwmht.sidepanel

import org.json.JSONObject

/** Handles `/api/` requests that passed the [AccessPolicy]. */
fun interface Api {
    fun handle(method: String, path: String, body: String?, cookie: String?): ApiResponse
}

data class ApiResponse(val status: Int, val json: String? = null, val setCookie: String? = null)

/**
 * The editor's API: pairing, layout read/save, live preview (an unsaved draft the panel shows until it is
 * saved or discarded), widget catalogue and screen size. The panel fully validates layouts when loading;
 * here only "a JSON object with schema 1" is checked so nothing unreadable is ever stored.
 */
class EditorApi(
    private val store: LayoutStore,
    private val pairing: Pairing,
    private val catalog: () -> String,
    private val device: () -> String,
    private val reloadPanel: () -> Unit,
) : Api {
    @Volatile var draft: String? = null
        private set

    /** What the panel should show: the draft while previewing, otherwise the saved layout. */
    fun currentLayout(): String? = draft ?: store.read()

    fun clearDraft() {
        draft = null
    }

    fun handle(method: String, path: String, body: String?) = handle(method, path, body, null)

    override fun handle(method: String, path: String, body: String?, cookie: String?): ApiResponse = when (path) {
        "/api/pair" -> if (method == "POST") pair(body) else notAllowed()
        "/api/layout" -> when (method) {
            "GET" -> ApiResponse(200, JSONObject().put("layout", store.read() ?: JSONObject.NULL).toString())
            "PUT" -> withValidLayout(body) { store.write(it); draft = null; reloadPanel() }
            else -> notAllowed()
        }
        "/api/preview" -> when (method) {
            "PUT" -> withValidLayout(body) { draft = it; reloadPanel() }
            "DELETE" -> { draft = null; reloadPanel(); ApiResponse(204) }
            else -> notAllowed()
        }
        "/api/widgets" -> if (method == "GET") ApiResponse(200, catalog()) else notAllowed()
        "/api/device" -> if (method == "GET") ApiResponse(200, device()) else notAllowed()
        else -> error(404, "unknown API route")
    }

    private fun pair(body: String?): ApiResponse {
        val code = try { JSONObject(body ?: "").getString("code") } catch (e: Exception) { return error(400, "expected {\"code\": \"123456\"}") }
        val token = pairing.tryPair(code) ?: return error(401, "wrong or expired code")
        return ApiResponse(200, "{}", "${AccessPolicy.COOKIE}=$token; Path=/; HttpOnly; SameSite=Strict")
    }

    private fun withValidLayout(body: String?, action: (String) -> Unit): ApiResponse {
        val text = body?.trim() ?: ""
        // Android's JSONObject ignores text after the first value; refuse it so only clean JSON is stored.
        val doc = try { if (!text.endsWith("}")) null else JSONObject(text) } catch (e: Exception) { null }
            ?: return error(400, "layout is not a JSON object")
        if (doc.opt("schema") != 1) return error(400, "unsupported layout schema")
        return try {
            action(text)
            ApiResponse(204)
        } catch (e: Exception) {
            error(500, "could not save: " + (e.message ?: e.javaClass.simpleName))
        }
    }

    private fun notAllowed() = error(405, "method not allowed")

    private fun error(status: Int, message: String) = ApiResponse(status, JSONObject().put("error", message).toString())
}
