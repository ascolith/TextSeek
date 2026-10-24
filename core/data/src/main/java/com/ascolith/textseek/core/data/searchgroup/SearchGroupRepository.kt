package com.ascolith.textseek.core.data.searchgroup

import com.ascolith.textseek.core.common.di.Dispatcher
import com.ascolith.textseek.core.common.di.TsDispatcher
import com.ascolith.textseek.core.database.dao.SearchGroupDao
import com.ascolith.textseek.core.database.entity.DbSearchGroup
import com.ascolith.textseek.domain.model.SearchGroup
import com.ascolith.textseek.domain.model.SearchGroupId
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface ISearchGroupRepository {
    val searchGroups: Flow<List<SearchGroup>>

    suspend fun create(name: String, enabled: Boolean, colorArgb: Int): SearchGroupId
    suspend fun get(id: SearchGroupId): SearchGroup?
    suspend fun setGroupEnabled(id: SearchGroupId, enabled: Boolean)
    suspend fun update(searchGroup: SearchGroup)
    suspend fun deleteGroupAndChildren(id: SearchGroupId)
}

class DefaultSearchGroupRepository @Inject constructor(
    private val searchGroupDao: SearchGroupDao,
    @param:Dispatcher(TsDispatcher.IO) private val ioDispatcher: CoroutineContext,
) : ISearchGroupRepository {

    override val searchGroups: Flow<List<SearchGroup>> =
        searchGroupDao.getSearchGroups()
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
            .map { groups -> groups.map { it.toDomain() } }

    override suspend fun create(name: String, enabled: Boolean, colorArgb: Int): SearchGroupId =
        withContext(ioDispatcher) {
            val createdId = searchGroupDao.create(
                DbSearchGroup(
                    name = name,
                    enabled = enabled,
                    colorArgb = colorArgb,
                )
            )
            SearchGroupId(createdId)
        }

    override suspend fun get(id: SearchGroupId): SearchGroup? =
        withContext(ioDispatcher) {
            searchGroupDao.get(id.value)?.toDomain()
        }

    override suspend fun update(searchGroup: SearchGroup) {
        return withContext(ioDispatcher) {
            searchGroupDao.update(searchGroup.toDbModel())
        }
    }

    override suspend fun setGroupEnabled(id: SearchGroupId, enabled: Boolean) {
        return withContext(ioDispatcher) {
            searchGroupDao.setEnabled(id.value, enabled)
        }
    }

    override suspend fun deleteGroupAndChildren(id: SearchGroupId) {
        withContext(ioDispatcher) {
            searchGroupDao.deleteGroupAndChildren(id.value)
        }
    }
}
