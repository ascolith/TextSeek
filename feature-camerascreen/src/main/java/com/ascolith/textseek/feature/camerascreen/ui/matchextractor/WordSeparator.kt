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

    private fun isCjk(codePoint: Int) = Character.UnicodeScript.of(codePoint) in CJK_SCRIPTS

    private companion object {
        val CJK_SCRIPTS = setOf(
            Character.UnicodeScript.HAN,
            Character.UnicodeScript.HIRAGANA,
            Character.UnicodeScript.KATAKANA,
            Character.UnicodeScript.BOPOMOFO,
        )
    }
}
