package com.ascolith.textseek.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

object ColorCalculation {

    /**
     * Picks whichever of [lightForeground] / [darkForeground] is more readable on
     * [backgroundColor], where that background is drawn on top of [surfaceColor].
     *
     * The surface matters because [luminance] ignores alpha. A translucent background is only as
     * readable as whatever shows through it, and a fully transparent one carries no information at
     * all. Callers must say what is behind the background rather than let it be guessed.
     */
    fun getBestForegroundColor(
        backgroundColor: Color,
        surfaceColor: Color,
        lightForeground: Color = Color.White,
        darkForeground: Color = Color.Black,
    ): Color {
        val effectiveBackground = backgroundColor.compositeOver(surfaceColor)
        val contrastWithLight = contrastRatio(effectiveBackground, lightForeground)
        val contrastWithDark = contrastRatio(effectiveBackground, darkForeground)

        return if (contrastWithLight > contrastWithDark) lightForeground else darkForeground
    }

    /**
     * The WCAG contrast ratio between two colors, from 1:1 (identical) to 21:1 (black on white).
     * Alpha is ignored, so composite translucent colors onto their backdrop first.
     */
    fun contrastRatio(color1: Color, color2: Color): Float {
        val lum1 = color1.luminance()
        val lum2 = color2.luminance()
        val lighter = max(lum1, lum2)
        val darker = min(lum1, lum2)

        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
