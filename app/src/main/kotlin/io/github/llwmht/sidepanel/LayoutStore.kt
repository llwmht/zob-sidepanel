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

    /** Replaces the layout. Written to a temporary file first so a crash never leaves half a file. */
    @Synchronized fun write(text: String) {
        dir.mkdirs()
        val tmp = File(dir, "layout.json.tmp")
        tmp.writeText(text)
        val target = File(dir, "layout.json")
        if (!tmp.renameTo(target)) {
            target.delete()
            if (!tmp.renameTo(target)) error("could not save the layout")
        }
    }

    companion object {
        const val MAX_BYTES = 1024 * 1024
    }
}
