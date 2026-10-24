package com.ascolith.textseek.core.data

import com.ascolith.textseek.core.database.dao.SearchGroupDao
import com.ascolith.textseek.core.database.entity.DbSearchGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Fake implementation of [SearchGroupDao] for testing.
 */
class FakeSearchGroupDao : SearchGroupDao {

    private val data = mutableListOf<DbSearchGroup>()
    private var nextId = 1L

    override fun getSearchGroups(): Flow<List<DbSearchGroup>> {
        return flow { emit(data.sortedByDescending { it.uid }) }
    }

    override suspend fun get(uid: Long): DbSearchGroup? {
        return data.find { it.uid == uid }
    }

    override suspend fun create(item: DbSearchGroup): Long {
        val newItem = if (item.uid == 0L) item.copy(uid = nextId) else item
        nextId = maxOf(nextId, newItem.uid) + 1
        data.add(newItem)
        return newItem.uid
    }

    override suspend fun update(item: DbSearchGroup) {
        val index = data.indexOfFirst { it.uid == item.uid }
        if (index != -1) {
            data[index] = item
        }
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        val index = data.indexOfFirst { it.uid == id }
        if (index != -1) {
            data[index] = data[index].copy(enabled = enabled)
        }
    }

    override suspend fun delete(id: Long) {
        data.removeAll { it.uid == id }
    }

    override suspend fun deleteGroupChildren(id: Long) {
        // Children live in FakeSearchSettingDao; nothing to remove here.
    }

    override suspend fun deleteAll() {
        data.clear()
    }

    override suspend fun insertAll(items: List<DbSearchGroup>) {
        items.forEach { create(it) }
    }
}
