package com.ascolith.textseek.core.data.log

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.ascolith.textseek.core.common.di.Dispatcher
import com.ascolith.textseek.core.common.di.TsDispatcher
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext

interface ILogRepository {
    /** All retained log output, oldest first; empty string if nothing has been logged yet. */
    suspend fun readLogs(): Either<Throwable, String>
}

class DefaultLogRepository @Inject constructor(
    private val location: LogFileLocation,
    @param:Dispatcher(TsDispatcher.IO) private val ioDispatcher: CoroutineContext,
) : ILogRepository {

    // Failures are returned rather than logged: a log line here would be written back into the
    // very files being read.
    override suspend fun readLogs(): Either<Throwable, String> = withContext(ioDispatcher) {
        try {
            val logs = location.existingFilesOldestFirst().joinToString(separator = "") { it.readText() }
            val crash = location.crashFile.takeIf { it.exists() }?.readText()
            (if (crash == null) logs else "$logs$CRASH_SEPARATOR$crash").right()
        } catch (e: IOException) {
            e.left()
        }
    }

    private companion object {
        const val CRASH_SEPARATOR = "--- Last crash ---\n"
    }
}
