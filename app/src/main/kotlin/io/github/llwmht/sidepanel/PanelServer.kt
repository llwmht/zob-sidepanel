package io.github.llwmht.sidepanel

import fi.iki.elonen.NanoHTTPD
import java.io.InputStream
import java.net.URI

/** Reads a file of the web bundle. `path` is relative ("panel/index.html"); null when absent. */
fun interface AssetSource {
    fun open(path: String): InputStream?
}

/** Serves the web bundle (panel, shared code) on loopback only. */
class PanelServer(private val assets: AssetSource, port: Int = 8770) : NanoHTTPD("127.0.0.1", port) {

    override fun serve(session: IHTTPSession): Response {
        if (session.method != Method.GET) return text(Response.Status.METHOD_NOT_ALLOWED, "method not allowed")
        val uri = session.uri ?: "/"
        if (uri == "/") {
            return text(Response.Status.FOUND, "").apply { addHeader("Location", "/panel/") }
        }
        val path = safePath(uri) ?: return notFound()
        val file = if (path.isEmpty() || path.endsWith("/")) path + "index.html" else path
        val stream = assets.open(file) ?: return notFound()
        return newChunkedResponse(Response.Status.OK, mimeOf(file), stream).apply {
            addHeader("Cache-Control", "no-cache")
        }
    }

    /** The URI is already percent-decoded by NanoHTTPD; reject anything that could leave the bundle. */
    private fun safePath(uri: String): String? {
        val segments = uri.removePrefix("/").split("/")
        val bad = segments.withIndex().any { (i, s) -> s == ".." || s == "." || (s.isEmpty() && i != segments.lastIndex) }
        return if (bad || !uri.startsWith("/")) null else segments.joinToString("/")
    }

    private fun notFound() = text(Response.Status.NOT_FOUND, "not found")

    private fun text(status: Response.Status, body: String) =
        newFixedLengthResponse(status, "text/plain; charset=utf-8", body).apply {
            addHeader("Cache-Control", "no-cache")
        }

    private fun mimeOf(file: String): String = when (file.substringAfterLast('.', "").lowercase()) {
        "html" -> "text/html; charset=utf-8"
        "js" -> "text/javascript; charset=utf-8"
        "css" -> "text/css; charset=utf-8"
        "json" -> "application/json; charset=utf-8"
        "svg" -> "image/svg+xml"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "woff2" -> "font/woff2"
        else -> "application/octet-stream"
    }
}

/** Starts the server; returns null on success or a one-line reason. Never throws. */
fun PanelServer.startOrError(): String? =
    try {
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, true)
        null
    } catch (e: Exception) {
        e.message ?: e.javaClass.simpleName
    }

val PanelServer.baseUrl: String get() = "http://127.0.0.1:$listeningPort"

/** True only for http://127.0.0.1:<port>/… — the panel's own origin. */
fun isPanelUrl(url: String?, port: Int): Boolean {
    if (url == null) return false
    val u = try { URI(url) } catch (e: Exception) { return false }
    return u.scheme == "http" && u.host == "127.0.0.1" && u.port == port
}
