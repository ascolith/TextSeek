package com.ascolith.textseek.domain.model

/**
 * A single recognized word. [lineIndex] identifies the line the word belongs to within a frame,
 * so consecutive words sharing it can be rejoined to match multi-word search terms.
 */
data class RecognizedText(
    val text: String,
    val boundingBox: TsRect,
    val angle: Float,
    val lineIndex: Int,
)
