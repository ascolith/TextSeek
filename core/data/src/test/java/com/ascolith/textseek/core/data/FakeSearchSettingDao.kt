package com.ascolith.textseek.core.data

import com.ascolith.textseek.core.database.dao.SearchSettingDao
import com.ascolith.textseek.core.database.entity.DbSearchSetting
import com.ascolith.textseek.domain.model.SearchSettingId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Fake implementation of [SearchSettingDao] for testing.
 */
class FakeSearchSettingDao : SearchSettingDao {

    private val data = mutableListOf<DbSearchSetting>()
    private var nextId = 1L

    override fun getSearchSettings(): Flow<List<DbSearchSetting>> {
        return flow { emit(data.sortedByDescending { it.uid }) }
    }

    override suspend fun get(uid: Long): DbSearchSetting? {
        return data.find { it.uid == uid }
    }

    override fun getInGroup(groupId: Long): Flow<List<DbSearchSetting>> {
        return flow { emit(data.filter { it.searchGroupId == groupId }.sortedByDescending { it.uid }) }
    }

    override suspend fun create(item: DbSearchSetting): Long {
        val newItem = if (item.uid == 0L) item.copy(uid = nextId) else item
        nextId = maxOf(nextId, newItem.uid) + 1
        data.add(newItem)
        return newItem.uid
    }

    override suspend fun update(item: DbSearchSetting) {
        val index = data.indexOfFirst { it.uid == item.uid }
        if (index != -1) {
            data[index] = item
        }
    }

    override suspend fun update(id: Long, text: String) {
        val index = data.indexOfFirst { it.uid == id }
        if (index != -1) {
            data[index] = data[index].copy(text = text)
        }
    }

    override suspend fun delete(searchSettingIds: Set<SearchSettingId>) {
        val idsToDelete = searchSettingIds.map { it.value }.toSet()
        data.removeAll { it.uid in idsToDelete }
    }

    override suspend fun delete(id: Long) {
        data.removeAll { it.uid == id }
    }

    override suspend fun deleteAll() {
        data.clear()
    }

    override suspend fun insertAll(items: List<DbSearchSetting>) {
        items.forEach { create(it) }
    }
}
