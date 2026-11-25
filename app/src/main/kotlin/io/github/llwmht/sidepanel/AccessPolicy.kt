package io.github.llwmht.sidepanel

/**
 * Who may use the local server. The panel (loopback, exact Host) may read the web files, never the API.
 * Other devices only while LAN editing is on, with a paired session cookie, an IPv4 Host (DNS-rebinding guard) and, for writes, `X-Sidepanel: 1`
 * (cross-site forms cannot send it without a preflight this server never answers).
 */
object AccessPolicy {
    const val PORT = 8770
    const val COOKIE = "sp_session"

    data class Request(
        val remoteIp: String?,
        val host: String?,
        val method: String,
        val path: String,
        val cookie: String?,
        val xSidepanel: String?,
    )

    private val pairingPaths = setOf("/editor/pair.html", "/editor/pair.js", "/editor/editor.css", "/api/pair")
    private val ipv4Host = Regex("""\d{1,3}(\.\d{1,3}){3}:$PORT""")

    /** Null when allowed, otherwise the HTTP status to answer. */
    fun decide(r: Request, lanEnabled: Boolean, sessionValid: (String?) -> Boolean): Int? {
        if (r.remoteIp == "127.0.0.1") {
            // The panel itself: exact Host (DNS rebinding by a page on the device), and no API, which the panel
            // never uses (it talks to the app through the bridge) and other apps on the device must not reach.
            if (r.host != "127.0.0.1:$PORT" || r.path.startsWith("/api/")) return 403
            return null
        }
        if (!lanEnabled) return 403
        if (r.host == null || !ipv4Host.matches(r.host)) return 403
        if (r.method != "GET" && r.xSidepanel != "1") return 403
        if (r.path in pairingPaths) return null
        return if (sessionValid(sessionToken(r.cookie))) null else 401
    }

    fun sessionToken(cookie: String?): String? =
        cookie?.split(';')?.map { it.trim() }?.firstOrNull { it.startsWith("$COOKIE=") }?.substringAfter('=')
}
