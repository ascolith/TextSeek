package com.ascolith.textseek.core.data.file

import android.content.Context
import android.net.Uri
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import co.touchlab.kermit.Logger
import com.ascolith.textseek.core.common.di.Dispatcher
import com.ascolith.textseek.core.common.di.TsDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Reads the text behind a content Uri, for file-picker style flows. The counterpart to
 * [ICacheFileWriter].
 */
fun interface IUriContentReader {
    /** Reads [uri] as text. */
    suspend fun readText(uri: Uri): Either<Throwable, String>
}

class UriContentReader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:Dispatcher(TsDispatcher.IO) private val ioDispatcher: CoroutineContext,
) : IUriContentReader {

    override suspend fun readText(uri: Uri): Either<Throwable, String> = withContext(ioDispatcher) {
        try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: return@withContext noStreamFailure(uri).left()

            stream.bufferedReader().use { it.readText() }.right()
        } catch (e: IOException) {
            // Neither the full Uri nor the exception is logged: both can carry the file's name.
            Logger.e { "Failed to read from ${uri.authority}: ${e::class.simpleName}" }
            e.left()
        }
    }

    private fun noStreamFailure(uri: Uri): Throwable =
        IllegalStateException("Content provider returned no stream for $uri")
            .also { Logger.e { "Failed to read from ${uri.authority}: no stream" } }
}
