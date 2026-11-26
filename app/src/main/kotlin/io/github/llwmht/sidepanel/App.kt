package io.github.llwmht.sidepanel

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.util.Log
import org.json.JSONObject
import java.security.SecureRandom

class App : Application() {
    lateinit var startGuard: StartGuard
        private set
    lateinit var pairing: Pairing
        private set
    lateinit var editorApi: EditorApi
        private set
    lateinit var night: NightController
        private set
    var server: PanelServer? = null
        private set

    /** LAN editing: other devices may reach the server (after pairing). Off by default. */
    @Volatile var lanEnabled = false
        private set
    @Volatile private var lastLanRequest = 0L

    /** Set by the panel activity: reloads the panel after a preview or a save. */
    @Volatile var onPanelReload: (() -> Unit)? = null

    private val main = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("start", Context.MODE_PRIVATE)
        startGuard = StartGuard(object : FlagStore {
            override var pending: Boolean
                get() = prefs.getBoolean("pending", false)
                // commit(), not apply(): the flag must be on disk before a crash can kill the process.
                set(value) { prefs.edit().putBoolean("pending", value).commit() }
        })
        val access = getSharedPreferences("access", Context.MODE_PRIVATE)
        val random = SecureRandom()
        pairing = Pairing({ n -> ByteArray(n).also { random.nextBytes(it) } }, object : TokenStore {
            override fun load(): Set<String> = HashSet(access.getStringSet("tokens", emptySet()) ?: emptySet())
            override fun save(tokens: Set<String>) { access.edit().putStringSet("tokens", HashSet(tokens)).commit() }
        })
        editorApi = EditorApi(
            LayoutStore(filesDir),
            pairing,
            catalog = { widgetCatalog({ assets.list(it) }, { p -> try { assets.open(p) } catch (e: Exception) { null } }) },
            device = {
                val m = resources.displayMetrics
                JSONObject().put("width", (m.widthPixels / m.density).toInt()).put("height", (m.heightPixels / m.density).toInt()).toString()
            },
            reloadPanel = { onPanelReload?.invoke() },
        )
        // On Android 7 the system clears the default home app when a non-system home app is reported
        // as crashed, so we end the process ourselves instead of delegating to the default handler.
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            Log.e(TAG, "uncaught exception, ending process", e)
            Process.killProcess(Process.myPid())
        }
        night = NightController(this)
        night.reschedule()
        startServer()
    }

    /** (Re)starts the panel server; null on success, otherwise a one-line reason. */
    fun startServer(): String? {
        server?.stop()
        val s = PanelServer(
            { path -> try { assets.open(path) } catch (e: Exception) { null } },
            host = if (lanEnabled) "0.0.0.0" else "127.0.0.1",
            api = editorApi,
            access = { r ->
                val decision = AccessPolicy.decide(r, lanEnabled, pairing::isValid)
                // Only a paired browser's activity keeps editing on; guesses and refused requests must not.
                if (decision == null && r.remoteIp != "127.0.0.1" && pairing.isValid(AccessPolicy.sessionToken(r.cookie))) {
                    lastLanRequest = SystemClock.elapsedRealtime()
                }
                decision
            },
        )
        server = s
        return s.startOrError()
    }

    /**
     * Opens the server to the local network. Called each time the editing screen opens: a new pairing code
     * is made only when there is none (first time, or after pairing locked on wrong guesses), and the server
     * restarts only when it was not already open, so editors in progress are not cut off.
     */
    fun enableLan(): String? {
        if (pairing.currentCode() == null) pairing.newCode()
        lastLanRequest = SystemClock.elapsedRealtime()
        main.removeCallbacks(idleCheck)
        main.postDelayed(idleCheck, IDLE_CHECK_MS)
        if (lanEnabled) return null
        lanEnabled = true
        return startServer()
    }

    /** Back to loopback only; drops the code and any unsaved preview. */
    fun disableLan() {
        lanEnabled = false
        pairing.stop()
        main.removeCallbacks(idleCheck)
        val hadDraft = editorApi.draft != null
        editorApi.clearDraft()
        startServer()
        if (hadDraft) onPanelReload?.invoke()
    }

    private val idleCheck = object : Runnable {
        override fun run() {
            if (!lanEnabled) return
            if (SystemClock.elapsedRealtime() - lastLanRequest > LAN_IDLE_MS) disableLan()
            else main.postDelayed(this, IDLE_CHECK_MS)
        }
    }

    companion object {
        const val TAG = "sidepanel"
        const val LAN_IDLE_MS = 15 * 60 * 1000L
        private const val IDLE_CHECK_MS = 60 * 1000L
    }
}
