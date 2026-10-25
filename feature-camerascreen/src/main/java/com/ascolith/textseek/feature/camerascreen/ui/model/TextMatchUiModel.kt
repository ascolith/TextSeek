package com.ascolith.textseek.feature.camerascreen.ui.model

import androidx.annotation.ColorInt
import androidx.compose.ui.geometry.Rect as ComposeRect
import com.ascolith.textseek.domain.model.RecognizedText

data class TextMatchUiModel(
    val text: String,
    val angle: Float,
    val boundingBox: ComposeRect,
    val color: Int,
)

object TextMatchFactory {

    /** [text] is the recognized text spanning [words], as it reads on the line. */
    fun createTextMatch(
        text: String,
        words: List<RecognizedText>,
        @ColorInt color: Int,
    ): TextMatchUiModel {
        require(words.isNotEmpty()) { "A text match must cover at least one word" }
        val boundingBox = ComposeRect(
            left = words.minOf { it.boundingBox.left },
            top = words.minOf { it.boundingBox.top },
            right = words.maxOf { it.boundingBox.right },
            bottom = words.maxOf { it.boundingBox.bottom },
        )
        return TextMatchUiModel(
            text = text,
            angle = words.first().angle,
            boundingBox = boundingBox,
            color = color,
        )
    }
}
