package io.github.llwmht.sidepanel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartGuardTest {
    private val guard = StartGuard(object : FlagStore { override var pending = false })

    @Test fun firstStartIsNormal() = assertFalse(guard.begin())

    @Test fun unconfirmedStartMakesNextSafe() {
        guard.begin()
        assertTrue(guard.begin())
    }

    @Test fun confirmedStartIsNormal() {
        guard.begin(); guard.confirmHealthy()
        assertFalse(guard.begin())
    }

    @Test fun safeModeStaysUntilReset() {
        guard.begin(); guard.begin()
        assertTrue(guard.begin())
    }

    @Test fun orderlyStopBeforeHealthyIsNotAFailure() {
        guard.begin(); guard.stopOrderly()
        assertFalse(guard.begin())
    }

    @Test fun orderlyStopKeepsSafeMode() {
        guard.begin(); guard.begin(); guard.stopOrderly()
        assertTrue(guard.begin())
    }

    @Test fun resetLeavesSafeMode() {
        guard.begin(); guard.begin(); guard.reset()
        assertFalse(guard.begin())
    }
}
