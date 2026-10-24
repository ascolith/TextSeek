package com.ascolith.textseek.core.data.log

import co.touchlab.kermit.Logger
import co.touchlab.kermit.io.RollingFileLogWriter
import co.touchlab.kermit.platformLogWriter
import javax.inject.Inject

/** Routes the global Kermit [Logger] to logcat and to the rolling log files. */
class FileLoggingInitializer @Inject constructor(
    private val rollingFileLogWriter: RollingFileLogWriter,
) {
    fun install() {
        Logger.setLogWriters(platformLogWriter(), rollingFileLogWriter)
    }
}
