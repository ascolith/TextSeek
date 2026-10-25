package com.ascolith.textseek.domain.model

/** A single recognized word. */
data class RecognizedText(
    val text: String,
    val outline: TsQuad,
    val angle: Float,
)
