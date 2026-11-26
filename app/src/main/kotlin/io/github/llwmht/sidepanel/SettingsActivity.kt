package io.github.llwmht.sidepanel

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Opens the system screen where the owner picks the home app. */
fun openHomeSettings(context: Context) {
    val intent = Intent(Settings.ACTION_HOME_SETTINGS)
    context.startActivity(if (intent.resolveActivity(context.packageManager) != null) intent else Intent(Settings.ACTION_SETTINGS))
}

/**
 * On-device settings: things only the device can do. Layout, pages, night hours and the burn-in guard are
 * edited from a browser ("Edit from another device").
 */
class SettingsActivity : Activity() {
    private val app get() = application as App
    private lateinit var adminState: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (resources.displayMetrics.density * 24).toInt()
        fun label(text: String, size: Float = 16f, color: Int = Color.WHITE) = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            setPadding(0, pad / 2, 0, pad / 4)
        }
        adminState = label("", 14f, Color.GRAY)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(pad, pad, pad, pad)
            addView(label("Settings", 22f))
            addView(label("Pages, widgets, night hours and screen care are edited from a browser.", 14f, Color.GRAY))
            addView(Button(context).apply {
                text = "Edit from another device"
                setOnClickListener { startActivity(Intent(context, EditorAccessActivity::class.java)) }
            })
            addView(label("Home screen"))
            addView(Button(context).apply {
                text = "Choose the home app"
                setOnClickListener { openHomeSettings(context) }
            })
            addView(label("Night: turn the screen off"))
            addView(label("Needed only for the \"off\" night mode. Without it, the night is a dimmed clock.", 14f, Color.GRAY))
            addView(Button(context).apply {
                text = "Allow turning the screen off"
                setOnClickListener {
                    startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                        .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, app.night.adminComponent())
                        .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "zob-sidepanel turns the screen off at night and back on in the morning. It uses no other device-administrator power."))
                }
            })
            addView(adminState)
        })
    }

    override fun onResume() {
        super.onResume()
        adminState.text = if (app.night.canTurnScreenOff()) "Allowed." else "Not allowed."
    }
}
