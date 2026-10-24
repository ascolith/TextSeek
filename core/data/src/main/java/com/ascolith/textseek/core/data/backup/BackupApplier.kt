package com.ascolith.textseek.core.data.backup

import android.database.SQLException
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import co.touchlab.kermit.Logger
import com.ascolith.textseek.core.common.di.Dispatcher
import com.ascolith.textseek.core.common.di.TsDispatcher
import com.ascolith.textseek.core.data.backup.model.BackupData
import com.ascolith.textseek.core.data.settings.IAppSettingsRepository
import com.ascolith.textseek.core.database.dao.SearchGroupDao
import com.ascolith.textseek.core.database.dao.SearchSettingDao
import com.ascolith.textseek.core.database.transaction.ITransactionRunner
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext

interface IBackupApplier {
    /**
     * Replaces everything stored with [backupData]: the database in one transaction, then the app
     * settings in another. The two stores cannot share a transaction, so if the settings fail to
     * write, the database has already been replaced.
     */
    suspend fun applyBackup(backupData: BackupData): Either<Throwable, Unit>
}

class BackupApplier @Inject constructor(
    private val searchGroupDao: SearchGroupDao,
    private val searchSettingDao: SearchSettingDao,
    private val transactionRunner: ITransactionRunner,
    private val appSettingsRepository: IAppSettingsRepository,
    @param:Dispatcher(TsDispatcher.IO) private val ioDispatcher: CoroutineContext,
) : IBackupApplier {

    override suspend fun applyBackup(
        backupData: BackupData,
    ): Either<Throwable, Unit> = withContext(ioDispatcher) {
        replaceDatabase(backupData).flatMap {
            appSettingsRepository.importAll(backupData.appSettings)
        }
    }

    private suspend fun replaceDatabase(backupData: BackupData): Either<Throwable, Unit> =
        try {
            transactionRunner.withTransaction {
                // Delete settings first (they reference groups)
                searchSettingDao.deleteAll()
                searchGroupDao.deleteAll()
                // Insert groups first (settings reference groups)
                searchGroupDao.insertAll(backupData.searchGroups.map { it.toDbModel() })
                searchSettingDao.insertAll(backupData.searchSettings.map { it.toDbModel() })
            }.right()
        } catch (e: SQLException) {
            // The transaction rolls back, so the stored data is untouched.
            Logger.e(e) { "Failed to apply the backup" }
            e.left()
        }
}
