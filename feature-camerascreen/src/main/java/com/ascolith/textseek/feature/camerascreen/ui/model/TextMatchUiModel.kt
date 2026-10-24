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
        textElement: RecognizedText,
        @ColorInt color: Int,
    ): TextMatchUiModel {
        val boundingBox = textElement.boundingBox
        return TextMatchUiModel(
            text = textElement.text,
            angle = textElement.angle,
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
