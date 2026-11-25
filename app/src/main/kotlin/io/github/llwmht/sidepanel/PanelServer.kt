package io.github.llwmht.sidepanel

import fi.iki.elonen.NanoHTTPD
import java.io.DataInputStream
import java.io.InputStream
import java.net.URI

/** Reads a file of the web bundle. `path` is relative ("panel/index.html"); null when absent. */
fun interface AssetSource {
    fun open(path: String): InputStream?
}

/**
 * Serves the web bundle and, when an [Api] is given, the `/api/` routes. Every request first goes through
 * [access] (see [AccessPolicy]); it returns null to allow or the HTTP status to answer.
 */
class PanelServer(
    private val assets: AssetSource,
    port: Int = AccessPolicy.PORT,
    host: String = "127.0.0.1",
    private val api: Api? = null,
    private val access: (AccessPolicy.Request) -> Int? = { null },
) : NanoHTTPD(host, port) {

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri ?: "/"
        val h = session.headers
        val request = AccessPolicy.Request(session.remoteIpAddress, h["host"], session.method.name, uri, h["cookie"], h["x-sidepanel"])
        access(request)?.let { return text(it, "access denied") }

        if (uri.startsWith("/api/")) return serveApi(session, uri)
        if (session.method != Method.GET) return text(405, "method not allowed")
        if (uri == "/") return text(302, "").apply { addHeader("Location", "/panel/") }
        val path = safePath(uri) ?: return notFound()
        val file = if (path.isEmpty() || path.endsWith("/")) path + "index.html" else path
        val stream = assets.open(file) ?: return notFound()
        return newChunkedResponse(Response.Status.OK, mimeOf(file), stream).apply {
            addHeader("Cache-Control", "no-cache")
        }
    }

    private fun serveApi(session: IHTTPSession, uri: String): Response {
        val api = api ?: return notFound()
        val length = session.headers["content-length"]?.toLongOrNull() ?: 0L
        if (length > MAX_BODY) return text(413, "body too large")
        val body = if (length > 0) {
            val bytes = ByteArray(length.toInt())
            DataInputStream(session.inputStream).readFully(bytes)
            String(bytes, Charsets.UTF_8)
        } else null
        val r = api.handle(session.method.name, uri, body, session.headers["cookie"])
        val response = if (r.json == null) newFixedLengthResponse(status(r.status), null, null as InputStream?, 0)
        else newFixedLengthResponse(status(r.status), "application/json; charset=utf-8", r.json)
        r.setCookie?.let { response.addHeader("Set-Cookie", it) }
        response.addHeader("Cache-Control", "no-store")
        return response
    }

    /** The URI is already percent-decoded by NanoHTTPD; reject anything that could leave the bundle. */
    private fun safePath(uri: String): String? {
        val segments = uri.removePrefix("/").split("/")
        val bad = segments.withIndex().any { (i, s) -> s == ".." || s == "." || (s.isEmpty() && i != segments.lastIndex) }
        return if (bad || !uri.startsWith("/")) null else segments.joinToString("/")
    }

    private fun notFound() = text(404, "not found")

    private fun text(code: Int, body: String) =
        newFixedLengthResponse(status(code), "text/plain; charset=utf-8", body).apply {
            addHeader("Cache-Control", "no-cache")
        }

    private fun status(code: Int): Response.IStatus = Response.Status.lookup(code) ?: object : Response.IStatus {
        override fun getDescription() = "$code"
        override fun getRequestStatus() = code
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

    companion object {
        const val MAX_BODY = 1024L * 1024L
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
