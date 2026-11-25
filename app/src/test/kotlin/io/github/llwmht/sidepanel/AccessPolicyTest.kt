package io.github.llwmht.sidepanel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccessPolicyTest {
    private val valid: (String?) -> Boolean = { it == "good" }
    private fun req(
        ip: String = "10.0.0.20", host: String? = "10.0.0.5:8770", method: String = "GET",
        path: String = "/editor/index.html", cookie: String? = "sp_session=good", x: String? = null,
    ) = AccessPolicy.Request(ip, host, method, path, cookie, x)

    @Test fun loopbackAlwaysAllowed() {
        assertNull(AccessPolicy.decide(req(ip = "127.0.0.1", host = "127.0.0.1:8770", cookie = null), false, valid))
        assertNull(AccessPolicy.decide(req(ip = "127.0.0.1", method = "PUT", cookie = null), true, valid))
    }

    @Test fun remoteRefusedWhenLanOff() = assertEquals(403, AccessPolicy.decide(req(), false, valid))

    @Test fun remoteWithoutSessionIs401() = assertEquals(401, AccessPolicy.decide(req(cookie = null), true, valid))

    @Test fun badSessionIs401() = assertEquals(401, AccessPolicy.decide(req(cookie = "sp_session=bad"), true, valid))

    @Test fun pairingPathsNeedNoSession() {
        for (p in listOf("/editor/pair.html", "/editor/pair.js", "/editor/editor.css")) {
            assertNull(p, AccessPolicy.decide(req(path = p, cookie = null), true, valid))
        }
        assertNull(AccessPolicy.decide(req(method = "POST", path = "/api/pair", cookie = null, x = "1"), true, valid))
    }

    @Test fun pairPostStillNeedsCustomHeader() =
        assertEquals(403, AccessPolicy.decide(req(method = "POST", path = "/api/pair", cookie = null), true, valid))

    @Test fun domainHostRefused() =
        assertEquals(403, AccessPolicy.decide(req(host = "evil.example:8770"), true, valid))

    @Test fun otherPortOrMissingHostRefused() {
        assertEquals(403, AccessPolicy.decide(req(host = "10.0.0.5:80"), true, valid))
        assertEquals(403, AccessPolicy.decide(req(host = null), true, valid))
    }

    @Test fun validSessionGetAllowed() = assertNull(AccessPolicy.decide(req(), true, valid))

    @Test fun sessionAmongOtherCookies() =
        assertNull(AccessPolicy.decide(req(cookie = "a=1; sp_session=good; b=2"), true, valid))

    @Test fun writesNeedCustomHeader() {
        assertEquals(403, AccessPolicy.decide(req(method = "PUT", path = "/api/layout"), true, valid))
        assertNull(AccessPolicy.decide(req(method = "PUT", path = "/api/layout", x = "1"), true, valid))
    }
}
