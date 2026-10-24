package com.ascolith.textseek.core.data.backup

import android.net.Uri
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import com.ascolith.textseek.core.common.di.Dispatcher
import com.ascolith.textseek.core.common.di.TsDispatcher
import com.ascolith.textseek.core.data.file.ICacheFileWriter
import com.ascolith.textseek.core.data.file.IUriContentReader
import com.ascolith.textseek.domain.feature.time.ITimeProvider
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

interface IBackupRepository {
    suspend fun exportToUri(): Either<Throwable, Uri>

    /** Reads the backup at [uri] and applies it. */
    suspend fun importFromUri(uri: Uri): Either<Throwable, Unit>
}

/**
 * Sequences a backup: collect, serialize, write; and in reverse, decode and apply.
 * Owns the order of those steps, not the rules of any one of them.
 */
class DefaultBackupRepository @Inject constructor(
    private val backupCollector: IBackupCollector,
    private val backupApplier: IBackupApplier,
    private val backupSerializer: IBackupSerializer,
    private val cacheFileWriter: ICacheFileWriter,
    private val uriContentReader: IUriContentReader,
    private val timeProvider: ITimeProvider,
    @param:Dispatcher(TsDispatcher.IO) private val ioDispatcher: CoroutineContext,
) : IBackupRepository {

    override suspend fun exportToUri(): Either<Throwable, Uri> {
        val json = backupSerializer.encode(backupCollector.collectBackup())
            .getOrElse { return it.left() }
        val today = timeProvider.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return cacheFileWriter.writeToCache(backupFileName(today), json)
    }

    override suspend fun importFromUri(uri: Uri): Either<Throwable, Unit> {
        val json = uriContentReader.readText(uri)
            .getOrElse { return it.left() }
        val backupData = withContext(ioDispatcher) { backupSerializer.decode(json) }
            .getOrElse { return it.left() }
        return backupApplier.applyBackup(backupData)
    }

    companion object {
        /** e.g. `txt_seek_backup_2026-09-27.json` */
        fun backupFileName(date: LocalDate): String =
            "txt_seek_backup_$date.json"
    }
}
