package io.github.llwmht.sidepanel

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.window.OnBackInvokedDispatcher
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/** The home screen: the panel WebView, or a native fallback view when the panel cannot run. */
class PanelActivity : Activity() {
    private val app get() = application as App
    private val handler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    /** Chromium major version of the system WebView, 0 when unknown. Read by the `info` bridge method. */
    var webviewMajor = 0
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) { openDrawer() }
        }
        start()
    }

    private fun start() {
        if (app.startGuard.begin()) return showFallback("safe", null)
        app.startServer()?.let { return showFallback("server", it) }
        val server = app.server ?: return showFallback("server", "no server")

        val view = try { WebView(this) } catch (e: Exception) {
            Log.e(App.TAG, "WebView unavailable", e)
            return showFallback("webview", e.message)
        }
        val ua = view.settings.userAgentString
        webviewMajor = WebViewCheck.chromiumMajor(ua) ?: 0
        if (!WebViewCheck.isSupported(ua) || !WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            view.destroy()
            return showFallback("webview", "Chromium $webviewMajor")
        }
        setUpWebView(view, server)
        webView = view
        watchNetwork()
        setContentView(view)
        view.loadUrl(server.baseUrl + "/panel/")
        maybeCrashForTest(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // As the home app the activity is usually already running, so the test hook must work here too.
        maybeCrashForTest(intent)
    }

    /** Test hook for the crash path (docs/device-checks.md, M1 check 9). Debug builds only. */
    private fun maybeCrashForTest(intent: Intent?) {
        if (BuildConfig.DEBUG && intent?.getBooleanExtra("crash", false) == true) {
            handler.postDelayed({ throw RuntimeException("debug crash requested") }, 2000)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setUpWebView(view: WebView, server: PanelServer) {
        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
        val port = server.listeningPort
        view.setBackgroundColor(Color.BLACK)
        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = true
        view.settings.allowFileAccess = false
        view.settings.allowContentAccess = false
        // Pages shown by the web-page widget live in frames of another origin; without third-party
        // cookies, sites that keep a session in a cookie would log out (they work top-level in a browser).
        CookieManager.getInstance().setAcceptThirdPartyCookies(view, true)
        val dispatcher = BridgeDispatcher(deviceHandlers(this))
        WebViewCompat.addWebMessageListener(view, "sidepanelNative", setOf(server.baseUrl)) { _, message, _, _, reply ->
            reply.postMessage(dispatcher.handle(message.data))
        }
        view.webViewClient = object : WebViewClient() {
            // M1 has no external links: anything outside the panel origin is blocked, nothing is handed to other apps.
            override fun shouldOverrideUrlLoading(v: WebView, request: WebResourceRequest) =
                !isPanelUrl(request.url.toString(), port)

            override fun onPageFinished(v: WebView, url: String) {
                if (isPanelUrl(url, port)) handler.postDelayed({ app.startGuard.confirmHealthy() }, HEALTHY_AFTER_MS)
            }
        }
    }

    /**
     * WebView does not follow connectivity by itself: without this, navigator.onLine never changes and
     * the `online` event never fires, so widgets cannot recover after the network comes back.
     */
    private fun watchNetwork() {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { handler.post { webView?.setNetworkAvailable(true) } }
            override fun onLost(network: Network) { handler.post { webView?.setNetworkAvailable(false) } }
        }
        cm.registerDefaultNetworkCallback(callback)
        networkCallback = callback
    }

    private fun showFallback(reason: String, detail: String?) {
        // Server and WebView problems are handled states, not failed starts.
        if (reason != "safe") app.startGuard.confirmHealthy()
        val message = when (reason) {
            "safe" -> "The panel did not start properly last time, so it was skipped."
            "server" -> "The panel server could not start: $detail"
            else -> "This device's WebView cannot run the panel ($detail).\n" +
                "Update Chrome or Android System WebView (version ${WebViewCheck.MIN_CHROMIUM} or newer)."
        }
        val pad = (resources.displayMetrics.density * 24).toInt()
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
            setPadding(pad, pad, pad, pad)
            addView(TextView(context).apply {
                text = message
                setTextColor(Color.WHITE)
                textSize = 20f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, pad)
            })
            addView(Button(context).apply { text = "Open apps"; setOnClickListener { openDrawer() } })
            addView(Button(context).apply {
                text = "Try again"
                setOnClickListener { app.startGuard.reset(); recreate() }
            })
        })
    }

    fun openDrawer() {
        startActivity(Intent(this, AppDrawerActivity::class.java))
    }

    @Deprecated("Still the Back path below API 33")
    override fun onBackPressed() = openDrawer()

    @Suppress("DEPRECATION")
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        // An orderly destroy (recreate, config change) before the healthy mark is not a failed start.
        app.startGuard.stopOrderly()
        networkCallback?.let { getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(it) }
        networkCallback = null
        webView?.destroy()
        webView = null
        super.onDestroy()
    }

    companion object {
        private const val HEALTHY_AFTER_MS = 5000L
    }
}
