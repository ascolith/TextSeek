package com.ascolith.textseek.feature.camerascreen.ui.matchextractor

import com.ascolith.textseek.domain.model.RecognizedLine
import com.ascolith.textseek.domain.model.RecognizedText
import com.ascolith.textseek.feature.camerascreen.ui.model.TextMatchFactory
import com.ascolith.textseek.feature.camerascreen.ui.model.TextMatchUiModel

/**
 * A recognized line's words joined back into one string, so a term can match across them, and
 * each hit can be traced back to the words it covers.
 */
internal class SearchableLine(line: RecognizedLine) {
    private val words: List<RecognizedText> = line.words
    private val wordStarts = IntArray(words.size)
    private val text = buildString {
        words.forEachIndexed { index, word ->
            if (index > 0) append(separatorBetween(words[index - 1].text, word.text))
            wordStarts[index] = length
            append(word.text)
        }
    }

    /** Returns a match for each occurrence of [term], one per distinct run of words it covers. */
    fun findMatches(term: SearchTerm): List<TextMatchUiModel> {
        var start = text.indexOf(term.text, ignoreCase = term.ignoreCase)
        if (start < 0) return emptyList()

        val matches = mutableListOf<TextMatchUiModel>()
        var previous = IntRange.EMPTY
        while (start >= 0) {
            val covered = wordIndexAt(start)..wordIndexAt(start + term.text.length - 1)
            // Hits over the same words, like "aa" twice in "aaaa", are one match. Starts only
            // increase, so a repeat always directly follows the hit it repeats.
            if (covered != previous) {
                matches += TextMatchFactory.createTextMatch(
                    text = text.substring(wordStarts[covered.first], wordEnd(covered.last)),
                    words = words.subList(covered.first, covered.last + 1),
                    color = term.colorArgb,
                )
                previous = covered
            }
            start = text.indexOf(term.text, startIndex = start + 1, ignoreCase = term.ignoreCase)
        }
        return matches
    }

    /** The index of the word [offset] falls in, or of the word before it on a separator. */
    private fun wordIndexAt(offset: Int): Int {
        val index = wordStarts.binarySearch(offset)
        // When offset isn't a word's first character, the insertion point is just past its word.
        return if (index >= 0) index else -index - 2
    }

    private fun wordEnd(index: Int) = wordStarts[index] + words[index].text.length
}
