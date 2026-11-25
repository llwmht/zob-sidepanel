package io.github.llwmht.sidepanel

import java.io.File

/** The user's layout, kept as `layout.json` in app storage. The panel validates it. */
class LayoutStore(private val dir: File) {

    /** The raw file, or null when missing, unreadable or larger than [MAX_BYTES]. */
    fun read(): String? {
        val f = File(dir, "layout.json")
        return try {
            if (!f.isFile || f.length() > MAX_BYTES) null else f.readText()
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val MAX_BYTES = 1024 * 1024
    }
}
