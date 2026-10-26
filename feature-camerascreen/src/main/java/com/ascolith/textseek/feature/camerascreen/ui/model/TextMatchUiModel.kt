package com.ascolith.textseek.feature.camerascreen.ui.model

import androidx.annotation.ColorInt
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.ascolith.textseek.domain.model.RecognizedText
import kotlin.math.hypot

data class TextMatchUiModel(
    val text: String,
    val angle: Float,
    val outline: MatchOutline,
    val color: Int,
)

/** The corners of a match's outline, in drawing order. */
data class MatchOutline(
    val topLeft: Offset,
    val topRight: Offset,
    val bottomRight: Offset,
    val bottomLeft: Offset,
) {
    fun map(transform: (Offset) -> Offset) = MatchOutline(
        topLeft = transform(topLeft),
        topRight = transform(topRight),
        bottomRight = transform(bottomRight),
        bottomLeft = transform(bottomLeft),
    )

    companion object {
        fun of(rect: Rect) = MatchOutline(rect.topLeft, rect.topRight, rect.bottomRight, rect.bottomLeft)
    }
}

object TextMatchFactory {

    /** [text] is the recognized text spanning [words], as it reads on the line. */
    fun createTextMatch(
        text: String,
        words: List<RecognizedText>,
        @ColorInt color: Int,
    ): TextMatchUiModel {
        require(words.isNotEmpty()) { "A text match must cover at least one word" }
        return TextMatchUiModel(
            text = text,
            angle = words.first().angle,
            outline = outlineAround(words),
            color = color,
        )
    }

    /**
     * The smallest rectangle around [words] that is aligned with the text rather than the frame,
     * so a tilted phrase isn't outlined by an upright box that takes in the rows around it.
     */
    private fun outlineAround(words: List<RecognizedText>): MatchOutline {
        // The words' top and bottom edges all run along the line, so their sum is its direction.
        var dx = 0f
        var dy = 0f
        words.forEach {
            with(it.outline) {
                dx += topRight.x - topLeft.x + bottomRight.x - bottomLeft.x
                dy += topRight.y - topLeft.y + bottomRight.y - bottomLeft.y
            }
        }
        val length = hypot(dx, dy)
        if (length == 0f) {
            dx = 1f
            dy = 0f
        } else {
            dx /= length
            dy /= length
        }

        // Measure every corner along the line and across it, then rebuild the extremes.
        val corners = words.flatMap { it.outline.corners }
        val alongMin = corners.minOf { it.x * dx + it.y * dy }
        val alongMax = corners.maxOf { it.x * dx + it.y * dy }
        val acrossMin = corners.minOf { it.y * dx - it.x * dy }
        val acrossMax = corners.maxOf { it.y * dx - it.x * dy }
        fun at(along: Float, across: Float) = Offset(along * dx - across * dy, along * dy + across * dx)
        return MatchOutline(
            topLeft = at(alongMin, acrossMin),
            topRight = at(alongMax, acrossMin),
            bottomRight = at(alongMax, acrossMax),
            bottomLeft = at(alongMin, acrossMax),
        )
    }
}
