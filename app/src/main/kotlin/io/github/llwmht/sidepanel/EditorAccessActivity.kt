package io.github.llwmht.sidepanel

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Turns on editing from another device: opens the server to the local network and shows the address and
 * the pairing code. Editing stays on after leaving this screen and turns itself off after 15 idle minutes.
 */
class EditorAccessActivity : Activity() {
    private val app get() = application as App
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var codeView: TextView
    private lateinit var stateView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val error = app.enableLan()
        val ip = wifiAddress()
        val pad = (resources.displayMetrics.density * 24).toInt()

        fun text(size: Float, color: Int = Color.WHITE) = TextView(this).apply {
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            setPadding(0, pad / 3, 0, pad / 3)
        }

        val address = text(22f).apply {
            text = when {
                error != null -> "Could not open the server: $error"
                ip == null -> "No Wi-Fi address. Connect the device to Wi-Fi and open this screen again."
                else -> "On a computer on the same Wi-Fi, open\nhttp://$ip:${AccessPolicy.PORT}/editor/"
            }
        }
        codeView = text(64f).apply { letterSpacing = 0.2f }
        stateView = text(14f, Color.GRAY)

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
            setPadding(pad, pad, pad, pad)
            addView(text(16f, Color.GRAY).apply { text = "Edit from another device" })
            addView(address)
            addView(text(16f, Color.GRAY).apply { text = "Pairing code" })
            addView(codeView)
            addView(stateView)
            addView(Button(context).apply {
                text = "Stop editing"
                setOnClickListener { app.disableLan(); finish() }
            })
            addView(Button(context).apply {
                text = "Forget paired browsers"
                setOnClickListener { app.pairing.forgetAll(); stateView.text = "Paired browsers forgotten." }
            })
        })
        refresh.run()
    }

    /** The code changes after each pairing and after 5 wrong tries, so it is redrawn every second. */
    private val refresh = object : Runnable {
        override fun run() {
            codeView.text = app.pairing.currentCode() ?: "------"
            stateView.text = when {
                !app.lanEnabled -> "Editing is off."
                app.pairing.isLocked() -> "Pairing locked after ${Pairing.MAX_WRONG} wrong codes. Close and reopen this screen for a new code."
                else -> stateView.text
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(refresh)
        super.onDestroy()
    }

    /** First IPv4 address of the Wi-Fi interface (wlan*), or any site-local IPv4 address. */
    private fun wifiAddress(): String? = try {
        val addrs = NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .sortedByDescending { it.name.startsWith("wlan") }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
        (addrs.firstOrNull { it.isSiteLocalAddress } ?: addrs.firstOrNull())?.hostAddress
    } catch (e: Exception) {
        null
    }
}
