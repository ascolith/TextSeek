package com.ascolith.textseek.feature.camerascreen.ui.matchextractor

import com.ascolith.textseek.domain.model.RecognizedText
import com.ascolith.textseek.feature.camerascreen.ui.model.SearchSettingUiModel
import com.ascolith.textseek.feature.camerascreen.ui.model.TextMatchFactory
import com.ascolith.textseek.feature.camerascreen.ui.model.TextMatchUiModel
import javax.inject.Inject

class TextMatchExtractor @Inject constructor() : ITextMatchExtractor {

    override fun extract(
        recognizedTexts: List<RecognizedText>,
        searchSettings: List<SearchSettingUiModel>,
    ): List<TextMatchUiModel> {
        // Recognized texts are single words, so a multi-word term can only match once each
        // line's words are rejoined.
        val lines = recognizedTexts.groupBy { it.lineIndex }.values.map(::RecognizedLine)
        return searchSettings
            .filter { it.enabled }
            .flatMap { search ->
                val term = search.text.normalizeWhitespace()
                if (term.isEmpty()) return@flatMap emptyList()
                lines.flatMap { line ->
                    line.findMatches(term, ignoreCase = !search.caseSensitive)
                        .map { TextMatchFactory.createTextMatch(it, search.colorArgb) }
                }
            }
    }

    private class RecognizedLine(val words: List<RecognizedText>) {
        val text = words.joinToString(" ") { it.text }
        private val wordStarts = words.runningFold(0) { start, word -> start + word.text.length + 1 }

        /** Returns the words covered by each occurrence of [term], one entry per distinct span. */
        fun findMatches(term: String, ignoreCase: Boolean): List<List<RecognizedText>> =
            generateSequence(text.indexOf(term, ignoreCase = ignoreCase)) { previous ->
                text.indexOf(term, startIndex = previous + 1, ignoreCase = ignoreCase)
            }
                .takeWhile { it >= 0 }
                .map { start -> wordIndicesCovering(start, start + term.length) }
                // Dedupe by position, not content: identical words can legitimately repeat.
                .distinct()
                .map { indices -> words.slice(indices) }
                .toList()

        private fun wordIndicesCovering(start: Int, end: Int): IntRange {
            val first = words.indices.first { wordStarts[it] + words[it].text.length > start }
            val last = words.indices.last { wordStarts[it] < end }
            return first..last
        }
    }

    private fun String.normalizeWhitespace() = trim().replace(WHITESPACE, " ")

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
