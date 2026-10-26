package com.ascolith.textseek.feature.camerascreen.ui.matchextractor

import javax.inject.Inject

/**
 * Joins words with a space, except next to a CJK character. Chinese and Japanese don't space
 * their words, but ML Kit still splits their runs into several elements, so a space there would
 * keep a term like 東京駅 from matching.
 */
class WordSeparator @Inject constructor() : IWordSeparator {

    override fun between(before: String, after: String): String {
        if (before.isEmpty() || after.isEmpty()) return " "
        val touchesCjk = isCjk(before.codePointBefore(before.length)) || isCjk(after.codePointAt(0))
        return if (touchesCjk) "" else " "
    }

    private fun isCjk(codePoint: Int): Boolean {
        val script = Character.UnicodeScript.of(codePoint)
        // Marks like ー, 〜 and fullwidth digits belong to no CJK script, so go by their block.
        // Korean spaces its words, so the Hangul that shares those blocks still doesn't count.
        return script in CJK_SCRIPTS ||
            script != Character.UnicodeScript.HANGUL && Character.UnicodeBlock.of(codePoint) in CJK_BLOCKS
    }

    private companion object {
        val CJK_SCRIPTS = setOf(
            Character.UnicodeScript.HAN,
            Character.UnicodeScript.HIRAGANA,
            Character.UnicodeScript.KATAKANA,
            Character.UnicodeScript.BOPOMOFO,
        )

        val CJK_BLOCKS = setOf(
            Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION,
            Character.UnicodeBlock.HIRAGANA,
            Character.UnicodeBlock.KATAKANA,
            Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS,
            Character.UnicodeBlock.CJK_COMPATIBILITY,
            Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS,
        )
    }
}
