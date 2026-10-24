package com.ascolith.textseek.core.hardware.vibrator

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import com.ascolith.textseek.domain.feature.vibration.IVibratorController
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration

@Singleton
class VibratorController @Inject constructor(
    @ApplicationContext context: Context,
    private val throttle: VibrationThrottle,
) : IVibratorController {

    private val vibrator = context.getSystemService(Vibrator::class.java)

    override fun vibrate(effect: IVibratorController.Effect, throttleWindow: Duration) {
        if (!throttle.tryAcquire(throttleWindow)) {
            return
        }
        val vibrationEffect = VibrationEffect.createPredefined(effect.toVibrationEffect())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(
                vibrationEffect,
                VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ACCESSIBILITY)
            )
        } else {
            vibrator.vibrate(vibrationEffect)
        }
    }

    private fun IVibratorController.Effect.toVibrationEffect(): Int =
        when (this) {
            IVibratorController.Effect.Click -> VibrationEffect.EFFECT_CLICK
            IVibratorController.Effect.DoubleClick -> VibrationEffect.EFFECT_DOUBLE_CLICK
            IVibratorController.Effect.HeavyClick -> VibrationEffect.EFFECT_HEAVY_CLICK
            IVibratorController.Effect.Tick -> VibrationEffect.EFFECT_TICK
        }
}
