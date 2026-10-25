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
        val terms = searchSettings.mapNotNull { search ->
            if (!search.enabled) return@mapNotNull null
            val text = search.text.normalizeWhitespace()
            if (text.isEmpty()) null else SearchTerm(text, !search.caseSensitive, search.colorArgb)
        }
        if (terms.isEmpty()) return emptyList()

        // Recognized texts are single words, so a multi-word term can only match once each
        // line's words are rejoined.
        val lines = recognizedTexts.groupBy { it.lineIndex }.values.map(::RecognizedLine)
        return terms.flatMap { term ->
            lines.flatMap { line ->
                line.findMatches(term.text, term.ignoreCase)
                    .map { TextMatchFactory.createTextMatch(it, term.colorArgb) }
            }
        }
    }

    private class SearchTerm(val text: String, val ignoreCase: Boolean, val colorArgb: Int)

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
                // A hit that lies only on a separator covers no words.
                .filterNot { it.isEmpty() }
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

    // Collapse before trimming: on Android \s also matches characters trim() keeps (e.g. U+0085),
    // so trimming first could leave a lone separator as the term.
    private fun String.normalizeWhitespace() = replace(WHITESPACE, " ").trim()

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
