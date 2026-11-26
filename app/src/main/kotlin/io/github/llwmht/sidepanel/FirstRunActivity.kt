package io.github.llwmht.sidepanel

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject

/** First run: make the app the home screen, then pick a starter layout. Shown once. */
class FirstRunActivity : Activity() {
    private val app get() = application as App

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (resources.displayMetrics.density * 24).toInt()
        fun label(text: String, size: Float, color: Int = Color.WHITE) = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            setPadding(0, pad / 2, 0, pad / 4)
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(pad, pad, pad, pad)
            addView(label("Welcome to zob-sidepanel", 24f))
            addView(label("1. Make it the home screen, so the device starts on the panel.", 16f))
            addView(Button(context).apply { text = "Choose the home app"; setOnClickListener { openHomeSettings(context) } })
            addView(label("2. Pick a starting layout. You can change everything later from a browser.", 16f))
            for (name in TEMPLATES) {
                val text = assets.open("panel/templates/$name.json").bufferedReader().use { it.readText() }
                addView(Button(context).apply {
                    this.text = JSONObject(text).optString("name", name)
                    setOnClickListener {
                        LayoutStore(filesDir).write(text)
                        markDone(context)
                        app.onPanelReload?.invoke()
                        finish()
                    }
                })
            }
        })
    }

    companion object {
        val TEMPLATES = listOf("clock", "clock-weather", "web-page")

        private fun prefs(context: Context) = context.getSharedPreferences("first_run", Context.MODE_PRIVATE)

        fun markDone(context: Context) = prefs(context).edit().putBoolean("done", true).apply()

        /** Needed once: never done before, and no layout stored (a device that already has one skips it). */
        fun needed(context: Context): Boolean {
            if (prefs(context).getBoolean("done", false)) return false
            if (LayoutStore(context.filesDir).read() != null) {
                markDone(context)
                return false
            }
            return true
        }
    }
}
