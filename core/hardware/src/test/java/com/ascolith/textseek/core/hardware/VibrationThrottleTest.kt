package com.ascolith.textseek.core.hardware

import com.ascolith.textseek.core.hardware.vibrator.VibrationThrottle
import com.ascolith.textseek.domain.feature.time.ITimeProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant

class VibrationThrottleTest {

    private var now: Instant = Instant.fromEpochMilliseconds(0)
    private val throttle = VibrationThrottle(timeProvider = ITimeProvider { now })

    private fun advanceBy(duration: Duration) {
        now += duration
    }

    private fun rewindBy(duration: Duration) {
        now -= duration
    }

    @Test
    fun `first acquisition succeeds immediately`() {
        assertTrue(throttle.tryAcquire(WINDOW))
    }

    @Test
    fun `acquisitions within the window fail`() {
        assertTrue(throttle.tryAcquire(WINDOW))

        advanceBy(500.milliseconds)
        assertFalse(throttle.tryAcquire(WINDOW))

        advanceBy(999.milliseconds)
        assertFalse(throttle.tryAcquire(WINDOW))
    }

    @Test
    fun `acquisition succeeds again once the window has elapsed`() {
        assertTrue(throttle.tryAcquire(WINDOW))

        advanceBy(WINDOW)
        assertTrue(throttle.tryAcquire(WINDOW))
    }

    @Test
    fun `failed acquisitions do not extend the window`() {
        assertTrue(throttle.tryAcquire(WINDOW))

        // Keep trying throughout the window; none of these should push out
        // the moment the throttle re-opens.
        repeat(14) {
            advanceBy(100.milliseconds)
            assertFalse(throttle.tryAcquire(WINDOW))
        }

        advanceBy(100.milliseconds)
        assertTrue(throttle.tryAcquire(WINDOW))
    }

    @Test
    fun `acquisition one nanosecond before the window has elapsed fails`() {
        assertTrue(throttle.tryAcquire(WINDOW))

        advanceBy(WINDOW - 1.nanoseconds)
        assertFalse(throttle.tryAcquire(WINDOW))
    }

    @Test
    fun `each success restarts the window`() {
        assertTrue(throttle.tryAcquire(WINDOW))

        advanceBy(WINDOW)
        assertTrue(throttle.tryAcquire(WINDOW))

        // The window now runs from the second success, not the first.
        advanceBy(WINDOW - 1.nanoseconds)
        assertFalse(throttle.tryAcquire(WINDOW))
    }

    @Test
    fun `a shorter window on a later call allows an earlier acquisition`() {
        assertTrue(throttle.tryAcquire(WINDOW))

        advanceBy(500.milliseconds)
        assertFalse(throttle.tryAcquire(WINDOW))
        // The window is per call, not remembered from the acquisition that set it.
        assertTrue(throttle.tryAcquire(400.milliseconds))
    }

    @Test
    fun `a longer window on a later call blocks an otherwise allowed acquisition`() {
        assertTrue(throttle.tryAcquire(400.milliseconds))

        advanceBy(500.milliseconds)
        assertFalse(throttle.tryAcquire(WINDOW))
    }

    @Test
    fun `a zero window always succeeds`() {
        assertTrue(throttle.tryAcquire(Duration.ZERO))
        assertTrue(throttle.tryAcquire(Duration.ZERO))
    }

    @Test
    fun `the clock moving backwards blocks acquisition until it catches up`() {
        assertTrue(throttle.tryAcquire(WINDOW))

        // A backwards clock correction makes the elapsed time negative, which is still less than
        // the window, so the throttle stays closed rather than opening early.
        rewindBy(1.hours)
        assertFalse(throttle.tryAcquire(WINDOW))

        advanceBy(1.hours + WINDOW)
        assertTrue(throttle.tryAcquire(WINDOW))
    }

    companion object {
        private val WINDOW = 1500.milliseconds
    }
}
