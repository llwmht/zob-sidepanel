package io.github.llwmht.sidepanel

/** Decides from the WebView user agent whether the panel can run on this device. */
object WebViewCheck {
    const val MIN_CHROMIUM = 80

    private val chromeVersion = Regex("""Chrome/(\d+)\.""")

    fun chromiumMajor(userAgent: String?): Int? =
        userAgent?.let { chromeVersion.find(it)?.groupValues?.get(1)?.toIntOrNull() }

    fun isSupported(userAgent: String?): Boolean = (chromiumMajor(userAgent) ?: 0) >= MIN_CHROMIUM
}
