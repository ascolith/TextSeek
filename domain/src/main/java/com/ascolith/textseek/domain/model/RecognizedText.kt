package com.ascolith.textseek.domain.model

/** A single recognized word. */
data class RecognizedText(
    val text: String,
    val boundingBox: TsRect,
    val angle: Float,
)
