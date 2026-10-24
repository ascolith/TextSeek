package com.ascolith.textseek.core.hardware.vibrator

import com.ascolith.textseek.domain.feature.time.ITimeProvider
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Leading-edge throttle: the first acquisition succeeds immediately, then
 * further acquisitions fail until [Duration] has elapsed since the last success.
 */
class VibrationThrottle @Inject constructor(
    private val timeProvider: ITimeProvider,
) {

    private var lastAcquiredTime: Instant = Instant.DISTANT_PAST

    fun tryAcquire(window: Duration): Boolean {
        val timeNow = timeProvider.now()
        if ((timeNow - lastAcquiredTime) < window) {
            return false
        }
        lastAcquiredTime = timeNow
        return true
    }
}
