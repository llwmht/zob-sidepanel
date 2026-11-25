package io.github.llwmht.sidepanel

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetCatalogTest {
    private val files = mapOf(
        "widgets/clock/manifest.json" to """{"name":"Clock"}""",
        "widgets/battery/manifest.json" to """{"name":"Battery"}""",
        "widgets/broken/manifest.json" to "{{",
    )
    private val dirs = mapOf("widgets" to arrayOf("clock", "battery", "broken", "nomanifest", "widgets.css", "fit.js"))

    @Test fun listsWidgetsWithManifestsSortedByType() {
        val json = JSONArray(widgetCatalog({ dirs[it] }, { files[it]?.byteInputStream() }))
        assertEquals(2, json.length())
        assertEquals("battery", json.getJSONObject(0).getString("type"))
        assertEquals("Battery", json.getJSONObject(0).getJSONObject("manifest").getString("name"))
        assertEquals("clock", json.getJSONObject(1).getString("type"))
    }

    @Test fun noWidgetsFolderIsEmpty() =
        assertEquals(0, JSONArray(widgetCatalog({ null }, { null })).length())
}
