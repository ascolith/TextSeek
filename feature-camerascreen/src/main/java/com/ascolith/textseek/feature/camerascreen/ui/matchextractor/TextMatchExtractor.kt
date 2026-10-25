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
        val terms = termsFor(searchSettings)
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

    // Settings change far less often than frames arrive, so keep the last normalization.
    @Volatile
    private var cachedTerms: Pair<List<SearchSettingUiModel>, List<SearchTerm>>? = null

    private fun termsFor(searchSettings: List<SearchSettingUiModel>): List<SearchTerm> {
        cachedTerms?.let { (settings, terms) -> if (settings == searchSettings) return terms }
        val terms = searchSettings.mapNotNull { search ->
            if (!search.enabled) return@mapNotNull null
            val text = search.text.normalizeWhitespace()
            if (text.isEmpty()) null else SearchTerm(text, !search.caseSensitive, search.colorArgb)
        }
        cachedTerms = searchSettings to terms
        return terms
    }

    private class SearchTerm(val text: String, val ignoreCase: Boolean, val colorArgb: Int)

    private class RecognizedLine(val words: List<RecognizedText>) {
        val text = words.joinToString(" ") { it.text }
        private val wordStarts = words.runningFold(0) { start, word -> start + word.text.length + 1 }

        /** Returns the words covered by each occurrence of [term], one entry per distinct span. */
        fun findMatches(term: String, ignoreCase: Boolean): List<List<RecognizedText>> {
            var start = text.indexOf(term, ignoreCase = ignoreCase)
            if (start < 0) return emptyList()
            val matches = mutableListOf<List<RecognizedText>>()
            var previous = IntRange.EMPTY
            while (start >= 0) {
                val indices = wordIndicesCovering(start, start + term.length)
                // Skip a hit that lies only on a separator, and repeat hits on the same span.
                // Starts only increase, so a repeated span always follows its first hit.
                if (!indices.isEmpty() && indices != previous) {
                    matches += words.slice(indices)
                    previous = indices
                }
                start = text.indexOf(term, startIndex = start + 1, ignoreCase = ignoreCase)
            }
            return matches
        }

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
