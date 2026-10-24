package com.ascolith.textseek.core.data.log

import java.io.File

/**
 * Where the rolling log files live. Shared by the writer config and [DefaultLogRepository] so both
 * agree on Kermit's naming: `"$name.log"` is the newest file, `"$name-1.log"` the next oldest, and
 * so on up to `"$name-${maxLogFiles - 1}.log"`.
 */
data class LogFileLocation(
    val directory: File,
    val name: String,
    val maxLogFiles: Int,
) {
    /** The stack trace of the most recent crash. Outside Kermit's naming, so it is never rolled. */
    val crashFile: File get() = File(directory, "crash.log")

    /** Present from a crash until the user has answered the crash prompt. */
    val crashPendingMarker: File get() = File(directory, "crash.pending")

    /** The log files that currently exist, oldest first. */
    fun existingFilesOldestFirst(): List<File> =
        (maxLogFiles - 1 downTo 0)
            .map { index -> File(directory, if (index == 0) "$name.log" else "$name-$index.log") }
            .filter { it.exists() }
}
