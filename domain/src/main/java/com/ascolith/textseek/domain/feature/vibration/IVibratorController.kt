package com.ascolith.textseek.domain.feature.vibration

import kotlin.time.Duration

interface IVibratorController {
    /**
     * Vibrates with [effect], throttled: at most one vibration per [throttleWindow].
     */
    fun vibrate(effect: Effect, throttleWindow: Duration)

    enum class Effect {
        Click,
        DoubleClick,
        HeavyClick,
        Tick,
    }
}
