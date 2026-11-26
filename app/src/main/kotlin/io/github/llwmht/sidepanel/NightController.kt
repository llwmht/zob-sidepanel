package io.github.llwmht.sidepanel

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log

/** Lets the app turn the screen off (DevicePolicyManager.lockNow). Granted by the owner in system settings. */
class ScreenAdminReceiver : DeviceAdminReceiver()

/** Fires at the start and end of the night window. */
class NightAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        (context.applicationContext as App).night.onAlarm(intent.action == NightController.ACTION_START)
    }
}

/**
 * Native side of the night schedule. The panel handles "dim" itself while the screen is on; this class keeps
 * the schedule, and for "off" turns the screen off at the start and back on at the end, using exact alarms
 * so it works while the screen is off. The panel sends the schedule at every start (bridge `setNight`).
 */
class NightController(private val context: Context) {
    private val prefs = context.getSharedPreferences("night", Context.MODE_PRIVATE)
    private val admin = ComponentName(context, ScreenAdminReceiver::class.java)
    private val dpm get() = context.getSystemService(DevicePolicyManager::class.java)

    private val enabled get() = prefs.getBoolean("enabled", false)
    private val mode get() = prefs.getString("mode", "dim")
    private val schedule get() = NightSchedule(prefs.getString("from", null), prefs.getString("to", null))

    fun set(enabled: Boolean, from: String, to: String, mode: String) {
        prefs.edit().putBoolean("enabled", enabled).putString("from", from).putString("to", to)
            .putString("mode", if (mode == "off") "off" else "dim").apply()
        reschedule()
    }

    fun canTurnScreenOff(): Boolean = dpm?.isAdminActive(admin) == true

    fun adminComponent(): ComponentName = admin


    /** Sets the alarm for the next change; called after a change, at app start and after each alarm. */
    fun reschedule() {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        for (action in listOf(ACTION_START, ACTION_END)) am.cancel(pending(action))
        if (!enabled) return
        val (time, starts) = schedule.nextChange(System.currentTimeMillis()) ?: return
        val pi = pending(if (starts) ACTION_START else ACTION_END)
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
        } catch (e: SecurityException) {
            // Exact alarms can need a permission on recent Android; a few minutes late is acceptable.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
        }
    }

    /**
     * Called by the panel when it dims for the night. In "off" mode this turns the screen off; the panel only
     * dims after a minute without touches, so this never happens while someone is using the device.
     */
    fun screenOffIfNight() {
        if (enabled && mode == "off" && schedule.isNight(System.currentTimeMillis())) turnScreenOff()
    }

    fun onAlarm(nightStarts: Boolean) {
        if (enabled && mode == "off") {
            if (nightStarts) turnScreenOff() else wakeScreen()
        }
        reschedule()
    }

    private fun turnScreenOff() {
        if (!canTurnScreenOff()) return
        try { dpm?.lockNow() } catch (e: SecurityException) { Log.w(App.TAG, "lockNow refused", e) }
    }

    @Suppress("DEPRECATION") // the only way for a regular app to turn the screen on from the background
    private fun wakeScreen() {
        val pm = context.getSystemService(PowerManager::class.java) ?: return
        pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "sidepanel:morning",
        ).acquire(10_000)
        context.startActivity(Intent(context, PanelActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun pending(action: String): PendingIntent = PendingIntent.getBroadcast(
        context, if (action == ACTION_START) 1 else 2,
        Intent(context, NightAlarmReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val ACTION_START = "io.github.llwmht.sidepanel.NIGHT_START"
        const val ACTION_END = "io.github.llwmht.sidepanel.NIGHT_END"
    }
}
