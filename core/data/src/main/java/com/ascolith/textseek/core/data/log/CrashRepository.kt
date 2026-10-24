package com.ascolith.textseek.core.data.log

import com.ascolith.textseek.core.common.di.Dispatcher
import com.ascolith.textseek.core.common.di.TsDispatcher
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext

interface ICrashRepository {
    /** Whether the app crashed since the user last answered the crash prompt. */
    suspend fun hasPendingCrash(): Boolean

    /** Marks the crash as answered; the crash log itself is kept for the next report. */
    suspend fun clearPendingCrash()
}

class DefaultCrashRepository @Inject constructor(
    private val location: LogFileLocation,
    @param:Dispatcher(TsDispatcher.IO) private val ioDispatcher: CoroutineContext,
) : ICrashRepository {

    override suspend fun hasPendingCrash(): Boolean = withContext(ioDispatcher) {
        location.crashPendingMarker.exists()
    }

    override suspend fun clearPendingCrash() {
        withContext(ioDispatcher) { location.crashPendingMarker.delete() }
    }
}
