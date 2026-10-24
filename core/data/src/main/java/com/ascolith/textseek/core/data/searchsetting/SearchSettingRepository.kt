package com.ascolith.textseek.core.data.searchsetting

import com.ascolith.textseek.core.common.di.Dispatcher
import com.ascolith.textseek.core.common.di.TsDispatcher
import com.ascolith.textseek.core.database.dao.SearchSettingDao
import com.ascolith.textseek.core.database.entity.DbSearchSetting
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSetting
import com.ascolith.textseek.domain.model.SearchSettingId
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface ISearchSettingRepository {
    val searchSettings: Flow<List<SearchSetting>>

    suspend fun create(
        text: String,
        caseSensitive: Boolean,
        color: Int?,
        enabled: Boolean,
        searchGroupId: SearchGroupId?,
    ): Long

    suspend fun get(id: SearchSettingId): SearchSetting?
    suspend fun getInGroup(groupId: SearchGroupId): Flow<List<SearchSetting>>
    suspend fun update(searchSetting: SearchSetting)
    suspend fun update(id: SearchSettingId, text: String)
    suspend fun delete(ids: Set<SearchSettingId>)
    suspend fun delete(id: SearchSettingId)
}

class DefaultSearchSettingRepository @Inject constructor(
    private val searchSettingDao: SearchSettingDao,
    @param:Dispatcher(TsDispatcher.IO) private val ioDispatcher: CoroutineContext,
) : ISearchSettingRepository {

    override val searchSettings: Flow<List<SearchSetting>> =
        searchSettingDao.getSearchSettings()
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
            .map { settings -> settings.map { it.toDomain() } }

    override suspend fun get(id: SearchSettingId): SearchSetting? =
        withContext(ioDispatcher) {
            searchSettingDao.get(id.value)?.toDomain()
        }

    override suspend fun getInGroup(groupId: SearchGroupId): Flow<List<SearchSetting>> =
        searchSettingDao.getInGroup(groupId.value)
            .flowOn(ioDispatcher)
            .map { settings -> settings.map { it.toDomain() } }

    override suspend fun create(
        text: String,
        caseSensitive: Boolean,
        color: Int?,
        enabled: Boolean,
        searchGroupId: SearchGroupId?,
    ): Long {
        val setting = DbSearchSetting(
            text = text,
            caseSensitive = caseSensitive,
            color = color,
            enabled = enabled,
            searchGroupId = searchGroupId?.value,
        )
        return withContext(ioDispatcher) {
            searchSettingDao.create(setting)
        }
    }

    override suspend fun update(searchSetting: SearchSetting) {
        return withContext(ioDispatcher) {
            searchSettingDao.update(searchSetting.toDbModel())
        }
    }

    override suspend fun update(id: SearchSettingId, text: String) {
        withContext(ioDispatcher) {
            searchSettingDao.update(id.value, text)
        }
    }

    override suspend fun delete(ids: Set<SearchSettingId>) {
        withContext(ioDispatcher) {
            searchSettingDao.delete(ids)
        }
    }

    override suspend fun delete(id: SearchSettingId) {
        withContext(ioDispatcher) {
            searchSettingDao.delete(id.value)
        }
    }
}
