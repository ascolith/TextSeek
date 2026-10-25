package com.ascolith.textseek.feature.camerascreen.ui.matchextractor

private val CJK_SCRIPTS = setOf(
    Character.UnicodeScript.HAN,
    Character.UnicodeScript.HIRAGANA,
    Character.UnicodeScript.KATAKANA,
    Character.UnicodeScript.BOPOMOFO,
)

/**
 * What goes between two words when they're joined for matching: a space, except next to a CJK
 * character. Chinese and Japanese don't space their words, but ML Kit still splits their runs
 * into several elements, so a space there would keep a term like 東京駅 from matching.
 */
internal fun separatorBetween(before: String, after: String): String {
    if (before.isEmpty() || after.isEmpty()) return " "
    val touchesCjk = isCjk(before.codePointBefore(before.length)) || isCjk(after.codePointAt(0))
    return if (touchesCjk) "" else " "
}

private fun isCjk(codePoint: Int) = Character.UnicodeScript.of(codePoint) in CJK_SCRIPTS
