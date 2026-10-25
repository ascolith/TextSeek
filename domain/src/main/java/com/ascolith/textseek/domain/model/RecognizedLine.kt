package com.ascolith.textseek.domain.model

/** The words of one recognized line, in reading order, so multi-word terms can match across them. */
data class RecognizedLine(
    val words: List<RecognizedText>,
)
