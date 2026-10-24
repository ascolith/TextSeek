package com.ascolith.textseek.core.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorCalculationTest {

    @Test
    fun `a light background gets a dark foreground`() {
        assertEquals(Color.Black, bestForegroundOn(Color.White))
    }

    @Test
    fun `a dark background gets a light foreground`() {
        assertEquals(Color.White, bestForegroundOn(Color.Black))
    }

    @Test
    fun `a saturated yellow background gets a dark foreground`() {
        assertEquals(Color.Black, bestForegroundOn(Color.Yellow))
    }

    @Test
    fun `a saturated blue background gets a light foreground`() {
        assertEquals(Color.White, bestForegroundOn(Color.Blue))
    }

    @Test
    fun `custom foregrounds are honoured`() {
        val cream = Color(0xFFFFFDD0)
        val charcoal = Color(0xFF36454F)

        assertEquals(
            charcoal,
            ColorCalculation.getBestForegroundColor(
                backgroundColor = Color.White,
                surfaceColor = Color.White,
                lightForeground = cream,
                darkForeground = charcoal,
            ),
        )
    }

    @Test
    fun `a transparent background is read through to a light surface`() {
        // Regression: luminance() ignores alpha, so a transparent background used to score as
        // black and take a white foreground, which vanished against a light surface.
        assertEquals(Color.Black, bestForegroundOn(Color.Transparent, surface = Color.White))
    }

    @Test
    fun `a transparent background is read through to a dark surface`() {
        assertEquals(Color.White, bestForegroundOn(Color.Transparent, surface = Color.Black))
    }

    @Test
    fun `a translucent dark background over a light surface reads as the blend`() {
        // 25% black over white composites to a light grey, which still wants dark text.
        val quarterBlack = Color.Black.copy(alpha = 0.25f)

        assertEquals(Color.Black, bestForegroundOn(quarterBlack, surface = Color.White))
    }

    @Test
    fun `a mostly opaque dark background over a light surface reads as dark`() {
        val mostlyBlack = Color.Black.copy(alpha = 0.9f)

        assertEquals(Color.White, bestForegroundOn(mostlyBlack, surface = Color.White))
    }

    @Test
    fun `an opaque background ignores the surface behind it`() {
        assertEquals(
            bestForegroundOn(Color.White, surface = Color.Black),
            bestForegroundOn(Color.White, surface = Color.White),
        )
    }

    @Test
    fun `contrast ratio of black on white is the maximum`() {
        assertEquals(21f, ColorCalculation.contrastRatio(Color.Black, Color.White), 0.01f)
    }

    @Test
    fun `contrast ratio of a color with itself is the minimum`() {
        assertEquals(1f, ColorCalculation.contrastRatio(Color.Magenta, Color.Magenta), 0.001f)
    }

    @Test
    fun `contrast ratio is symmetric`() {
        assertEquals(
            ColorCalculation.contrastRatio(Color.Blue, Color.Yellow),
            ColorCalculation.contrastRatio(Color.Yellow, Color.Blue),
            0.001f,
        )
    }

    @Test
    fun `the default foregrounds always clear wcag aa, whatever the background`() {
        // Picking the better of black and white bottoms out around 4.58:1, at a mid grey near
        // luminance 0.179 — so the defaults can never fall below the 4.5:1 bar for normal text.
        (0..255).forEach { channel ->
            val value = channel / 255f
            val grey = Color(red = value, green = value, blue = value)
            val ratio = ColorCalculation.contrastRatio(bestForegroundOn(grey, surface = grey), grey)

            assertTrue("grey $channel only reached $ratio:1", ratio >= WCAG_AA_NORMAL_TEXT)
        }
    }

    @Test
    fun `custom foregrounds can fall below wcag aa`() {
        // The guarantee above comes from black and white specifically, not from the algorithm.
        val midGrey = Color(0xFF808080)
        val nearbyGrey = Color(0xFF8A8A8A)

        val chosen = ColorCalculation.getBestForegroundColor(
            backgroundColor = midGrey,
            surfaceColor = midGrey,
            lightForeground = nearbyGrey,
            darkForeground = nearbyGrey,
        )

        assertFalse(ColorCalculation.contrastRatio(chosen, midGrey) >= WCAG_AA_NORMAL_TEXT)
    }

    private fun bestForegroundOn(background: Color, surface: Color = Color.White) =
        ColorCalculation.getBestForegroundColor(backgroundColor = background, surfaceColor = surface)

    companion object {
        /** WCAG AA requires 4.5:1 for normal-size text. */
        private const val WCAG_AA_NORMAL_TEXT = 4.5f
    }

}
