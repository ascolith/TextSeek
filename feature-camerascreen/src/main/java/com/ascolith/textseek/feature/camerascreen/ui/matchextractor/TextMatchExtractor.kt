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
            val matches = if (" " in term.text) {
                lines.flatMap { it.findMatches(term.text, term.ignoreCase) }
            } else {
                // A term without spaces can't cross a separator, so it matches within one word.
                recognizedLines.flatMap { line ->
                    line.words.filter { it.text.contains(term.text, term.ignoreCase) }.map(::listOf)
                }
            }
            matches.map { TextMatchFactory.createTextMatch(it, term.colorArgb) }
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
        val text = words.joinToString(" ") { it.text }
        private val wordStarts = words.dropLast(1).runningFold(0) { start, word -> start + word.text.length + 1 }

        /** Returns the words covered by each occurrence of [term], one entry per distinct span. */
        fun findMatches(term: String, ignoreCase: Boolean): List<List<RecognizedText>> {
            var start = text.indexOf(term, ignoreCase = ignoreCase)
            if (start < 0) return emptyList()
            val matches = mutableListOf<List<RecognizedText>>()
            while (start >= 0) {
                val first = wordIndexAt(start)
                // Normalized terms never start with a space, but never box a hit that does.
                if (start < wordStarts[first] + words[first].text.length) {
                    matches += words.subList(first, wordIndexAt(start + term.length - 1) + 1)
                }
                // Words hold no spaces, so each of the term's spaces must fall on a separator, and
                // any later hit starting in the same word covers the same words.
                val next = first + 1
                if (next == words.size) break
                start = text.indexOf(term, startIndex = wordStarts[next], ignoreCase = ignoreCase)
            }
            return matches
        }

        /** The index of the word containing [offset], or of the word before it on a separator. */
        private fun wordIndexAt(offset: Int): Int {
            val index = wordStarts.binarySearch(offset)
            return if (index >= 0) index else -index - 2
        }
    }

    // Collapse before trimming: on Android \s also matches characters trim() keeps (e.g. U+0085),
    // so trimming first could leave a lone separator as the term.
    private fun String.normalizeWhitespace() = replace(WHITESPACE, " ").trim()

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
