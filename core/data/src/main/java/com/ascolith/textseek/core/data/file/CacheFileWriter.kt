package com.ascolith.textseek.core.data.file

import android.content.Context
import android.net.Uri
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import co.touchlab.kermit.Logger
import androidx.core.content.FileProvider
import com.ascolith.textseek.core.common.di.Dispatcher
import com.ascolith.textseek.core.common.di.TsDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Writes text into the cache directory and hands back a Uri other apps can read, for share-sheet
 * style flows.
 */
fun interface ICacheFileWriter {
    /** Writes [content] to [fileName] in the cache directory and returns a content Uri for it. */
    suspend fun writeToCache(fileName: String, content: String): Either<Throwable, Uri>
}

class CacheFileWriter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:Dispatcher(TsDispatcher.IO) private val ioDispatcher: CoroutineContext,
) : ICacheFileWriter {

    override suspend fun writeToCache(
        fileName: String,
        content: String,
    ): Either<Throwable, Uri> = withContext(ioDispatcher) {
        try {
            val file = File(context.cacheDir, fileName)
            file.writeText(content)

            FileProvider.getUriForFile(context, "${context.packageName}$FILE_PROVIDER_SUFFIX", file).right()
        } catch (e: IOException) {
            Logger.e(e) { "Failed to write $fileName to the cache directory" }
            e.left()
        } catch (e: IllegalArgumentException) {
            // FileProvider throws this when the file is outside the configured paths.
            Logger.e(e) { "Cache directory is not exposed by the file provider" }
            e.left()
        }
    }

    private companion object {
        const val FILE_PROVIDER_SUFFIX = ".fileprovider"
    }
}
