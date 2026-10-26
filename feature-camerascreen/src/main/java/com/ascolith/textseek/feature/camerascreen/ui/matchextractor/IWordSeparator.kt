package com.ascolith.textseek.feature.camerascreen.ui.matchextractor

/** Decides what goes between two recognized words when they're joined for matching. */
fun interface IWordSeparator {
    fun between(before: String, after: String): String
}
