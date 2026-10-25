package com.ascolith.textseek.feature.camerascreen.ui.model

import androidx.annotation.ColorInt
import androidx.compose.ui.geometry.Rect as ComposeRect
import com.ascolith.textseek.domain.model.RecognizedText
import com.ascolith.textseek.domain.model.TsRect

data class TextMatchUiModel(
    val text: String,
    val angle: Float,
    val boundingBox: ComposeRect,
    val color: Int,
)

object TextMatchFactory {

    fun createTextMatch(
        words: List<RecognizedText>,
        @ColorInt color: Int,
    ): TextMatchUiModel {
        require(words.isNotEmpty()) { "A text match must cover at least one word" }
        val boundingBox = TsRect(
            left = words.minOf { it.boundingBox.left },
            top = words.minOf { it.boundingBox.top },
            right = words.maxOf { it.boundingBox.right },
            bottom = words.maxOf { it.boundingBox.bottom },
        )
        return TextMatchUiModel(
            text = words.joinToString(" ") { it.text },
            angle = words.first().angle,
            boundingBox = boundingBox.toComposeRect(),
            color = color,
        )
    }

    private fun TsRect.toComposeRect(): ComposeRect {
        return ComposeRect(
            left = left,
            top = top,
            right = right,
            bottom = bottom
        )
    }
}
