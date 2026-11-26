package io.github.llwmht.sidepanel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class NightScheduleTest {
    private val tz = TimeZone.getTimeZone("Europe/Paris")

    private fun at(day: Int, h: Int, m: Int): Long = Calendar.getInstance(tz).apply {
        clear()
        set(2026, Calendar.NOVEMBER, day, h, m, 0)
    }.timeInMillis

    @Test fun parsesTimes() {
        assertEquals(0, NightSchedule.minutes("00:00"))
        assertEquals(22 * 60 + 30, NightSchedule.minutes("22:30"))
        assertNull(NightSchedule.minutes("24:00"))
        assertNull(NightSchedule.minutes("7:00"))
        assertNull(NightSchedule.minutes(null))
    }

    @Test fun isNightAcrossMidnight() {
        val s = NightSchedule("22:00", "07:00")
        assertTrue(s.isNight(at(26, 23, 0), tz))
        assertTrue(s.isNight(at(27, 3, 0), tz))
        assertFalse(s.isNight(at(27, 7, 0), tz))
        assertFalse(s.isNight(at(26, 12, 0), tz))
    }

    @Test fun nextChangeDuringTheDayIsTonightsStart() {
        val s = NightSchedule("22:00", "07:00")
        assertEquals(at(26, 22, 0) to true, s.nextChange(at(26, 12, 0), tz))
    }

    @Test fun nextChangeDuringTheNightIsTheMorning() {
        val s = NightSchedule("22:00", "07:00")
        assertEquals(at(27, 7, 0) to false, s.nextChange(at(26, 23, 15), tz))
        assertEquals(at(27, 7, 0) to false, s.nextChange(at(27, 2, 0), tz))
    }

    @Test fun exactlyAtAChangeMovesToTheFollowingOne() {
        val s = NightSchedule("13:00", "14:00")
        assertEquals(at(26, 14, 0) to false, s.nextChange(at(26, 13, 0), tz))
        assertEquals(at(27, 13, 0) to true, s.nextChange(at(26, 14, 0), tz))
    }

    @Test fun equalOrInvalidTimesNeverChange() {
        assertNull(NightSchedule("08:00", "08:00").nextChange(at(26, 3, 0), tz))
        assertNull(NightSchedule("nope", "08:00").nextChange(at(26, 3, 0), tz))
        assertFalse(NightSchedule("nope", "08:00").isNight(at(26, 3, 0), tz))
    }
}
