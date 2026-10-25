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

        // Recognized texts are single words, so a term that spans words can only match once each
        // line's words are rejoined. Only build the joined lines if such a term is active.
        val lines by lazy(LazyThreadSafetyMode.NONE) { recognizedLines.map { SearchableLine(it.words) } }
        return terms.flatMap { term ->
            if (term.canSpanWords) {
                lines.flatMap { it.findMatches(term) }
            } else {
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

    private class SearchTerm(val text: String, val ignoreCase: Boolean, val colorArgb: Int) {
        /**
         * Whether a hit can cross from one word into the next: only across a space, or across an
         * empty separator, which always borders a CJK character.
         */
        val canSpanWords = WORD_SEPARATOR in text || text.codePoints().anyMatch(::isCjk)
    }

    private class SearchableLine(val words: List<RecognizedText>) {
        private val text: String
        private val wordStarts = IntArray(words.size)

        /** Whether every pair of words is joined by a space, which lets the scan skip whole words. */
        private val spaceJoined: Boolean

        init {
            val builder = StringBuilder()
            var allSpaced = true
            words.forEachIndexed { index, word ->
                if (index > 0) {
                    val separator = separatorBetween(words[index - 1].text, word.text)
                    if (separator.isEmpty()) allSpaced = false
                    builder.append(separator)
                }
                wordStarts[index] = builder.length
                builder.append(word.text)
            }
            text = builder.toString()
            spaceJoined = allSpaced
        }

        /** Returns a match for each occurrence of [term], one per distinct span of words. */
        fun findMatches(term: SearchTerm): List<TextMatchUiModel> {
            var start = text.indexOf(term.text, ignoreCase = term.ignoreCase)
            if (start < 0) return emptyList()
            val matches = mutableListOf<TextMatchUiModel>()
            var previousSpan = IntRange.EMPTY
            while (start >= 0) {
                val first = wordIndexAt(start)
                // Normalized terms never start with a space, but never box a hit that does.
                if (start < wordEnd(first)) {
                    val span = first..wordIndexAt(start + term.text.length - 1)
                    // Starts only increase, so a repeated span always follows its first hit.
                    if (span != previousSpan) {
                        matches += TextMatchFactory.createTextMatch(
                            text = text.substring(wordStarts[span.first], wordEnd(span.last)),
                            words = words.subList(span.first, span.last + 1),
                            color = term.colorArgb,
                        )
                        previousSpan = span
                    }
                }
                // Words hold no spaces, so with only spaces between them each of the term's spaces
                // must fall on a separator, and any later hit starting in the same word covers the
                // same words. Without a separator a later start can reach further, so try each one.
                val next = if (spaceJoined) wordStarts.getOrNull(first + 1) ?: break else start + 1
                start = text.indexOf(term.text, startIndex = next, ignoreCase = term.ignoreCase)
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
    // so trimming first could leave a lone separator as the term. The pieces are then rejoined
    // the same way a line's words are, so a term typed with or without spaces between CJK
    // characters matches the same text.
    private fun String.normalizeWhitespace() = replace(WHITESPACE, WORD_SEPARATOR).trim()
        .split(WORD_SEPARATOR)
        .reduce { joined, piece -> joined + separatorBetween(joined, piece) + piece }

    private companion object {
        /** Joins words, and stands in for any whitespace run in a search term, outside CJK text. */
        const val WORD_SEPARATOR = " "
        val WHITESPACE = Regex("\\s+")

        private val CJK_SCRIPTS = setOf(
            Character.UnicodeScript.HAN,
            Character.UnicodeScript.HIRAGANA,
            Character.UnicodeScript.KATAKANA,
            Character.UnicodeScript.BOPOMOFO,
        )

        /**
         * Chinese and Japanese don't put spaces between words, but ML Kit still splits their runs
         * into several elements, so join next to a CJK character with no separator at all.
         */
        fun separatorBetween(before: String, after: String): String =
            if (before.isNotEmpty() && after.isNotEmpty() &&
                (isCjk(before.codePointBefore(before.length)) || isCjk(after.codePointAt(0)))
            ) "" else WORD_SEPARATOR

        fun isCjk(codePoint: Int) = Character.UnicodeScript.of(codePoint) in CJK_SCRIPTS
    }
}
