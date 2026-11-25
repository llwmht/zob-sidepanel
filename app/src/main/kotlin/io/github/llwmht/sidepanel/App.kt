package io.github.llwmht.sidepanel

import android.app.Application
import android.content.Context
import android.os.Process
import android.util.Log

class App : Application() {
    lateinit var startGuard: StartGuard
        private set
    var server: PanelServer? = null
        private set

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("start", Context.MODE_PRIVATE)
        startGuard = StartGuard(object : FlagStore {
            override var pending: Boolean
                get() = prefs.getBoolean("pending", false)
                // commit(), not apply(): the flag must be on disk before a crash can kill the process.
                set(value) { prefs.edit().putBoolean("pending", value).commit() }
        })
        // On Android 7 the system clears the default home app when a non-system home app is reported
        // as crashed, so we end the process ourselves instead of delegating to the default handler.
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            Log.e(TAG, "uncaught exception, ending process", e)
            Process.killProcess(Process.myPid())
        }
        startServer()
    }

    /** (Re)starts the panel server; null on success, otherwise a one-line reason. */
    fun startServer(): String? {
        server?.stop()
        val s = PanelServer({ path -> try { assets.open(path) } catch (e: Exception) { null } })
        server = s
        return s.startOrError()
    }

    companion object {
        const val TAG = "sidepanel"
    }
}
