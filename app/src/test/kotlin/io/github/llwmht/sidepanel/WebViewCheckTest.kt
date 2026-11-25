package io.github.llwmht.sidepanel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewCheckTest {
    private val k1 = "Mozilla/5.0 (Linux; Android 7.0; SHIELD Tablet K1 Build/NRD90M; wv) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Version/4.0 Chrome/101.0.4951.61 Safari/537.36"

    @Test fun parsesMajor() = assertEquals(101, WebViewCheck.chromiumMajor(k1))

    @Test fun factoryWebViewIsUnsupported() =
        assertFalse(WebViewCheck.isSupported(k1.replace("101.0.4951.61", "55.0.2883.91")))

    @Test fun exactlyMinimumIsSupported() =
        assertTrue(WebViewCheck.isSupported(k1.replace("101.0.4951.61", "80.0.0.0")))

    @Test fun noChromeTokenIsUnsupported() {
        assertNull(WebViewCheck.chromiumMajor("Mozilla/5.0 (Linux)"))
        assertFalse(WebViewCheck.isSupported("Mozilla/5.0 (Linux)"))
    }

    @Test fun nullOrEmptyIsUnsupported() {
        assertFalse(WebViewCheck.isSupported(null))
        assertFalse(WebViewCheck.isSupported(""))
    }
}
