package com.ascolith.textseek.core.data.backup

import com.ascolith.textseek.core.common.di.Dispatcher
import com.ascolith.textseek.core.common.di.TsDispatcher
import com.ascolith.textseek.core.data.backup.model.BackupData
import com.ascolith.textseek.core.data.settings.IAppSettingsRepository
import com.ascolith.textseek.core.database.dao.SearchGroupDao
import com.ascolith.textseek.core.database.dao.SearchSettingDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Gathers everything currently stored into the backup model, ready to be serialized.
 */
interface IBackupCollector {
    suspend fun collectBackup(): BackupData
}

class BackupCollector @Inject constructor(
    private val searchGroupDao: SearchGroupDao,
    private val searchSettingDao: SearchSettingDao,
    private val appSettingsRepository: IAppSettingsRepository,
    @param:Dispatcher(TsDispatcher.IO) private val ioDispatcher: CoroutineContext,
) : IBackupCollector {

    override suspend fun collectBackup(): BackupData = withContext(ioDispatcher) {
        BackupData(
            searchGroups = searchGroupDao.getSearchGroups().first().map { it.toBackup() },
            searchSettings = searchSettingDao.getSearchSettings().first().map { it.toBackup() },
            appSettings = appSettingsRepository.exportAll(),
        )
    }
}
