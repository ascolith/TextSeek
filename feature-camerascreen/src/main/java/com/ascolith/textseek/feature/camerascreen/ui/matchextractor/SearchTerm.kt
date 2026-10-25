package com.ascolith.textseek.feature.camerascreen.ui.matchextractor

import com.ascolith.textseek.feature.camerascreen.ui.model.SearchSettingUiModel

/** A search setting's text, with its words joined the way a [SearchableLine]'s are. */
internal class SearchTerm(
    val text: String,
    val ignoreCase: Boolean,
    val colorArgb: Int,
) {
    companion object {
        private val WHITESPACE = Regex("\\s+")

        /** Returns null for a disabled setting, or one with nothing to search for. */
        fun from(setting: SearchSettingUiModel, wordSeparator: IWordSeparator): SearchTerm? {
            if (!setting.enabled) return null
            // Drop empty pieces rather than rely on trim(): on Android \s also matches characters
            // trim() keeps, such as U+0085, and a term of only those must still come out empty.
            val words = setting.text.trim().split(WHITESPACE).filter { it.isNotEmpty() }
            if (words.isEmpty()) return null
            // Joined like a line's words, so a term typed with or without spaces between CJK
            // characters matches the same text.
            val text = words.reduce { joined, word -> joined + wordSeparator.between(joined, word) + word }
            return SearchTerm(text, ignoreCase = !setting.caseSensitive, colorArgb = setting.colorArgb)
        }
    }
}
