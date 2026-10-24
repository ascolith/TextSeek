package com.ascolith.textseek.core.data.log

import com.ascolith.textseek.domain.feature.time.ITimeProvider
import java.util.Collections
import java.util.IdentityHashMap
import javax.inject.Inject

/** Records uncaught exceptions so the next launch can offer to report them. */
class CrashRecorder @Inject constructor(
    private val location: LogFileLocation,
    private val timeProvider: ITimeProvider,
) {
    fun install() {
        Thread.setDefaultUncaughtExceptionHandler(
            handlerWrapping(Thread.getDefaultUncaughtExceptionHandler()),
        )
    }

    internal fun handlerWrapping(previous: Thread.UncaughtExceptionHandler?) =
        Thread.UncaughtExceptionHandler { thread, throwable ->
            try {
                record(thread.name, throwable)
            } catch (_: Throwable) {
                // Recording is best effort; failing here must not hide the original crash.
            }
            previous?.uncaughtException(thread, throwable)
        }

    internal fun record(threadName: String, throwable: Throwable) {
        location.crashFile.writeText(format(threadName, throwable))
        location.crashPendingMarker.createNewFile()
    }

    // Class names and frames only: exception messages can contain search terms.
    private fun format(threadName: String, throwable: Throwable): String = buildString {
        appendLine("Crashed at ${timeProvider.now()} on thread $threadName")
        val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        var current: Throwable? = throwable
        while (current != null && seen.add(current)) {
            if (current !== throwable) append("Caused by: ")
            appendLine(current.javaClass.name)
            current.stackTrace.forEach { frame -> appendLine("\tat $frame") }
            current = current.cause
        }
    }
}
