package com.ascolith.textseek.domain.model

data class RecognizedText(
    val text: String,
    val boundingBox: TsRect,
    val angle: Float,
)
