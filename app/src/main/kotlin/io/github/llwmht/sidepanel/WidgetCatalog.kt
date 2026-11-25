package io.github.llwmht.sidepanel

import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

/**
 * The built-in widgets for the editor's palette: `[{type, manifest}]`, sorted by type. A folder without a
 * readable manifest is skipped. `list` and `open` read the app's assets.
 */
fun widgetCatalog(list: (String) -> Array<String>?, open: (String) -> InputStream?): String {
    val out = JSONArray()
    for (type in (list("widgets") ?: emptyArray()).sorted()) {
        val text = try { open("widgets/$type/manifest.json")?.bufferedReader()?.use { it.readText() } } catch (e: Exception) { null }
        val manifest = try { JSONObject(text ?: continue) } catch (e: Exception) { continue }
        out.put(JSONObject().put("type", type).put("manifest", manifest))
    }
    return out.toString()
}
