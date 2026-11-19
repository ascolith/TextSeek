package com.ascolith.textseek.core.data.log

import java.io.File

/**
 * Where the most recent crash is kept.
 */
data class CrashFiles(val directory: File) {
    /** The stack trace of the most recent crash. Replaced by each crash. */
    val crashFile: File get() = File(directory, "crash.log")

    /** Present from a crash until the user has answered the crash prompt. */
    val pendingMarker: File get() = File(directory, "crash.pending")
}
