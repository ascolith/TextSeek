package com.ascolith.textseek.feature.camerascreen.ui.matchextractor

import com.ascolith.textseek.domain.model.RecognizedLine
import com.ascolith.textseek.domain.model.RecognizedText
import com.ascolith.textseek.feature.camerascreen.ui.model.SearchSettingUiModel
import com.ascolith.textseek.feature.camerascreen.ui.model.TextMatchFactory
import com.ascolith.textseek.feature.camerascreen.ui.model.TextMatchUiModel
import javax.inject.Inject

class TextMatchExtractor @Inject constructor() : ITextMatchExtractor {

    override fun extract(
        recognizedLines: List<RecognizedLine>,
        searchSettings: List<SearchSettingUiModel>,
    ): List<TextMatchUiModel> {
        val terms = termsFor(searchSettings)
        if (terms.isEmpty()) return emptyList()

        // Recognized texts are single words, so a multi-word term can only match once each
        // line's words are rejoined. Only build the joined lines if such a term is active.
        val lines by lazy(LazyThreadSafetyMode.NONE) { recognizedLines.map { SearchableLine(it.words) } }
        return terms.flatMap { term ->
            if (WORD_SEPARATOR in term.text) {
                lines.flatMap { it.findMatches(term) }
            } else {
                // A term without spaces can't cross a separator, so it matches within one word.
                recognizedLines.flatMap { line ->
                    line.words
                        .filter { it.text.contains(term.text, term.ignoreCase) }
                        .map { TextMatchFactory.createTextMatch(it.text, listOf(it), term.colorArgb) }
                }
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

    private class SearchableLine(val words: List<RecognizedText>) {
        private val text = words.joinToString(WORD_SEPARATOR) { it.text }
        private val wordStarts = words.dropLast(1).runningFold(0) { start, word ->
            start + word.text.length + WORD_SEPARATOR.length
        }

        /** Returns a match for each occurrence of [term], one per distinct span of words. */
        fun findMatches(term: SearchTerm): List<TextMatchUiModel> {
            var start = text.indexOf(term.text, ignoreCase = term.ignoreCase)
            if (start < 0) return emptyList()
            val matches = mutableListOf<TextMatchUiModel>()
            while (start >= 0) {
                val first = wordIndexAt(start)
                // Normalized terms never start with a space, but never box a hit that does.
                if (start < wordEnd(first)) {
                    val last = wordIndexAt(start + term.text.length - 1)
                    matches += TextMatchFactory.createTextMatch(
                        text = text.substring(wordStarts[first], wordEnd(last)),
                        words = words.subList(first, last + 1),
                        color = term.colorArgb,
                    )
                }
                // Words hold no spaces, so each of the term's spaces must fall on a separator, and
                // any later hit starting in the same word covers the same words.
                val next = first + 1
                if (next == words.size) break
                start = text.indexOf(term.text, startIndex = wordStarts[next], ignoreCase = term.ignoreCase)
            }
            return matches
        }

        /** The index of the word containing [offset], or of the word before it on a separator. */
        private fun wordIndexAt(offset: Int): Int {
            val index = wordStarts.binarySearch(offset)
            return if (index >= 0) index else -index - 2
        }

        private fun wordEnd(index: Int) = wordStarts[index] + words[index].text.length
    }

    // Collapse before trimming: on Android \s also matches characters trim() keeps (e.g. U+0085),
    // so trimming first could leave a lone separator as the term.
    private fun String.normalizeWhitespace() = replace(WHITESPACE, WORD_SEPARATOR).trim()

    private companion object {
        /** Joins a line's words, and stands in for any whitespace run in a search term. */
        const val WORD_SEPARATOR = " "
        val WHITESPACE = Regex("\\s+")
    }
}
