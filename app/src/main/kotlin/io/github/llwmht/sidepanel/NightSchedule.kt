package io.github.llwmht.sidepanel

import java.util.Calendar
import java.util.TimeZone

/**
 * A daily night window [from, to) in local time ("HH:MM"); it may cross midnight. Equal or invalid times
 * mean there is no night. Same rules as `web/panel/night.js`.
 */
class NightSchedule(from: String?, to: String?) {
    private val start = minutes(from)
    private val end = minutes(to)
    private val valid = start != null && end != null && start != end

    fun isNight(now: Long, tz: TimeZone = TimeZone.getDefault()): Boolean {
        if (!valid) return false
        val c = Calendar.getInstance(tz).apply { timeInMillis = now }
        val t = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        return if (start!! < end!!) t >= start && t < end else t >= start || t < end
    }

    /** The next moment the state changes, and whether night starts then; null when there is no night. */
    fun nextChange(now: Long, tz: TimeZone = TimeZone.getDefault()): Pair<Long, Boolean>? {
        if (!valid) return null
        val night = isNight(now, tz)
        val target = if (night) end!! else start!!
        val c = Calendar.getInstance(tz).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, target / 60)
            set(Calendar.MINUTE, target % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_MONTH, 1)
        }
        return c.timeInMillis to !night
    }

    companion object {
        private val TIME = Regex("""([01]\d|2[0-3]):([0-5]\d)""")

        fun minutes(text: String?): Int? {
            val m = TIME.matchEntire(text ?: return null) ?: return null
            return m.groupValues[1].toInt() * 60 + m.groupValues[2].toInt()
        }
    }
}
